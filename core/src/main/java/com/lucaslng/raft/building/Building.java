package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;

/**
 * A structure that can be placed on a {@link com.lucaslng.raft.raft.RaftTile}.
 *
 * <h3>Changes from the original</h3>
 * <ul>
 *   <li>{@link #setPosition} now takes a {@link Vector3} world position (the tile's
 *       world centre), not a local 2-D grid coord. This is needed for raft drift.</li>
 *   <li>Subclasses may optionally provide a {@link btRigidBody} via
 *       {@link #getBody()} for physics collision. The default returns {@code null}
 *       (no body), which means the building is visual-only — the raft tile itself
 *       still provides walkable collision. Subclasses that need a physics presence
 *       (e.g. a wall) override this and dispose the body in {@link #dispose()}.</li>
 *   <li>{@link #dispose()} is final here and calls {@link #doDispose()} for subclasses,
 *       ensuring the body is always cleaned up.</li>
 * </ul>
 */
public abstract class Building implements Disposable {

	protected final ModelInstance model;

	protected Building(ModelInstance model) {
		this.model = model;
	}

	/**
	 * Positions the building at the given world-space point.
	 * Called by {@link com.lucaslng.raft.raft.RaftTile#setBuilding(Building)}.
	 */
	public void setPosition(Vector3 worldPos) {
		model.transform.setToTranslation(worldPos);
	}

	/** Per-frame logic (timers, production, AI). */
	public abstract void update(float delta);

	/** Human-readable name shown in the HUD. */
	public abstract String getName();

	public ModelInstance getInstance() {
		return model;
	}

	/**
	 * Optional physics body for this building. Returns {@code null} by default
	 * (visual-only). Override in subclasses that need collision.
	 */
	public btRigidBody getBody() {
		return null;
	}

	/** Override to release subclass-specific resources. */
	protected void doDispose() {}

	@Override
	public final void dispose() {
		doDispose();
		// If the subclass provided a body it's responsible for disposing the
		// shape and motion state inside doDispose(); we just release the body ref.
		btRigidBody b = getBody();
		if (b != null) b.dispose();
	}
}