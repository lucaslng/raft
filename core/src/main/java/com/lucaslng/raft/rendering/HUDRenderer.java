package com.lucaslng.raft.rendering;

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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.building.Building;
import com.lucaslng.raft.building.BuildingRegistry;
import com.lucaslng.raft.building.SailBuilding;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.BuildingClickedEvent;
import com.lucaslng.raft.event.events.SailSteerEvent;
import com.lucaslng.raft.event.events.ToggleInventoryEvent;
import com.lucaslng.raft.item.Item;
import com.lucaslng.raft.player.Hotbar;
import com.lucaslng.raft.player.PlayerStats;
import com.lucaslng.raft.player.holdable.BuildingItem;
import com.lucaslng.raft.player.holdable.Hammer;
import com.lucaslng.raft.player.holdable.Holdable;
import com.lucaslng.raft.raft.RaftTile;
import com.lucaslng.raft.util.Util;
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
class HUDRenderer implements Disposable {

	// Pre-built hint label styles — created once, reused every frame.
	private final LabelStyle greenStyle;
	private final LabelStyle redStyle;
	private final LabelStyle yellowStyle;
	private final LabelStyle whiteStyle;

	private final Stage stage;
	private final Label fpsLabel;
	private final Label hintLabel;
	private final List<Disposable> disposables = new ArrayList<>();

	private final ProgressBar healthBar, hungerBar, thirstBar;

	private final Table hotbarTable = new Table();

	private final Table inventoryTable = new Table();
	private boolean isInventoryOpen = false;

	private final BuildingRegistry buildingRegistry;
	private final EventBus events;
	private final Skin skin;

	/** Currently open building panel — {@code null} if none. */
	private Window openBuildingPanel = null;

	protected HUDRenderer(Assets assets, EventBus events, BuildingRegistry buildingRegistry) {
		this.buildingRegistry = buildingRegistry;
		this.events = events;

		skin = assets.get("skin/golden-ui-skin.json", Skin.class);

		BitmapFont font = assets.get("main42.ttf", BitmapFont.class);
		whiteStyle = new LabelStyle(font, Color.WHITE);
		greenStyle = new LabelStyle(font, new Color(0.4f, 1f, 0.4f, 1f));
		redStyle = new LabelStyle(font, new Color(1f, 0.4f, 0.4f, 1f));
		yellowStyle = new LabelStyle(font, new Color(0.9f, 0.95f, 0.5f, 1f));

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		// ── FPS label ─────────────────────────────────────────────────────
		fpsLabel = new Label("", whiteStyle);
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
		hintLabel = new Label("", yellowStyle);
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

		// ── Building panel listener ────────────────────────────────────────
		events.subscribe(BuildingClickedEvent.class, event -> {
			openBuildingPanel(event.building);
		});
	}

	// ── Per-frame render ─────────────────────────────────────────────────────

	protected void render(World world, float delta) {
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
				hotbarTable.add(new Label(String.format("[%d] %s", i + 1, h.getName()),
						i == hotbar.getHeldIndex() ? greenStyle : whiteStyle)).row();
		}

		// ── Hint label ─────────────────────────────────────────────────────
		Holdable held = world.getPlayer().getHotbar().getHeldItem();

		if (held instanceof Hammer && world.getPlacementGhost().isVisible()) {
			int wood = world.getPlayer().getBackpack().getCount("Wood");
			boolean can = wood >= Hammer.WOOD_COST;
			hintLabel.setStyle(can ? greenStyle : redStyle);
			hintLabel.setText("[LMB] Place plank  (Wood: " + wood + " / " + Hammer.WOOD_COST + ")");

		} else if (held instanceof BuildingItem) {
			BuildingItem item = (BuildingItem) held;
			RaftTile tile = world.getHoveredRaftTile();

			if (tile == null) {
				hintLabel.setStyle(yellowStyle);
				hintLabel.setText(item.getName() + " — aim at an empty raft tile");
			} else if (tile.hasBuilding()) {
				hintLabel.setStyle(yellowStyle);
				hintLabel.setText("Tile occupied: " + tile.getBuilding().getName());
			} else {
				Map<String, Integer> cost = buildingRegistry.getCost(item.getName());
				boolean canAfford = canAffordAll(world, cost);
				hintLabel.setStyle(canAfford ? greenStyle : redStyle);
				hintLabel.setText(buildCostString(item.getName(), world, cost));
			}

		} else {
			hintLabel.setText("");
		}

		// ── Inventory list ─────────────────────────────────────────────────
		if (isInventoryOpen) {
			inventoryTable.clear();
			for (Map.Entry<Item, Integer> entry : world.getPlayer().getBackpack().getSortedBackpackView()) {
				inventoryTable.add(
						new Label(entry.getKey().name + " x" + entry.getValue(), whiteStyle)).row();
			}
		}

		stage.act(delta);
		stage.draw();
	}

	protected void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	/**
	 * Exposes the Stage so {@link GameRenderer} can add it to an InputMultiplexer.
	 */
	protected Stage getStage() {
		return stage;
	}

	@Override
	public void dispose() {
		for (Disposable d : disposables)
			d.dispose();
	}

	// ── Building panels ──────────────────────────────────────────────────────

	/**
	 * Closes any open panel, then opens the panel appropriate for
	 * the clicked building. Uncatches the cursor so the player can
	 * interact with the UI.
	 */
	private void openBuildingPanel(Building building) {
		closeBuildingPanel();
		Gdx.input.setCursorCatched(false);

		if (building instanceof SailBuilding) {
			openBuildingPanel = buildSailPanel((SailBuilding) building);
		} else {
			// Generic "info" panel for unknown building types
			openBuildingPanel = buildGenericPanel(building);
		}

		stage.addActor(openBuildingPanel);
	}

	private void closeBuildingPanel() {
		if (openBuildingPanel != null) {
			openBuildingPanel.remove();
			openBuildingPanel = null;
			Gdx.input.setCursorCatched(true);
		}
	}

	// ── Sail panel ────────────────────────────────────────────────────────────

	/**
	 * Builds the sail steering panel.
	 *
	 * <p>
	 * Contains:
	 * <ul>
	 * <li>Wind direction indicator (read-only compass label)</li>
	 * <li>Heading slider 0–360° (clockwise from north/+Z)</li>
	 * <li>Current heading display</li>
	 * <li>Preset direction buttons (N, NE, E, SE, S, SW, W, NW)</li>
	 * <li>Follow Wind button (resets to wind direction)</li>
	 * </ul>
	 */
	private Window buildSailPanel(SailBuilding sail) {
		Window win = new Window("Sail Control", skin);
		win.setMovable(true);
		win.defaults().pad(5f);

		// ── Wind info ──────────────────────────────────────────────────────
		float windDeg = SailBuilding.toDeg(sail.getWindDir());
		win.add(new Label("Wind: " + formatBearing(windDeg), whiteStyle))
				.left().colspan(4).row();

		// ── Current heading label (updated by slider) ──────────────────────
		Label headingLabel = new Label("Heading: " + formatBearing(sail.getSteerAngleDeg()), whiteStyle);
		win.add(headingLabel).left().colspan(4).row();

		// ── Compass slider (0–360) ─────────────────────────────────────────
		Slider compass = new Slider(0f, 360f, 1f, false, skin);
		compass.setValue(sail.getSteerAngleDeg());

		compass.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				float deg = compass.getValue();
				headingLabel.setText("Heading: " + formatBearing(deg));
				events.post(new SailSteerEvent(deg));
			}
		});
		win.add(compass).colspan(4).width(300f).row();

		// ── Preset cardinal / intercardinal direction buttons ──────────────
		String[][] presets = {
				{ "NW", "N", "NE" },
				{ "W", "·", "E" },
				{ "SW", "S", "SE" }
		};
		float[] presetAngles = {
				315f, 0f, 45f,
				270f, -1f, 90f,
				225f, 180f, 135f
		};

		int idx = 0;
		for (String[] row : presets) {
			for (String label : row) {
				final float angle = presetAngles[idx++];
				if (label.equals("·")) {
					win.add(); // spacer
					continue;
				}
				TextButton btn = new TextButton(label, skin);
				btn.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						compass.setValue(angle);
						events.post(new SailSteerEvent(angle));
					}
				});
				win.add(btn).size(54f, 40f);
			}
			win.row();
		}

		// ── Follow Wind button ─────────────────────────────────────────────
		TextButton followWind = new TextButton("Follow Wind", skin);
		followWind.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				float windAngle = SailBuilding.toDeg(sail.getWindDir());
				compass.setValue(windAngle);
				events.post(new SailSteerEvent(windAngle));
			}
		});
		win.add(followWind).colspan(4).width(200f).padTop(6f).row();

		// ── Close ──────────────────────────────────────────────────────────
		TextButton close = new TextButton("Close", skin);
		close.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				closeBuildingPanel();
			}
		});
		win.add(close).colspan(4).width(120f).padTop(4f).row();

		win.pack();
		centerWindow(win);
		return win;
	}

	// ── Generic panel ─────────────────────────────────────────────────────────

	private Window buildGenericPanel(Building building) {
		Window win = new Window(building.getName(), skin);
		win.setMovable(true);
		win.defaults().pad(6f);
		win.add(new Label(building.getName(), whiteStyle)).left().row();
		TextButton close = new TextButton("Close", skin);
		close.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				closeBuildingPanel();
			}
		});
		win.add(close).padTop(10f).width(120f).row();
		win.pack();
		centerWindow(win);
		return win;
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private static ProgressBar makeStatBar(Texture bg, Texture fill) {
		ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
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

	/**
	 * Formats a bearing in degrees as a human-readable string, e.g. "45° NE".
	 */
	private static String formatBearing(float deg) {
		deg = ((deg % 360f) + 360f) % 360f;
		String[] cardinals = { "N", "NE", "E", "SE", "S", "SW", "W", "NW", "N" };
		int idx = (int) Math.round(deg / 45.0) % 8;
		return (int) deg + "°  " + cardinals[idx];
	}

	private void centerWindow(Window win) {
		float sw = Gdx.graphics.getBackBufferWidth();
		float sh = Gdx.graphics.getBackBufferHeight();
		win.setPosition((sw - win.getWidth()) / 2f, (sh - win.getHeight()) / 2f);
	}
}