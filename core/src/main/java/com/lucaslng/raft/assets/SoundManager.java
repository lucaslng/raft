package com.lucaslng.raft.assets;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.lucaslng.raft.event.Event;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.*;

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
		sfx(HoldableItemRecievedEvent.class, "holdable-recieved.mp3", 1f);
		sfx(TrashCollectedEvent.class, "trash-collected.mp3", .9f);
		events.subscribe(StatChangeEvent.class, new Subscriber<StatChangeEvent>() {
			Sound damaged = getSfx("damaged.mp3");
			Sound eat = getSfx("eat.mp3");
			Sound drink = getSfx("drink.mp3");
			@Override
			public void accept(StatChangeEvent event) {
				if (event.health < 0f)
					damaged.play(1f * master());
				if (event.hunger > 0f)
					eat.play(1f * master());
				if (event.thirst > 0f)
					drink.play(1f * master());
			}
		});
	}

	private void sfx(Class<? extends Event> eventType, String fileName, float volume) {
		Sound sound = getSfx(fileName);
		events.subscribe(eventType, e -> sound.play(volume * master()));
	}

	private Sound getSfx(String fileName) {
		return assets.get("sfx/" + fileName);
	}


	private float master() {
		return 1f;
	}
	
}
