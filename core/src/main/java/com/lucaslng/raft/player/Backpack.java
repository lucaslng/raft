package com.lucaslng.raft.player;

import java.util.*;
import java.util.Map.Entry;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.ItemCollectedEvent;
import com.lucaslng.raft.item.*;

// Stores players stackable items
public class Backpack {

	private final Map<Item, Integer> backpack = new HashMap<>();
	private final Map<String, Item> nameIndex = new HashMap<>();
	private final List<Entry<Item, Integer>> sortedBackpack = new ArrayList<>();

	public Backpack(EventBus events) {
		events.subscribe(ItemCollectedEvent.class, new Subscriber<ItemCollectedEvent>() {
			@Override
			public void accept(ItemCollectedEvent event) {
				ItemStack items = event.items;
				add(items.item, items.quantity);
			}
		});
	}

	public void add(Item item, int quantity) {
		backpack.merge(item, quantity, Integer::sum);
		nameIndex.putIfAbsent(item.name, item);
		rebuildSortedView();
	}

	public int getCount(String itemName) {
		Item item = nameIndex.get(itemName);
		if (item == null)
			return 0;
		return backpack.getOrDefault(item, 0);
	}

	// Consume items, returns actual items consumed
	public int consume(String itemName, int quantity) {
		Item item = nameIndex.get(itemName);
		if (item == null)
			return 0;
		int have = backpack.getOrDefault(item, 0);
		int toRemove = Math.min(have, quantity);
		if (toRemove <= 0)
			return 0;
		int remaining = have - toRemove;
		if (remaining == 0) {
			backpack.remove(item);
		} else {
			backpack.put(item, remaining);
		}
		rebuildSortedView();
		return toRemove;
	}

	private void rebuildSortedView() {
		sortedBackpack.clear();
		sortedBackpack.addAll(backpack.entrySet());
		Collections.sort(sortedBackpack, new Comparator<Entry<Item, Integer>>() {
			@Override
			public int compare(Entry<Item, Integer> a, Entry<Item, Integer> b) {
				return b.getValue() - a.getValue();
			}
		});
	}

	public Iterable<Entry<Item, Integer>> getSortedBackpackView() {
		return sortedBackpack;
	}
}