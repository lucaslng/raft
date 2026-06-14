package com.lucaslng.raft.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.player.holdable.Holdable;

/**
 * Central registry for craftable recipes, unlocked in order as the player
 * collects blueprints from the ocean.
 *
 * <h3>Unlock order</h3>
 * <p>
 * Recipes are registered in the order they should be unlocked. The first
 * blueprint unlocks recipe index 0, the second unlocks index 1, and so on.
 * Excess blueprints beyond the total registered are silently ignored, leaving
 * room for future recipes to be added.
 * </p>
 *
 * <h3>Crafting</h3>
 * <p>
 * {@link #craft(String, Backpack)} checks that the recipe is unlocked, the
 * player can afford it, consumes the ingredients, and returns the crafted
 * {@link Holdable}. Returns {@code null} if the craft is not possible.
 * </p>
 */
public class CraftingRegistry {

	/** All registered recipes in unlock order. */
	private final List<CraftingRecipe> allRecipes = new ArrayList<>();

	/** How many recipes are currently available to the player. */
	private int unlockedCount = 0;

	// ── Registration ─────────────────────────────────────────────────────────

	/**
	 * Registers a recipe at the end of the unlock queue. This must be called
	 * before any blueprints are collected.
	 */
	public void register(CraftingRecipe recipe) {
		allRecipes.add(recipe);
	}

	// ── Blueprint-driven unlock ───────────────────────────────────────────────

	/**
	 * Called whenever a blueprint is collected. Unlocks the next recipe in the
	 * queue, if any remain locked.
	 */
	public void onBlueprintCollected() {
		if (unlockedCount < allRecipes.size()) {
			unlockedCount++;
		}
		// Extra blueprints beyond the total are intentionally ignored.
	}

	// ── Queries ───────────────────────────────────────────────────────────────

	/** Returns an unmodifiable view of all currently unlocked recipes. */
	public List<CraftingRecipe> getUnlocked() {
		if (unlockedCount == 0)
			return Collections.emptyList();
		return Collections.unmodifiableList(allRecipes.subList(0, unlockedCount));
	}

	/** Returns the recipe with the given name, or {@code null} if not found. */
	public CraftingRecipe getRecipe(String name) {
		for (CraftingRecipe r : allRecipes) {
			if (r.name.equals(name))
				return r;
		}
		return null;
	}

	/** Whether a recipe with the given name is currently unlocked. */
	public boolean isUnlocked(String name) {
		for (int i = 0; i < unlockedCount; i++) {
			if (allRecipes.get(i).name.equals(name))
				return true;
		}
		return false;
	}

	// ── Crafting ─────────────────────────────────────────────────────────────

	/**
	 * Attempts to craft the recipe with the given name using the player's
	 * backpack.
	 *
	 * <ol>
	 * <li>Checks the recipe exists and is unlocked.</li>
	 * <li>Checks the backpack has enough of every ingredient.</li>
	 * <li>Consumes the ingredients.</li>
	 * <li>Returns the crafted {@link Holdable}.</li>
	 * </ol>
	 *
	 * @return the crafted holdable, or {@code null} if the craft failed.
	 */
	public Holdable craft(String recipeName, Backpack backpack) {
		if (!isUnlocked(recipeName))
			return null;

		CraftingRecipe recipe = getRecipe(recipeName);
		if (recipe == null)
			return null;

		// Check affordability.
		for (Map.Entry<String, Integer> e : recipe.ingredients.entrySet()) {
			if (backpack.getCount(e.getKey()) < e.getValue())
				return null;
		}

		// Consume ingredients.
		for (Map.Entry<String, Integer> e : recipe.ingredients.entrySet()) {
			backpack.consume(e.getKey(), e.getValue());
		}

		return recipe.craft();
	}

	/** Total number of registered recipes (locked + unlocked). */
	public int getTotalCount() {
		return allRecipes.size();
	}

	/** Number of currently unlocked recipes. */
	public int getUnlockedCount() {
		return unlockedCount;
	}
}