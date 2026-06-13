package com.lucaslng.raft.rendering.hud;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucaslng.raft.world.World;

public interface Panel {

	public void populate(Table table, World world);
	
}
