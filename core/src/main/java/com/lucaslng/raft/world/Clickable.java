package com.lucaslng.raft.world;

import com.lucaslng.raft.event.EventBus;

// Something in the world that can be clicked
public interface Clickable {
  void onClick(EventBus events);

  String getInteractHint();
}