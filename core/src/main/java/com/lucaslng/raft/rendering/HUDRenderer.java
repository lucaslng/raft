package com.lucaslng.raft.rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.ToggleInventoryEvent;
import com.lucaslng.raft.item.Item;
import com.lucaslng.raft.player.PlayerStats;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.util.Util;
import com.lucaslng.raft.world.World;

/**
 * Renders the 2-D heads-up display.
 */
class HUDRenderer implements Disposable {

	// Pre-built hint label styles — created once, reused every frame.
	private final LabelStyle hintGreen;
	private final LabelStyle hintRed;
	private final LabelStyle hintYellow;
	private final LabelStyle mainStyle;

	private final Stage         stage;
	private final Label         fpsLabel;
	private final Label         hintLabel;
	private final List<Disposable> disposables = new ArrayList<>();

	private final ProgressBar healthBar, hungerBar, thirstBar;

	private boolean     isInventoryOpen = false;
	private final Table inventoryTable;

	private final BuildingRegistry buildingRegistry;

	protected HUDRenderer(Assets assets, EventBus events, BuildingRegistry buildingRegistry) {
		this.buildingRegistry = buildingRegistry;

		BitmapFont font = assets.get("main18.ttf", BitmapFont.class);
		mainStyle   = new LabelStyle(font, Color.WHITE);
		hintGreen   = new LabelStyle(font, new Color(0.4f, 1f, 0.4f, 1f));
		hintRed     = new LabelStyle(font, new Color(1f, 0.4f, 0.4f, 1f));
		hintYellow  = new LabelStyle(font, new Color(0.9f, 0.95f, 0.5f, 1f));

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		// ── FPS label ─────────────────────────────────────────────────────
		fpsLabel = new Label("", mainStyle);
		fpsLabel.addAction(new Action() {
			@Override public boolean act(float delta) {
				((Label) actor).setText("FPS: " + Gdx.graphics.getFramesPerSecond());
				return false;
			}
		});
		stage.addActor(new Container<>(fpsLabel).bottom().left().padLeft(10f).padBottom(6f));

		// ── Crosshair ─────────────────────────────────────────────────────
		Container<Image> crosshair = new Container<>(
				new Image(assets.get("images/crosshair-normal.png", Texture.class)));
		crosshair.setFillParent(true);
		crosshair.center().size(16f);
		stage.addActor(crosshair);

		// ── Hint label ────────────────────────────────────────────────────
		hintLabel = new Label("", hintYellow);
		Container<Label> hintContainer = new Container<>(hintLabel).top().center().padTop(50f);
		hintContainer.setFillParent(true);
		stage.addActor(hintContainer);

		// ── Stat bars ─────────────────────────────────────────────────────
		Texture barBg     = Util.generateTexture(Color.BROWN, 20);
		Texture healthFill = Util.generateTexture(new Color(0.85f, 0.15f, 0.15f, 1f), 20);
		Texture hungerFill = Util.generateTexture(new Color(0.90f, 0.65f, 0.10f, 1f), 20);
		Texture thirstFill = Util.generateTexture(new Color(0.20f, 0.55f, 0.90f, 1f), 20);
		disposables.add(barBg);
		disposables.add(healthFill);
		disposables.add(hungerFill);
		disposables.add(thirstFill);

		healthBar = makeStatBar(barBg, healthFill);
		hungerBar = makeStatBar(barBg, hungerFill);
		thirstBar = makeStatBar(barBg, thirstFill);

		Table statTable = new Table();
		statTable.setFillParent(true);
		statTable.defaults().width(300).height(60).padBottom(4).right();
		statTable.add(healthBar).row();
		statTable.add(hungerBar).row();
		statTable.add(thirstBar);
		statTable.bottom().left().pad(16);
		stage.addActor(statTable);

		// ── Inventory ─────────────────────────────────────────────────────
		inventoryTable = new Table();
		inventoryTable.setFillParent(true);
		inventoryTable.top().left().pad(14f);
		inventoryTable.setVisible(false);
		stage.addActor(inventoryTable);

		events.subscribe(ToggleInventoryEvent.class, event -> {
			isInventoryOpen = !isInventoryOpen;
			Gdx.input.setCursorCatched(!isInventoryOpen);
			inventoryTable.setVisible(isInventoryOpen);
		});
	}

	protected void render(World world, float delta) {
		// ── Stat bars ──────────────────────────────────────────────────────
		PlayerStats stats = world.getPlayer().getStats();
		healthBar.setValue(stats.getHealth());
		hungerBar.setValue(stats.getHunger());
		thirstBar.setValue(stats.getThirst());

		// ── Hint label ─────────────────────────────────────────────────────
		Holdable held = world.getPlayer().getHotbar().getHeldItem();

		if (held instanceof Hammer && world.getPlacementGhost().isVisible()) {
			int  wood     = world.getPlayer().getBackpack().getCount("Wood");
			boolean can   = wood >= Hammer.WOOD_COST;
			hintLabel.setStyle(can ? hintGreen : hintRed);
			hintLabel.setText("[LMB] Place plank  (Wood: " + wood + " / " + Hammer.WOOD_COST + ")");

		} else if (held instanceof BuildingItem) {
			BuildingItem item = (BuildingItem) held;
			RaftTile tile    = world.getHoveredRaftTile();

			if (tile == null) {
				hintLabel.setStyle(hintYellow);
				hintLabel.setText(item.getName() + " — aim at an empty raft tile");
			} else if (tile.hasBuilding()) {
				hintLabel.setStyle(hintYellow);
				hintLabel.setText("Tile occupied: " + tile.getBuilding().getName());
			} else {
				Map<String, Integer> cost = buildingRegistry.getCost(item.getName());
				boolean canAfford = canAffordAll(world, cost);
				hintLabel.setStyle(canAfford ? hintGreen : hintRed);
				hintLabel.setText(buildCostString(item.getName(), world, cost));
			}

		} else {
			hintLabel.setText("");
		}

		// ── Inventory list ─────────────────────────────────────────────────
		if (isInventoryOpen) {
			inventoryTable.clear();
			for (Map.Entry<Item, Integer> entry :
					world.getPlayer().getBackpack().getSortedBackpackView()) {
				inventoryTable.add(
						new Label(entry.getKey().name + " x" + entry.getValue(), mainStyle)).row();
			}
		}

		stage.act(delta);
		stage.draw();
	}

	protected void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		for (Disposable d : disposables) d.dispose();
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private static ProgressBar makeStatBar(Texture bg, Texture fill) {
		ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
		style.background  = new TextureRegionDrawable(bg);
		style.knobBefore  = new TextureRegionDrawable(fill);
		style.knob        = new TextureRegionDrawable(fill);
		ProgressBar bar   = new ProgressBar(0f, 1f, 0.001f, false, style);
		bar.setValue(1f);
		return bar;
	}

	private boolean canAffordAll(World world, Map<String, Integer> cost) {
		if (cost == null) return false;
		for (Map.Entry<String, Integer> e : cost.entrySet())
			if (world.getPlayer().getBackpack().getCount(e.getKey()) < e.getValue()) return false;
		return true;
	}

	private String buildCostString(String name, World world, Map<String, Integer> cost) {
		StringBuilder sb = new StringBuilder("[LMB] Place ").append(name).append("  (");
		if (cost != null) {
			boolean first = true;
			for (Map.Entry<String, Integer> e : cost.entrySet()) {
				if (!first) sb.append("  ");
				first = false;
				int have = world.getPlayer().getBackpack().getCount(e.getKey());
				sb.append(e.getKey()).append(": ").append(have).append('/').append(e.getValue());
			}
		}
		return sb.append(')').toString();
	}
}