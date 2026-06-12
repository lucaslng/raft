package com.lucaslng.raft.player;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.PlayerDeathEvent;

public class PlayerStats {

	public final StatValue health, hunger, thirst;
	private final EventBus events;
	
	public PlayerStats(EventBus events) {
		this.events = events;
		health = new StatValue(1f, 1f, .001f);
		hunger = new StatValue(.9f, 1f, -.004f);
		thirst = new StatValue(.8f, 1f, -.005f);
	}

	public void update(float delta) {
		hunger.update(delta);
		thirst.update(delta);

		if (hunger.isDepleted())
			health.deplete(.05f);
		if (thirst.isDepleted())
			health.deplete(.05f);

		if (health.isDepleted()) {
			events.post(new PlayerDeathEvent());
			return;
		}

		health.update(delta);
	}

	public void eat(float restoredHunger) {
		hunger.add(restoredHunger);
	}

	public void drink(float restoredThirst) {
		thirst.add(restoredThirst);
	}

	public void damage(float damageTaken) {
		health.deplete(damageTaken);
	}

	public float getHealth() {
		return health.get();
	}
	
	public float getHunger() {
		return hunger.get();
	}

	public float getThirst() {
		return thirst.get();
	}
}
