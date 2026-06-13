package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.WaterFilterTickEvent;

public class WaterFilter extends Building {

	/** Seconds between water drips. */
	public static final float DRIP_INTERVAL  = 5f;
	/** Fraction of thirst restored per drip [0,1]. */
	public static final float THIRST_RESTORE = 0.08f;

	private final EventBus events;
	private float timer = 0f;

	public WaterFilter(Model model, EventBus events) {
		super(new ModelInstance(model));
		this.events = events;
	}

	@Override
	public void update(float delta) {
		timer += delta;
		if (timer >= DRIP_INTERVAL) {
			timer -= DRIP_INTERVAL;
			events.post(new WaterFilterTickEvent(THIRST_RESTORE));
		}
	}

	@Override
	public String getName() {
		return "Water Filter";
	}

	// No extra resources to clean up — model is shared and disposed via Assets.
}