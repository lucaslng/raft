package com.lucaslng.raft.building;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.g3d.Model;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.util.Util;

/**
 * Central registry of all buildable structures.
 *
 * <h3>Usage</h3>
 * <ul>
 *   <li>{@link #create} — instantiates a new {@link Building} (including its 3-D model).</li>
 *   <li>{@link #createHoldable} — returns a {@link BuildingItem} for the player's hotbar.</li>
 *   <li>{@link #getNames()} — ordered list of all registered building names.</li>
 *   <li>{@link #getCost(String)} — ingredient map for a given building.</li>
 * </ul>
 *
 * <h3>Changes from original</h3>
 * <ul>
 *   <li>Removed {@code giveStartingItems}: distributing holdables is the responsibility
 *       of the initialisation code in {@code World} (or a tutorial system), not the
 *       registry itself. Call {@link #getNames()} and post events externally.</li>
 *   <li>Cost map is wrapped in {@link Collections#unmodifiableMap} — callers cannot
 *       accidentally mutate it.</li>
 *   <li>Sail and future buildings can be registered here without changing any other class.</li>
 * </ul>
 */
public class BuildingRegistry {

	private final List<String>                     names     = new ArrayList<>();
	private final Map<String, Supplier<Building>>  factories = new HashMap<>();
	private final Map<String, Map<String, Integer>> costs    = new HashMap<>();

	public BuildingRegistry(Assets assets, EventBus events) {
		// ── Water Filter ─────────────────────────────────────────────────────
		Model waterFilterModel = assets.get("models/water-filter/water-filter.g3dj", Model.class);
		Util.scaleModel(waterFilterModel, .006f);
		register(
				"Water Filter",
				() -> new WaterFilter(waterFilterModel, events),
				Map.of());
				// Map.of("String", 4, "Wood", 4, "Stone", 5));

		// ── Sail (stub — future feature) ──────────────────────────────────
		// Uncomment and implement SailBuilding when the raft-drift feature is ready.
		// register("Sail", () -> new SailBuilding(sailModel, raftSystem),
		//         Map.of("String", 6, "Wood", 8));

		// ── Cooking Pot (stub) ────────────────────────────────────────────
		// register("Cooking Pot", () -> new CookingPot(potModel, events),
		//         Map.of("Stone", 6));

		// ── Crafting Bench (stub) ─────────────────────────────────────────
		// register("Crafting Bench", () -> new CraftingBench(benchModel, events),
		//         Map.of("Wood", 10, "Stone", 4));
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

	/** Returns the ingredient cost map for a building, or {@code null} if unknown. */
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