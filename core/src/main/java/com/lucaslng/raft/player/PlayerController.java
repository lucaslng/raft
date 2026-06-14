package com.lucaslng.raft.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.lucaslng.raft.entity.Player;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.HotbarIndexEvent;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.settings.Settings;

// Handles player's first person controls
public class PlayerController extends InputAdapter {

	private static final float EYE_HEIGHT = 0.85f;
	private static final float MAX_PITCH = 89f; // prevent flipping camera around
	public static final float WALK_SPEED = 5f;
	private static final float JUMP_IMPULSE = 80f; // jump strength
	private static final float GROUNDED_VY_THRESHOLD = 0.6f; // vy threshold for jumping
	private static final float RAFT_ATTACH_RADIUS = 8f; // radius of being in the raft before it leaves you

	private static final float WATER_LINEAR_DAMPING = 0.5f;
	private static final float WATER_ANGULAR_DAMPING = 0.95f;
	private static final float AIR_LINEAR_DAMPING = 0.4f;
	private static final float AIR_ANGULAR_DAMPING = 0.0f;
	private static final float GROUND_DAMPING = 0.0f;

	private final PerspectiveCamera camera;
	private final Player player;
	private final btRigidBody body;
	private final RaftSystem raftSystem;
	private final Settings settings;

	private float yawDeg = 0f;
	private float pitchDeg = 0f;

	// temporary vectors reused per frame
	private final Vector3 _forward = new Vector3();
	private final Vector3 _right = new Vector3();
	private final Vector3 _move = new Vector3();
	private final Vector3 _vel = new Vector3();
	private final Vector3 _pos = new Vector3();
	private final Vector3 _impulse = new Vector3();
	private final Vector2 _raftTileCheck = new Vector2();

	private boolean mouseLookActive = true;

	public PlayerController(PerspectiveCamera camera, Player player, RaftSystem raftSystem) {
		this.camera = camera;
		this.player = player;
		this.body = player.getBody();
		this.raftSystem = raftSystem;
		this.settings = Settings.get();

		body.setAngularFactor(Vector3.Zero);
		float yawRad = yawDeg * MathUtils.degreesToRadians;
		float pitchRad = pitchDeg * MathUtils.degreesToRadians;
		float cosPitch = MathUtils.cos(pitchRad);
		camera.direction.set(
				cosPitch * MathUtils.sin(yawRad),
				MathUtils.sin(pitchRad),
				cosPitch * MathUtils.cos(yawRad)).nor();
	}

	public void update(float delta) {
		applyMouseLook();
		applyMovement(delta);
		player.getPosition(_pos);
		camera.position.set(_pos.x, _pos.y + EYE_HEIGHT, _pos.z);
		camera.update();
	}

	// Apply mouse movement to camera direction
	private void applyMouseLook() {
		if (!mouseLookActive)
			return;

		yawDeg -= Gdx.input.getDeltaX() * settings.mouseSensitivity;
		pitchDeg -= Gdx.input.getDeltaY() * settings.mouseSensitivity;
		pitchDeg = MathUtils.clamp(pitchDeg, -MAX_PITCH, MAX_PITCH);
		float yawRad = yawDeg * MathUtils.degreesToRadians;
		float pitchRad = pitchDeg * MathUtils.degreesToRadians;
		float cosPitch = MathUtils.cos(pitchRad);
		camera.direction.set(
				cosPitch * MathUtils.sin(yawRad),
				MathUtils.sin(pitchRad),
				cosPitch * MathUtils.cos(yawRad)).nor();
	}

	// apply wasd movement
	private void applyMovement(float delta) {
		_vel.set(body.getLinearVelocity());
		player.getPosition(_pos);

		// check if feet are below water level
		boolean inWater = _pos.y < 0f;
		boolean grounded = Math.abs(_vel.y) < GROUNDED_VY_THRESHOLD;

		// damping in water and air
		if (inWater) {
			body.setDamping(WATER_LINEAR_DAMPING, WATER_ANGULAR_DAMPING);
		} else if (grounded) {
			body.setDamping(GROUND_DAMPING, 0f);
		} else {
			body.setDamping(AIR_LINEAR_DAMPING, AIR_ANGULAR_DAMPING);
		}

		float yawRad = yawDeg * MathUtils.degreesToRadians;
		float sinYaw = MathUtils.sin(yawRad);
		float cosYaw = MathUtils.cos(yawRad);
		_forward.set(sinYaw, 0f, cosYaw);
		_right.set(-cosYaw, 0f, sinYaw);

		_move.setZero();
		if (settings.moveForward.isPressed())
			_move.add(_forward);
		if (settings.moveBack.isPressed())
			_move.sub(_forward);
		if (settings.moveRight.isPressed())
			_move.add(_right);
		if (settings.moveLeft.isPressed())
			_move.sub(_right);

		boolean moving = _move.len2() > 0.0001f;
		if (moving) {
			_move.nor();
			player.setRotation(_forward, Vector3.Y);
		}

		// water movement, apply buoyancy and even more buoyancy when space bar pressed
		if (inWater) {
			if (moving) {
				body.applyCentralForce(_impulse.set(_move.x * 40f, 0f, _move.z * 40f));
			}
			if (isNearRaft()) {
				computeRaftVelocity(_move);
				body.applyCentralForce(_impulse.set(_move.x * 20f, 0f, _move.z * 20f));
			}
			float depth = Math.max(0f, -_pos.y);
			float buoyancy = 80f + depth * 20f;
			if (settings.jump.isPressed()) {
				buoyancy += 170f;
			}
			body.applyCentralForce(_impulse.set(0f, buoyancy, 0f));
			return;
		}

		// land/air movement, account for raft movement
		float desiredVx = _move.x * WALK_SPEED;
		float desiredVz = _move.z * WALK_SPEED;

		if (isNearRaft()) {
			computeRaftVelocity(_forward);
			desiredVx += _forward.x;
			desiredVz += _forward.z;
		}

		float accel = grounded ? 15f : 3f;
		float vx = MathUtils.lerp(_vel.x, desiredVx, delta * accel);
		float vz = MathUtils.lerp(_vel.z, desiredVz, delta * accel);
		_vel.set(vx, _vel.y, vz);
		body.setLinearVelocity(_vel);

		if (grounded && settings.jump.isKeyJustPressed()) {
			body.applyCentralImpulse(_impulse.set(0f, JUMP_IMPULSE, 0f));
		}
	}

	// if the player is near the raft, we have to move with it
	private boolean isNearRaft() {
		player.getPosition(_pos);
		Vector2 raftPos = raftSystem.getRaftPosition();
		float localX = _pos.x - raftPos.x;
		float localZ = _pos.z - raftPos.y;

		if (Math.abs(localX) <= RAFT_ATTACH_RADIUS && Math.abs(localZ) <= RAFT_ATTACH_RADIUS) {
			return true;
		}
		_raftTileCheck.set(Math.round(localX), Math.round(localZ));
		return raftSystem.hasTile(_raftTileCheck);
	}

	private void computeRaftVelocity(Vector3 out) {
		float mult = raftSystem.getSailMultiplier();
		Vector2 dir = raftSystem.getSailDirection();
		if (mult < 0.0001f || dir.len2() < 0.0001f) {
			out.setZero();
		} else {
			out.set(dir.x * mult, 0f, dir.y * mult);
		}
	}

	public void setMouseLookActive(boolean active) {
		mouseLookActive = active;
	}

	public boolean isMouseLookActive() {
		return mouseLookActive;
	}

	// Other misc controls
	@Override
	public boolean keyDown(int keycode) {
		for (int i = 0; i < settings.hotbar.length; i++) {
			if (keycode == settings.hotbar[i].getKey()) {
				EventBus.get().post(new HotbarIndexEvent(i));
				return true;
			}
		}
		return false;
	}

	public PerspectiveCamera getCamera() {
		return camera;
	}

	public float getYawDeg() {
		return yawDeg;
	}
}