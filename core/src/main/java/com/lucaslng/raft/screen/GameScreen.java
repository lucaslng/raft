package com.lucaslng.raft.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.lucaslng.raft.assets.SoundManager;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.*;
import com.lucaslng.raft.player.PlayerController;
import com.lucaslng.raft.rendering.GameRenderer;
import com.lucaslng.raft.rendering.hud.GreetingPanel;
import com.lucaslng.raft.settings.Settings;
import com.lucaslng.raft.world.World;

// Main game screen
public class GameScreen implements Screen {

	private final World world;
	private final EventBus events;
	private final GameRenderer renderer;
	private final PlayerController playerController;
	private final InputMultiplexer inputMultiplexer;

	private boolean panelOpen = false;

	public GameScreen() {
		this.events = new EventBus();
		Settings settings = Settings.get();
		new SoundManager();
		world = new World();

		PerspectiveCamera camera = new PerspectiveCamera(
				settings.fov,
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight());
		camera.near = 0.05f;
		camera.far = 500f;
		camera.update();

		playerController = new PlayerController(
				camera,
				world.getPlayer(),
				world.getRaftSystem());

		renderer = new GameRenderer(world);

		// Order of input handling:
		inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(renderer.getHudStage());
		inputMultiplexer.addProcessor(playerController);
		inputMultiplexer.addProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				if (panelOpen)
					return false;
				if (button == Input.Buttons.LEFT) {
					world.handleLeftClick();
					return true;
				}
				if (button == Input.Buttons.RIGHT) {
					world.handleRightClick();
					return true;
				}
				return false;
			}
		});

		Gdx.input.setInputProcessor(inputMultiplexer);
		Gdx.input.setCursorCatched(true);

		// track whether there is a panel open
		events.subscribe(PanelOpenedEvent.class, new Subscriber<PanelOpenedEvent>() {
			@Override
			public void accept(PanelOpenedEvent event) {
				panelOpen = (event.panel != null);
				refreshCursor();
			}
		});

		// on death, show death screen
		// pop then push, so that when we go pop from the DeathScreen we go to the MainMenuScreen
		events.subscribe(PlayerDeathEvent.class, new Subscriber<PlayerDeathEvent>() {
			@Override
			public void accept(PlayerDeathEvent event) {
				Gdx.app.postRunnable(() -> {
					ScreenManager.get().pop();
					ScreenManager.get().push(new DeathScreen());
				});
			}
		});

		// on win, show win screen, same as death screen
		events.subscribe(WinEvent.class, new Subscriber<WinEvent>() {
			@Override
			public void accept(WinEvent event) {
				Gdx.app.postRunnable(() -> {
					ScreenManager.get().pop();
					ScreenManager.get().push(new WinScreen());
				});
			}
		});
	}

	private void refreshCursor() {
		boolean uiOpen = panelOpen; // there could be other ui's in the future
		Gdx.input.setCursorCatched(!uiOpen);
		playerController.setMouseLookActive(!uiOpen);
	}

	@Override
	public void show() {
		events.post(new PanelOpenedEvent(new GreetingPanel())); // show greeting panel at the start of game
		Gdx.input.setInputProcessor(inputMultiplexer);
		refreshCursor();
	}

	@Override
	public void render(float delta) {
		playerController.update(delta);
		world.update(delta, playerController.getCamera());
		renderer.render(delta, playerController.getCamera());
	}

	@Override
	public void resize(int width, int height) {
		renderer.resize(width, height);

		PerspectiveCamera cam = playerController.getCamera();
		cam.viewportWidth = width;
		cam.viewportHeight = height;
		cam.update();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
		Gdx.input.setCursorCatched(false);
	}

	@Override
	public void dispose() {
		world.dispose();
		renderer.dispose();
	}
}