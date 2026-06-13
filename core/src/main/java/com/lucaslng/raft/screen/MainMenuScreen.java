package com.lucaslng.raft.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;

class MainMenuScreen implements Screen {

	private final Stage stage;
	private final List<Disposable> disposables = new ArrayList<>();

	protected MainMenuScreen() {
		Assets assets = Assets.get();
		ScreenManager screenManager = ScreenManager.get();
		Skin skin = assets.getSkin();

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table table = new Table(skin);
		table.setFillParent(true);
		table.setBackground(skin.getDrawable("bg"));
		stage.addActor(table);

		table.add(new Label("Raft", skin, "title")).padBottom(48f).row();

		TextButton playButton = new TextButton("Play", skin);
		playButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new GameScreen());
			}
		});
		table.add(playButton).width(320f).height(60f).padBottom(12f).row();

		TextButton instructionsButton = new TextButton("Instructions", skin);
		instructionsButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new InstructionsScreen());
			}
		});
		table.add(instructionsButton).width(320f).height(60f).padBottom(12f).row();

		TextButton settingsButton = new TextButton("Settings", skin);
		settingsButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new SettingsScreen());
			}
		});
		table.add(settingsButton).width(320f).height(60f).padBottom(12f).row();

		TextButton aboutButton = new TextButton("About", skin);
		aboutButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new AboutScreen());
			}
		});
		table.add(aboutButton).width(320f).height(60f).padBottom(12f).row();

		TextButton exitButton = new TextButton("Exit", skin);
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Gdx.app.exit();
			}
		});
		table.add(exitButton).width(320f).height(60f).row();
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

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
	}

	@Override
	public void dispose() {
		for (Disposable d : disposables)
			d.dispose();
	}
}