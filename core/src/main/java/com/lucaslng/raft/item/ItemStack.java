package com.lucaslng.raft.item;

public class ItemStack {
	private final Item item;
	private int quantity;

	public ItemStack(Item item, int quantity) {
		this.item = item;
		this.quantity = quantity;
	}

	public void add(int quantity) {
		this.quantity += quantity; // TODO: check overadd / oversub
	}

	public void subtract(int quantity) {
		this.quantity -= quantity;
	}

	// Getters

	public Item getItem() {
		return item;
	}

	public int getQuantity() {
		return quantity;
	}
}
