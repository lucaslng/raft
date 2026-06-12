package com.lucaslng.raft.input;

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
