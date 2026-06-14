package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.lucaslng.raft.crafting.CraftingRegistry;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.physics.PlayerPhysics;
import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.player.Hotbar;
import com.lucaslng.raft.player.PlayerBlueprints;
import com.lucaslng.raft.player.PlayerStats;
import com.lucaslng.raft.util.Util;

// Player class, manages classes which have different responsibilities for the player
public class Player extends Entity {

	private final PlayerPhysics physics;
	private final PlayerStats stats;
	private final Backpack backpack;
	private final Hotbar hotbar;
	private final PlayerBlueprints blueprints;

	// currently no animations, but in the future animations could be added
	private final AnimationController animationController;

	public Player(Model model, Vector3 position, EventBus events,
			CraftingRegistry craftingRegistry) {
		super(new ModelInstance(model, position));

		Util.scaleModelInstance(this.model, .02f);

		physics = new PlayerPhysics(this.model);

		animationController = new AnimationController(this.model);

		stats = new PlayerStats(events);
		backpack = new Backpack(events);
		hotbar = new Hotbar(events);
		blueprints = new PlayerBlueprints(events, craftingRegistry);
	}

	@Override
	public void update(float delta) {
		animationController.update(delta);
		stats.update(delta);
	}
	
	// don't render player, hide it
	@Override
	public ModelInstance getInstance() {
		return null;
	}

	@Override
	public btRigidBody getBody() {
		return physics.getBody();
	}

	public void setRotation(Vector3 direction, Vector3 up) {
		transform.rotateTowardDirection(direction, up);
	}

	// Getters

	public PlayerStats getStats() {
		return stats;
	}

	public Backpack getBackpack() {
		return backpack;
	}

	public Hotbar getHotbar() {
		return hotbar;
	}

	public PlayerBlueprints getBlueprints() {
		return blueprints;
	}

	@Override
	public void onClick(EventBus events) {
	}

	@Override
	public String getInteractHint() {
		return "";
	}

	@Override
	public void dispose() {
		super.dispose();
		physics.dispose();
	}
}