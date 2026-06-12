package com.lucaslng.raft.player;

import com.lucaslng.raft.crafting.Blueprint;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.BlueprintLearnedEvent;

public class PlayerBlueprints {

	private int learned;
	static private Blueprint[] blueprints = new Blueprint[] {};
	
	PlayerBlueprints(EventBus events) {
		events.subscribe(BlueprintLearnedEvent.class, new Subscriber<BlueprintLearnedEvent>() {
			@Override
			public void accept(BlueprintLearnedEvent event) {
				learned++;
			}
		});
	}
}
