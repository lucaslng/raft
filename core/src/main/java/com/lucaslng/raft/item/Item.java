package com.lucaslng.raft.item;

import java.util.concurrent.atomic.AtomicInteger;

import com.badlogic.gdx.graphics.g3d.Model;

// Immutable record for an item type
// Natural sort order is alphabetical by name
public class Item implements Comparable<Item> {
	
	private static final AtomicInteger ID_GEN = new AtomicInteger();

	public final int id;
	public final String name, description;
	public final Model model;


	Item(String name, String description, Model model) {
		this.id = ID_GEN.getAndIncrement();
		this.name = name;
		this.description = description;
		this.model = model;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Item) {
			Item i = (Item) o;
			return this.id == i.id;
		}
		return false;
	}

	@Override
	public int compareTo(Item o) {
		return this.name.compareTo(o.name);
	}

}
