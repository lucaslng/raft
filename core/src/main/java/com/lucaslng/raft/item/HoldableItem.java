package com.lucaslng.raft.item;

// Buildings, tools, and other misc stuff
public class HoldableItem {

	private final Item item;
	
	public HoldableItem(Item item) {
		if (item.type != ItemType.HOLDABLE)
			throw new IllegalArgumentException("Item is not holdable!");

		this.item = item;
	}

	// Getters

	public Item getItem() {
		return item;
	}
}
