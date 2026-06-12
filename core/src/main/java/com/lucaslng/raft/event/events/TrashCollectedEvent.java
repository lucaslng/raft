package com.lucaslng.raft.event.events;

import com.lucaslng.raft.entity.OceanTrash;
import com.lucaslng.raft.event.Event;

public class TrashCollectedEvent extends Event {

	public final OceanTrash oceanTrash;

	public TrashCollectedEvent(OceanTrash oceanTrash) {
		this.oceanTrash = oceanTrash;
	}
	
}
