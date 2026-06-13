package com.lucaslng.raft.building;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.g3d.Model;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.util.Util;

/**
 * Central registry of all buildable structures.
 * Call {@link #create} to get a new {@link Building} instance by name.
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
			Map.of("String", 4, "Wood", 4, "Stone", 5)
		);
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

	public Map<String, Integer> getCost(String name) {
		return costs.get(name);
	}

	public List<String> getNames() {
		return names;
	}
}