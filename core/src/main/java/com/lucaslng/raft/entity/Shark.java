package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

public class Shark extends Entity {

	private final AnimationController animationController;
	private final btRigidBody body;

	public Shark(Model model, Vector3 position) {
		super(new ModelInstance(model, position));

		Util.scaleModelInstance(this.model, 1f);
		Vector3 center = Util.centerModelInstance(this.model);

		Vector3 dimensions = Util.getDimensions(this.model);
		btBoxShape shape = new btBoxShape(dimensions);
		btCompoundShape compound = new btCompoundShape();
		Matrix4 childTransform = new Matrix4().setToTranslation(center);
		compound.addChildShape(childTransform, shape);
		MotionState motionState = new MotionState(transform, 0f);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, compound);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
		body.userData = this;

		animationController = new AnimationController(this.model);
		animationController.setAnimation("Armature|Swim", -1);

	}

	@Override
	public void update(float delta) {
		animationController.update(delta);
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}
}
