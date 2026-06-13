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
import com.lucaslng.raft.event.events.BuildingClickedEvent;
import com.lucaslng.raft.event.events.SailSteerEvent;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.raft.RaftSystem;
import com.lucaslng.raft.util.Util;

/**
 * A sail that allows the player to steer the raft independently of the wind.
 *
 * <h3>Drift behaviour</h3>
 * <ul>
 * <li>When placed the sail registers a drift multiplier with
 * {@link RaftSystem},
 * enabling raft movement.</li>
 * <li>By default the raft drifts with the wind. The player can open the sail
 * UI (right-click) and pick a custom heading; this overrides the wind
 * direction via {@link RaftSystem#setSailDirection(Vector2)}.</li>
 * <li>When the sail is destroyed the raft stops drifting and the sail direction
 * is reset to wind direction.</li>
 * </ul>
 *
 * <h3>Steering</h3>
 * <p>
 * {@link SailSteerEvent} carries an angle in degrees (0 = +Z / north,
 * clockwise). The sail converts this to a normalised {@link Vector2} and pushes
 * it to the {@link RaftSystem}.
 * </p>
 *
 * <h3>Registration</h3>
 * 
 * <pre>
 * register("Sail", () -> new SailBuilding(sailModel, raftSystem, windDir, events),
 * 		Map.of("String", 6, "Wood", 8));
 * </pre>
 */
public class SailBuilding extends Building {

	public static final String NAME = "Sail";
	/**
	 * Base drift speed in world-units / second (multiplied by RaftSystem speed).
	 */
	public static final float DRIFT_MULTIPLIER = 2f;

	private final RaftSystem raftSystem;
	private final Vector2 windDir;

	private final btRigidBody body;
	private final btBoxShape shape;
	private final MotionState motionState;

	private final Vector3 dims;

	/** Current steering angle, degrees clockwise from +Z. Default = wind angle. */
	private float steerAngleDeg;

	public SailBuilding(Model model, RaftSystem raftSystem, Vector2 windDir, EventBus events) {
		super(new ModelInstance(model));
		this.raftSystem = raftSystem;
		this.windDir = windDir;

		// Default heading follows the wind
		steerAngleDeg = toDeg(windDir);

		// Listen for steering updates from the UI
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

	/**
	 * Opens the sail steering UI.
	 */
	@Override
	public void onClick(EventBus events) {
		events.post(new BuildingClickedEvent(this));
	}

	/** Returns the current steering angle so the UI can initialise correctly. */
	public float getSteerAngleDeg() {
		return steerAngleDeg;
	}

	/** Returns the wind direction so the UI can show it as a reference. */
	public Vector2 getWindDir() {
		return windDir;
	}

	@Override
	public void dispose() {
		raftSystem.setSailMultiplier(0f);
		// Reset drift direction back to wind
		raftSystem.setSailDirection(new Vector2(windDir));

		body.dispose();
		shape.dispose();
		motionState.dispose();
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	/**
	 * Converts an angle in degrees (CW from +Z) to a normalised XZ Vector2.
	 * +Z = 0°, +X = 90°, -Z = 180°, -X = 270°.
	 */
	public static Vector2 fromDeg(float deg) {
		float rad = MathUtils.degreesToRadians * deg;
		return new Vector2(MathUtils.sin(rad), MathUtils.cos(rad)).nor();
	}

	/**
	 * Converts a normalised XZ direction to degrees clockwise from +Z.
	 */
	public static float toDeg(Vector2 dir) {
		float deg = MathUtils.radiansToDegrees * MathUtils.atan2(dir.x, dir.y);
		return (deg + 360f) % 360f;
	}
}