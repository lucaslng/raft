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
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.util.Util;
import com.lucaslng.raft.world.World;

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
 *
 * <h3>Crafting integration</h3>
 * <p>
 * Buildings that are obtained through the workbench crafting system are
 * registered in the {@link CraftingRegistry} rather than given directly to the
 * player. The workbench itself is the only building given to the player at game
 * start.
 * </p>
 */
public class BuildingRegistry {

	private final List<String> names = new ArrayList<>();
	private final Map<String, Supplier<Building>> factories = new HashMap<>();
	private final Map<String, Map<String, Integer>> costs = new HashMap<>();

	public BuildingRegistry(Assets assets, EventBus events, World world,
			CraftingRegistry craftingRegistry) {
		// ── Workbench ────────────────────────────────────────────────────────
		// The workbench is the only building given to the player at game start.
		// It is not a crafted item — it is placed via a BuildingItem in the hotbar.
		Model workbenchModel = assets.get("models/workbench.g3db", Model.class);
		register(
				Workbench.NAME,
				() -> new Workbench(workbenchModel, events),
				Map.of());

		// ── Water Filter ──────────────────────────────────────────────────────
		// Unlocked via the first blueprint — crafted at the workbench.
		Model waterFilterModel = assets.get("models/water-filter/water-filter.g3dj", Model.class);
		Util.scaleModel(waterFilterModel, .006f);
		register(
				"Water Filter",
				() -> new WaterFilter(waterFilterModel, events),
				Map.of("Wood", 6, "String", 2));

		// ── Cooking Pot ───────────────────────────────────────────────────────
		// Unlocked via the second blueprint.
		Model cookingPotModel = assets.get("models/pot.g3db", Model.class);
		register(
				"Cooking Pot",
				() -> new CookingPot(cookingPotModel, events),
				Map.of("Wood", 4, "Stone", 6));

		// ── Sail ──────────────────────────────────────────────────────────────
		// Unlocked via the third blueprint.
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
				() -> new SailBuilding(sailModel, world.getRaftSystem(), world.getWindDir(), events),
				Map.of("String", 6, "Wood", 8));

		// ── Register craftable buildings in the CraftingRegistry ──────────────
		// Order determines unlock order: blueprint 1 → Water Filter, etc.
		registerCraftableRecipes(craftingRegistry);
	}

	/**
	 * Registers each craftable building as a recipe in the provided
	 * {@link CraftingRegistry} in unlock order. The workbench itself is excluded
	 * because it is given to the player at game start.
	 */
	private void registerCraftableRecipes(CraftingRegistry craftingRegistry) {
		// Recipe 1 — unlocked by first blueprint
		craftingRegistry.register(new CraftingRecipe(
				"Water Filter",
				costs.get("Water Filter"),
				() -> new BuildingItem("Water Filter")));

		// Recipe 2 — unlocked by second blueprint
		craftingRegistry.register(new CraftingRecipe(
				"Cooking Pot",
				costs.get("Cooking Pot"),
				() -> new BuildingItem("Cooking Pot")));

		// Recipe 3 — unlocked by third blueprint
		craftingRegistry.register(new CraftingRecipe(
				SailBuilding.NAME,
				costs.get(SailBuilding.NAME),
				() -> new BuildingItem(SailBuilding.NAME)));
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