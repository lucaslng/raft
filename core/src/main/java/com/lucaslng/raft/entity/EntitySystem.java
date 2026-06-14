package com.lucaslng.raft.entity;

import java.util.*;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.physics.PhysicsSystem;

// Loops over entities on each frame to handle deferred adds and removes
// This would be more useful if we had multiplayer and multithreading
public class EntitySystem implements Disposable {

	private final List<Entity> entities = new ArrayList<>();
	private final List<Entity> toAdd = new ArrayList<>();
	private final List<Entity> toRemove = new ArrayList<>();
	private final ArrayList<ModelInstance> out = new ArrayList<>();

	private final PhysicsSystem physics;

	public EntitySystem(PhysicsSystem physics) {
		this.physics = physics;
	}

	public void add(Entity entity) {
		toAdd.add(entity);
	}

	public void remove(Entity entity) {
		entity.kill();
		toRemove.add(entity);
	}

	public void update(float delta) {
		// handle adds
		for (Entity e : toAdd) {
			entities.add(e);
			physics.addEntity(e);
		}
		toAdd.clear();

		// handle removes
		for (Entity e : toRemove) {
			entities.remove(e);
			physics.removeEntity(e);
			e.dispose();
		}
		toRemove.clear();

		// update entities
		for (Entity e : entities) {
			e.update(delta);
		}
	}

	public List<Entity> getAll() {
		return Collections.unmodifiableList(entities);
	}

	public Collection<ModelInstance> getInstances() {
		out.clear();
		out.ensureCapacity(entities.size());
		for (Entity e : entities) {
			if (e.getInstance() != null)
				out.add(e.getInstance());
		}
		return out;
	}

	// first entity of a given type
	@SuppressWarnings("unchecked")
	public <T extends Entity> T getFirst(Class<T> type) {
		for (Entity e : entities) {
			if (type.isInstance(e))
				return (T) e;
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