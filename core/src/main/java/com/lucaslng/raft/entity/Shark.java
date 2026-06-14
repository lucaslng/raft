package com.lucaslng.raft.entity;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

// Shark object which attacks the player
// When player is on raft, the shark circles the raft
// When player gets off raft, the shark swims towards player and attacks it
public class Shark extends Entity {


	private static final float LURK_DEPTH = -4f;
	private static final float LURK_RADIUS = 6f;
	private static final float LURK_SPEED = 0.4f;
	private static final float CHASE_SPEED = 6f;
	private static final float CHASE_Y_TRIGGER = -0.3f; // player height that shark starts chasing
	private static final float RETURN_Y_TRIGGER = 0.5f; // player height that shark stops chasing
	
	public static final float DAMAGE = -0.15f;
	public static final float DAMAGE_COOLDOWN = 0.5f;

	// bit flag for Bullet contact system
	public static final int FLAG = 1 << 2;

	private enum State {
		LURKING, CHASING
	}

	private State state = State.LURKING;

	private float lurkAngle = 0f;

	private float headingDeg = 0f;

	private final Vector3 position = new Vector3();

	// tracks raft
	private final Vector3 lurkCenter = new Vector3();

	private final AnimationController animationController;
	private final btRigidBody body;
	private final List<Disposable> disposables;

	private final Vector3 _scratch = new Vector3();


	public Shark(Model model, Vector3 spawnPosition) {
		super(new ModelInstance(model));
		disposables = new ArrayList<>();

		Util.scaleModelInstance(this.model, 1f);

		position.set(spawnPosition);
		lurkCenter.set(spawnPosition.x, LURK_DEPTH, spawnPosition.z);

		// Kinematic because the shark is not driven by physics engine
		Vector3 dimensions = Util.getDimensions(this.model);
		btBoxShape shape = new btBoxShape(dimensions);
		MotionState motionState = new MotionState(transform, 0f);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();

		body.setCollisionFlags(
				body.getCollisionFlags()
						| btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT
						| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		body.setActivationState(Collision.DISABLE_DEACTIVATION);

		body.setContactCallbackFlag(FLAG);
		body.setContactCallbackFilter(FLAG);

		body.userData = this;

		animationController = new AnimationController(this.model);
		animationController.setAnimation("Armature|Swim", -1); // loop animation forever

		disposables.add(body);
		disposables.add(shape);
		disposables.add(motionState);

		applyTransform();
	}

	public void update(float delta, Vector3 playerPos, Vector2 raftOrigin) {
		animationController.update(delta);

		// track raft
		lurkCenter.set(raftOrigin.x, LURK_DEPTH, raftOrigin.y);

		// set state
		if (state == State.LURKING && playerPos.y < CHASE_Y_TRIGGER) {
			state = State.CHASING;
		} else if (state == State.CHASING && playerPos.y > RETURN_Y_TRIGGER) {
			state = State.LURKING;
		}

		switch (state) {
			case LURKING:
				lurkMovement(delta);
				break;
			case CHASING:
				chaseMovement(delta, playerPos);
				break;
		}

		applyTransform();
	}

	// empty because there is a seperate update method for Shark due to its complexity
	@Override
	public void update(float delta) {
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	// Maybe in the future you could hit the shark!
	@Override
	public void onClick(EventBus events) {
	}

	@Override
	public String getInteractHint() {
		return "Get away!";
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Disposable d : disposables)
			d.dispose();
	}

	// circle around lurkCenter
	private void lurkMovement(float delta) {
		lurkAngle += LURK_SPEED * delta;

		float nx = lurkCenter.x + MathUtils.cos(lurkAngle) * LURK_RADIUS;
		float nz = lurkCenter.z + MathUtils.sin(lurkAngle) * LURK_RADIUS;

		float dx = -MathUtils.sin(lurkAngle);
		float dz = MathUtils.cos(lurkAngle);
		headingDeg = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;

		position.set(nx, LURK_DEPTH, nz);
	}

	// chase player
	private void chaseMovement(float delta, Vector3 playerPos) {
		float targetY = Math.min(playerPos.y - 0.5f, -0.5f);

		_scratch.set(playerPos.x, targetY, playerPos.z).sub(position);

		// stop when close
		float dist = _scratch.len();
		if (dist < 0.1f)
			return;

		_scratch.scl(1f / dist); // normalise

		headingDeg = MathUtils.atan2(_scratch.x, _scratch.z) * MathUtils.radiansToDegrees;

		float step = Math.min(CHASE_SPEED * delta, dist);
		position.x += _scratch.x * step;
		position.y += _scratch.y * step;
		position.z += _scratch.z * step;
	}
	
	private void applyTransform() {
		transform.setToTranslation(position).rotate(Vector3.Y, headingDeg);
	}
}