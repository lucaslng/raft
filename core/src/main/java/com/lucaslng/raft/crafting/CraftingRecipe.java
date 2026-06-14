package com.lucaslng.raft.crafting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.lucaslng.raft.player.holdable.Holdable;

/**
 * A single craftable recipe: a name, an ordered map of ingredient costs, and a
 * factory that produces the resulting {@link Holdable}.
 *
 * <p>
 * The {@link #outputFactory} is called each time the player successfully
 * crafts the recipe, so every craft produces a fresh instance.
 * </p>
 */
public class CraftingRecipe {

  /** Display name shown in the workbench panel. */
  public final String name;

  /**
   * Ordered ingredient requirements: item-name → quantity.
   * Insertion order is preserved so the UI always lists ingredients in the
   * same order they were registered.
   */
  public final Map<String, Integer> ingredients;

  /** Produces a fresh {@link Holdable} for the player on each craft. */
  public final Supplier<Holdable> outputFactory;

  public CraftingRecipe(String name, Map<String, Integer> ingredients,
      Supplier<Holdable> outputFactory) {
    this.name = name;
    // Defensive copy, preserving insertion order.
    this.ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
    this.outputFactory = outputFactory;
  }

  /** Convenience: create a new output holdable. */
  public Holdable craft() {
    return outputFactory.get();
  }
}