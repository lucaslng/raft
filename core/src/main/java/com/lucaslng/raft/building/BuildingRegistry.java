package com.lucaslng.raft.building;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.crafting.CraftingRecipe;
import com.lucaslng.raft.crafting.CraftingRegistry;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.util.Util;
import com.lucaslng.raft.world.World;

// Registry for buildings
public class BuildingRegistry {

	private final List<String> names = new ArrayList<>();
	private final Map<String, Supplier<Building>> factories = new HashMap<>();
	private final Map<String, Map<String, Integer>> costs = new HashMap<>();

	public BuildingRegistry(World world, CraftingRegistry craftingRegistry) {
		Assets assets = Assets.get();
		
		// workbench
		Model workbenchModel = assets.get("models/workbench.g3db", Model.class);
		Util.scaleModel(workbenchModel, 2f);
		register(
				Workbench.NAME,
				() -> new Workbench(workbenchModel),
				Map.of());

		// water filter
		Model waterFilterModel = assets.get("models/water-filter/water-filter.g3dj", Model.class);
		Util.scaleModel(waterFilterModel, .006f);
		register(
				"Water Filter",
				() -> new WaterFilter(waterFilterModel),
				Map.of("Wood", 6, "String", 2));

		// cooking pot
		Model cookingPotModel = assets.get("models/pot.g3db", Model.class);
		register(
				"Cooking Pot",
				() -> new CookingPot(cookingPotModel),
				Map.of("Wood", 4, "Stone", 6));

		// farm
		Model farmModel = assets.get("models/farm/farm.g3dj", Model.class);
		Util.scaleModel(farmModel, .02f);
		register("Farm", () -> new Farm(farmModel), Map.of("Wood", 10, "Cauliflower", 5));

		// sail
		Model sailModel = assets.get("models/sail/sail.g3dj", Model.class);
		for (Node node : sailModel.nodes) {
			node.rotation.set(Vector3.Z, 0f);
		}
		sailModel.calculateTransforms();
		for (Material m : sailModel.materials) {
			m.set(IntAttribute.createCullFace(0));
		}
		register(
				SailBuilding.NAME,
				() -> new SailBuilding(sailModel, world.getRaftSystem(), world.getWindDir()),
				Map.of("String", 6, "Wood", 8));

		registerCraftableRecipes(craftingRegistry);
	}

	// Register buildings into crafting registry
	private void registerCraftableRecipes(CraftingRegistry craftingRegistry) {
		craftingRegistry.register(new CraftingRecipe(
				"Water Filter",
				costs.get("Water Filter"),
				() -> new BuildingItem("Water Filter")));

		craftingRegistry.register(new CraftingRecipe(
				"Cooking Pot",
				costs.get("Cooking Pot"),
				() -> new BuildingItem("Cooking Pot")));

		craftingRegistry.register(new CraftingRecipe(
				"Farm",
				costs.get("Farm"),
				() -> new BuildingItem("Farm")));

		craftingRegistry.register(new CraftingRecipe(
				SailBuilding.NAME,
				costs.get(SailBuilding.NAME),
				() -> new BuildingItem(SailBuilding.NAME)));
	}

	// helper method to register a building
	private void register(String name, Supplier<Building> factory, Map<String, Integer> cost) {
		names.add(name);
		factories.put(name, factory);
		costs.put(name, Collections.unmodifiableMap(new HashMap<>(cost)));
	}

	// building factory
	public Building create(String name) {
		Supplier<Building> f = factories.get(name);
		return f != null ? f.get() : null;
	}

	// create a holdable building to be placed
	public BuildingItem createHoldable(String name) {
		return factories.containsKey(name) ? new BuildingItem(name) : null;
	}

	public Map<String, Integer> getCost(String name) {
		return costs.get(name);
	}

	public List<String> getNames() {
		return Collections.unmodifiableList(names);
	}

	public boolean isRegistered(String name) {
		return factories.containsKey(name);
	}
}