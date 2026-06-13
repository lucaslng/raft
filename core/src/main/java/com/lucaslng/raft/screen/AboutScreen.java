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

class AboutScreen implements Screen {

	private final Stage stage;
	private final List<Disposable> disposables = new ArrayList<>();

	protected AboutScreen() {
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
		table.top();
		stage.addActor(table);

		table.add(new Label("About", skin, "title")).padBottom(40f).row();

		// Author / date / description block
		String body =
				"Lucas Leung\n" +
				"Friday, June 12th, 2026\n\n" +
				"Survive the harsh seas on your little raft!\n" +
				"Collect debris, build structures, manage your hunger\n" +
				"and thirst, and stay alive as long as you can.";

		Label bodyLabel = new Label(body, skin, "white");
		bodyLabel.setWrap(true);
		table.add(bodyLabel).width(700f).padBottom(40f).row();

		table.add().expandY().row(); // vertical spacer

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