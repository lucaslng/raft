package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.WaterFilterTickEvent;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

public class WaterFilter extends Building {

	/** Seconds between water drips. */
	public static final float DRIP_INTERVAL  = 5f;
	/** Fraction of thirst restored per drip [0,1]. */
	public static final float THIRST_RESTORE = 0.08f;

	private final btRigidBody body;

	private final EventBus events;
	private float timer = 0f;

	public WaterFilter(Model model, EventBus events) {
		super(new ModelInstance(model));
		this.events = events;

		Vector3 dimensions = Util.getDimensions(this.model);
		btBoxShape shape = new btBoxShape(dimensions);
		MotionState motionState = new MotionState(this.model.transform, dimensions.y);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
		body.userData = this;
	}

	@Override
	public void update(float delta) {
		timer += delta;
		if (timer >= DRIP_INTERVAL) {
			timer -= DRIP_INTERVAL;
			events.post(new WaterFilterTickEvent(THIRST_RESTORE));
		}
	}

	@Override
	public String getName() {
		return "Water Filter";
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	// No extra resources to clean up — model is shared and disposed via Assets.
}