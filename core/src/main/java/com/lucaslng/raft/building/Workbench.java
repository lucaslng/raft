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
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.physics.MotionState;
import com.lucaslng.raft.rendering.hud.WorkbenchPanel;
import com.lucaslng.raft.util.Util;

/**
 * A crafting bench that lets the player craft unlocked recipes in exchange for
 * backpack items. Opening the workbench (right-click) shows the
 * {@link WorkbenchPanel} which lists all currently unlocked recipes.
 *
 * <p>
 * The workbench has no per-frame logic — it is purely interactive.
 * </p>
 */
public class Workbench extends Building {

  public static final String NAME = "Workbench";

  private final btRigidBody body;
  private final btBoxShape shape;
  private final MotionState motionState;

  protected Workbench(Model model, EventBus events) {
    super(new ModelInstance(model));

    Vector3 dimensions = Util.getDimensions(this.model);
    shape = new btBoxShape(dimensions);
    motionState = new MotionState(this.model.transform, dimensions.y);
    btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(1f, motionState, shape);
    body = new btRigidBody(info);
    info.dispose();
    body.setCollisionFlags(
        body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
    body.setActivationState(Collision.DISABLE_DEACTIVATION);
    body.userData = this;
  }

  @Override
  public void onClick(EventBus events) {
    super.onClick(events);
    events.post(new PanelOpenedEvent(new WorkbenchPanel(events)));
  }

  @Override
  public void update(float delta) {
    // No per-frame logic needed.
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
  public String getInteractHint() {
    return "[RMB] Open workbench";
  }

  @Override
  public void dispose() {
    body.dispose();
    shape.dispose();
    motionState.dispose();
  }
}