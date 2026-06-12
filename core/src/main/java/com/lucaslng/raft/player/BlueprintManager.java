package com.lucaslng.raft.player;

import java.util.HashSet;
import java.util.Set;

import com.lucaslng.raft.crafting.Blueprint;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.BlueprintLearnedEvent;

public class BlueprintManager {

	private final Set<Blueprint> learned;
	
	public BlueprintManager(EventBus events) {
		learned = new HashSet<>();

		events.subscribe(BlueprintLearnedEvent.class, new Subscriber<BlueprintLearnedEvent>() {
			@Override
			public void accept(BlueprintLearnedEvent event) {
				learned.add(event.blueprint);
			}
		});
	}

	public boolean hasLearned(Blueprint blueprint) {
		return learned.contains(blueprint);
	}
}
