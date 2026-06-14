package com.lucaslng.raft.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.player.holdable.Holdable;

// Registry for crafting recipes
public class CraftingRegistry {

	// Ordered by unlock order
	private final List<CraftingRecipe> allRecipes = new ArrayList<>();

	private int unlockedCount = 0;

	public void register(CraftingRecipe recipe) {
		allRecipes.add(recipe);
	}

	public void onBlueprintCollected() {
		if (unlockedCount < allRecipes.size()) {
			unlockedCount++;
		}
	}

	public List<CraftingRecipe> getUnlocked() {
		if (unlockedCount == 0)
			return Collections.emptyList();
		return Collections.unmodifiableList(allRecipes.subList(0, unlockedCount));
	}

	public CraftingRecipe getRecipe(String name) {
		for (CraftingRecipe r : allRecipes) {
			if (r.name.equals(name))
				return r;
		}
		return null;
	}

	public boolean isUnlocked(String name) {
		for (int i = 0; i < unlockedCount; i++) {
			if (allRecipes.get(i).name.equals(name))
				return true;
		}
		return false;
	}

	// Craft a recipe, only crafts if player has enough ingredients in backpack
	public Holdable craft(String recipeName, Backpack backpack) {
		if (!isUnlocked(recipeName))
			return null;

		CraftingRecipe recipe = getRecipe(recipeName);
		if (recipe == null)
			return null;

		for (Map.Entry<String, Integer> e : recipe.ingredients.entrySet()) {
			if (backpack.getCount(e.getKey()) < e.getValue())
				return null;
		}

		for (Map.Entry<String, Integer> e : recipe.ingredients.entrySet()) {
			backpack.consume(e.getKey(), e.getValue());
		}

		return recipe.craft();
	}

	public int getTotalCount() {
		return allRecipes.size();
	}

	public int getUnlockedCount() {
		return unlockedCount;
	}
}