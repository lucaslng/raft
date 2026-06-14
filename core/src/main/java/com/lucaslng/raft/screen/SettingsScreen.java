package com.lucaslng.raft.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.settings.Keybind;
import com.lucaslng.raft.settings.Settings;

/**
 * Settings screen exposing:
 * <ul>
 * <li>Master volume slider</li>
 * <li>FOV slider</li>
 * <li>Debug wireframe toggle</li>
 * <li>Keybind rebinding for all movement/action keys</li>
 * <li>Reset all to defaults</li>
 * <li>Back button</li>
 * </ul>
 *
 * Uses the project's custom skin ({@link Assets#getSkin()}) — NOT the golden-ui
 * skin.
 * Keybind rebinding works by clicking a button to enter "listening" mode, then
 * pressing any key to assign it. ESC cancels rebinding.
 */
class SettingsScreen implements Screen {

	private final Stage stage;
	private final ScrollPane scrollPane;
	private final List<Disposable> disposables = new ArrayList<>();
	private final Settings settings;

	/** The keybind currently waiting for a key press. Null = not rebinding. */
	private Keybind rebindingTarget = null;
	/** The button label showing "[press key…]" during rebinding. */
	private TextButton rebindingButton = null;
	/** Original button text, restored on cancel. */
	private String rebindingOriginalText = null;

	protected SettingsScreen() {
		settings = Settings.get();
		Assets assets = Assets.get();
		ScreenManager screenManager = ScreenManager.get();

		// Use the project's custom skin, not golden-ui
		Skin skin = assets.getSkin();

		stage = new Stage(new ExtendViewport(
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight()));
		disposables.add(stage);

		Table wrapper = new Table(skin);
		wrapper.setFillParent(true);
		wrapper.setBackground(skin.getDrawable("bg"));
		stage.addActor(wrapper);

		// Root table — fills the screen, centres all content
		Table root = new Table(skin);
		root.pad(30f);
		root.top();
		scrollPane = new ScrollPane(root, skin);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false);
		wrapper.add(scrollPane).expand().fill();

		// ── Title ──────────────────────────────────────────────────────────
		Label title = new Label("Settings", skin, "title");
		root.add(title).colspan(2).padBottom(30f).row();

		// ── Audio ──────────────────────────────────────────────────────────
		addSectionHeader(root, skin, "Audio");

		Label volLabel = new Label("Master Volume:  " + pct(settings.masterVolume), skin);
		Slider volSlider = new Slider(0f, 1f, 0.01f, false, skin);
		volSlider.setValue(settings.masterVolume);
		volSlider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				settings.masterVolume = volSlider.getValue();
				volLabel.setText("Master Volume:  " + pct(settings.masterVolume));
			}
		});
		root.add(volLabel).left().padBottom(6f);
		root.add(volSlider).width(400f).padBottom(6f).row();

		// ── Graphics ───────────────────────────────────────────────────────
		addSectionHeader(root, skin, "Graphics");

		Label fovLabel = new Label("Field of View:  " + (int) settings.fov + "°", skin);
		Slider fovSlider = new Slider(50f, 120f, 1f, false, skin);
		fovSlider.setValue(settings.fov);
		fovSlider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				settings.fov = fovSlider.getValue();
				fovLabel.setText("Field of View:  " + (int) settings.fov + "°");
			}
		});
		root.add(fovLabel).left().padBottom(6f);
		root.add(fovSlider).width(400f).padBottom(6f).row();

		Label debugLabel = new Label("Debugging:", skin);
		TextButton debugButton = new TextButton("", skin);
		debugButton.setChecked(settings.debug);
		debugButton.setText(debugButton.isChecked() ? "ON" : "OFF");
		debugButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				settings.debug = debugButton.isChecked();
				debugButton.setText(debugButton.isChecked() ? "ON" : "OFF");
			}
		});
		root.add(debugLabel).left().padBottom(6f);
		root.add(debugButton).right().width(200f).padBottom(6f).row();

		// ── Keybinds ───────────────────────────────────────────────────────
		addSectionHeader(root, skin, "Keybinds");

		// Named pairs: (display name, keybind)
		String[] keybindNames = { "Move Forward", "Move Back", "Move Left", "Move Right", "Jump", "Toggle Inventory" };
		Keybind[] keybinds = {
				settings.moveForward,
				settings.moveBack,
				settings.moveLeft,
				settings.moveRight,
				settings.jump,
				settings.toggleInventory,
		};

		for (int i = 0; i < keybinds.length; i++) {
			addKeybindRow(root, skin, keybindNames[i], keybinds[i]);
		}

		// Hotbar keys
		for (int i = 0; i < settings.hotbar.length; i++) {
			addKeybindRow(root, skin, "Hotbar " + (i + 1), settings.hotbar[i]);
		}

		// ── Bottom buttons ─────────────────────────────────────────────────
		root.row(); // vertical spacer

		Table buttonRow = new Table();
		root.add(buttonRow).colspan(2).fillX().padTop(20f).row();

		TextButton resetButton = new TextButton("Reset Defaults", skin);
		resetButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				cancelRebind();
				resetAllDefaults(volSlider, volLabel, fovSlider, fovLabel, debugButton);
			}
		});

		TextButton backButton = new TextButton("Back", skin);
		backButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				cancelRebind();
				screenManager.pop();
			}
		});

		buttonRow.add(resetButton).width(300f).height(60f).padRight(20f);
		buttonRow.add(backButton).width(200f).height(60f);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	/** Adds a full-width section header row. */
	private void addSectionHeader(Table root, Skin skin, String text) {
		Label header = new Label(text, skin, "yellow");
		root.add(header).colspan(2).left().padTop(16f).padBottom(4f).row();
	}

	/** Adds one keybind row: label on the left, rebind button on the right. */
	private void addKeybindRow(Table root, Skin skin, String name, Keybind keybind) {
		root.add(new Label(name + ":", skin)).left().padBottom(4f);

		TextButton btn = new TextButton(keybind.toString(), skin);
		btn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (rebindingTarget == keybind) {
					// Second click cancels
					cancelRebind();
				} else {
					startRebind(keybind, btn);
				}
			}
		});
		root.add(btn).width(200f).height(44f).padBottom(4f).row();
	}

	/** Enters rebinding mode for the given keybind and button. */
	private void startRebind(Keybind keybind, TextButton button) {
		cancelRebind(); // cancel any previous
		rebindingTarget = keybind;
		rebindingButton = button;
		rebindingOriginalText = button.getText().toString();
		button.setText("[press key]");
	}

	/** Cancels rebinding without changing anything. */
	private void cancelRebind() {
		if (rebindingButton != null) {
			rebindingButton.setText(rebindingOriginalText);
		}
		rebindingTarget = null;
		rebindingButton = null;
		rebindingOriginalText = null;
	}

	/** Commits a rebind and exits rebinding mode. */
	private void commitRebind(int keycode) {
		if (rebindingTarget == null)
			return;
		rebindingTarget.setKey(keycode);
		if (rebindingButton != null) {
			rebindingButton.setText(Input.Keys.toString(keycode));
		}
		rebindingTarget = null;
		rebindingButton = null;
		rebindingOriginalText = null;
	}

	/** Resets all settings to their defaults and syncs the UI controls. */
	private void resetAllDefaults(Slider volSlider, Label volLabel,
			Slider fovSlider, Label fovLabel,
			TextButton debugButton) {
		// Volume
		settings.masterVolume = 1f;
		volSlider.setValue(1f);
		volLabel.setText("Master Volume:  " + pct(1f));

		// FOV
		settings.fov = 67f;
		fovSlider.setValue(67f);
		fovLabel.setText("Field of View:  67°");

		// Debug
		settings.debug = true;
		debugButton.setChecked(true);

		// Keybinds
		settings.moveForward.reset();
		settings.moveBack.reset();
		settings.moveLeft.reset();
		settings.moveRight.reset();
		settings.jump.reset();
		settings.toggleInventory.reset();
		for (Keybind kb : settings.hotbar)
			kb.reset();

		Gdx.app.postRunnable(() -> ScreenManager.get().replace(new SettingsScreen()));
	}

	private static String pct(float v) {
		return (int) (v * 100f) + "%";
	}

	// ── Screen lifecycle ──────────────────────────────────────────────────────

	@Override
	public void show() {
		// Stage gets normal UI input. An InputAdapter layered second captures raw
		// key events for keybind rebinding without blocking stage clicks.
		InputAdapter rebindListener = new InputAdapter() {
			@Override
			public boolean keyDown(int keycode) {
				if (rebindingTarget == null)
					return false;
				if (keycode == Input.Keys.ESCAPE) {
					cancelRebind();
				} else {
					commitRebind(keycode);
				}
				return true; // consume so the key doesn't also fire on stage
			}
		};

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(stage); // UI buttons first
		multiplexer.addProcessor(rebindListener); // raw key capture second
		Gdx.input.setInputProcessor(multiplexer);
		stage.setScrollFocus(scrollPane);
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