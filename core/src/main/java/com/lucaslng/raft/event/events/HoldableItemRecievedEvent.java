package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.item.HoldableItem;

public class HoldableItemRecievedEvent extends Event {

	public final HoldableItem item;
	
	public HoldableItemRecievedEvent(HoldableItem item) {
		this.item = item;
	}
}
