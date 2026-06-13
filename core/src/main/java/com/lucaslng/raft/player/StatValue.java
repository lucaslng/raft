package com.lucaslng.raft.player;

/**
 * A bounded float stat (e.g. health, hunger, thirst).
 */
public class StatValue {

	private float current, max, changeRate, min;

	public StatValue(float current, float max, float changeRate) {
		this(current, max, changeRate, 0f);
	}

	public StatValue(float current, float max, float changeRate, float min) {
		this.min = min;
		this.max = max;
		this.changeRate = changeRate;
		this.current = Math.min(max, Math.max(min, current));
	}

	public void update(float delta) {
		add(delta * changeRate);
	}

	public void add(float amount) {
		current = Math.min(max, current + amount);
	}

	/** Drains the stat. Will not go below {@code min}. */
	public void deplete(float amount) {
		current = Math.max(min, current - amount);
	}

	public boolean isDepleted() {
		return current <= min;
	}

	public float get() {
		return current;
	}

	public float getMin() {
		return min;
	}

	public float getMax() {
		return max;
	}

	public float getNormalized() {
		float range = max - min;
		if (range == 0f) return 0f;
		return (current - min) / range;
	}

	public void setChangeRate(float rate) {
		this.changeRate = rate;
	}
}