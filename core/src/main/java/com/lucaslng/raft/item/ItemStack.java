package com.lucaslng.raft.item;


// Immutable pair of item type and quantity (must be positive)
public class ItemStack {
	
	public final Item item;
	public final int quantity;

	public ItemStack(Item item, int quantity) {
		if (quantity <= 0)
			throw new IllegalArgumentException("Item quantity cannot be 0 or less.");

		this.item = item;
		this.quantity = quantity;
	}
}
