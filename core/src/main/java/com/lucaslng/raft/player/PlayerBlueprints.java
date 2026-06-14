package com.lucaslng.raft.player;

import com.lucaslng.raft.crafting.CraftingRegistry;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.BlueprintLearnedEvent;

// Tracks how many blueprints the player has collected and forwards each pickup
public class PlayerBlueprints {

	private int learned;

	public PlayerBlueprints(EventBus events, CraftingRegistry craftingRegistry) {
		events.subscribe(BlueprintLearnedEvent.class, new Subscriber<BlueprintLearnedEvent>() {
			@Override
			public void accept(BlueprintLearnedEvent event) {
				learned++;
				craftingRegistry.onBlueprintCollected();
			}
		});
	}

	public int getLearned() {
		return learned;
	}
}