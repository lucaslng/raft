package com.lucaslng.raft.rendering.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.building.SailBuilding;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.*;
import com.lucaslng.raft.item.Item;
import com.lucaslng.raft.player.Hotbar;
import com.lucaslng.raft.player.PlayerStats;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.util.Util;
import com.lucaslng.raft.world.Clickable;
import com.lucaslng.raft.world.World;

/**
 * Renders the 2-D heads-up display including:
 * <ul>
 * <li>Stat bars (health, hunger, thirst)</li>
 * <li>Placement hint label</li>
 * <li>Inventory overlay</li>
 * <li>Building UI panels — opened when the player right-clicks a building</li>
 * </ul>
 *
 * <h3>Building panels</h3>
 * <p>
 * When a {@link BuildingClickedEvent} is posted the HUD closes any open
 * panel and opens the one appropriate for the clicked building type.
 * Panels are implemented as Scene2D {@link Window} actors added to the stage.
 * Closing a panel releases the cursor back to the game.
 * </p>
 *
 * <h3>Sail compass</h3>
 * <p>
 * The sail panel contains a 360° compass wheel (rendered as a {@link Slider}
 * from 0–360 with a degree label). Moving the slider fires a
 * {@link SailSteerEvent} which the {@link SailBuilding} subscribes to.
 * </p>
 */
public class HUDRenderer implements Disposable {

	private final Skin skin = new Skin();

	private final Stage stage;
	private final Label fpsLabel;
	private final Label hintLabel;
	private final List<Disposable> disposables = new ArrayList<>();

	private final ProgressBar healthBar, hungerBar, thirstBar;

	private final Table hotbarTable = new Table(skin);

	private final Table inventoryTable = new Table(skin);
	private boolean isInventoryOpen = false;

	private final BuildingRegistry buildingRegistry;

	private final Table panelTable = new Table(skin);

	public HUDRenderer(Assets assets, EventBus events, BuildingRegistry buildingRegistry) {
		this.buildingRegistry = buildingRegistry;

		BitmapFont font = assets.get("main42.ttf", BitmapFont.class);
		LabelStyle whiteStyle = new LabelStyle(font, Color.WHITE);
		LabelStyle greenStyle = new LabelStyle(font, new Color(0.4f, 1f, 0.4f, 1f));
		LabelStyle redStyle = new LabelStyle(font, new Color(1f, 0.4f, 0.4f, 1f));
		LabelStyle yellowStyle = new LabelStyle(font, new Color(0.9f, 0.95f, 0.5f, 1f));

		Texture buttonUp = Util.generateTexture(
				new Color(0.25f, 0.25f, 0.25f, 1f), 4);

		Texture buttonDown = Util.generateTexture(
				new Color(0.15f, 0.15f, 0.15f, 1f), 4);

		Texture sliderBg = Util.generateTexture(
				new Color(0.3f, 0.3f, 0.3f, 1f), 4);

		Texture sliderKnob = Util.generateTexture(
				Color.WHITE, 16);

		disposables.add(buttonUp);
		disposables.add(buttonDown);
		disposables.add(sliderBg);
		disposables.add(sliderKnob);

		TextButtonStyle buttonStyle = new TextButtonStyle();
		buttonStyle.up = new TextureRegionDrawable(buttonUp);
		buttonStyle.down = new TextureRegionDrawable(buttonDown);
		buttonStyle.font = font;

		SliderStyle sliderStyle = new SliderStyle();
		sliderStyle.background = new TextureRegionDrawable(sliderBg);
		sliderStyle.knob = new TextureRegionDrawable(sliderKnob);

		skin.add("white", whiteStyle);
		skin.add("default", whiteStyle);
		skin.add("green", greenStyle);
		skin.add("red", redStyle);
		skin.add("yellow", yellowStyle);
		skin.add("default", buttonStyle);
		skin.add("default-horizontal", sliderStyle);

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		// ── FPS label ─────────────────────────────────────────────────────
		fpsLabel = new Label("", skin);
		fpsLabel.addAction(new Action() {
			@Override
			public boolean act(float delta) {
				((Label) actor).setText("FPS: " + Gdx.graphics.getFramesPerSecond());
				return false;
			}
		});
		stage.addActor(new Container<>(fpsLabel).top().left().padLeft(10f).padTop(50f));

		// ── Crosshair ──────────────────────────────────────────────────────
		Container<Image> crosshair = new Container<>(
				new Image(assets.get("images/crosshair-normal.png", Texture.class)));
		crosshair.setFillParent(true);
		crosshair.center().size(32);
		stage.addActor(crosshair);

		// ── Hint label ────────────────────────────────────────────────────
		hintLabel = new Label("", skin, "yellow");
		Container<Label> hintContainer = new Container<>(hintLabel).top().center().padTop(64f);
		hintContainer.setFillParent(true);
		stage.addActor(hintContainer);

		// ── Stat bars ─────────────────────────────────────────────────────
		Texture barBg = Util.generateTexture(Color.BROWN, 20);
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
		statTable.defaults().width(300).height(60).padBottom(4).left();
		statTable.add(healthBar).row();
		statTable.add(hungerBar).row();
		statTable.add(thirstBar);
		statTable.bottom().left().pad(16);
		stage.addActor(statTable);

		// Hotbar
		hotbarTable.setFillParent(true);
		hotbarTable.defaults().width(280).height(50).padBottom(4).right();
		hotbarTable.bottom().right().pad(16);
		stage.addActor(hotbarTable);

		// ── Inventory ─────────────────────────────────────────────────────
		inventoryTable.setFillParent(true);
		inventoryTable.top().left().pad(14f);
		inventoryTable.setVisible(false);
		stage.addActor(inventoryTable);

		events.subscribe(ToggleInventoryEvent.class, event -> {
			isInventoryOpen = !isInventoryOpen;
			Gdx.input.setCursorCatched(!isInventoryOpen);
			inventoryTable.setVisible(isInventoryOpen);
		});

		// Panels
		panelTable.setVisible(false);
		panelTable.setFillParent(true);
		Texture bg = Util.generateTexture(
				new Color(0f, 0f, 0f, 0.85f));
		panelTable.setBackground(new TextureRegionDrawable(bg));
		disposables.add(bg);
		stage.addActor(panelTable);
		events.subscribe(PanelOpenedEvent.class, event -> {
			if (event.panel != null) {
				panelTable.clear();
				panelTable.setVisible(true);
				event.panel.populate(panelTable);
				Gdx.input.setCursorCatched(false);
			} else {
				panelTable.setVisible(false);
				panelTable.clear();
				Gdx.input.setCursorCatched(true);
			}
		});
	}

	// ── Per-frame render ─────────────────────────────────────────────────────

	public void render(World world, float delta) {
		// ── Stat bars ──────────────────────────────────────────────────────
		PlayerStats stats = world.getPlayer().getStats();
		healthBar.setValue(stats.getHealth());
		hungerBar.setValue(stats.getHunger());
		thirstBar.setValue(stats.getThirst());

		// hotbar
		hotbarTable.clear();
		Hotbar hotbar = world.getPlayer().getHotbar();
		for (int i = 0; i < Hotbar.HOTBAR_SIZE; i++) {
			Holdable h = hotbar.getItem(i);
			if (h != null)
				hotbarTable.add(new Label(String.format("[%d] %s", i + 1, h.getName()), skin,
						i == hotbar.getHeldIndex() ? "green" : "white")).row();
		}

		// ── Hint label ─────────────────────────────────────────────────────
		Holdable held = world.getPlayer().getHotbar().getHeldItem();

		if (held instanceof Hammer && world.getPlacementGhost().isVisible()) {
			int wood = world.getPlayer().getBackpack().getCount("Wood");
			boolean can = wood >= Hammer.WOOD_COST;
			hintLabel.setStyle(can ? skin.get("green", LabelStyle.class) : skin.get("red", LabelStyle.class));
			hintLabel.setText("[LMB] Place plank  (Wood: " + wood + " / " + Hammer.WOOD_COST + ")");

		} else if (held instanceof BuildingItem) {
			BuildingItem item = (BuildingItem) held;
			RaftTile tile = world.getHoveredRaftTile();

			if (tile == null) {
				hintLabel.setStyle(skin.get("yellow", LabelStyle.class));
				hintLabel.setText(item.getName() + " - aim at an empty raft tile");
			} else if (tile.hasBuilding()) {
				hintLabel.setStyle(skin.get("yellow", LabelStyle.class));
				hintLabel.setText("Tile occupied: " + tile.getBuilding().getName());
			} else {
				Map<String, Integer> cost = buildingRegistry.getCost(item.getName());
				boolean canAfford = canAffordAll(world, cost);
				hintLabel.setStyle(canAfford ? skin.get("green", LabelStyle.class) : skin.get("red", LabelStyle.class));
				hintLabel.setText(buildCostString(item.getName(), world, cost));
			}

		} else {
			Clickable hoveredClickable = world.getHoveredClickable();
			hintLabel.setText(hoveredClickable != null ? hoveredClickable.getInteractHint() : "");
		}

		// ── Inventory list ─────────────────────────────────────────────────
		if (isInventoryOpen) {
			inventoryTable.clear();
			for (Map.Entry<Item, Integer> entry : world.getPlayer().getBackpack().getSortedBackpackView()) {
				inventoryTable.add(
						new Label(entry.getKey().name + " x" + entry.getValue(), skin)).row();
			}
		}

		stage.act(delta);
		stage.draw();
	}

	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	public Stage getStage() {
		return stage;
	}

	@Override
	public void dispose() {
		for (Disposable d : disposables)
			d.dispose();
	}

	private static ProgressBar makeStatBar(Texture bg, Texture fill) {
		ProgressBarStyle style = new ProgressBarStyle();
		style.background = new TextureRegionDrawable(bg);
		style.knobBefore = new TextureRegionDrawable(fill);
		style.knob = new TextureRegionDrawable(fill);
		ProgressBar bar = new ProgressBar(0f, 1f, 0.001f, false, style);
		bar.setValue(1f);
		return bar;
	}

	private boolean canAffordAll(World world, Map<String, Integer> cost) {
		if (cost == null)
			return false;
		for (Map.Entry<String, Integer> e : cost.entrySet())
			if (world.getPlayer().getBackpack().getCount(e.getKey()) < e.getValue())
				return false;
		return true;
	}

	private String buildCostString(String name, World world, Map<String, Integer> cost) {
		StringBuilder sb = new StringBuilder("[LMB] Place ").append(name).append("  (");
		if (cost != null) {
			boolean first = true;
			for (Map.Entry<String, Integer> e : cost.entrySet()) {
				if (!first)
					sb.append("  ");
				first = false;
				int have = world.getPlayer().getBackpack().getCount(e.getKey());
				sb.append(e.getKey()).append(": ").append(have).append('/').append(e.getValue());
			}
		}
		return sb.append(')').toString();
	}
}