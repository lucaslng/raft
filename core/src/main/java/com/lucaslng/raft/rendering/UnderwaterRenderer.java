package com.lucaslng.raft.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Disposable;

// render blue tint while underwater
class UnderwaterRenderer implements Disposable {

	private static final Color TINT = new Color(0.04f, 0.16f, 0.35f, 0.65f);

	private final ShapeRenderer shapes = new ShapeRenderer();

	void renderIfSubmerged(float cameraY) {
		if (cameraY >= 0f)
			return;

		// deeper -> more blue
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