package com.lucaslng.raft.player.holdable;

import com.lucaslng.raft.world.World;

// Buildings, tools, and other misc stuff
// Defines callbacks
public abstract class Holdable {

	public Holdable() {
	}

	abstract public String getName();

	abstract public void onHeld();

	abstract public void onUnheld();

	abstract public void onRightClick();

	abstract public void onLeftClick(World world);
	
}
