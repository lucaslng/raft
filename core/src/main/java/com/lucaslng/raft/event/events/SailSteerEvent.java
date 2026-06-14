package com.lucaslng.raft.event.events;

import com.lucaslng.raft.event.Event;

public class SailSteerEvent extends Event {

  public final float angleDegrees;

  public SailSteerEvent(float angleDegrees) {
    this.angleDegrees = angleDegrees;
  }
}