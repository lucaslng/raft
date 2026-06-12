package com.lucaslng.raft.event.events;

import com.lucaslng.raft.crafting.Blueprint;
import com.lucaslng.raft.event.Event;

public class BlueprintLearnedEvent extends Event {
	
	public final Blueprint blueprint;

	public BlueprintLearnedEvent(Blueprint blueprint) {
		this.blueprint = blueprint;
	}
	
}
