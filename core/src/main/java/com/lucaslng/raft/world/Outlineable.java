package com.lucaslng.raft.world;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

// Something that should have an outline when hovered
public interface Outlineable {
    // The model instance to outline, not nullable
    ModelInstance getOutlineInstance();
}