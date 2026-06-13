package com.lucaslng.raft.raft;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
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
 *   <li>{@link #coord} — local grid coordinate (integer-like, e.g. (0,0), (1,0)).</li>
 *   <li>World position — {@code raftOrigin + coord}, updated by
 *       {@link #setWorldPosition(Vector2)} when the raft drifts.</li>
 * </ul>
 */
public class RaftTile implements Disposable {

	/** Local grid coordinate within the raft. Never changes after construction. */
	public final Vector2 coord;

	private final ModelInstance model;
	private final btRigidBody body;
	private final btBoxShape shape;
	private final MotionState motionState;

	private Building building;

	public RaftTile(Vector2 localCoord, Vector2 worldCoord, Model tileModel) {
		this.coord = localCoord;

		model = new ModelInstance(tileModel);
		model.transform.setToTranslation(worldCoord.x, 0f, worldCoord.y);

		BoundingBox bb  = new BoundingBox();
		model.calculateBoundingBox(bb);
		Vector3 dims = new Vector3();
		bb.getDimensions(dims);
		dims.scl(0.5f);   // half-extents

		motionState = new MotionState(model.transform, dims.y);
		shape       = new btBoxShape(dims);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.userData = this;
	}

	/** Updates the visual and physics world position for raft drift. */
	public void setWorldPosition(Vector2 worldCoord) {
		model.transform.setToTranslation(worldCoord.x, 0f, worldCoord.y);
		// Bullet reads the new transform via MotionState.getWorldTransform() on the
		// next physics step — no explicit body transform call needed for kinematic tiles.
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
		if (building != null) building.dispose();
		building = b;
		if (building != null) {
			// Position is world-space — caller should pass toWorldCoord(coord).
			building.setPosition(model.transform.getTranslation(new Vector3()));
		}
	}

	@Override
	public void dispose() {
		body.dispose();
		shape.dispose();
		motionState.dispose();
		if (building != null) building.dispose();
	}
}