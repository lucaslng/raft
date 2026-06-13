package com.lucaslng.raft.raft;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector2;

// Ghost tile that is currently selected
public class PlacementGhost {

	private final ModelInstance ghostInstance;
	private Vector2 targetCoord = null;

	private static final float GHOST_R = 0.4f;
	private static final float GHOST_G = 0.9f;
	private static final float GHOST_B = 0.6f;
	private static final float GHOST_A = 0.45f;

	public PlacementGhost(Model tileModel) {
		ghostInstance = new ModelInstance(tileModel);

		for (com.badlogic.gdx.graphics.g3d.Material mat : ghostInstance.materials) {
			mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GHOST_A));
			mat.set(ColorAttribute.createDiffuse(GHOST_R, GHOST_G, GHOST_B, GHOST_A));
		}
	}

	public void setTarget(Vector2 coord) {
		this.targetCoord = coord;
		if (coord != null)
			ghostInstance.transform.setToTranslation(coord.x, 0f, coord.y);
	}

	public boolean isVisible() {
		return targetCoord != null;
	}

	public void render(ModelBatch batch, Environment env) {
		if (!isVisible()) return;

		Gdx.gl.glDepthMask(false);
		batch.render(ghostInstance, env);
		Gdx.gl.glDepthMask(true);
	}

}