package com.lucaslng.raft.player;

import java.util.*;
import java.util.Map.Entry;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.ItemCollectedEvent;
import com.lucaslng.raft.item.*;

public class Backpack {

	private final Map<Item, Integer> backpack;
	private final Map<String, Item> nameIndex;

	private List<Entry<Item, Integer>> sortedBackpack;

	public Backpack(EventBus events) {
		backpack = new TreeMap<>();
		nameIndex = new HashMap<>();
		sortedBackpack = new ArrayList<>();

		events.subscribe(ItemCollectedEvent.class, new Subscriber<ItemCollectedEvent>() {
			@Override
			public void accept(ItemCollectedEvent event) {
				ItemStack items = event.oceanItem.getItems();
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
		sortedBackpack = new ArrayList<>(backpack.entrySet());
		Collections.sort(sortedBackpack, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
	}

	public Iterable<Entry<Item, Integer>> getSortedBackpackView() {
		return sortedBackpack;
	}
}