package com.lucaslng.raft.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.entity.*;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.TrashCollectedEvent;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.item.ItemStack;
import com.lucaslng.raft.physics.PhysicsSystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Spawns, despawns, and updates OceanTrash
public class TrashSystem {

	private static final float SPAWN_RADIUS = 40f;
	private static final float DESPAWN_RADIUS_SQ = (SPAWN_RADIUS * 2f) * (SPAWN_RADIUS * 2f);
	private static final float SPAWN_CHANCE = 0.9f; // expected spawns per second
	private static final float BLUEPRINT_CHANCE = 0.1f; // fraction of spawns that are blueprints

	private static final String[] ITEM_TYPES = { "Wood", "Stone", "String", "Cauliflower" };

	private final List<OceanTrash> trash = new ArrayList<>();

	private final PhysicsSystem physics;
	private final ItemRegistry itemRegistry;
	private final Assets assets;
	private final Vector2 windDir;

	public TrashSystem(EventBus events, PhysicsSystem physics,
			ItemRegistry itemRegistry, Assets assets, Vector2 windDir) {
		this.physics = physics;
		this.itemRegistry = itemRegistry;
		this.assets = assets;
		this.windDir = windDir;

		events.subscribe(TrashCollectedEvent.class, event -> {
			OceanTrash t = event.oceanTrash;
			if (trash.remove(t)) {
				physics.removeEntity(t);
				t.dispose();
			}
		});
	}

	public void update(float delta, Player player) {
		Vector3 playerPos = player.getPosition();

		// despawn out of range trash
		Iterator<OceanTrash> it = trash.iterator();
		while (it.hasNext()) {
			OceanTrash t = it.next();
			Vector3 tp = t.getPosition();
			float dx = tp.x - playerPos.x;
			float dz = tp.z - playerPos.z;
			if (dx * dx + dz * dz > DESPAWN_RADIUS_SQ) {
				physics.removeEntity(t);
				t.dispose();
				it.remove();
			} else {
				t.update(delta);
			}
		}

		// spawn trash
		if (Math.random() < SPAWN_CHANCE * delta) {
			// Spawn in circle around player
			float angle = MathUtils.random(0f, MathUtils.PI2);
			float x = playerPos.x + MathUtils.cos(angle) * SPAWN_RADIUS;
			float z = playerPos.z + MathUtils.sin(angle) * SPAWN_RADIUS;
			Vector2 spawnPos = new Vector2(x, z);

			OceanTrash t;
			if (Math.random() < BLUEPRINT_CHANCE) {
				t = new OceanBlueprint(spawnPos, windDir, player, assets);
			} else {
				String itemName = ITEM_TYPES[MathUtils.random(ITEM_TYPES.length - 1)];
				int quantity = MathUtils.random(1, 5);
				t = new OceanItem(new ItemStack(itemRegistry.getItem(itemName), quantity), spawnPos, windDir, player);
			}

			trash.add(t);
			physics.addEntity(t);
		}
	}

	public Iterable<OceanTrash> getTrash() {
		return trash;
	}
}