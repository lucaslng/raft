package com.lucaslng.raft.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;

class InstructionsScreen implements Screen {

	private Stage stage;
	private List<Disposable> disposables;

	protected InstructionsScreen(Assets assets, ScreenManager screenManager) {
		disposables = new ArrayList<>();

		Skin skin = assets.get("skin/golden-ui-skin.json", Skin.class);
		
		stage = new Stage(new ExtendViewport(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.setBackground(new TextureRegionDrawable(assets.get("images/2.png", Texture.class)));

		Label title = new Label("Instructions", skin.get("title", LabelStyle.class));
		table.add(title).row();

		TextButton backButton = new TextButton("Back", skin);
		backButton.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.pop();
			}
		});
		table.add(backButton).width(300f).row();
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
		for (Disposable disposable : disposables)
			disposable.dispose();
	}
	
}
