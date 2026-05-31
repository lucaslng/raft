package com.lucaslng.raft.event;

import java.util.*;

public class EventBus {

	private Map<Class<? extends Event>, List<Subscriber<Event>>> subscribers;

	public EventBus() {
		subscribers = new HashMap<>();
	}

	public <E extends Event> void subscribe(Class<E> eventType, Subscriber<E> observer) {
		subscribers.computeIfAbsent(eventType, k -> new LinkedList<>()).add((Subscriber<Event>) observer);
	}

	public void post(Event event) {
		if (subscribers.containsKey(event.getClass()))
			for (Subscriber<Event> subscriber : subscribers.get(event.getClass()))
				subscriber.accept(event);
	}
}
