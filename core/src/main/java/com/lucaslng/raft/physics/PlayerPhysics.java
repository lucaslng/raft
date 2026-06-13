package com.lucaslng.raft.physics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.util.Util;

public class PlayerPhysics implements Disposable {

	private final btRigidBody body;
	private final MotionState motionState;
	private final btBoxShape shape;

	public PlayerPhysics(ModelInstance model) {
		Vector3 dimensions = Util.getDimensions(model);
		 motionState = new MotionState(model.transform, dimensions.y);
		 shape = new btBoxShape(dimensions);
		float mass = 2f;
		Vector3 inertia = new Vector3();
		shape.calculateLocalInertia(mass, inertia);

		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(mass, motionState, shape, inertia);
		body = new btRigidBody(info);
		info.dispose();
		body.setActivationState(Collision.DISABLE_DEACTIVATION);
		

	}

	public void getWorldTransform() {
		
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
