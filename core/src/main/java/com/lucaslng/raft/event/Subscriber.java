package com.lucaslng.raft.event;

public interface Subscriber<E extends Event> {
	public void accept(E event);
}
