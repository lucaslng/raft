package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.player.holdable.Holdable;

public class HoldableItemRecievedEvent extends Event {

	public final Holdable item;
	
	public HoldableItemRecievedEvent(Holdable item) {
		this.item = item;
	}
}
