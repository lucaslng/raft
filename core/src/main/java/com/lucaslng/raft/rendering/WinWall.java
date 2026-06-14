package com.lucaslng.raft.rendering;

import com.badlogic.gdx.graphics.Color;
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

// Render a big blue glowing wall at the end of the game
// Pulsing animation
class WinWall implements Disposable {

	// Wall dimensions
	private static final float WIDTH = 2000f;
	private static final float HEIGHT = 100f;
	private static final float DEPTH = 3f;

	private static final Color color = Color.CYAN;

	private static final float PULSE_BASE = 0.45f;
	private static final float PULSE_AMP = 0.25f;
	private static final float PULSE_SPEED = 2.5f;

	private final Model model;
	private final ModelInstance instance;
	private final BlendingAttribute blending;
	private float time;

	WinWall() {
		blending = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, PULSE_BASE);

		ModelBuilder mb = new ModelBuilder();
		model = mb.createBox(
				WIDTH, HEIGHT, DEPTH,
				new Material(
						blending,
						ColorAttribute.createDiffuse(color),
						new ColorAttribute(ColorAttribute.Emissive, color)),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

		instance = new ModelInstance(model);
		instance.transform.setToTranslation(0f, HEIGHT * 0.5f, World.WIN_DISTANCE); // center at sea level
	}

	// update pulsing animation
	void update(float delta) {
		time += delta;
		blending.opacity = PULSE_BASE + PULSE_AMP * (float) Math.sin(time * PULSE_SPEED);
	}

	void render(ModelBatch batch, Environment env) {
		batch.render(instance, env);
	}

	@Override
	public void dispose() {
		model.dispose();
	}
}