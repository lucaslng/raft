package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.TrashCollectedEvent;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;
import com.lucaslng.raft.world.Outlineable;

// A floating piece of trash, doesn't really do much when collected because it is supposed to be inherited
public class OceanTrash extends Entity implements Outlineable {

	private static final float TRASH_SPEED = 1.5f;
	private static final float RANDOMNESS = .5f;

	private final Vector2 windDir;
	private final btRigidBody body;
	private final btConvexHullShape shape; // kept for disposal
	private final MotionState motionState; // kept for disposal
	private final String hint = "[LMB] Collect trash";
	private final Vector2 moveDir = new Vector2();

	public OceanTrash(Model model, Vector2 position, Vector2 windDir, Player player) {
		super(new ModelInstance(model));
		this.windDir = windDir;

		Vector3 myPos = getPosition();
		Vector3 playerPos = player.getPosition();

		moveDir.set(
				playerPos.x - myPos.x,
				playerPos.z - myPos.z).nor();

		// Add angular noise
		float angleOffset = MathUtils.random(-RANDOMNESS, RANDOMNESS);
		moveDir.rotateRad(angleOffset);

		transform.setToTranslation(position.x, .2f, position.y);

		shape = Util.buildConvexHullShape(this.model);
		motionState = new MotionState(transform, shape.getImplicitShapeDimensions().y);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT
				| btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
		body.setActivationState(Collision.DISABLE_DEACTIVATION);
		body.userData = this;
	}

	@Override
	public void update(float delta) {
		transform.translate(windDir.x * delta, 0f, windDir.y * delta);
		transform.translate(moveDir.x * TRASH_SPEED * delta, 0f, moveDir.y * TRASH_SPEED * delta);
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void onClick(EventBus events) {
		events.post(new TrashCollectedEvent(this));
	}

	@Override
	public void dispose() {
		body.dispose();
		shape.dispose();
		motionState.dispose();
	}

	@Override
	public ModelInstance getOutlineInstance() {
		return model;
	}

	@Override
	public String getInteractHint() {
		return hint;
	}
}