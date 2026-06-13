package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;

public class StatChangeEvent extends Event {

	public final float health, hunger, thirst;

	public StatChangeEvent(float health, float hunger, float thirst) {
		this.health = health;
		this.hunger = hunger;
		this.thirst = thirst;
	}
	
}
