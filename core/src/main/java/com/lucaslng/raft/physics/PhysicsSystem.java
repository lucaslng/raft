package com.lucaslng.raft.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.entity.Entity;
import com.lucaslng.raft.entity.Shark;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.BuildingPlacedEvent;
import com.lucaslng.raft.event.events.StatChangeEvent;

/**
 * Wraps the Bullet physics world.
 *
 * <p>
 * In addition to dynamics, this class registers a {@link ContactListener}
 * that detects shark–player contact and posts a {@link StatChangeEvent} with a
 * negative health delta (damage) whenever the two bodies touch.
 *
 * <h3>Contact-callback wiring</h3>
 * <ol>
 * <li>The shark body must carry {@code CF_CUSTOM_MATERIAL_CALLBACK} and have
 * its {@code contactCallbackFlag} set to {@link Shark#FLAG}.</li>
 * <li>The player body must have its {@code contactCallbackFilter} set to the
 * same flag (done in {@link PlayerPhysics}).</li>
 * <li>The {@link ContactListener} installed here fires
 * {@code onContactAdded} and posts damage when {@code match0} or
 * {@code match1} is {@code true}.</li>
 * </ol>
 */
public class PhysicsSystem implements Disposable {

	// ── Damage tuning ───────────────────────────────────────────────────────

	/** Health removed per contact event. */
	private static final float SHARK_DAMAGE = -0.15f;

	/**
	 * Minimum seconds between damage ticks. Bullet can fire many
	 * {@code onContactAdded} events per second — throttle them so each bite
	 * removes health at a predictable rate rather than instantly.
	 */
	private static final float DAMAGE_COOLDOWN = 0.5f;

	// ── Bullet infrastructure ───────────────────────────────────────────────

	private final btDefaultCollisionConfiguration config;
	private final btCollisionDispatcher dispatcher;
	private final btDbvtBroadphase broadphase;
	private final btSequentialImpulseConstraintSolver solver;
	private final btDiscreteDynamicsWorld world;

	/** Must be kept alive — ContactListener is a native-backed object. */
	private final ContactListener contactListener;

	/** Wall-clock accumulator used to throttle damage ticks. */
	private float damageCooldownRemaining = 0f;

	public PhysicsSystem() {
		config = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(config);
		broadphase = new btDbvtBroadphase();
		solver = new btSequentialImpulseConstraintSolver();
		world = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, config);
		world.setGravity(new Vector3(0, -8f, 0));
		broadphase.getOverlappingPairCache()
				.setInternalGhostPairCallback(new btGhostPairCallback());

		EventBus events = EventBus.get();
		// ── Building physics listener ────────────────────────────────────
		events.subscribe(BuildingPlacedEvent.class, new Subscriber<BuildingPlacedEvent>() {
			@Override
			public void accept(BuildingPlacedEvent event) {
				btRigidBody b = event.building.getBody();
				if (b != null)
					addBody(b);
			}
		});

		// ── Shark–player contact listener ────────────────────────────────
		// Instantiating ContactListener registers it globally with Bullet.
		// Only one instance should exist at a time.
		contactListener = new ContactListener() {
			@Override
			public boolean onContactAdded(int userValue0, int partId0, int index0,
					boolean match0,
					int userValue1, int partId1, int index1,
					boolean match1) {
				// match0 is true when object0's callbackFlag matches object1's filter
				// (or vice versa for match1). Either means a shark–player contact.
				if (match0 || match1) {
					if (damageCooldownRemaining <= 0f) {
						events.post(new StatChangeEvent(SHARK_DAMAGE, 0f, 0f));
						damageCooldownRemaining = DAMAGE_COOLDOWN;
					}
				}
				return true;
			}
		};
	}

	// ── Per-frame tick ──────────────────────────────────────────────────────

	public void rayCast(Vector3 from, Vector3 to, RayResultCallback callback) {
		world.rayTest(from, to, callback);
	}

	public void update(float delta) {
		// Tick down the damage cooldown outside the native callback.
		if (damageCooldownRemaining > 0f) {
			damageCooldownRemaining -= delta;
		}
		world.stepSimulation(delta, 5, 1 / 60f);
	}

	// ── Body management ─────────────────────────────────────────────────────

	public void addBody(btRigidBody body) {
		world.addRigidBody(body);
	}

	public void addEntity(Entity entity) {
		addBody(entity.getBody());
	}

	public void removeBody(btRigidBody body) {
		world.removeRigidBody(body);
	}

	public void removeEntity(Entity entity) {
		removeBody(entity.getBody());
	}

	// ── Debug drawing ────────────────────────────────────────────────────────

	public void setDebugDraw(btIDebugDraw debugDraw) {
		world.setDebugDrawer(debugDraw);
	}

	public void debugDrawWorld() {
		world.debugDrawWorld();
	}

	// ── Disposal ─────────────────────────────────────────────────────────────

	@Override
	public void dispose() {
		contactListener.dispose();
		world.dispose();
		solver.dispose();
		broadphase.dispose();
		dispatcher.dispose();
		config.dispose();
	}
}