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
import com.lucaslng.raft.world.Outlineable;

// One grid cell for raft
public class RaftTile implements Disposable, Outlineable {

	// local grid coordinates within raft
	public final Vector2 coord;

	private final ModelInstance model;
	private final btRigidBody body;
	private final btBoxShape shape;
	private final MotionState motionState;
	private final Vector3 dims;

	// temp matrix
	private final Matrix4 scratchTransform = new Matrix4();

	// optional building
	private Building building;

	public RaftTile(Vector2 localCoord, Vector2 worldCoord, Model tileModel) {
		this.coord = localCoord;

		model = new ModelInstance(tileModel);
		model.transform.setToTranslation(worldCoord.x, 0f, worldCoord.y);

		BoundingBox bb = new BoundingBox();
		model.calculateBoundingBox(bb);
		dims = new Vector3();
		bb.getDimensions(dims);
		dims.scl(0.5f);

		motionState = new MotionState(model.transform, dims.y);
		shape = new btBoxShape(dims);

		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();

		body.setCollisionFlags(
				body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);

		body.setActivationState(Collision.DISABLE_DEACTIVATION);

		body.userData = this;
	}

	public void setWorldPosition(Vector2 worldCoord) {
		model.transform.setToTranslation(worldCoord.x, 0f, worldCoord.y);

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

	// Places a building on this tile, disposes any previous one
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

	@Override
	public ModelInstance getOutlineInstance() {
		return model;
	}
}