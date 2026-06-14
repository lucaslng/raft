package com.lucaslng.raft.screen;

import java.util.Stack;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.Main;

// manages a Stack of screens
// Singleton
public class ScreenManager implements Disposable {
	
	private final Main game;
	private final Stack<Screen> stack;

	private static ScreenManager instance;

	public ScreenManager(Main game) {
		instance = this;
		
		this.game = game;
		stack = new Stack<>();
	}

	private void setScreen(Screen screen) {
		game.setScreen(screen);
	}

	// resets the stack with a new screen
	public void replace(Screen screen) {
		dispose();
		stack.clear();
		stack.add(screen);
		setScreen(screen);
	}

	// adds to the top of the stack
	void push(Screen screen) {
		stack.add(screen);
		setScreen(screen);
	}

	// removes the top of the stack
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

	public static ScreenManager get() {
		return instance;
	}
	
}
