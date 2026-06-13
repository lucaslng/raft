package com.lucaslng.raft.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.entity.OceanBlueprint;
import com.lucaslng.raft.entity.OceanItem;
import com.lucaslng.raft.entity.OceanTrash;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.TrashCollectedEvent;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.item.ItemStack;
import com.lucaslng.raft.physics.PhysicsSystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Spawns, updates, and despawns ocean debris.
 */
public class TrashSystem {

	private static final float  SPAWN_RADIUS      = 40f;
	private static final float  DESPAWN_RADIUS_SQ = (SPAWN_RADIUS * 2f) * (SPAWN_RADIUS * 2f);
	private static final float  SPAWN_CHANCE      = 0.9f; // expected spawns per second
	private static final float  BLUEPRINT_CHANCE  = 0.1f; // fraction of spawns that are blueprints

	private static final String[] ITEM_TYPES = {"Wood", "Stone", "String"};

	private final List<OceanTrash> trash = new ArrayList<>();

	private final PhysicsSystem physics;
	private final ItemRegistry  itemRegistry;
	private final Assets        assets;
	private final Vector2       windDir;

	public TrashSystem(EventBus events, PhysicsSystem physics,
	                   ItemRegistry itemRegistry, Assets assets, Vector2 windDir) {
		this.physics      = physics;
		this.itemRegistry = itemRegistry;
		this.assets       = assets;
		this.windDir      = windDir;

		events.subscribe(TrashCollectedEvent.class, event -> {
			OceanTrash t = event.oceanTrash;
			if (trash.remove(t)) {
				physics.removeEntity(t);
				t.dispose();
			}
		});
	}

	public void update(float delta, Vector3 playerPos) {
		// ── Despawn out-of-range trash ────────────────────────────────────
		Iterator<OceanTrash> it = trash.iterator();
		while (it.hasNext()) {
			OceanTrash t  = it.next();
			Vector3    tp = t.getPosition();
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

		// ── Spawn new trash ───────────────────────────────────────────────
		if (Math.random() < SPAWN_CHANCE * delta) {
			Vector2 perp     = new Vector2(-windDir.y, windDir.x); // perpendicular
			Vector2 spawnPos = windDir.cpy()
					.scl(-SPAWN_RADIUS)
					.add(perp.scl(MathUtils.random(-SPAWN_RADIUS, SPAWN_RADIUS)))
					.add(playerPos.x, playerPos.z);

			OceanTrash t;
			if (Math.random() < BLUEPRINT_CHANCE) {
				t = new OceanBlueprint(spawnPos, windDir, assets);
			} else {
				String itemName = ITEM_TYPES[MathUtils.random(ITEM_TYPES.length - 1)];
				int    quantity = MathUtils.random(1, 5);
				t = new OceanItem(new ItemStack(itemRegistry.get(itemName), quantity), spawnPos, windDir);
			}

			trash.add(t);
			physics.addEntity(t);
		}
	}

	public Iterable<OceanTrash> getTrash() {
		return trash;
	}
}