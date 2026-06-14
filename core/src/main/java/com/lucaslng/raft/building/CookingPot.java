package com.lucaslng.raft.building;

import java.util.Set;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.event.events.StatChangeEvent;
import com.lucaslng.raft.item.Item;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.rendering.hud.CookingPanel;
import com.lucaslng.raft.util.Util;

public class CookingPot extends Building {

	public static final float COOK_TIME = 10f;
	public static final float REPLENISH_AMOUNT = .2f;
	public static final Set<String> cookableItems = Set.of("Cauliflower");
	public static enum CookingState {
		EMPTY, COOKING, DONE
	}

	private final btRigidBody body;
	private final btBoxShape shape;
	private final MotionState motionState;
	
	private float timer = 0f;
	private Item currentItem = null;
	private CookingState state = CookingState.EMPTY;

	protected CookingPot(Model model) {
		super(new ModelInstance(model));

		Vector3 dimensions = Util.getDimensions(this.model);
		shape = new btBoxShape(dimensions);
		motionState = new MotionState(this.model.transform, dimensions.y);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
		body.setActivationState(Collision.DISABLE_DEACTIVATION);
		body.userData = this;
	}

	static public boolean isCookable(Item item) {
		return cookableItems.contains(item.name);
	}

	public void cook(Item item) {
		if (!isCookable(item))
			throw new IllegalArgumentException();

		currentItem = item;
		timer = 0f;
		state = CookingState.COOKING;
	}

	public void collect() {
		currentItem = null;
		state = CookingState.EMPTY;
		EventBus.get().post(new StatChangeEvent(0f, REPLENISH_AMOUNT, 0f));
	}

	public Item getCurrentItem() {
		return currentItem;
	}

	public CookingState getState() {
		return state;
	}

	public float getProgress() {
		return timer / COOK_TIME;
	}

	@Override
	public void onClick(EventBus events) {
		super.onClick(events);
		events.post(new PanelOpenedEvent(new CookingPanel(this, events)));
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	@Override
	public String getInteractHint() {
		switch (state) {
			case EMPTY:
				return "[RMB] Cook food";
			case COOKING:
				return "[RMB] Cooking...";
			case DONE:
				return "[RMB] Collect food";
		}
		return "[RMB] Open cooking pot";
	}

	@Override
	public void update(float delta) {
		if (state == CookingState.COOKING) {
			timer += delta;
			if (timer >= COOK_TIME)
				state = CookingState.DONE;
		}
	}

	@Override
	public String getName() {
		return "Cooking Pot";
	}

	@Override
	public void dispose() {
		body.dispose();
		motionState.dispose();
		shape.dispose();
	}

}
