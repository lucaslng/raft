package com.lucaslng.raft.event;


// functional interface for EventBus subscribers
public interface Subscriber<E extends Event> {
	public void accept(E event);
}
