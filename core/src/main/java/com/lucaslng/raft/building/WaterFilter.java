package com.lucaslng.raft.building;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.StatChangeEvent;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

public class WaterFilter extends Building {

  public static final float THIRST_RESTORE_PER_SEC = .01f;

  private final btRigidBody body;
  private final btBoxShape shape;
  private final MotionState motionState;

  private float timer = 0f;

  public WaterFilter(Model model, EventBus events) {
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

  @Override
  public void update(float delta) {
    timer += delta;
  }

  @Override
  public String getName() {
    return "Water Filter";
  }

  @Override
  public btRigidBody getBody() {
    return body;
  }

  @Override
  public void onClick(EventBus events) {
    super.onClick(events);
    events.post(new StatChangeEvent(0f, 0f, getRestoreValue()));
    timer = 0f;
  }

  @Override
  public void dispose() {
    body.dispose();
    shape.dispose();
    motionState.dispose();
  }

  @Override
  public String getInteractHint() {
    return String.format("[RMB] Restore %d%% thirst", Math.round(getRestoreValue() * 100f));
  }

  private float getRestoreValue() {
    return timer * THIRST_RESTORE_PER_SEC;
  }
}