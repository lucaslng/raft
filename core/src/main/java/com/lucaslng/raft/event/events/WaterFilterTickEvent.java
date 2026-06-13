package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;

public class WaterFilterTickEvent extends Event {

	public final float thirstRestored;

	public WaterFilterTickEvent(float thirstRestored) {
		this.thirstRestored = thirstRestored;
	}
}