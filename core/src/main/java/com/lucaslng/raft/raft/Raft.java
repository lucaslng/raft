package com.lucaslng.raft.raft;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.physics.MotionState;

public class Raft implements Disposable {

	private final btRigidBody body;
	private final ModelInstance model;
	
	public Raft(Assets assets) {
		model = new ModelInstance(assets.get("models/platform.g3db", Model.class));
		BoundingBox boundingBox = new BoundingBox();
		model.calculateBoundingBox(boundingBox);

		Vector3 dimensions = new Vector3();
		boundingBox.getDimensions(dimensions);
		dimensions.scl(.5f);

		MotionState motionState = new MotionState(model.transform, dimensions.y);
		btBoxShape boxShape = new btBoxShape(dimensions);

		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, boxShape);
		body = new btRigidBody(info);
		info.dispose();
	}

	public void update(float delta) {

	}

	public ModelInstance getInstance() {
		return model;
	}

	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void dispose() {
		body.dispose();
	}

}
