package com.lucaslng.raft.player;

// A stat with min, max, and changeRate
public class StatValue {

	private float current, max, changeRate, min;

	StatValue(float current, float max, float changeRate) {
		this(current, max, changeRate, 0f);
	}

	StatValue(float current, float max, float changeRate, float min) {
		this.min = min;
		this.max = max;
		this.changeRate = changeRate;
		this.current = Math.min(max, Math.max(min, current));
	}

	void update(float delta) {
		add(delta * changeRate);
	}

	void add(float amount) {
		current = Math.min(max, current + amount);
	}

	void deplete(float amount) {
		current = Math.max(min, current - amount);
	}

	// Getters

	boolean isDepleted() {
		return current <= min;
	}

	float get() {
		return current;
	}

	float getMin() {
		return min;
	}

	float getMax() {
		return max;
	}

	float getNormalized() {
		float range = max - min;
		if (range == 0f) return 0f;
		return (current - min) / range;
	}

	void setChangeRate(float rate) {
		this.changeRate = rate;
	}
}