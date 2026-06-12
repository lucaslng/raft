package com.lucaslng.raft.player.holdable;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.lucaslng.raft.world.World;

// Buildings, tools, and other misc stuff
public abstract class Holdable {

	public Holdable() {
	}

	abstract public String getName();

	abstract public void onHeld();

	abstract public void onUnheld();


	abstract public void onRightClick();

	abstract public void onLeftClick(World world);
	
}
