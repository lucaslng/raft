package com.lucaslng.raft.item;

import java.util.HashMap;

import com.badlogic.gdx.graphics.g3d.Model;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.util.Util;


// registry of item types
public class ItemRegistry {

	private final HashMap<String, Item> registry = new HashMap<>();

	private static ItemRegistry instance;

	public ItemRegistry() {
		instance = this;
		Assets assets = Assets.get();
		put("Wood", "A floating wood plank.", assets.get("models/debris-wood.g3db", Model.class));
		put("Stone", "A few rocks", assets.get("models/debris-stone.g3db", Model.class));

		Model stringModel = assets.get("models/string.g3db", Model.class);
		Util.scaleModel(stringModel, .4f);
		put("String", "A short piece of rope", stringModel);
		put("Cauliflower", "A raw, dirty piece of cauliflower", assets.get("models/cauliflower.g3db"));
	}

	private void put(String name, String description, Model model) {
		registry.put(name, new Item(name, description, model));
	}

	public Item getItem(String itemName) {
		return registry.get(itemName);
	}

	public static ItemRegistry get() {
		return instance;
	}
	
}
