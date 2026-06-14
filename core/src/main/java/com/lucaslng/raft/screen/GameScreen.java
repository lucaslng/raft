package com.lucaslng.raft.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.PanelOpenedEvent;
import com.lucaslng.raft.event.events.PlayerDeathEvent;
import com.lucaslng.raft.event.events.ToggleInventoryEvent;
import com.lucaslng.raft.player.PlayerController;
import com.lucaslng.raft.rendering.GameRenderer;
import com.lucaslng.raft.world.SwimmingSystem;
import com.lucaslng.raft.world.World;

/**
 * The main game screen.
 *
 * <h3>Execution order (each frame)</h3>
 * <ol>
 * <li>{@link PlayerController#update(float)} — mouse-look + WASD velocity</li>
 * <li>{@link SwimmingSystem#update} — buoyancy + water drag (needs updated cam
 * direction)</li>
 * <li>{@link World#update(float, com.badlogic.gdx.graphics.Camera)} — physics +
 * AI + raft + raycast</li>
 * <li>{@link GameRenderer#render(float, com.badlogic.gdx.graphics.Camera)} —
 * draw</li>
 * </ol>
 *
 * <h3>Input pipeline</h3>
 * 
 * <pre>
 *   InputMultiplexer
 *     1. HUD Stage          — Scene2D UI eats events when panels/inventory open
 *     2. PlayerController   — hotbar number keys, inventory toggle (keyDown)
 *     3. ClickInputAdapter  — left/right click for world interaction
 * </pre>
 *
 * <h3>Cursor policy</h3>
 * The cursor is caught (hidden) for first-person mouse-look. It is released
 * whenever a UI panel or the inventory opens, and re-caught when they close.
 */
public class GameScreen implements Screen {

	// ── Core systems ──────────────────────────────────────────────────────────

	private final World world;
	private final GameRenderer renderer;
	private final PlayerController playerController;
	private final SwimmingSystem swimmingSystem;

	// ── Input ─────────────────────────────────────────────────────────────────

	private final InputMultiplexer inputMultiplexer;

	// ── State ─────────────────────────────────────────────────────────────────

	/** True while any panel (workbench, cooking, sail …) is open. */
	private boolean panelOpen = false;

	/** True while the backpack / inventory overlay is visible. */
	private boolean inventoryOpen = false;

	// ── Constructor ───────────────────────────────────────────────────────────

	public GameScreen() {
		EventBus events = new EventBus();
		world = new World();

		// ── Camera ────────────────────────────────────────────────────────
		PerspectiveCamera camera = new PerspectiveCamera(
				70f,
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight());
		camera.near = 0.05f;
		camera.far = 500f;
		camera.update();

		// ── Swimming ──────────────────────────────────────────────────────
		swimmingSystem = new SwimmingSystem();

		// ── Player controller ─────────────────────────────────────────────
		playerController = new PlayerController(
				camera,
				world.getPlayer(),
				swimmingSystem,
				world.getRaftSystem());

		// ── Renderer ──────────────────────────────────────────────────────
		renderer = new GameRenderer(world);

		// ── Input multiplexer ─────────────────────────────────────────────
		inputMultiplexer = new InputMultiplexer();
		// 1. Stage — Scene2D UI must be first.
		inputMultiplexer.addProcessor(renderer.getHudStage());
		// 2. PlayerController — handles hotbar keys and inventory toggle.
		inputMultiplexer.addProcessor(playerController);
		// 3. Click handler.
		inputMultiplexer.addProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				if (panelOpen || inventoryOpen)
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

		// ── Event subscriptions ───────────────────────────────────────────

		// Panel opened/closed (workbench, cooking, sail …)
		events.subscribe(PanelOpenedEvent.class, new Subscriber<PanelOpenedEvent>() {
			@Override
			public void accept(PanelOpenedEvent event) {
				panelOpen = (event.panel != null);
				refreshCursor();
			}
		});

		// Inventory toggle (E key) — HUDRenderer flips its own boolean;
		// we mirror it here for cursor/mouselook control.
		events.subscribe(ToggleInventoryEvent.class, new Subscriber<ToggleInventoryEvent>() {
			@Override
			public void accept(ToggleInventoryEvent event) {
				inventoryOpen = !inventoryOpen;
				refreshCursor();
			}
		});

		events.subscribe(PlayerDeathEvent.class, new Subscriber<PlayerDeathEvent>() {
			@Override
			public void accept(PlayerDeathEvent event) {
				Gdx.app.log("GameScreen", "Player died.");
				// TODO: transition to death/respawn screen.
			}
		});
	}

	/**
	 * Syncs cursor-caught state and mouse-look enable with panel/inventory flags.
	 * Mouse-look is active only when the cursor is caught.
	 */
	private void refreshCursor() {
		boolean uiOpen = panelOpen || inventoryOpen;
		Gdx.input.setCursorCatched(!uiOpen);
		playerController.setMouseLookActive(!uiOpen);
	}

	// ── Screen lifecycle ──────────────────────────────────────────────────────

	@Override
	public void show() {
		Gdx.input.setInputProcessor(inputMultiplexer);
		refreshCursor();
	}

	@Override
	public void render(float delta) {
		// Cap delta so physics don't explode after a hitch.
		float dt = Math.min(delta, 1f / 30f);

		// 1. Mouse-look + WASD velocity (sets body velocity + camera direction).
		playerController.update(dt);

		// 2. Swimming (uses camera.direction for swim steering).
		swimmingSystem.update(
				world.getPlayer().getBody(),
				world.getPlayer().getPosition().y,
				playerController.getCamera().direction);

		// 3. Physics, AI, raft drift, trash, raycast.
		world.update(dt, playerController.getCamera());

		// 4. Render.
		renderer.render(dt, playerController.getCamera());
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