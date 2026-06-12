package com.lucaslng.raft.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.physics.PlayerPhysics;
import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.player.PlayerStats;
import com.lucaslng.raft.util.Util;

public class Player extends Entity {

	private final PlayerPhysics physics;
	private final PlayerStats stats;
	private final Backpack backpack;
	private final AnimationController animationController;

	public Player(Model model, Vector3 position, EventBus events) {
		super(new ModelInstance(model, position));

		Util.scaleModelInstance(this.model, .02f);

		physics = new PlayerPhysics(this.model);

		animationController = new AnimationController(this.model);
		// animationController.setAnimation("walk", -1);

		stats = new PlayerStats(events);
		backpack = new Backpack(events);
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

	@Override
	public void onClicked(EventBus events) {}
	
}


// Animations

// idle
// walk
// sprint
// sit
// drive
// die
// pick-up
// emote-yes
// emote-no
// holding-right
// holding-left
// holding-both
// holding-right-shoot
// holding-left-shoot
// holding-both-shoot
// attack-melee-right
// attack-melee-left
// attack-kick-right
// attack-kick-left
// interact-right
// interact-left
// wheelchair-sit
// wheelchair-move-forward
// wheelchair-move-back
// wheelchair-move-left
// wheelchair-move-right