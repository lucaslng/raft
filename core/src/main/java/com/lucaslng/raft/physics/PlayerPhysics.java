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
 * <h3>Shape choice</h3>
 * <p>
 * A {@link btCapsuleShape} is used instead of a box. Capsules have a rounded
 * bottom that naturally slides over low steps and ramp edges, which prevents
 * the player from getting snagged on raft-tile borders. The radius and height
 * are derived from the model's bounding box so that the capsule roughly fits
 * the mesh.
 * </p>
 *
 * <h3>Angular locking</h3>
 * <p>
 * Angular factor is set to zero on all axes in
 * {@link com.lucaslng.raft.player.PlayerController} so the capsule never
 * topples over. It is not set here because the controller is responsible for
 * the movement policy.
 * </p>
 *
 * <h3>Shark-contact wiring</h3>
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
	private final btCapsuleShape shape;

	public PlayerPhysics(ModelInstance model) {
		// Derive capsule dimensions from the model bounding box.
		// getDimensions() returns half-extents.
		Vector3 halfExtents = Util.getDimensions(model);
		float radius = Math.max(halfExtents.x, halfExtents.z);
		// btCapsuleShape height = cylinder height (excludes the two hemisphere caps).
		// Total height = cylinderHeight + 2 * radius.
		// We want total capsule height ≈ 2 * halfExtents.y * 2, so:
		float totalHeight = halfExtents.y * 2f;
		float cylinderHeight = Math.max(0.01f, totalHeight - 2f * radius);

		// The MotionState offsets the body origin to the bottom of the model;
		// for a capsule we want the origin at the geometric centre, so pass 0
		// for halfHeight here and position the player at Y = capsule half-height
		// above the raft surface in World.
		motionState = new MotionState(model.transform, 0f);
		shape = new btCapsuleShape(radius, cylinderHeight);

		float mass = 20f;
		Vector3 inertia = new Vector3();
		shape.calculateLocalInertia(mass, inertia);

		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(mass, motionState, shape, inertia);
		body = new btRigidBody(info);
		info.dispose();

		body.setActivationState(Collision.DISABLE_DEACTIVATION);

		// ── Shark-contact filter ─────────────────────────────────────────
		body.setContactCallbackFlag(Shark.FLAG);
		body.setContactCallbackFilter(Shark.FLAG);
	}

	public btRigidBody getBody() {
		return body;
	}

	/**
	 * Writes the body's current linear velocity into {@code out}.
	 *
	 * @param out receives the velocity
	 * @return {@code out} (for chaining)
	 */
	public Vector3 getLinearVelocity(Vector3 out) {
		return out.set(body.getLinearVelocity());
	}

	@Override
	public void dispose() {
		body.dispose();
		motionState.dispose();
		shape.dispose();
	}
}