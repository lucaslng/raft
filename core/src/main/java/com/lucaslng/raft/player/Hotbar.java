package com.lucaslng.raft.player;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.HoldableItemRecievedEvent;
import com.lucaslng.raft.event.events.HotbarIndexEvent;
import com.lucaslng.raft.player.holdable.Holdable;

public class Hotbar {
	
	public static final int HOTBAR_SIZE = 8;

	private Holdable[] hotbar;
	private int heldIndex;

	Hotbar(EventBus events) {
		hotbar = new Holdable[HOTBAR_SIZE];
		heldIndex = 0;

		events.subscribe(HoldableItemRecievedEvent.class, new Subscriber<HoldableItemRecievedEvent>() {
			@Override
			public void accept(HoldableItemRecievedEvent event) {
				hotbar[nextFreeSlot()] = event.item;
			}
		});

		events.subscribe(HotbarIndexEvent.class, new Subscriber<HotbarIndexEvent>() {
			@Override
			public void accept(HotbarIndexEvent event) {
				heldIndex = event.index;
			}
		});
	}

	public Holdable getHeldItem() {
		return hotbar[heldIndex];
	}

	private boolean isFull() {
		return nextFreeSlot() == -1;
	}

	private int nextFreeSlot() {
		for (int i=0;i<hotbar.length;i++)
			if (hotbar[i] == null)
				return i;
		return -1;
	}

	
}
