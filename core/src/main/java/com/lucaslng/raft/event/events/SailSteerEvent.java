package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;

/**
 * Posted by the sail UI when the player chooses a new steering angle.
 * {@link #angleDegrees} is measured clockwise from +Z (north), matching the
 * compass convention used in the sail UI.
 */
public class SailSteerEvent extends Event {

  /** Steering angle in degrees, clockwise from the +Z axis. */
  public final float angleDegrees;

  public SailSteerEvent(float angleDegrees) {
    this.angleDegrees = angleDegrees;
  }
}