package com.lucaslng.raft.player;

import java.util.*;
import java.util.Map.Entry;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.ItemCollectedEvent;
import com.lucaslng.raft.item.*;

public class Backpack {

	private final Map<String, Integer> counts = new HashMap<>();
	private final Map<String, Item> items = new HashMap<>();

	public Backpack(EventBus events) {
		events.subscribe(ItemCollectedEvent.class, event -> {
			ItemStack stack = event.items;
			add(stack.item, stack.quantity);
		});
	}

	public void add(Item item, int quantity) {
		counts.merge(item.name, quantity, Integer::sum);
		items.putIfAbsent(item.name, item);
	}

	public int getCount(String itemName) {
		return counts.getOrDefault(itemName, 0);
	}

	public int consume(String itemName, int quantity) {
		int have = counts.getOrDefault(itemName, 0);
		int toRemove = Math.min(have, quantity);
		if (toRemove <= 0) {
			return 0;
		}
		int remaining = have - toRemove;
		if (remaining == 0) {
			counts.remove(itemName);
		} else {
			counts.put(itemName, remaining);
		}
		return toRemove;
	}

	public Iterable<Entry<Item, Integer>> getSortedBackpackView() {
		List<Entry<Item, Integer>> list = new ArrayList<>();
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			list.add(Map.entry(items.get(e.getKey()), e.getValue()));
		}
		list.sort(new Comparator<Entry<Item, Integer>>() {
			@Override
			public int compare(Entry<Item, Integer> a, Entry<Item, Integer> b) {
				return b.getValue() - a.getValue();
			}
		});
		return list;
	}
}