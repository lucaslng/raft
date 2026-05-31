package com.lucaslng.raft.item;

public class Item {
	
	public final int id, stackSize;
	public final String name, description;

	private static int idCounter = 0;

	public Item(String name, String description, int stackSize) {
		this.id = idCounter++;
		this.name = name;
		this.description = description;
		this.stackSize = stackSize;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Item) {
			Item i = (Item) o;
			return this.id == i.id;
		}
		return false;
	}

}
