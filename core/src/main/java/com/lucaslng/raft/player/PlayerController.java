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
import com.lucaslng.raft.event.events.ToggleInventoryEvent;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.settings.Settings;
import com.lucaslng.raft.world.SwimmingSystem;

/**
 * First-person player controller.
 *
 * <h3>Design</h3>
 * <ul>
 * <li>Owns the {@link PerspectiveCamera} — positioned at eye height above the
 * player physics body each frame.</li>
 * <li>Mouse delta drives yaw (horizontal) and pitch (vertical) with clamped
 * pitch so the camera never flips upside-down.</li>
 * <li>WASD maps to horizontal movement in camera-facing XZ plane.
 * Sprint doubles walk speed.</li>
 * <li>Space bar jumps when the player is grounded. The controller detects
 * "grounded" by checking whether the physics body's Y velocity is near
 * zero (and the player is above the water surface).</li>
 * <li><b>Raft-relative movement</b>: when the player is standing on the raft
 * every movement impulse is applied in world space, but the raft's own
 * velocity is added so the player drifts with it for free. More
 * precisely: each frame the raft's linear velocity is added to the
 * desired velocity before setting it on the body. This keeps the player
 * standing still on a moving raft without any explicit "glue" logic.</li>
 * <li><b>Off-raft</b>: the raft is not tracked once the player leaves it,
 * so the raft keeps drifting away naturally.</li>
 * <li>Swimming is delegated to {@link SwimmingSystem}.</li>
 * <li>The player's 3-D model is kept but its rendering is skipped in
 * {@code GameRenderer} because the camera is inside it (first-person).
 * The model is still used for physics collision.</li>
 * </ul>
 *
 * <h3>Raft-on-raft detection</h3>
 * A simple radius check is used: if the player's XZ position is within
 * {@link #RAFT_ATTACH_RADIUS} world units of any raft tile's world centre the
 * player is considered "on the raft". This is cheap, framerate-independent,
 * and avoids a second raycast per frame.
 */
public class PlayerController extends InputAdapter {

	// ── Constants ─────────────────────────────────────────────────────────────

	/** Camera vertical offset above physics body origin. */
	private static final float EYE_HEIGHT = 0.85f;

	/** Maximum pitch in degrees (looking up). */
	private static final float MAX_PITCH = 89f;

	/** Vertical impulse applied on jump. */
	private static final float JUMP_IMPULSE = 7f;

	/** Y-velocity threshold for grounded detection. */
	private static final float GROUNDED_VY_THRESHOLD = 0.5f;

	/**
	 * XZ radius within which the player is considered "on the raft" and will
	 * inherit the raft's drift velocity.
	 */
	private static final float RAFT_ATTACH_RADIUS = 4f;

	/** Angular damping kept constant (prevents the capsule from rolling). */
	private static final float ANGULAR_DAMPING = 0.99f;

	/** Linear air damping (controls how quickly horizontal speed bleeds off). */
	private static final float AIR_LINEAR_DAMPING = 3.5f;

	// ── Core refs ─────────────────────────────────────────────────────────────

	private final PerspectiveCamera camera;
	private final Player player;
	private final btRigidBody body;
	private final SwimmingSystem swimming;
	private final RaftSystem raftSystem;
	private final EventBus events;
	private final Settings settings;

	// ── Camera state ──────────────────────────────────────────────────────────

	/** Accumulated yaw in degrees (horizontal rotation). */
	private float yawDeg = 0f;
	/** Accumulated pitch in degrees (vertical rotation, clamped). */
	private float pitchDeg = 0f;

	// ── Scratch vectors — never stored across frames ───────────────────────────

	private final Vector3 _forward = new Vector3();
	private final Vector3 _right = new Vector3();
	private final Vector3 _move = new Vector3();
	private final Vector3 _velocity = new Vector3();
	private final Vector3 _pos = new Vector3();
	private final Vector3 _raftVel = new Vector3();
	private final Vector3 _setVel = new Vector3(); // reused for setLinearVelocity
	private final Vector3 _impulse = new Vector3(); // reused for applyCentralImpulse

	// ── State ─────────────────────────────────────────────────────────────────

	/** Whether mouse look is active (cursor caught). */
	private boolean mouseLookActive = true;


	public static final float SPRINT_SPEED = 10f;
	public static final float WALK_SPEED = 5f;

	// ── Constructor ───────────────────────────────────────────────────────────

	public PlayerController(PerspectiveCamera camera, Player player,
			SwimmingSystem swimming, RaftSystem raftSystem) {
		this.camera = camera;
		this.player = player;
		this.body = player.getBody();
		this.swimming = swimming;
		this.raftSystem = raftSystem;
		this.events = EventBus.get();
		this.settings = Settings.get();

		// Prevent the player physics body from toppling over.
		body.setAngularFactor(new Vector3(0f, 0f, 0f));
		body.setDamping(AIR_LINEAR_DAMPING, ANGULAR_DAMPING);

		// Snap camera direction to match initial yaw/pitch = 0.
		updateCameraDirection();
	}

	// ── Per-frame update ──────────────────────────────────────────────────────

	/**
	 * Must be called every frame from the game screen's render loop, BEFORE
	 * {@code world.update()}.
	 *
	 * @param delta frame delta in seconds
	 */
	public void update(float delta) {
		applyMouseLook();
		applyMovement(delta);
		positionCamera();
		camera.update();
	}

	// ── Mouse look ────────────────────────────────────────────────────────────

	/**
	 * Reads raw mouse delta from libGDX and rotates the camera.
	 * libGDX only provides reliable deltas when the cursor is caught.
	 */
	private void applyMouseLook() {
		if (!mouseLookActive)
			return;

		float dx = Gdx.input.getDeltaX();
		float dy = Gdx.input.getDeltaY();

		yawDeg -= dx * settings.mouseSensitivity;
		pitchDeg -= dy * settings.mouseSensitivity; // dy is screen-down → negate

		pitchDeg = MathUtils.clamp(pitchDeg, -MAX_PITCH, MAX_PITCH);

		updateCameraDirection();
	}

	/**
	 * Rebuilds {@code camera.direction} and {@code camera.up} from the stored
	 * {@link #yawDeg} and {@link #pitchDeg}.
	 *
	 * <p>
	 * Uses standard first-person spherical-to-Cartesian:
	 * 
	 * <pre>
	 *   forward.x = cos(pitch) * sin(yaw)
	 *   forward.y = sin(pitch)
	 *   forward.z = cos(pitch) * cos(yaw)
	 * </pre>
	 */
	private void updateCameraDirection() {
		float yawRad = yawDeg * MathUtils.degreesToRadians;
		float pitchRad = pitchDeg * MathUtils.degreesToRadians;

		float cosPitch = MathUtils.cos(pitchRad);

		camera.direction.set(
				cosPitch * MathUtils.sin(yawRad),
				MathUtils.sin(pitchRad),
				cosPitch * MathUtils.cos(yawRad)).nor();

		// Maintain a stable up vector by computing the right vector first,
		// then up = right × forward. Avoids gimbal flip near ±90 °.
		_right.set(camera.direction).crs(Vector3.Y).nor();
		camera.up.set(_right).crs(camera.direction).nor();
	}

	// ── Movement ──────────────────────────────────────────────────────────────

	private void applyMovement(float delta) {
		_velocity.set(player.getBody().getLinearVelocity());
		boolean submerged = swimming.isSubmerged();

		// ── Horizontal move direction (camera-facing XZ plane) ────────────
		_forward.set(camera.direction.x, 0f, camera.direction.z).nor();
		_right.set(camera.direction).crs(Vector3.Y).nor();
		_move.setZero();

		if (Gdx.input.isKeyPressed(settings.moveForward.getKey()))
			_move.add(_forward);
		if (Gdx.input.isKeyPressed(settings.moveBack.getKey()))
			_move.sub(_forward);
		if (Gdx.input.isKeyPressed(settings.moveRight.getKey()))
			_move.add(_right);
		if (Gdx.input.isKeyPressed(settings.moveLeft.getKey()))
			_move.sub(_right);

		boolean moving = _move.len2() > 0.001f;
		if (moving)
			_move.nor();

		float speed = settings.sprint.isPressed()
				? SPRINT_SPEED
				: WALK_SPEED;

		// ── Desired horizontal velocity ───────────────────────────────────
		float desiredVx = _move.x * speed;
		float desiredVz = _move.z * speed;

		// ── Raft velocity inheritance ──────────────────────────────────────
		// If the player is on or near the raft, add the raft's drift velocity
		// so the player moves with it naturally.
		if (isNearRaft()) {
			getRaftVelocity(_raftVel);
			desiredVx += _raftVel.x;
			desiredVz += _raftVel.z;
		}

		// ── Rotate player model to face movement direction ─────────────────
		if (moving) {
			player.setRotation(_forward, Vector3.Y);
		}

		// ── Apply horizontal velocity (preserve physics vertical) ──────────
		float vy = _velocity.y;
		// For both walking and swimming: set horizontal components, preserve vertical.
		// SwimmingSystem handles vertical forces (buoyancy) separately.
		body.setLinearVelocity(_setVel.set(desiredVx, vy, desiredVz));

		// ── Jump ──────────────────────────────────────────────────────────
		boolean grounded = !submerged && Math.abs(vy) < GROUNDED_VY_THRESHOLD;
		if (grounded && settings.jump.isKeyJustPressed()) {
			body.applyCentralImpulse(_impulse.set(0f, JUMP_IMPULSE, 0f));
		}

		// ── Swimming vertical input ────────────────────────────────────────
		// SwimmingSystem.update() is called from World.update() — it reads
		// camera.direction for swim steering, which we've already set above.
	}

	// ── Raft proximity ────────────────────────────────────────────────────────

	/**
	 * Returns {@code true} when the player is within {@link #RAFT_ATTACH_RADIUS}
	 * world units of any raft tile centre, meaning they are standing on or
	 * jumping above the raft.
	 *
	 * <p>
	 * Iterating tiles (rather than comparing against the raft origin) handles
	 * a large raft correctly — the origin stays at (0,0) while new tiles extend
	 * outward from it.
	 * </p>
	 */
	private boolean isNearRaft() {
		player.getPosition(_pos);
		Vector2 raftPos = raftSystem.getRaftPosition();
		// Each tile's world position = raftOrigin + tile.coord.
		// Rather than iterating all tiles (potentially expensive for large rafts),
		// we test the bounding square: if the player is within ATTACH_RADIUS of the
		// nearest raft-grid integer point derived from their local position.
		float localX = _pos.x - raftPos.x;
		float localZ = _pos.z - raftPos.y; // Vector2.y maps to world Z
		// Round to nearest tile coord.
		float nearX = Math.round(localX);
		float nearZ = Math.round(localZ);
		float dx = localX - nearX;
		float dz = localZ - nearZ;
		// Is that snapped tile actually on the raft?
		com.badlogic.gdx.math.Vector2 tileCoord = new com.badlogic.gdx.math.Vector2(nearX, nearZ);
		if (raftSystem.hasTile(tileCoord)) {
			return dx * dx + dz * dz < RAFT_ATTACH_RADIUS * RAFT_ATTACH_RADIUS;
		}
		// Fallback: check straight distance to raft origin for the initial single tile.
		float ox = _pos.x - raftPos.x;
		float oz = _pos.z - raftPos.y;
		return ox * ox + oz * oz < RAFT_ATTACH_RADIUS * RAFT_ATTACH_RADIUS;
	}

	/**
	 * Approximates the raft's per-frame velocity from its sail multiplier and
	 * direction, writing the result into {@code out}.
	 *
	 * <p>
	 * We reconstruct velocity from the raft's drift parameters rather than
	 * differencing positions, which avoids one-frame lag.
	 * </p>
	 */
	private void getRaftVelocity(Vector3 out) {
		float multiplier = raftSystem.getSailMultiplier();
		if (multiplier < 0.0001f) {
			out.setZero();
			return;
		}
		Vector2 dir = raftSystem.getSailDirection();
		if (dir.len2() < 0.0001f) {
			out.setZero();
			return;
		}
		out.set(dir.x * multiplier, 0f, dir.y * multiplier);
	}

	// ── Camera positioning ────────────────────────────────────────────────────

	/** Places the camera at the player's eye position each frame. */
	private void positionCamera() {
		player.getPosition(_pos);
		camera.position.set(_pos.x, _pos.y + EYE_HEIGHT, _pos.z);
	}

	// ── Mouse look toggle (called when panel/inventory opens) ─────────────────

	/** Enable or disable mouse-look. Normally toggled when a UI panel opens. */
	public void setMouseLookActive(boolean active) {
		this.mouseLookActive = active;
	}

	public boolean isMouseLookActive() {
		return mouseLookActive;
	}

	// ── InputAdapter ─────────────────────────────────────────────────────────

	@Override
	public boolean keyDown(int keycode) {
		Settings s = settings;

		// Inventory toggle
		if (keycode == s.toggleInventory.getKey()) {
			events.post(new ToggleInventoryEvent());
			// toggleInventory flips mouseLook internally via PanelOpenedEvent
			return true;
		}

		// Hotbar slots 1–8
		for (int i = 0; i < s.hotbar.length; i++) {
			if (keycode == s.hotbar[i].getKey()) {
				events.post(new HotbarIndexEvent(i));
				return true;
			}
		}

		return false;
	}

	// ── Accessors ─────────────────────────────────────────────────────────────

	/** The first-person camera owned by this controller. */
	public PerspectiveCamera getCamera() {
		return camera;
	}

	/** Current yaw in degrees (horizontal facing). */
	public float getYawDeg() {
		return yawDeg;
	}
}