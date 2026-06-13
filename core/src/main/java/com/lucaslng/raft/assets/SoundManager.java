package com.lucaslng.raft.assets;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.BuildingPlacedEvent;
import com.lucaslng.raft.event.events.TilePlacedEvent;

public class SoundManager {

	private final Assets assets;
	private final EventBus events;

	public SoundManager() {
		assets = Assets.get();
		events = EventBus.get();

		Music music = assets.get("music/The Pirate's Waltz.mp3");
		music.setVolume(.2f * master());
		music.setLooping(true);
		music.play();

		sfx(TilePlacedEvent.class, "tile-placed.mp3", .9f);
		sfx(BuildingPlacedEvent.class, "building-placed.mp3", .9f);
	}

	private void sfx(Class<? extends Event> eventType, String fileName, float volume) {
		Sound sound = assets.get("sfx/" + fileName);
		events.subscribe(eventType, e -> sound.play(volume * master()));
	}


	private float master() {
		return 1f;
	}
	
}
