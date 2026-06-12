package com.lucaslng.raft.player;

import java.util.*;
import java.util.Map.Entry;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.TrashCollectedEvent;
import com.lucaslng.raft.item.*;

public class Backpack {

	private Map<Item, Integer> backpack;
	private List<Entry<Item, Integer>> sortedBackpack;

	public Backpack(EventBus events) {
		backpack = new HashMap<>();
		sortedBackpack = new ArrayList<>();

		events.subscribe(TrashCollectedEvent.class, new Subscriber<TrashCollectedEvent>() {
			@Override
			public void accept(TrashCollectedEvent event) {
				backpack.merge(event.oceanTrash.getItems().item, event.oceanTrash.getItems().quantity, Integer::sum);
				rebuildSortedView();
			}
		});

	}

	private void rebuildSortedView() {
		sortedBackpack = new ArrayList<>(backpack.entrySet());
		Collections.sort(sortedBackpack, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
	}

	public List<Entry<Item, Integer>> getSortedBackpackView() {
		return sortedBackpack.subList(0, sortedBackpack.size());
	}

}
