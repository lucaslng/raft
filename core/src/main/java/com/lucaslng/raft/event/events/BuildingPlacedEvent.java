package com.lucaslng.raft.event.events;

import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.raft.RaftTile;

public class BuildingPlacedEvent extends Event {

	public final RaftTile tile;
	public final Building building;

	public BuildingPlacedEvent(RaftTile tile, Building building) {
		this.tile = tile;
		this.building = building;
	}
}