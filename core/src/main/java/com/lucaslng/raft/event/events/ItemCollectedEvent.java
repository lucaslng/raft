package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.item.ItemStack;

public class ItemCollectedEvent extends Event {

	public final ItemStack items;

	public ItemCollectedEvent(ItemStack items) {
		this.items = items;
	}
	
}
