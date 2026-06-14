package com.lucaslng.raft.physics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.entity.Shark;
import com.lucaslng.raft.util.Util;

/**
 * Player rigid-body setup.
 *
 * <p>
 * The player body sets its {@code contactCallbackFilter} to
 * {@link Shark#FLAG} so that the global
 * {@link com.badlogic.gdx.physics.bullet.collision.ContactListener}
 * in {@link PhysicsSystem} fires when the shark body (which has
 * {@code contactCallbackFlag == Shark.FLAG}) makes contact.
 * </p>
 */
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

		// ── Shark-contact filter ─────────────────────────────────────────
		// When the shark's callbackFlag (Shark.FLAG) touches an object whose
		// callbackFilter includes Shark.FLAG, the ContactListener fires with
		// match = true for that side. Setting both flag and filter on the
		// player body means the listener fires for *either* side of the pair.
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