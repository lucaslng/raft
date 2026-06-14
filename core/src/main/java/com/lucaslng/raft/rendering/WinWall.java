package com.lucaslng.raft.rendering;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.world.World;

/**
 * A wide, additively-blended glowing wall drawn at the win boundary.
 *
 * <p>
 * Uses {@code GL_SRC_ALPHA → GL_ONE} (additive) blending so the wall
 * brightens whatever is behind it — producing a light-emission look without
 * any post-processing. Rendered with the outline environment (ambient = white)
 * so the emissive cyan is not dimmed by the sun direction.
 * </p>
 *
 * <p>
 * Opacity pulses each frame via a sine wave to animate the glow.
 * Depth writing is disabled during rendering so the wall does not occlude
 * objects drawn later (e.g. the HUD outline pass).
 * </p>
 */
class WinWall implements Disposable {

	// Wall dimensions
	private static final float WIDTH = 2000f; // spans the whole visible ocean
	private static final float HEIGHT = 60f; // tall enough to see from the raft
	private static final float DEPTH = 3f; // thin slab

	// Glow colour — bright cyan
	private static final float R = 0.15f;
	private static final float G = 0.85f;
	private static final float B = 1.00f;

	// Pulse: opacity = BASE ± AMP (range 0.20 – 0.70)
	private static final float PULSE_BASE = 0.45f;
	private static final float PULSE_AMP = 0.25f;
	private static final float PULSE_SPEED = 2.5f;

	private final Model model;
	private final ModelInstance instance;
	private final BlendingAttribute blending;
	private float time = 0f;

	WinWall() {
		// Additive blend: source * srcAlpha + dest * 1 → adds light
		blending = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, PULSE_BASE);

		ModelBuilder mb = new ModelBuilder();
		model = mb.createBox(
				WIDTH, HEIGHT, DEPTH,
				new Material(
						blending,
						ColorAttribute.createDiffuse(R, G, B, 1f),
						new ColorAttribute(ColorAttribute.Emissive, R, G, B, 1f)),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

		instance = new ModelInstance(model);
		// Centre the wall: bottom sits near y = 0 (water surface)
		instance.transform.setToTranslation(0f, HEIGHT * 0.5f, World.WIN_DIST);
	}

	/** Advance the pulse animation. Call once per frame before {@link #render}. */
	void update(float delta) {
		time += delta;
		blending.opacity = PULSE_BASE + PULSE_AMP * (float) Math.sin(time * PULSE_SPEED);
	}

	/**
	 * Renders the wall into the given batch.
	 * Caller is responsible for depth-mask management (disable before, restore
	 * after).
	 */
	void render(ModelBatch batch, Environment env) {
		batch.render(instance, env);
	}

	@Override
	public void dispose() {
		model.dispose(); // ModelInstance owns nothing; Model owns the mesh
	}
}