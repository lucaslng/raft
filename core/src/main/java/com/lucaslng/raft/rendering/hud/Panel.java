package com.lucaslng.raft.rendering.hud;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucaslng.raft.world.World;

// A Panel populates a table when opened
public interface Panel {

	public void populate(Table table, World world);
	
}
