package com.lucaslng.raft.player;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.PlayerDeathEvent;
import com.lucaslng.raft.event.events.StatChangeEvent;

// Manages players health, hunger, and thirst
// They update each frame, as well as through StatChangeEvent
public class PlayerStats {

	public final StatValue health, hunger, thirst;
	private final EventBus events;
	
	public PlayerStats(EventBus events) {
		this.events = events;
		health = new StatValue(1f, 1f, .001f);
		hunger = new StatValue(.9f, 1f, -.004f);
		thirst = new StatValue(.8f, 1f, -.005f);

		events.subscribe(StatChangeEvent.class, new Subscriber<StatChangeEvent>() {
			@Override
			public void accept(StatChangeEvent event) {
				health.add(event.health);
				hunger.add(event.hunger);
				thirst.add(event.thirst);
			}
		});
	}

	public void update(float delta) {
		hunger.update(delta);
		thirst.update(delta);

		if (hunger.isDepleted())
			health.deplete(.1f * delta);
		if (thirst.isDepleted())
			health.deplete(.1f * delta);

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