package com.lucaslng.raft.crafting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.lucaslng.raft.item.ItemStack;

public class Recipe {

	public final List<ItemStack> required;
	public final ItemStack output;
	
	public Recipe(ItemStack[] required, ItemStack output) {
		this.required = Collections.unmodifiableList(Arrays.asList(required));
		this.output = output;
	}

}
