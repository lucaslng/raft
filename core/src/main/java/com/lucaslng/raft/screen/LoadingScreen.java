package com.lucaslng.raft.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.util.Util;

// Shows loading bar for assets
// Can't use assets because they are loading
public class LoadingScreen implements Screen {

	private final Assets assets;
	private final ScreenManager screenManager;
	private final Stage stage;
	private final ProgressBar progressBar;
	private final List<Disposable> disposables = new ArrayList<>();

	public LoadingScreen() {
		assets = Assets.get();
		screenManager = ScreenManager.get();

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Texture barBg   = Util.generateTexture(new Color(0.3f, 0.3f, 0.3f, 1f), 8);
		Texture barFill = Util.generateTexture(Color.WHITE, 8);
		disposables.add(barBg);
		disposables.add(barFill);

		ProgressBarStyle style = new ProgressBarStyle();
		style.background = new TextureRegionDrawable(barBg);
		style.knobBefore  = new TextureRegionDrawable(barFill);

		progressBar = new ProgressBar(0f, 1f, 0.001f, false, style);

		Table table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.add(progressBar).center().width(400f).height(12f);

		Bullet.init(); // init physics engine
	}

	@Override
	public void render(float delta) {
		if (assets.update()) { // returns true when assets done loading
			screenManager.replace(new MainMenuScreen());
		}

		progressBar.setValue(assets.getProgress());

		ScreenUtils.clear(0.12f, 0.12f, 0.12f, 1f);
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		for (Disposable d : disposables) d.dispose();
	}

	@Override public void show()   {}
	@Override public void pause()  {}
	@Override public void resume() {}
	@Override public void hide()   {}
}