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

class InstructionsScreen implements Screen {

	private final Stage stage;
	private final ScrollPane scrollPane;
	private final List<Disposable> disposables = new ArrayList<>();

	protected InstructionsScreen() {
		ScreenManager screenManager = ScreenManager.get();
		Assets assets = Assets.get();
		Skin skin = assets.getSkin();

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table wrapper = new Table(skin);
		wrapper.setFillParent(true);
		wrapper.setBackground(skin.getDrawable("bg"));
		stage.addActor(wrapper);

		Table table = new Table(skin);
		table.pad(40f);
		table.top();

		scrollPane = new ScrollPane(table, skin);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false);
		wrapper.add(scrollPane).expand().fill();

		table.add(new Label("Instructions", skin, "title")).padBottom(40f).row();

		// ── Controls ──────────────────────────────────────────────────────
		addSection(table, skin, "Controls");
		addRow(table, skin, "Move",           "W / A / S / D");
		addRow(table, skin, "Look",           "Mouse");
		addRow(table, skin, "Jump / Swim up", "Space");
		addRow(table, skin, "Dive",           "Shift");
		addRow(table, skin, "Place / Use",    "Left Mouse Button");
		addRow(table, skin, "Interact",       "Right Mouse Button");
		addRow(table, skin, "Inventory",      "Tab");
		addRow(table, skin, "Hotbar slots",   "1 - 8");
		addRow(table, skin, "Unlock cursor",  "Escape");

		// ── Gameplay ──────────────────────────────────────────────────────
		addSection(table, skin, "Gameplay");
		addRow(table, skin, "Collect debris",  "Approach floating items and left-click");
		addRow(table, skin, "Expand raft",     "Hold Hammer (slot 1), aim at raft edge, left-click");
		addRow(table, skin, "Place buildings", "Select a building from the hotbar, aim at empty tile");
		addRow(table, skin, "Cook food",       "Place a Cooking Pot, right-click it, select an item");
		addRow(table, skin, "Drink water",     "Place a Water Filter and right-click when charged");
		addRow(table, skin, "Steer raft",      "Place a Sail and right-click it to set heading");

		table.add().expandY().row(); // vertical spacer

		TextButton backButton = new TextButton("Back", skin);
		backButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				screenManager.pop();
			}
		});
		table.add(backButton).colspan(2).width(240f).height(60f).padTop(20f).row();
	}

	// ── Layout helpers ────────────────────────────────────────────────────────

	/** Yellow section header spanning both columns. */
	private void addSection(Table table, Skin skin, String heading) {
		table.add(new Label(heading, skin, "yellow"))
				.colspan(2).left().padTop(20f).padBottom(6f).row();
	}

	/** One instruction row: white key label on left, white description on right. */
	private void addRow(Table table, Skin skin, String key, String description) {
		table.add(new Label(key + ":", skin, "white")).right().padRight(20f).padBottom(4f);
		table.add(new Label(description, skin, "white")).left().padBottom(4f).row();
	}

	// ── Screen lifecycle ──────────────────────────────────────────────────────

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
		stage.setScrollFocus(scrollPane);
	}

	@Override public void pause()  {}
	@Override public void resume() {}
	@Override public void hide()   {}

	@Override
	public void dispose() {
		for (Disposable d : disposables) d.dispose();
	}
}