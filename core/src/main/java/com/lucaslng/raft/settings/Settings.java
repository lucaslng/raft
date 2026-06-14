package com.lucaslng.raft.settings;

import com.badlogic.gdx.Input.Keys;
import com.lucaslng.raft.player.Hotbar;

public class Settings {

	public final Keybind moveLeft = new Keybind(Keys.A);
	public final Keybind moveRight = new Keybind(Keys.D);
	public final Keybind moveForward = new Keybind(Keys.W);
	public final Keybind moveBack = new Keybind(Keys.S);
	public final Keybind jump = new Keybind(Keys.SPACE);
	public final Keybind sprint = new Keybind(Keys.SHIFT_LEFT);
	public final Keybind toggleInventory = new Keybind(Keys.TAB);
	public final Keybind[] hotbar = new Keybind[Hotbar.HOTBAR_SIZE];

	public float fov = 67f;
	public boolean debug = true;
	public float masterVolume = .8f;
	public float mouseSensitivity = .2f;

	private static Settings instance;

	public Settings() {
		instance = this;

		for (int i=0;i<hotbar.length;i++) {
			hotbar[i] = new Keybind(Keys.NUM_1 + i);
		}
	}

	public static Settings get() {
		return instance;
	}
	
}
