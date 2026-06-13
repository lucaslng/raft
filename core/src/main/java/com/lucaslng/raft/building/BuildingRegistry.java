package com.lucaslng.raft.building;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.util.Util;

/**
 * Central registry of all buildable structures.
 *
 * <h3>Usage</h3>
 * <ul>
 * <li>{@link #create} — instantiates a new {@link Building} (including its 3-D
 * model).</li>
 * <li>{@link #createHoldable} — returns a {@link BuildingItem} for the player's
 * hotbar.</li>
 * <li>{@link #getNames()} — ordered list of all registered building names.</li>
 * <li>{@link #getCost(String)} — ingredient map for a given building.</li>
 * </ul>
 */
public class BuildingRegistry {

	private final List<String> names = new ArrayList<>();
	private final Map<String, Supplier<Building>> factories = new HashMap<>();
	private final Map<String, Map<String, Integer>> costs = new HashMap<>();

	public BuildingRegistry(Assets assets, EventBus events, RaftSystem raftSystem, Vector2 windDir) {
		// ── Water Filter ──────────────────────────────────────────────────────
		Model waterFilterModel = assets.get("models/water-filter/water-filter.g3dj", Model.class);
		Util.scaleModel(waterFilterModel, .006f);
		register(
				"Water Filter",
				() -> new WaterFilter(waterFilterModel, events),
				Map.of()); // TODO

		// ── Sail ──────────────────────────────────────────────────────────────
		Model sailModel = assets.get("models/sail/sail.g3dj", Model.class);
		for (Node node : sailModel.nodes) {
			node.rotation.set(Vector3.Z, 0f);
		}
		sailModel.calculateTransforms();

		register(
				SailBuilding.NAME,
				() -> new SailBuilding(sailModel, raftSystem, windDir, events),
				Map.of()); // TODO
	}

	private void register(String name, Supplier<Building> factory, Map<String, Integer> cost) {
		names.add(name);
		factories.put(name, factory);
		costs.put(name, Collections.unmodifiableMap(new HashMap<>(cost)));
	}

	/** Creates a fresh {@link Building} instance, or {@code null} if unknown. */
	public Building create(String name) {
		Supplier<Building> f = factories.get(name);
		return f != null ? f.get() : null;
	}

	/** Creates a {@link BuildingItem} holdable for the given building name. */
	public BuildingItem createHoldable(String name) {
		return factories.containsKey(name) ? new BuildingItem(name) : null;
	}

	/**
	 * Returns the ingredient cost map for a building, or {@code null} if unknown.
	 */
	public Map<String, Integer> getCost(String name) {
		return costs.get(name);
	}

	/** Ordered list of all registered building names. */
	public List<String> getNames() {
		return Collections.unmodifiableList(names);
	}

	public boolean isRegistered(String name) {
		return factories.containsKey(name);
	}
}