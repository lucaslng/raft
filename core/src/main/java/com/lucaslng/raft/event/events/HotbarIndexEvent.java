package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;

public class HotbarIndexEvent extends Event {

	public final int index;

	public HotbarIndexEvent(int index) {
		this.index = index;
	}
	
}
