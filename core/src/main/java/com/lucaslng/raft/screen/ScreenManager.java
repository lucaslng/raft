package com.lucaslng.raft.screen;

import java.util.Stack;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.Main;

public class ScreenManager implements Disposable {
	
	private final Main game;
	private final Stack<Screen> stack;

	public ScreenManager(Main game) {
		this.game = game;
		stack = new Stack<>();
	}

	private void setScreen(Screen screen) {
		game.setScreen(screen);
	}

	public void replace(Screen screen) {
		dispose();
		stack.clear();
		stack.add(screen);
		setScreen(screen);
	}

	void push(Screen screen) {
		stack.add(screen);
		setScreen(screen);
	}

	void pop() {
		if (stack.size() <= 1)
			return;

		Screen popped = stack.pop();
		setScreen(stack.peek());
		popped.dispose();
	}

	@Override
	public void dispose() {
		for (Screen screen : stack)
			screen.dispose();
	}
	
}
