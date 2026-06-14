package com.lucaslng.raft.event;

import java.util.*;

// EventBus
public class EventBus {

	private final Map<Class<? extends Event>, List<Subscriber<Event>>> subscribers = new HashMap<>();

	private static EventBus instance;

	public EventBus() {
		instance = this;
	}

	@SuppressWarnings("unchecked")
	public <E extends Event> void subscribe(Class<E> eventType, Subscriber<E> subscriber) {
		subscribers
				.computeIfAbsent(eventType, k -> new ArrayList<>())
				.add((Subscriber<Event>) subscriber);
	}

	@SuppressWarnings("unchecked")
	public void post(Event event) {
		List<Subscriber<Event>> list = subscribers.get(event.getClass());
		if (list == null || list.isEmpty()) return;
		Subscriber<Event>[] snapshot = list.toArray(new Subscriber[0]);
		for (Subscriber<Event> s : snapshot) {
			s.accept(event);
		}
	}

	public static EventBus get() {
		return instance;
	}
}