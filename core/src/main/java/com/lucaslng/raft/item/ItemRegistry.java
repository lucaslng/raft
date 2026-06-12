package com.lucaslng.raft.item;

import java.util.HashMap;

import com.badlogic.gdx.graphics.g3d.Model;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.util.Util;

public class ItemRegistry {

	private final HashMap<String, Item> registry;

	public ItemRegistry(Assets assets) {
		registry = new HashMap<>();
		registry.put("Wood", new Item("Wood", "A floating wood plank.", assets.get("models/debris-wood.g3db", Model.class)));
		registry.put("Stone", new Item("Stone", "A few rocks", assets.get("models/debris-stone.g3db", Model.class)));

		Model stringModel = assets.get("models/string.g3db", Model.class);
		Util.scaleModel(stringModel, .4f);
		registry.put("String", new Item("String", "A short piece of rope", stringModel));
	}

	public Item get(String itemName) {
		return registry.get(itemName);
	}
	
}
