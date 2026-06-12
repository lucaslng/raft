package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.item.ItemStack;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

public class OceanTrash extends Entity {

	private final ItemStack itemStack;
	private final Vector2 windDir;
	private final btRigidBody body;

	public OceanTrash(ItemStack itemStack, Vector2 position, Vector2 windDir) {
		super(new ModelInstance(itemStack.item.model));
		this.itemStack = itemStack;
		this.windDir = windDir;
		transform.setToTranslation(position.x, .2f, position.y);

		btConvexHullShape shape = Util.buildConvexHullShape(this.model);
		MotionState motionState = new MotionState(transform, shape.getImplicitShapeDimensions().y);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
		body.userData = this;
	}

	

	@Override
	public void update(float delta) {
		float speed = 1f;
		speed *= delta;
		transform.translate(windDir.x * speed, 0f, windDir.y * speed);
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	public ItemStack getItems() {
		return itemStack;
	}

}
