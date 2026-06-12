package com.lucaslng.raft.player;

public class StatValue {

	private float current, max, changeRate;

	protected StatValue(float current, float max, float changeRate) {
		this.current = current;
		this.max = max;
		this.changeRate = changeRate;
	}

	public void update(float delta) {
		add(delta * changeRate);
	}

	public void add(float amount) {
		current = Math.min(max, current + amount);
	}

	public void deplete(float amount) {
		current -= amount; // allow going below min
	}

	public boolean isDepleted() {
		return current <= 0f;
	}

	public float get() {
		return current;
	}

	public float getNormalized() {
		return current / max;
	}

}
