package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.BuildingClickedEvent;
import com.lucaslng.raft.world.Clickable;
import com.lucaslng.raft.world.Outlineable;

// Abstract class representing a structure that can be placed on a RaftTile. Has a model and optional physics.
public abstract class Building implements Disposable, Clickable, Outlineable {

	protected final ModelInstance model;

	protected Building(ModelInstance model) {
		this.model = model;
	}

	// called by raft tile to update position as the raft drifts
	public void setPosition(Vector3 worldPos) {
		model.transform.setToTranslation(worldPos);
	}

	// Called per frame
	public abstract void update(float delta);

	public abstract String getName();

	public ModelInstance getInstance() {
		return model;
	}

	@Override
	public void onClick(EventBus events) {
		events.post(new BuildingClickedEvent(this));
	}

	// optional physics body
	public btRigidBody getBody() {
		return null;
	}

	@Override
	public ModelInstance getOutlineInstance() {
		return model;
	}
}