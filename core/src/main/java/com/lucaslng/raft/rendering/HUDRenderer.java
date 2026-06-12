package com.lucaslng.raft.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.player.Backpack;
import com.lucaslng.raft.player.Hotbar;
import com.lucaslng.raft.player.PlayerStats;
import com.lucaslng.raft.util.Util;
import com.lucaslng.raft.world.World;

class HUDRenderer implements Disposable {

	private Stage stage;
	private Label fpsLabel;
	private List<Disposable> disposables;

	private ProgressBar healthBar, hungerBar, thirstBar;

	protected HUDRenderer(Assets assets) {
		disposables = new ArrayList<>();

		BitmapFont mainFont = assets.get("main18.ttf", BitmapFont.class);

		stage = new Stage(new ExtendViewport(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		// fps
		fpsLabel = new Label("", new LabelStyle(mainFont, Color.WHITE));
		fpsLabel.addAction(new Action() {
			@Override
			public boolean act(float delta) {
				((Label) actor).setText("FPS: " + Gdx.graphics.getFramesPerSecond());
				return false;
			}
		});
		Container<Label> fpsContainer = new Container<Label>(fpsLabel).bottom().left().padLeft(10f).padBottom(6f);
		stage.addActor(fpsContainer);

		// crosshair
		Container<Image> crosshairContainer = new Container<>(
				new Image(assets.get("images/crosshair-normal.png", Texture.class)));
		crosshairContainer.setFillParent(true);
		stage.addActor(crosshairContainer);
		crosshairContainer.center().size(16f);


		// hotbar
		HorizontalGroup hotbar = new HorizontalGroup();
		hotbar.setFillParent(true);
		stage.addActor(hotbar);
		hotbar.center().bottom();
		hotbar.setDebug(true, true);

		Texture slotBgTexture = Util.generateTexture(Color.BROWN);
		Texture slotfgTexture = Util.generateTexture(Color.GOLD);
		disposables.add(slotBgTexture);
		for (int i = 0; i < Hotbar.HOTBAR_SIZE; i++) {
			Container<Image> bg = new Container<>(new Image(slotBgTexture)).size(24);
			Container<Image> fg = new Container<>(new Image(slotfgTexture)).size(20).center();
			Stack slot = new Stack(bg, fg);
			hotbar.addActor(slot);
		}

		// stat bars
		Texture barBgTexture = Util.generateTexture(Color.BROWN, 20);
		Texture healthFill = Util.generateTexture(new Color(0.85f, 0.15f, 0.15f, 1f), 20); // red
		Texture hungerFill = Util.generateTexture(new Color(0.90f, 0.65f, 0.10f, 1f), 20); // orange
		Texture thirstFill = Util.generateTexture(new Color(0.20f, 0.55f, 0.90f, 1f), 20); // blue
		disposables.add(barBgTexture);
		disposables.add(healthFill);
		disposables.add(hungerFill);
		disposables.add(thirstFill);

		healthBar = makeStatBar(barBgTexture, healthFill);
		hungerBar = makeStatBar(barBgTexture, hungerFill);
		thirstBar = makeStatBar(barBgTexture, thirstFill);

		Table statTable = new Table();
		statTable.setFillParent(true);
		statTable.defaults().width(300).height(60).padBottom(4).right();
		statTable.add(healthBar).row();
		statTable.add(hungerBar).row();
		statTable.add(thirstBar);
		statTable.bottom().left().pad(16);
		stage.addActor(statTable);
	}

	private ProgressBar makeStatBar(Texture bg, Texture fill) {
		ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
		style.background = new TextureRegionDrawable(bg);
		style.knobBefore = new TextureRegionDrawable(fill);
		style.knob = new TextureRegionDrawable(fill);
		ProgressBar bar = new ProgressBar(0f, 1f, 0.001f, false, style);
		bar.setValue(1f);
		return bar;
	}

	protected void render(World world, float delta) {
		PlayerStats stats = world.getPlayer().getStats();
		healthBar.setValue(stats.getHealth());
    hungerBar.setValue(stats.getHunger());
    thirstBar.setValue(stats.getThirst());

		stage.act(delta);
		stage.draw();
	}

	protected void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		for (Disposable disposable : disposables)
			disposable.dispose();
	}

}
