package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.raft.RaftTile;

public class TilePlacedEvent extends Event {

	public final RaftTile tile;

	public TilePlacedEvent(RaftTile tile) {
		this.tile = tile;
	}
}