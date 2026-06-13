package com.lucaslng.raft.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.Subscriber;
import com.lucaslng.raft.event.events.*;
import com.lucaslng.raft.input.InputManager;
import com.lucaslng.raft.input.Keybinds;
import com.lucaslng.raft.rendering.GameRenderer;
import com.lucaslng.raft.world.World;

/**
 * The main gameplay screen.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Owns the camera and drives its movement from {@link Keybinds}.</li>
 *   <li>Dispatches hotbar key presses and inventory toggle events.</li>
 *   <li>Forwards left-click to {@link World#handleLeftClick()} — click dispatch
 *       is no longer buried inside {@code World.update()}.</li>
 *   <li>Stores the {@link Music} reference so it can be stopped and GC'd when
 *       the screen is disposed.</li>
 * </ul>
 *
 * <h3>Camera</h3>
 * Currently a free-flying camera. When first-person character control is
 * implemented, this block should be replaced with a controller that reads
 * {@code world.getPlayer()} position and applies physics-based movement forces.
 */
class GameScreen implements Screen {

	private static final float CAMERA_SENSITIVITY = 4f;
	private static final float CAMERA_SPEED       = 0.1f;

	private final World        world;
	private final GameRenderer gameRenderer;
	private final EventBus     events;
	private final Keybinds     keybinds;
	private final PerspectiveCamera cam;

	protected GameScreen(Assets assets, ScreenManager screenManager) {
		events = new EventBus();
		world  = new World(assets, events);

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 1f, 0f);
		cam.lookAt(0, 0, 1);
		cam.near = .1f;
		cam.far  = 1000f;
		cam.update();

		keybinds     = new Keybinds();
		gameRenderer = new GameRenderer(assets, world, events);

		Music music = assets.get("music/The Pirate's Waltz.mp3", Music.class);
		music.setVolume(.2f);
		music.setLooping(true);
		music.play();

		// ── Sound effect subscriptions ────────────────────────────────────
		Sound tileSfx = assets.get("sfx/tile-placed.mp3", Sound.class);
		events.subscribe(TilePlacedEvent.class, e -> tileSfx.play(.9f));

		Sound buildSfx = assets.get("sfx/building-placed.mp3", Sound.class);
		events.subscribe(BuildingPlacedEvent.class, e -> buildSfx.play(.9f));
	}

	@Override
	public void show() {
		Gdx.input.setCursorCatched(true);
		Gdx.input.setInputProcessor(new InputManager());
	}

	@Override
	public void render(float delta) {
		// ── Camera rotation ───────────────────────────────────────────────
		if (Gdx.input.isCursorCatched()) {
			float scale  = delta * CAMERA_SENSITIVITY;
			float deltaX = -Gdx.input.getDeltaX() * scale;
			float deltaY = -Gdx.input.getDeltaY() * scale;
			cam.rotate(Vector3.Y, deltaX);
			cam.rotate(cam.direction.cpy().crs(cam.up).nor(), deltaY);
		}

		// ── Camera translation ─────────────────────────────────────────────
		Vector3 flatDir = new Vector3(cam.direction).nor();
		flatDir.y = 0f;
		if (keybinds.moveForward.isPressed()) cam.translate(flatDir.cpy().scl( CAMERA_SPEED));
		if (keybinds.moveBack   .isPressed()) cam.translate(flatDir.cpy().scl(-CAMERA_SPEED));
		Vector3 right = flatDir.cpy().crs(cam.up).nor();
		if (keybinds.moveRight  .isPressed()) cam.translate(right.cpy().scl( CAMERA_SPEED));
		if (keybinds.moveLeft   .isPressed()) cam.translate(right.cpy().scl(-CAMERA_SPEED));

		cam.update();

		// ── Hotbar / inventory input ───────────────────────────────────────
		if (keybinds.toggleInventory.isKeyJustPressed())
			events.post(new ToggleInventoryEvent());

		for (int i = 0; i < keybinds.hotbar.length; i++) {
			if (keybinds.hotbar[i].isKeyJustPressed())
				events.post(new HotbarIndexEvent(i));
		}

		// ── Click dispatch ────────────────────────────────────────────────
		// Left-click is forwarded after world.update() has run checkRaycast(),
		// so hoveredEntity / hoveredRaftTile are always current this frame.
		world.update(delta, cam);

		if (Gdx.input.isButtonJustPressed(Buttons.LEFT))
			world.handleLeftClick();

		// ── Render ────────────────────────────────────────────────────────
		gameRenderer.render(delta, cam);
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth  = width;
		cam.viewportHeight = height;
		cam.update();
		gameRenderer.resize(width, height);
	}

	@Override public void pause()  {}
	@Override public void resume() {}
	@Override public void hide()   {}

	@Override
	public void dispose() {
		world.dispose();
		gameRenderer.dispose();
	}
}