package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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
	private final Player player;
	private final Vector2 noiseDir = new Vector2();

	public OceanTrash(Model model, Vector2 position, Vector2 windDir, Player player) {
		super(new ModelInstance(model));
		this.windDir = windDir;
		this.player = player;
		float angle = (float) (Math.random() * Math.PI * 2.0);
		noiseDir.set(MathUtils.cos(angle), MathUtils.sin(angle)).scl(RANDOMNESS);

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
		Vector3 myPos = getPosition();
		Vector3 dir = player.getPosition().sub(myPos);
		dir.y = 0; // ignore height, trash floats on surface
		dir.x += noiseDir.x;
		dir.z += noiseDir.y;

		if (dir.len2() < 0.001f)
			return; // already at player, avoid zero vector

		dir.nor().scl(TRASH_SPEED * delta);
		transform.translate(dir.x, dir.y, dir.z);
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void onClick(EventBus events) {
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

	@Override
	public ModelInstance getOutlineInstance() {
		return model;
	}

	@Override
	public String getInteractHint() {
		return hint;
	}
}