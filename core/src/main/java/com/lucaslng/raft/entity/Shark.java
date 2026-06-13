package com.lucaslng.raft.entity;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

public class Shark extends Entity {

	private final AnimationController animationController;
	private final btRigidBody body;
	private final List<Disposable> disposables;

	public Shark(Model model, Vector3 position) {
		super(new ModelInstance(model, position));
		disposables = new ArrayList<>();

		Util.scaleModelInstance(this.model, 1f);

		Vector3 dimensions = Util.getDimensions(this.model);
		btBoxShape shape = new btBoxShape(dimensions);
		MotionState motionState = new MotionState(transform, 0f);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
		body.userData = this;

		animationController = new AnimationController(this.model);
		animationController.setAnimation("Armature|Swim", -1);

		disposables.add(body);
		disposables.add(shape);
		disposables.add(motionState);
	}

	@Override
	public void update(float delta) {
		animationController.update(delta);
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void onClicked(EventBus events) {
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Disposable d : disposables)
			d.dispose();
	}
}
