package com.lucaslng.raft.player.holdable;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.world.World;

public class Hammer extends Holdable {

	static private final String NAME = "Hammer";

	public Hammer() {
		super();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void onHeld() {
	}

	@Override
	public void onUnheld() {
	}

	@Override
	public void onLeftClick(World world) {
		// if (world.getHoveredEntity())

	}

	@Override
	public void onRightClick() {
	}
	
}
