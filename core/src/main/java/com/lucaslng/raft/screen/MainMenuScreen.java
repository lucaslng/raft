package com.lucaslng.raft.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;

class MainMenuScreen implements Screen {

	private Stage stage;
	private List<Disposable> disposables;

	protected MainMenuScreen(Assets assets, ScreenManager screenManager) {
		disposables = new ArrayList<>();

		Skin skin = assets.get("skin/golden-ui-skin.json", Skin.class);

		stage = new Stage(new ExtendViewport(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.setBackground(new TextureRegionDrawable(assets.get("images/3.png", Texture.class)));

		Label title = new Label("Raft", skin.get("title", LabelStyle.class));
		table.add(title).row();

		TextButton playButton = new TextButton("Play", skin);
		playButton.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new GameScreen(screenManager));
			}
		});
		table.add(playButton).width(300f).row();

		TextButton instructionsButton = new TextButton("Instructions", skin);
		instructionsButton.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new InstructionsScreen(screenManager));
			}
		});
		table.add(instructionsButton).width(300f).row();

		TextButton settingsButton = new TextButton("Settings", skin);
		settingsButton.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new SettingsScreen(screenManager));
			}
		});
		table.add(settingsButton).width(300f).row();

		TextButton aboutButton = new TextButton("About", skin);
		aboutButton.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.push(new AboutScreen(screenManager));
			}
		});
		table.add(aboutButton).width(300f).row();
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
	public void dispose() {
		for (Disposable disposable : disposables)
			disposable.dispose();
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

}
