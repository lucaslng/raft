package com.lucaslng.raft.entity;

import java.util.concurrent.atomic.AtomicInteger;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.world.Clickable;

public abstract class Entity implements Disposable, Clickable {
	
	private static final AtomicInteger ID_GEN = new AtomicInteger();
	public final int id;

	protected final Matrix4 transform;
	protected final ModelInstance model;

	protected boolean active = true;

	protected Entity(ModelInstance model) {
		id = ID_GEN.getAndIncrement();
		this.model = model;
		this.transform = model.transform;
	}

	abstract public void update(float delta);

	public Vector3 getPosition() {
		return transform.getTranslation(new Vector3());
	}

	public Vector3 getPosition(Vector3 dest) {
		return transform.getTranslation(dest);
	}

	public ModelInstance getInstance() {
		return model;
	}

	abstract public btRigidBody getBody();
	
	public boolean isActive() {
		return active;
	}

	public boolean isDead() {
		return !active;
	}

	public void kill() {
		active = false;
	}

	@Override
	public void dispose() {
	}
}
