package com.lucaslng.raft.world;

import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.raft.Raft;

public class World {

	private EventBus events;
	private Raft raft;

	public Raft getRaft() {
		return raft;
	}

	public World() {
		events = new EventBus();
		raft = new Raft();
	}
	
	public void update(float delta) {

	}

	

}
