package com.lucaslng.raft.world;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 * Implemented by anything that should receive a selection outline
 * when hovered. Companion to {@link Clickable}.
 */
public interface Outlineable {
    /** The model instance to outline. Must not be null. */
    ModelInstance getOutlineInstance();
}