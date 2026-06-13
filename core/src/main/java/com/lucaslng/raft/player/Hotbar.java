package com.lucaslng.raft.player;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.HoldableItemRecievedEvent;
import com.lucaslng.raft.event.events.HotbarIndexEvent;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.WaterFilterTool;

public class Hotbar {
	
	public static final int HOTBAR_SIZE = 8;

	private final Holdable[] hotbar;
	private int heldIndex;

	public Hotbar(EventBus events) {
		hotbar = new Holdable[HOTBAR_SIZE];
		heldIndex = 0;

		// TODO: remove water filter
		hotbar[0] = new Hammer();
		hotbar[1] = new WaterFilterTool();

		events.subscribe(HoldableItemRecievedEvent.class, new Subscriber<HoldableItemRecievedEvent>() {
			@Override
			public void accept(HoldableItemRecievedEvent event) {
				int slot = nextFreeSlot();
				if (slot >= 0) hotbar[slot] = event.item;
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

	public int getHeldIndex() {
		return heldIndex;
	}

	public Holdable getItem(int index) {
		return (index >= 0 && index < HOTBAR_SIZE) ? hotbar[index] : null;
	}

	private int nextFreeSlot() {
		// Skip slots 0 and 1 which are reserved for Hammer and WaterFilterTool
		for (int i = 2; i < hotbar.length; i++)
			if (hotbar[i] == null)
				return i;
		return -1;
	}
}