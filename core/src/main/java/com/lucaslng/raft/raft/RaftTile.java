package com.lucaslng.raft.raft;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.physics.MotionState;

/**
 * One grid cell of the raft.
 *
 * <h3>Coordinate spaces</h3>
 * <ul>
 * <li>{@link #coord} — local grid coordinate (integer-like, e.g. (0,0),
 * (1,0)).</li>
 * <li>World position — {@code raftOrigin + coord}, updated by
 * {@link #setWorldPosition(Vector2)} when the raft drifts.</li>
 * </ul>
 *
 * <h3>Physics body</h3>
 * <p>
 * The tile body is <strong>kinematic</strong> (mass 0 +
 * {@code CF_KINEMATIC_OBJECT} + {@code DISABLE_DEACTIVATION}).
 *
 * <p>
 * Why kinematic and not static? A static body's transform is set <em>once</em>
 * at construction and Bullet never re-reads it from the MotionState. Kinematic
 * bodies call {@link MotionState#getWorldTransform} every physics sub-step so
 * the collision shape follows the drifting tile.
 *
 * <p>
 * Why {@code DISABLE_DEACTIVATION}? Without it Bullet deactivates the body
 * after it stops "moving" (from Bullet's perspective the kinematic body has no
 * velocity), and stops calling {@code getWorldTransform} — the hitbox freezes.
 *
 * <p>
 * {@link #setWorldPosition(Vector2)} also calls
 * {@link btRigidBody#setWorldTransform} directly so the broadphase AABB is
 * refreshed immediately, making raycasts reliable on the same frame as a drift
 * update (Bullet only pumps MotionState transforms during
 * {@code stepSimulation}, not during {@code rayTest}).
 */
public class RaftTile implements Disposable {

	/** Local grid coordinate within the raft. Never changes after construction. */
	public final Vector2 coord;

	private final ModelInstance model;
	private final btRigidBody body;
	private final btBoxShape shape;
	private final MotionState motionState;
	private final Vector3 dims;

	/** Scratch matrix reused by setWorldPosition to avoid per-frame allocation. */
	private final Matrix4 scratchTransform = new Matrix4();

	private Building building;

	public RaftTile(Vector2 localCoord, Vector2 worldCoord, Model tileModel) {
		this.coord = localCoord;

		model = new ModelInstance(tileModel);
		model.transform.setToTranslation(worldCoord.x, 0f, worldCoord.y);

		BoundingBox bb = new BoundingBox();
		model.calculateBoundingBox(bb);
		dims = new Vector3();
		bb.getDimensions(dims);
		dims.scl(0.5f); // convert to half-extents for Bullet

		motionState = new MotionState(model.transform, dims.y);
		shape = new btBoxShape(dims);

		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();

		// ── Kinematic setup ──────────────────────────────────────────────
		// CF_KINEMATIC_OBJECT tells Bullet this body is moved by application
		// code via the MotionState, not by forces.
		body.setCollisionFlags(
				body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);

		// DISABLE_DEACTIVATION prevents Bullet from putting the body to sleep
		// (after which it stops polling getWorldTransform on the MotionState).
		body.setActivationState(Collision.DISABLE_DEACTIVATION);

		body.userData = this;
	}

	/**
	 * Moves the tile to a new world position after a raft drift step.
	 *
	 * <p>
	 * Two things must happen for the physics body to follow:
	 * </p>
	 * <ol>
	 * <li><b>Visual transform</b> — {@code model.transform} is updated so
	 * the rendered mesh is in the right place. {@link MotionState} wraps
	 * this matrix, so the next {@code stepSimulation} will pull the
	 * correct transform out of it.</li>
	 * <li><b>Immediate body transform</b> — {@link btRigidBody#setWorldTransform}
	 * is called directly so the broadphase AABB updates on the same frame.
	 * Without this, raycasts fired before the next {@code stepSimulation}
	 * still hit the old position.</li>
	 * </ol>
	 */
	public void setWorldPosition(Vector2 worldCoord) {
		// 1. Update the visual / MotionState transform.
		model.transform.setToTranslation(worldCoord.x, 0f, worldCoord.y);

		// 2. Push the new transform directly into the Bullet body.
		// MotionState.getWorldTransform offsets by HALF_HEIGHT, so we
		// replicate that here so the physics shape stays centred on the mesh.
		scratchTransform.set(model.transform);
		scratchTransform.val[Matrix4.M13] += dims.y; // add HALF_HEIGHT offset
		body.setWorldTransform(scratchTransform);

		if (building != null)
			building.setPosition(new Vector3(worldCoord.x, dims.y, worldCoord.y));
	}

	public Vector3 getWorldCenter() {
		return model.transform.getTranslation(new Vector3());
	}

	public ModelInstance getInstance() {
		return model;
	}

	public btRigidBody getBody() {
		return body;
	}

	public boolean hasBuilding() {
		return building != null;
	}

	public Building getBuilding() {
		return building;
	}

	/** Places a building on this tile, disposing any previous one. */
	public void setBuilding(Building b) {
		if (building != null)
			building.dispose();
		building = b;
		if (building != null) {
			Vector3 position = new Vector3();
			model.transform.getTranslation(position);
			position.y += dims.y;
			building.setPosition(position);
		}
	}

	@Override
	public void dispose() {
		body.dispose();
		shape.dispose();
		motionState.dispose();
		if (building != null)
			building.dispose();
	}
}