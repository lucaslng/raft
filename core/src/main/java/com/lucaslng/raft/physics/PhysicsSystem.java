package com.lucaslng.raft.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.entity.Entity;
import com.lucaslng.raft.event.EventBus;

public class PhysicsSystem implements Disposable {

	private final btDefaultCollisionConfiguration config;
	private final btCollisionDispatcher dispatcher;
	private final btDbvtBroadphase broadphase;
	private final btSequentialImpulseConstraintSolver solver;
	private final btDiscreteDynamicsWorld world;

	public PhysicsSystem(EventBus events) {
		config = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(config);
		broadphase = new btDbvtBroadphase();
		solver = new btSequentialImpulseConstraintSolver();
		world = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, config);
		world.setGravity(new Vector3(0, -8f, 0));
		broadphase.getOverlappingPairCache()
				.setInternalGhostPairCallback(new btGhostPairCallback());
	}

	public void rayCast(Vector3 from, Vector3 to, RayResultCallback callback) {
		world.rayTest(from, to, callback);
	}

	public void update(float delta) {
		world.stepSimulation(delta, 5, 1/60f);
	}

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

	@Override
	public void dispose() {
		world.dispose();
		solver.dispose();
		broadphase.dispose();
		dispatcher.dispose();
		config.dispose();
	}

	public void setDebugDraw(btIDebugDraw debugDraw) {
		world.setDebugDrawer(debugDraw);
	}

	public void debugDrawWorld() {
		world.debugDrawWorld();
	}

}
