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

// Wraps Bullet physics engine
public class PhysicsSystem implements Disposable {

	private final btDefaultCollisionConfiguration config;
	private final btCollisionDispatcher dispatcher;
	private final btDbvtBroadphase broadphase;
	private final btSequentialImpulseConstraintSolver solver;
	private final btDiscreteDynamicsWorld world;

	private final ContactListener contactListener;

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

		contactListener = new ContactListener() {
			@Override
			public boolean onContactAdded(int userValue0, int partId0, int index0, boolean match0,
					int userValue1, int partId1, int index1, boolean match1) {
				// match0 is true when object0's callbackFlag matches object1's filter
				// (or vice versa for match1).
				// Means that shark touched player
				if (match0 || match1) {
					if (damageCooldownRemaining <= 0f) {
						events.post(new StatChangeEvent(Shark.DAMAGE, 0f, 0f));
						damageCooldownRemaining = Shark.DAMAGE_COOLDOWN;
					}
				}
				return true;
			}
		};
	}

	public void rayCast(Vector3 from, Vector3 to, RayResultCallback callback) {
		world.rayTest(from, to, callback);
	}

	public void update(float delta) {
		if (damageCooldownRemaining > 0f) {
			damageCooldownRemaining -= delta;
		}
		world.stepSimulation(delta, 5, 1 / 60f);
	}

	// Methods to manage physics bodies

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

	// debug drawing

	public void setDebugDraw(btIDebugDraw debugDraw) {
		world.setDebugDrawer(debugDraw);
	}

	public void debugDrawWorld() {
		world.debugDrawWorld();
	}

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