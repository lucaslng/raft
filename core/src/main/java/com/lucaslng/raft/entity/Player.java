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

/**
 * The player entity.
 *
 * <h3>First-person rendering</h3>
 * <p>
 * In a first-person game the player's own 3-D mesh must NOT be drawn — the
 * camera is effectively inside the model's head. {@link #getInstance()} is
 * overridden to return {@code null} so that
 * {@link com.lucaslng.raft.entity.EntitySystem}
 * skips this instance when building the opaque draw list.
 * </p>
 * <p>
 * The physics capsule/box shape (managed by {@link PlayerPhysics}) is still
 * fully active and provides correct collision response with the raft tiles,
 * shark, buildings, etc.
 * </p>
 *
 * <h3>Animation</h3>
 * <p>
 * The {@link AnimationController} is retained so animations can be triggered in
 * future (e.g. if a third-person shadow or hands are added). Each frame
 * {@link AnimationController#update(float)} is called to keep bone state
 * current.
 * </p>
 */
public class Player extends Entity {

	private final PlayerPhysics physics;
	private final PlayerStats stats;
	private final Backpack backpack;
	private final Hotbar hotbar;
	private final PlayerBlueprints blueprints;
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

	// ── Entity interface ──────────────────────────────────────────────────────

	@Override
	public void update(float delta) {
		// Keep animation state advancing (bones stay posed for future hand/shadow
		// rendering even when not shown).
		animationController.update(delta);
		stats.update(delta);
	}

	/**
	 * Returns {@code null} so that {@link EntitySystem#getInstances()} skips the
	 * player mesh in the opaque render list.
	 *
	 * <p>
	 * This is the simplest first-person hiding technique — no separate
	 * render pass, no stencil, no layer mask.
	 * </p>
	 */
	@Override
	public ModelInstance getInstance() {
		return null; // hidden in first-person — physics body still active
	}

	@Override
	public btRigidBody getBody() {
		return physics.getBody();
	}

	// ── Transform helpers ─────────────────────────────────────────────────────

	/**
	 * Syncs the model transform from the physics body so helper methods like
	 * {@link #getPosition()} return the correct value.
	 *
	 * <p>
	 * Called from {@link PlayerPhysics} via the
	 * {@link com.lucaslng.raft.physics.MotionState}
	 * each physics sub-step — no manual call needed per frame.
	 * </p>
	 */
	public void setRotation(Vector3 direction, Vector3 up) {
		transform.rotateTowardDirection(direction, up);
	}

	// ── Stat / inventory accessors ────────────────────────────────────────────

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

	// ── Clickable ─────────────────────────────────────────────────────────────

	@Override
	public void onClick(EventBus events) {
		// No interaction when clicking on oneself
	}

	@Override
	public String getInteractHint() {
		return "";
	}

	// ── Disposal ──────────────────────────────────────────────────────────────

	@Override
	public void dispose() {
		super.dispose();
		physics.dispose();
	}
}