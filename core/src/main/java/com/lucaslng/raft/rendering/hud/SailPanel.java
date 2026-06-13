package com.lucaslng.raft.rendering.hud;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.lucaslng.raft.building.SailBuilding;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.event.events.SailSteerEvent;
import com.lucaslng.raft.world.World;

public class SailPanel implements Panel {

	private final SailBuilding sail;
	private final EventBus events;

	public SailPanel(SailBuilding sail, EventBus events) {
		this.sail = sail;
		this.events = events;
	}

	@Override
	public void populate(Table table, World world) {
		Skin skin = table.getSkin();

		float windDeg = SailBuilding.toDeg(sail.getWindDir());

		table.add(new Label("Wind: " + formatBearing(windDeg), skin)).row();

		Label headingLabel = new Label("Heading: " + formatBearing(sail.getSteerAngleDeg()), skin);

		table.add(headingLabel)
				.padBottom(20f)
				.row();

		Slider slider = new Slider(0f, 360f, 1f, false, skin);

		slider.setValue(sail.getSteerAngleDeg());

		slider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				float angle = slider.getValue();
				headingLabel.setText("Heading: " + formatBearing(angle));
				events.post(new SailSteerEvent(angle));
			}
		});

		table.add(slider).width(500f).padBottom(30f).row();

		TextButton close = new TextButton("Close", skin);

		close.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				events.post(new PanelOpenedEvent(null));
			}
		});

		table.add(close).width(180f).height(60f);
	}

	private static String formatBearing(float deg) {
		deg = ((deg % 360f) + 360f) % 360f;
		String[] cardinals = { "N", "NE", "E", "SE", "S", "SW", "W", "NW", "N" };
		int idx = (int) Math.round(deg / 45.0) % 8;
		return (int) deg + "°  " + cardinals[idx];
	}
}
