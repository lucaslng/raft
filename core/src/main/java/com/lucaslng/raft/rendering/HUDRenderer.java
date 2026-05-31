package com.lucaslng.raft.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.world.World;

class HUDRenderer implements Disposable {

	private Stage stage;
	private BitmapFont mainFont;
	private Texture crosshair;
	private Label fpsLabel;

	HUDRenderer(Assets assets) {
		FreeTypeFontParameter fontParam = new FreeTypeFontParameter();
		fontParam.size = 18;
		fontParam.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
		mainFont = assets.generateFont("main", fontParam);

		stage = new Stage();

		fpsLabel = new Label("", new LabelStyle(mainFont, Color.WHITE));
		stage.addActor(fpsLabel);

		crosshair = assets.finishLoadingAsset("images/crosshair-normal.png");
	}

	void render(World world) {
		fpsLabel.setText(" FPS: " + Gdx.graphics.getFramesPerSecond());
		
		stage.draw();
		stage.getBatch().begin();
		stage.getBatch().draw(crosshair, Gdx.graphics.getBackBufferWidth() / 2 - 16,
				Gdx.graphics.getBackBufferHeight() / 2 - 16, 32f, 32f);
		stage.getBatch().end();
	}

	public void dispose() {
		stage.dispose();
		mainFont.dispose();
	}

}
