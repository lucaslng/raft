package com.lucaslng.raft.physics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.entity.Shark;
import com.lucaslng.raft.util.Util;

// Configures the players physics
public class PlayerPhysics implements Disposable {

	private final btRigidBody body;
	private final MotionState motionState;
	private final btCapsuleShape shape;

	public PlayerPhysics(ModelInstance model) {
		Vector3 dims = Util.getDimensions(model);
		float radius = Math.max(dims.x, dims.z);
		float totalHeight = dims.y * 2f;
		float cylinderHeight = Math.max(0.01f, totalHeight - 2f * radius);

		motionState = new MotionState(model.transform, 0f);
		shape = new btCapsuleShape(radius, cylinderHeight);

		float mass = 20f;

		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(mass, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();

		body.setActivationState(Collision.DISABLE_DEACTIVATION);

		body.setContactCallbackFlag(Shark.FLAG);
		body.setContactCallbackFilter(Shark.FLAG);
	}

	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void dispose() {
		body.dispose();
		motionState.dispose();
		shape.dispose();
	}
}