package com.lucaslng.raft.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
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

public class LoadingScreen implements Screen {

	private final Assets assets;
	private final ScreenManager screenManager;
	private final Stage stage;
	private final ProgressBar progressBar;
	private final List<Disposable> disposables = new ArrayList<>();

	public LoadingScreen() {
		assets = Assets.get();
		screenManager = ScreenManager.get();

		stage = new Stage(new ExtendViewport(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table mainTable = new Table();
		mainTable.setFillParent(true);
		stage.addActor(mainTable);

		progressBar = createProgressBar(disposables);

		mainTable.add(progressBar).center().width(400).height(50);

		Bullet.init();
	}

	@Override
	public void render(float delta) {
		if (assets.update()) {
			screenManager.replace(new MainMenuScreen());
		}

		float progress = assets.getProgress();
		progressBar.setValue(progress);

		ScreenUtils.clear(0.6588235294f, 0.4352941176f, 0.3294117647f, 1f);
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		for (Disposable disposable : disposables)
			disposable.dispose();
	}

	private static ProgressBar createProgressBar(List<Disposable> disposables) {
		int height = 30;
		Texture backgroundTexture = Util.generateTexture(Color.WHITE, height);
		Texture knobTexture = Util.generateTexture(Color.BROWN, 30);

		ProgressBarStyle style = new ProgressBarStyle();
		style.background = new TextureRegionDrawable(backgroundTexture);
		style.knobBefore = new TextureRegionDrawable(knobTexture);

		ProgressBar bar = new ProgressBar(0f, 1f, .01f, false, style);

		disposables.add(backgroundTexture);
		disposables.add(knobTexture);

		return bar;
	}

	@Override
	public void show() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
	}

}
