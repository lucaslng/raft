package com.lucaslng.raft.input;

import com.badlogic.gdx.Input.Keys;
import com.lucaslng.raft.player.Hotbar;

public class Keybinds {

	public final Keybind moveLeft = new Keybind(Keys.A);
	public final Keybind moveRight = new Keybind(Keys.D);
	public final Keybind moveForward = new Keybind(Keys.W);
	public final Keybind moveBack = new Keybind(Keys.S);
	public final Keybind jump = new Keybind(Keys.SPACE);
	public final Keybind toggleInventory = new Keybind(Keys.TAB);

	public final Keybind[] hotbar = new Keybind[Hotbar.HOTBAR_SIZE];

	public Keybinds() {
		for (int i=0;i<hotbar.length;i++) {
			hotbar[i] = new Keybind(Keys.NUM_1 + i);
		}
	}
	
}
