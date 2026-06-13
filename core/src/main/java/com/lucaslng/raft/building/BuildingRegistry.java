package com.lucaslng.raft.building;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.g3d.Model;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.HoldableItemRecievedEvent;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.util.Util;

/**
 * Central registry of all buildable structures.
 *
 * <ul>
 * <li>{@link #create} — returns a new {@link Building} instance (3-D
 * model).</li>
 * <li>{@link #createHoldable} — returns a {@link BuildingItem} the player can
 * carry in their hotbar and left-click to place.</li>
 * <li>{@link #giveStartingItems} — posts {@link HoldableItemRecievedEvent}s so
 * the player starts the game with one of every registered building tool.</li>
 * </ul>
 */
public class BuildingRegistry {

	private final List<String> names = new ArrayList<>();
	private final Map<String, Supplier<Building>> factories = new HashMap<>();
	private final Map<String, Map<String, Integer>> costs = new HashMap<>();

	public BuildingRegistry(Assets assets, EventBus events) {
		Model waterFilterModel = assets.get("models/water-filter/water-filter.g3dj", Model.class);
		Util.scaleModel(waterFilterModel, .006f);
		register(
				"Water Filter",
				() -> new WaterFilter(waterFilterModel, events),
				Map.of("String", 4, "Wood", 4, "Stone", 5));
	}

	private void register(String name, Supplier<Building> factory, Map<String, Integer> cost) {
		names.add(name);
		factories.put(name, factory);
		costs.put(name, cost);
	}

	public Building create(String name) {
		Supplier<Building> f = factories.get(name);
		return f != null ? f.get() : null;
	}

	public BuildingItem createHoldable(String name) {
		if (!factories.containsKey(name))
			return null;
		return new BuildingItem(name);
	}

	public void giveStartingItems(EventBus events) {
		for (String name : names) {
			events.post(new HoldableItemRecievedEvent(new BuildingItem(name)));
		}
	}

	public Map<String, Integer> getCost(String name) {
		return costs.get(name);
	}

	public List<String> getNames() {
		return names;
	}
}