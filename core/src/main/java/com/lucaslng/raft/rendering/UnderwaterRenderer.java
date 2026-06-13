package com.lucaslng.raft.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Disposable;

/**
 * Renders a full-screen blue tint when the camera is below the water surface (y
 * < 0).
 * Simple and cheap — just an alpha-blended rectangle over the whole screen.
 */
class UnderwaterRenderer implements Disposable {

	private static final Color TINT = new Color(0.04f, 0.16f, 0.35f, 0.65f);

	private final ShapeRenderer shapes = new ShapeRenderer();

	/** Call after all 3D rendering, before the HUD. */
	void renderIfSubmerged(float cameraY) {
		if (cameraY >= 0f)
			return;

		// Fade in as the camera goes deeper (max alpha at depth >= 3 units)
		float depth = Math.min(-cameraY / 3f, 1f);
		float alpha = TINT.a * depth;

		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		shapes.begin(ShapeType.Filled);
		shapes.setColor(TINT.r, TINT.g, TINT.b, alpha);
		shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		shapes.end();

		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	@Override
	public void dispose() {
		shapes.dispose();
	}
}