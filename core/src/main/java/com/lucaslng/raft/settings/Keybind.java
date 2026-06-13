package com.lucaslng.raft.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class Keybind {

	private final int defaultKey;
	private int key;

	Keybind(int defaultKey) {
		this.defaultKey = defaultKey;
		this.key = defaultKey;
	}

	public void reset() {
		key = defaultKey;
	}

	/** Rebind this keybind to a new key. */
	public void setKey(int newKey) {
		key = newKey;
	}

	/** Returns the current keycode. */
	public int getKey() {
		return key;
	}

	public boolean isPressed() {
		return Gdx.input.isKeyPressed(key);
	}

	public boolean isKeyJustPressed() {
		return Gdx.input.isKeyJustPressed(key);
	}

	@Override
	public String toString() {
		return Input.Keys.toString(key);
	}

}