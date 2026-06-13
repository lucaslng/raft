package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.physics.PhysicsSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for all live {@link Entity} instances.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Add / remove entities while safely iterating (pending-add / pending-remove queues).</li>
 *   <li>Drive the per-frame {@link Entity#update(float)} loop.</li>
 *   <li>Provide a typed view of all model instances for the renderer.</li>
 *   <li>Dispose and remove from physics on entity death.</li>
 * </ul>
 *
 * <p>This replaces the separate {@code player}, {@code shark}, and {@code trashSystem}
 * entity management spread across {@code World}. Adding a new entity type (e.g. a
 * destination buoy, a swimming player state) requires no changes here — just
 * {@code entitySystem.add(newEntity)}.
 */
public class EntitySystem implements Disposable {

	private final List<Entity> entities    = new ArrayList<>();
	private final List<Entity> toAdd       = new ArrayList<>();
	private final List<Entity> toRemove    = new ArrayList<>();

	private final PhysicsSystem physics;

	public EntitySystem(PhysicsSystem physics) {
		this.physics = physics;
	}

	/** Schedules an entity to be added at the start of the next {@link #update}. */
	public void add(Entity entity) {
		toAdd.add(entity);
	}

	/** Schedules an entity to be removed and disposed at the start of the next {@link #update}. */
	public void remove(Entity entity) {
		entity.kill();
		toRemove.add(entity);
	}

	/**
	 * Updates all live entities. Pending additions and removals are flushed first.
	 */
	public void update(float delta) {
		// Flush additions
		for (Entity e : toAdd) {
			entities.add(e);
			physics.addEntity(e);
		}
		toAdd.clear();

		// Flush removals (entity.kill() already called in remove())
		for (Entity e : toRemove) {
			entities.remove(e);
			physics.removeEntity(e);
			e.dispose();
		}
		toRemove.clear();

		// Update survivors
		for (Entity e : entities) {
			e.update(delta);
		}
	}

	/**
	 * Returns an unmodifiable view of all live entities.
	 * Safe to iterate during the same frame (mutations are deferred).
	 */
	public List<Entity> getAll() {
		return Collections.unmodifiableList(entities);
	}

	/**
	 * Returns all live model instances — used by the renderer to build the
	 * opaque draw list.
	 */
	public List<ModelInstance> getInstances() {
		List<ModelInstance> out = new ArrayList<>(entities.size());
		for (Entity e : entities) {
			if (e.getInstance() != null) out.add(e.getInstance());
		}
		return out;
	}

	/** Convenience: first entity of the given type, or {@code null}. */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T getFirst(Class<T> type) {
		for (Entity e : entities) {
			if (type.isInstance(e)) return (T) e;
		}
		return null;
	}

	@Override
	public void dispose() {
		for (Entity e : entities) {
			physics.removeEntity(e);
			e.dispose();
		}
		entities.clear();
		toAdd.clear();
		toRemove.clear();
	}
}