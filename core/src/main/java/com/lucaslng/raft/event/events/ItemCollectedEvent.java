package com.lucaslng.raft.event.events;

import com.lucaslng.raft.entity.OceanItem;
import com.lucaslng.raft.event.Event;

public class ItemCollectedEvent extends Event {

	public final OceanItem oceanItem;

	public ItemCollectedEvent(OceanItem oceanItem) {
		this.oceanItem = oceanItem;
	}
	
}
