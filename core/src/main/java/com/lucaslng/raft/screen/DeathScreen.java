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

// Shows Death screen
class DeathScreen implements Screen {

	private final Stage stage;
	private final List<Disposable> disposables = new ArrayList<>();

	protected DeathScreen() {
		ScreenManager screenManager = ScreenManager.get();
		Assets assets = Assets.get();
		Skin skin = assets.getSkin();

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table table = new Table(skin);
		table.setFillParent(true);
		table.setBackground(skin.getDrawable("bg"));
		table.pad(40f);
		table.center();
		table.defaults().pad(40f);
		stage.addActor(table);

		table.add(new Label("You Died", skin, "title")).row();

		Label bodyLabel = new Label("Better luck next time...", skin, "red");
		table.add(bodyLabel).row();

		TextButton backButton = new TextButton("Back", skin);
		backButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.pop();
			}
		});
		table.add(backButton).width(240f).height(60f).row();
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