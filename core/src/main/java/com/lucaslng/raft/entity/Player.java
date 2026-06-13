package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.physics.PlayerPhysics;
import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.player.Hotbar;
import com.lucaslng.raft.player.PlayerStats;
import com.lucaslng.raft.util.Util;

public class Player extends Entity {

	private final PlayerPhysics physics;
	private final PlayerStats stats;
	private final Backpack backpack;
	private final Hotbar hotbar;
	private final AnimationController animationController;

	public Player(Model model, Vector3 position, EventBus events) {
		super(new ModelInstance(model, position));

		Util.scaleModelInstance(this.model, .02f);

		physics = new PlayerPhysics(this.model);

		animationController = new AnimationController(this.model);

		stats = new PlayerStats(events);
		backpack = new Backpack(events);
		hotbar = new Hotbar(events);
	}

	public void update(float delta) {
		animationController.update(delta);
		stats.update(delta);
	}

	public void setRotation(Vector3 direction, Vector3 up) {
		transform.rotateTowardDirection(direction, up);
	}

	@Override
	public btRigidBody getBody() {
		return physics.getBody();
	}

	@Override
	public void dispose() {
		super.dispose();
		physics.dispose();
	}

	public PlayerStats getStats() {
		return stats;
	}

	public Backpack getBackpack() {
		return backpack;
	}

	public Hotbar getHotbar() {
		return hotbar;
	}

	@Override
	public void onClick(EventBus events) {}

	@Override
	public String getInteractHint() {
		return "";
	}
}