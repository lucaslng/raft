package com.lucaslng.raft.entity;

import com.badlogic.gdx.math.Vector2;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.ItemCollectedEvent;
import com.lucaslng.raft.item.ItemStack;

public class OceanItem extends OceanTrash {

	private final ItemStack items;

	public OceanItem(ItemStack itemStack, Vector2 position, Vector2 windDir) {
		super(itemStack.item.model, position, windDir);
		this.items = itemStack;
	}

	public ItemStack getItems() {
		return items;
	}

	@Override
	public void onClick(EventBus events) {
		super.onClick(events);
		events.post(new ItemCollectedEvent(this));
	}
	
}
