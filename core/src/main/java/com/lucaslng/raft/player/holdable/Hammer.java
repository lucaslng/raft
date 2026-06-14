package com.lucaslng.raft.player.holdable;

import com.badlogic.gdx.math.Vector2;
import com.lucaslng.raft.settings.Settings;
import com.lucaslng.raft.world.World;

// tool to place tiles
public class Hammer extends Holdable {

	public static final String NAME = "Hammer";
	public static final int WOOD_COST = 2;

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
	public void onRightClick() {
	}

	@Override
	public void onLeftClick(World world) {
		Vector2 target = world.getGhostTarget();
		if (target == null)
			return;

		if (!Settings.get().cheats) {
			if (world.getPlayer().getBackpack().getCount("Wood") < WOOD_COST)
				return;
			world.getPlayer().getBackpack().consume("Wood", WOOD_COST);
		}

		world.getRaftSystem().placeTile(target);
		world.getRaftSystem().markDirty();
	}
}