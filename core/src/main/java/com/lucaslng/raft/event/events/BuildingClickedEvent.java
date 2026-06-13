package com.lucaslng.raft.event.events;

import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.event.Event;

public class BuildingClickedEvent extends Event {

	public final Building building;

	public BuildingClickedEvent(Building building) {
		this.building = building;
	}
}