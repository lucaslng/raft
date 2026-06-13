package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

/**
 * A structure that can be placed on a {@link com.lucaslng.raft.raft.RaftTile}.
 * Subclasses implement per-tick logic (e.g. water filter drip, campfire burn).
 */
public abstract class Building implements Disposable {

	protected final ModelInstance model;

	protected Building(ModelInstance model) {
		this.model = model;
	}

	/** Called once by RaftTile when the building is placed. */
	public void setPosition(Vector2 coord) {
		model.transform.setToTranslation(coord.x, 0f, coord.y);
	}

	/** Per-frame logic. */
	public abstract void update(float delta);

	/** Human-readable name shown in the HUD. */
	public abstract String getName();

	public ModelInstance getInstance() {
		return model;
	}

	@Override
	public void dispose() {
	}
}