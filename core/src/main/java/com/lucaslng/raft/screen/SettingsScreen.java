package com.lucaslng.raft.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.settings.Settings;

class SettingsScreen implements Screen {

	private final Stage stage;
	private final List<Disposable> disposables = new ArrayList<>();
	private final Settings settings;

	protected SettingsScreen(ScreenManager screenManager) {
		settings = Settings.get();
		Assets assets = Assets.get();

		Skin skin = assets.getSkin();

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.setBackground(new TextureRegionDrawable(assets.get("images/2.png", Texture.class)));
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
	}

	@Override public void pause()  {}
	@Override public void resume() {}
	@Override public void hide()   {}

	@Override
	public void dispose() {
		for (Disposable d : disposables) d.dispose();
	}
}