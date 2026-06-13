package com.lucaslng.raft.world;

import com.lucaslng.raft.event.EventBus;

/**
 * Anything that can be targeted by the player's raycast and clicked.
 * Assigned to {@code btRigidBody.userData} so the raycast handler
 * needs no type-switch — it just calls {@link #onClick(EventBus)}.
 */
public interface Clickable {
    void onClick(EventBus events);
}