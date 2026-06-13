package com.lucaslng.raft.world;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

/**
 * Handles swimming physics when the player is below the water surface (y < 0).
 *
 * <p>
 * While submerged:
 * <ul>
 * <li><b>Buoyancy</b> — an upward force counters gravity so the player floats
 * toward the surface.</li>
 * <li><b>Drag</b> — heavy linear damping replaces the default air damping,
 * slowing fall/rise.</li>
 * <li><b>Vertical swim</b> — the player can push up by looking/moving up; no
 * jump, but holding
 * the swim-up direction (camera look direction) propels them toward the
 * surface.</li>
 * </ul>
 *
 * <p>
 * When the player is above water, normal physics (gravity + jump) take over and
 * damping is
 * restored to the default value.
 */
public class SwimmingSystem {

  /** Y coordinate of the water surface. */
  public static final float WATER_SURFACE_Y = 0f;

  /**
   * Upward force applied each frame while submerged (counters gravity ~8 m/s² ×
   * mass 2 kg).
   */
  private static final float BUOYANCY_FORCE = 14f;

  /** How fast the player can swim (force magnitude). */
  private static final float SWIM_FORCE = 18f;

  /** Linear damping while submerged (heavy drag). */
  private static final float WATER_DAMPING = 3.5f;

  /**
   * Linear damping in air (must match what PlayerPhysics sets — default Bullet
   * value).
   */
  private static final float AIR_DAMPING = 0f;

  /** Angular damping kept constant (prevents tumbling). */
  private static final float ANGULAR_DAMPING = 0.9f;

  private boolean wasSubmerged = false;

  /** Scratch vector — never stored across calls. */
  private final Vector3 temp = new Vector3();

  /**
   * Apply swimming physics for this frame.
   *
   * @param body         the player's rigid body
   * @param playerY      the player's current world-space Y position
   * @param camDirection normalised camera look direction (for swim steering)
   */
  public void update(btRigidBody body, float playerY, Vector3 camDirection) {
    boolean submerged = playerY < WATER_SURFACE_Y;

    if (submerged != wasSubmerged) {
      // Transition: swap damping
      float linear = submerged ? WATER_DAMPING : AIR_DAMPING;
      body.setDamping(linear, ANGULAR_DAMPING);
      wasSubmerged = submerged;
    }

    if (!submerged)
      return;

    // ── Buoyancy ──────────────────────────────────────────────────────
    // Partial buoyancy: stronger near the surface, weaker when deep.
    float depth = -playerY; // positive depth below surface
    float buoyancyScale = Math.max(0.4f, 1f - depth * 0.05f); // taper off at extreme depth
    body.applyCentralForce(temp.set(0, BUOYANCY_FORCE * buoyancyScale, 0));

    // ── Swim steering ────────────────────────────────────────────────
    // The camera look direction drives horizontal + vertical swim movement.
    // GameScreen already reads WASD and moves the camera, so we apply a
    // force along the camera direction while submerged so the player
    // actually moves underwater (rather than just the free-fly camera).
    temp.set(camDirection).nor().scl(SWIM_FORCE);
    // Suppress any downward swim force — buoyancy + gravity handle vertical.
    // Allow upward swim force so the player can surface.
    if (temp.y < 0)
      temp.y = 0;
    body.applyCentralForce(temp);
  }

  public boolean isSubmerged() {
    return wasSubmerged;
  }
}
