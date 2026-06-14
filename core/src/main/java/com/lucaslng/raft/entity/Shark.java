package com.lucaslng.raft.entity;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

/**
 * The shark lurks underwater below the raft, slowly circling.
 * When the player submerges below the water surface it switches to CHASING,
 * homing in on the player. On physical contact it posts a damage event.
 *
 * <h3>Rotation fix</h3>
 * <p>
 * {@link AnimationController#update(float)} internally calls
 * {@code ModelInstance.calculateTransforms()}, which rebuilds
 * {@code instance.transform} from the node hierarchy — wiping any rotation
 * previously written to {@code transform}. To work around this:
 * <ol>
 * <li>We call {@code animationController.update(delta)} first (so bones are
 * posed correctly).</li>
 * <li>Then we overwrite {@code instance.transform} with a fresh
 * translation × rotateY matrix built from our stored heading angle.</li>
 * </ol>
 * Using {@code Matrix4.setToTranslation(...).rotate(Vector3.Y, degrees)} is
 * correct here because it orients the model's local +Z axis toward the travel
 * direction via a simple Y-axis yaw — no camera-convention inversion needed.
 * {@code Matrix4.setToLookAt()} is a view matrix and produces the inverse
 * rotation, so it is intentionally avoided.
 * </p>
 *
 * <h3>Collision flags</h3>
 * The shark body carries {@code CF_CUSTOM_MATERIAL_CALLBACK} so that the global
 * {@link ContactListener} registered in
 * {@link com.lucaslng.raft.physics.PhysicsSystem} can detect shark–player
 * contact and post damage via
 * {@link com.lucaslng.raft.event.events.StatChangeEvent}.
 */
public class Shark extends Entity {


	private static final float LURK_DEPTH = -4f;
	private static final float LURK_RADIUS = 6f;
	private static final float LURK_SPEED = 0.4f;
	private static final float CHASE_SPEED = 6f;
	private static final float CHASE_Y_TRIGGER = -0.3f;
	private static final float RETURN_Y_TRIGGER = 0.5f;

	// ── Contact-callback identifier ──────────────────────────────────────────

	/**
	 * Bit flag for the Bullet contact-callback system.
	 * Set on both the shark body (callbackFlag + callbackFilter) and the
	 * player body (callbackFlag + callbackFilter) in
	 * {@link com.lucaslng.raft.physics.PlayerPhysics}.
	 */
	public static final int FLAG = 1 << 2;

	// ── State ────────────────────────────────────────────────────────────────

	private enum State {
		LURKING, CHASING
	}

	private State state = State.LURKING;

	/** Angle (radians) used to trace the lurk circle. */
	private float lurkAngle = 0f;

	/**
	 * Current heading in degrees (Y-axis rotation applied to the model).
	 * Stored persistently so we can reapply it after AnimationController
	 * overwrites instance.transform each frame.
	 */
	private float headingDeg = 0f;

	/** Current world-space position of the shark. */
	private final Vector3 position = new Vector3();

	/** Centre of the lurk circle — tracks the raft origin. */
	private final Vector3 lurkCenter = new Vector3();

	private final AnimationController animationController;
	private final btRigidBody body;
	private final List<Disposable> disposables;

	// Scratch — never stored across frames.
	private final Vector3 _scratch = new Vector3();

	// ── Constructor ──────────────────────────────────────────────────────────

	public Shark(Model model, Vector3 spawnPosition) {
		super(new ModelInstance(model));
		disposables = new ArrayList<>();

		Util.scaleModelInstance(this.model, 1f);

		position.set(spawnPosition);
		lurkCenter.set(spawnPosition.x, LURK_DEPTH, spawnPosition.z);

		// Physics body — kinematic so we drive it ourselves.
		Vector3 dimensions = Util.getDimensions(this.model);
		btBoxShape shape = new btBoxShape(dimensions);
		MotionState motionState = new MotionState(transform, 0f);
		btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0f, motionState, shape);
		body = new btRigidBody(info);
		info.dispose();

		body.setCollisionFlags(
				body.getCollisionFlags()
						| btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT
						| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		body.setActivationState(Collision.DISABLE_DEACTIVATION);

		body.setContactCallbackFlag(FLAG);
		body.setContactCallbackFilter(FLAG);

		body.userData = this;

		animationController = new AnimationController(this.model);
		animationController.setAnimation("Armature|Swim", -1);

		disposables.add(body);
		disposables.add(shape);
		disposables.add(motionState);

		// Apply initial transform.
		applyTransform();
	}

	// ── Public update (called by World with full context) ────────────────────

	/**
	 * Full AI update — called explicitly from
	 * {@link com.lucaslng.raft.world.World#update} every frame.
	 *
	 * @param delta      frame delta in seconds
	 * @param playerPos  current world-space player position
	 * @param raftOrigin world-space XZ origin of the raft
	 */
	public void update(float delta, Vector3 playerPos, Vector2 raftOrigin) {
		// 1. Advance bone animation — this internally calls calculateTransforms()
		// which resets instance.transform to the node hierarchy root.
		animationController.update(delta);

		// 2. Update lurk centre to track the drifting raft.
		lurkCenter.set(raftOrigin.x, LURK_DEPTH, raftOrigin.y);

		// 3. State transitions.
		if (state == State.LURKING && playerPos.y < CHASE_Y_TRIGGER) {
			state = State.CHASING;
		} else if (state == State.CHASING && playerPos.y > RETURN_Y_TRIGGER) {
			state = State.LURKING;
		}

		// 4. Move + compute heading.
		switch (state) {
			case LURKING:
				lurkMovement(delta);
				break;
			case CHASING:
				chaseMovement(delta, playerPos);
				break;
		}

		// 5. Overwrite instance.transform with translation × rotateY(heading).
		// Done AFTER animationController.update() so our rotation wins.
		applyTransform();
	}

	/**
	 * No-op override — {@link EntitySystem} calls this but the real tick is
	 * the 3-arg {@link #update(float, Vector3, Vector2)} driven by World.
	 */
	@Override
	public void update(float delta) {
		// Intentionally empty: World drives the full update.
	}

	// ── Entity interface ─────────────────────────────────────────────────────

	@Override
	public btRigidBody getBody() {
		return body;
	}

	@Override
	public void onClick(EventBus events) {
	}

	@Override
	public String getInteractHint() {
		return "Get away!";
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Disposable d : disposables)
			d.dispose();
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private void lurkMovement(float delta) {
		lurkAngle += LURK_SPEED * delta;

		float nx = lurkCenter.x + MathUtils.cos(lurkAngle) * LURK_RADIUS;
		float nz = lurkCenter.z + MathUtils.sin(lurkAngle) * LURK_RADIUS;

		// Heading = tangent to the circle = direction of travel.
		// Derivative of (cos θ, sin θ) is (-sin θ, cos θ).
		float dx = -MathUtils.sin(lurkAngle);
		float dz = MathUtils.cos(lurkAngle);
		headingDeg = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;

		position.set(nx, LURK_DEPTH, nz);
	}

	private void chaseMovement(float delta, Vector3 playerPos) {
		float targetY = Math.min(playerPos.y - 0.5f, -0.5f);

		_scratch.set(playerPos.x, targetY, playerPos.z).sub(position);
		float dist = _scratch.len();
		if (dist < 0.1f)
			return;

		_scratch.scl(1f / dist); // normalise

		// Heading: atan2(x, z) gives degrees clockwise from +Z.
		headingDeg = MathUtils.atan2(_scratch.x, _scratch.z) * MathUtils.radiansToDegrees;

		float step = Math.min(CHASE_SPEED * delta, dist);
		position.x += _scratch.x * step;
		position.y += _scratch.y * step;
		position.z += _scratch.z * step;
	}

	/**
	 * Writes the shark's current {@link #position} and {@link #headingDeg} into
	 * {@code instance.transform}.
	 *
	 * <p>
	 * Called <em>after</em> {@link AnimationController#update(float)} each
	 * frame because the animation controller internally calls
	 * {@code ModelInstance.calculateTransforms()}, which resets
	 * {@code instance.transform} from the node hierarchy — discarding any
	 * rotation we set earlier in the same frame. By writing the transform last
	 * we guarantee our orientation sticks for rendering and physics.
	 * </p>
	 *
	 * <p>
	 * We use {@code setToTranslation(...).rotate(Vector3.Y, degrees)} rather
	 * than {@code setToLookAt()} because {@code setToLookAt} produces a camera
	 * view matrix (inverse-rotation convention) which would point the model
	 * backwards.
	 * </p>
	 */
	private void applyTransform() {
		transform.setToTranslation(position).rotate(Vector3.Y, headingDeg);
	}
}