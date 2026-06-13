package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.TrashCollectedEvent;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

public class OceanTrash extends Entity {

	private final Vector2 windDir;
	private final btRigidBody body;
	private final btConvexHullShape shape; // kept for disposal
	private final MotionState motionState; // kept for disposal

	public OceanTrash(Model model, Vector2 position, Vector2 windDir) {
		super(new ModelInstance(model));
		this.windDir = windDir;
		transform.setToTranslation(position.x, .2f, position.y);

		shape = Util.buildConvexHullShape(this.model);
		motionState = new MotionState(transform, shape.getImplicitShapeDimensions().y);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT
				| btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE);
		body.userData = this;
	}

	@Override
	public void update(float delta) {
		float speed = delta;
		transform.translate(windDir.x * speed, 0f, windDir.y * speed);
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void onClicked(EventBus events) {
		events.post(new TrashCollectedEvent(this));
	}

	/**
	 * Disposes the rigid body, the collision shape, and the motion state.
	 * Previously only {@code body} was disposed, leaking the shape and motion
	 * state.
	 */
	@Override
	public void dispose() {
		body.dispose();
		shape.dispose();
		motionState.dispose();
	}
}