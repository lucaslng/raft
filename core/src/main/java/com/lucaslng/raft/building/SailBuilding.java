package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.event.events.SailSteerEvent;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.rendering.hud.SailPanel;
import com.lucaslng.raft.util.Util;

// Allows player to steer raft
public class SailBuilding extends Building {

	public static final String NAME = "Sail";
	public static final float DRIFT_MULTIPLIER = 2f;

	private final RaftSystem raftSystem;
	private final Vector2 windDir;

	private final btRigidBody body;
	private final btBoxShape shape;
	private final MotionState motionState;

	private final Vector3 dims;

	private float steerAngleDeg;

	public SailBuilding(Model model, RaftSystem raftSystem, Vector2 windDir) {
		super(new ModelInstance(model));
		this.raftSystem = raftSystem;
		this.windDir = windDir;
		EventBus events = EventBus.get();

		steerAngleDeg = toDeg(windDir);

		// listen from UI
		events.subscribe(SailSteerEvent.class, e -> {
			steerAngleDeg = e.angleDegrees;
			raftSystem.setSailDirection(fromDeg(steerAngleDeg));
		});

		dims = Util.getDimensions(this.model);
		shape = new btBoxShape(dims);
		motionState = new MotionState(this.model.transform, 0f);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();
		body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
		body.setActivationState(Collision.DISABLE_DEACTIVATION);
		body.userData = this;
	}

	// ── Building lifecycle ────────────────────────────────────────────────────

	@Override
	public void setPosition(com.badlogic.gdx.math.Vector3 worldPos) {
		Vector3 p = worldPos.cpy();
		p.y += dims.y;
		super.setPosition(p);
		raftSystem.setSailMultiplier(DRIFT_MULTIPLIER);
		raftSystem.setSailDirection(fromDeg(steerAngleDeg));
	}

	@Override
	public void update(float delta) {
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void onClick(EventBus events) {
		super.onClick(events);
		events.post(new PanelOpenedEvent(new SailPanel(this, events)));
	}

	public float getSteerAngleDeg() {
		return steerAngleDeg;
	}

	public Vector2 getWindDir() {
		return windDir;
	}

	@Override
	public void dispose() {
		raftSystem.setSailMultiplier(0f);
		raftSystem.setSailDirection(new Vector2(windDir));

		body.dispose();
		shape.dispose();
		motionState.dispose();
	}

	// Converts degrees to vector
	public static Vector2 fromDeg(float deg) {
		float rad = MathUtils.degreesToRadians * deg;
		return new Vector2(MathUtils.sin(rad), MathUtils.cos(rad)).nor();
	}

	// Converts vector to degrees
	public static float toDeg(Vector2 dir) {
		float deg = MathUtils.radiansToDegrees * MathUtils.atan2(dir.x, dir.y);
		return (deg + 360f) % 360f;
	}

	@Override
	public String getInteractHint() {
		return "[RMB] Control sail";
	}
}