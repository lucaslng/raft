package com.lucaslng.raft.world;

import java.util.*;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.entity.OceanTrash;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.TrashCollectedEvent;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.item.ItemStack;
import com.lucaslng.raft.physics.PhysicsSystem;

public class TrashSystem {

	static final float SPAWN_DIST = 40f;
	static final float DESPAWN_DIST = SPAWN_DIST * SPAWN_DIST * 4f;
	static final private float SPAWN_CHANCE = .9f; // roughly avg spawns per second

	private List<OceanTrash> trash = new ArrayList<>();

	private final PhysicsSystem physics;
	private final ItemRegistry itemRegistry;
	private final Vector2 windDir;

	public TrashSystem(EventBus events, PhysicsSystem physics, ItemRegistry itemRegistry, Vector2 windDir) {
		this.physics = physics;
		this.itemRegistry = itemRegistry;
		this.windDir = windDir;

		events.subscribe(TrashCollectedEvent.class, new Subscriber<TrashCollectedEvent>() {
			@Override
			public void accept(TrashCollectedEvent event) {
				OceanTrash t = event.oceanTrash;
				physics.removeEntity(t);
				t.dispose();
				trash.remove(t);
			}
		});
	}

	public void update(float delta, Vector3 playerPosition) {
		// Iterate over all trash
		Iterator<OceanTrash> iterator = trash.iterator();
		while (iterator.hasNext()) {
			OceanTrash t = iterator.next();

			// Remove trash out of range, else update it
			float tx = t.getPosition().x;
			float tz = t.getPosition().z;
			float dx = tx - playerPosition.x;
			float dz = tz - playerPosition.z;
			float distSquared = dx * dx + dz * dz;
			if (distSquared > DESPAWN_DIST) {
				physics.removeEntity(t);
				t.dispose();
				iterator.remove();
			} else {
				t.update(delta);
			}
		}

		// Spawn trash
		if (Math.random() < SPAWN_CHANCE * delta) {
			String item = "";
			switch (MathUtils.random(1, 3)) {
				case 1: item = "Wood"; break;
				case 2: item = "Stone"; break;
				case 3: item = "String"; break;
			}
			int quantity = MathUtils.random(1, 5);
			ItemStack items = new ItemStack(itemRegistry.get(item), quantity);
			Vector2 perp = new Vector2(-windDir.y, windDir.x); // 90 degrees left
			Vector2 position = windDir.cpy()
					.scl(-SPAWN_DIST)
					.add(perp.scl(MathUtils.random(-SPAWN_DIST, SPAWN_DIST)))
					.add(playerPosition.x, playerPosition.z);
			OceanTrash t = new OceanTrash(items, position, windDir);
			trash.add(t);
			physics.addEntity(t);
		}
	}

	public Iterable<OceanTrash> getTrash() {
		return trash;
	}

}
