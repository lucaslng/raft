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
 * Grid coordinates (gridX, gridZ) map to world position (gridX * TILE_SIZE, 0, gridZ * TILE_SIZE).
 */
public class RaftTile implements Disposable {

	public final Vector2 coord;

	private final ModelInstance model;
	private final btRigidBody body;

	/** Optional building placed on this tile (null = empty). */
	private Building building;

	public RaftTile(Vector2 coord, Model tileModel) {
		this.coord = coord;

		model = new ModelInstance(tileModel);
		model.transform.setToTranslation(coord.x, 0f, coord.y);

		BoundingBox bb = new BoundingBox();
		model.calculateBoundingBox(bb);
		Vector3 dims = new Vector3();
		bb.getDimensions(dims);
		dims.scl(0.5f);

		MotionState motionState = new MotionState(model.transform, dims.y);
		btBoxShape shape = new btBoxShape(dims);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		// Store this tile as userData so raycasts can identify it
		body.userData = this;
	}

	public Vector3 getWorldCenter() {
		return new Vector3(coord.x, 0f, coord.y);
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

	/** Places a building on this tile. Disposes any previous building. */
	public void setBuilding(Building b) {
		if (building != null) building.dispose();
		building = b;
		if (building != null) {
			building.setPosition(coord);
		}
	}

	@Override
	public void dispose() {
		body.dispose();
		if (building != null) building.dispose();
	}
}