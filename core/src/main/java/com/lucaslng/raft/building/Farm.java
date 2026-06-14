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
import com.lucaslng.raft.event.events.ItemCollectedEvent;
import com.lucaslng.raft.item.ItemRegistry;
import com.lucaslng.raft.item.ItemStack;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.util.Util;

public class Farm extends Building {

  public static final float PLANT_SEC = 10f;

  private final btRigidBody body;
  private final btBoxShape shape;
  private final MotionState motionState;

  private float timer = 0f;

  public Farm(Model model) {
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
    return "Farm";
  }

  @Override
  public btRigidBody getBody() {
    return body;
  }

  @Override
  public void onClick(EventBus events) {
    super.onClick(events);
    if (getYield() <= 0)
      return;
    events.post(new ItemCollectedEvent(new ItemStack(ItemRegistry.get().getItem("Cauliflower"), getYield())));
    timer %= PLANT_SEC;
  }

  @Override
  public void dispose() {
    body.dispose();
    shape.dispose();
    motionState.dispose();
  }

  @Override
  public String getInteractHint() {
    return String.format("[RMB] Claim %dx Cauliflowers", getYield());
  }

  private int getYield() {
    return (int) (timer / PLANT_SEC);
  }
}