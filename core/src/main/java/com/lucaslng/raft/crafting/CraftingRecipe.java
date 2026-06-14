package com.lucaslng.raft.crafting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.lucaslng.raft.player.holdable.Holdable;

// stores required items -> holdable item factory
public class CraftingRecipe {

  public final String name;

  public final Map<String, Integer> ingredients;

  public final Supplier<Holdable> outputFactory;

  public CraftingRecipe(String name, Map<String, Integer> ingredients,
      Supplier<Holdable> outputFactory) {
    this.name = name;
    this.ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
    this.outputFactory = outputFactory;
  }

  public Holdable craft() {
    return outputFactory.get();
  }
}