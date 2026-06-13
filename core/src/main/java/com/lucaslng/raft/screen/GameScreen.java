package com.lucaslng.raft.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.assets.SoundManager;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.event.events.*;
import com.lucaslng.raft.input.InputManager;
import com.lucaslng.raft.rendering.GameRenderer;
import com.lucaslng.raft.settings.Settings;
import com.lucaslng.raft.world.World;

/**
 * The main gameplay screen.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Owns the camera and drives its movement from {@link Settings}.</li>
 * <li>Dispatches hotbar key presses and inventory toggle events.</li>
 * <li>Forwards left-click to {@link World#handleLeftClick()}.</li>
 * <li>Forwards right-click to {@link World#handleRightClick()} so the player
 * can open building UI panels.</li>
 * </ul>
 */
class GameScreen implements Screen {

	private static final float CAMERA_SENSITIVITY = 4f;
	private static final float CAMERA_SPEED = 0.1f;

	private final World world;
	private final GameRenderer gameRenderer;
	private final EventBus events;
	private final Settings keybinds;
	private final PerspectiveCamera cam;

	protected GameScreen() {
		events = new EventBus();
		world = new World();

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 1f, 0f);
		cam.lookAt(0, 0, 1);
		cam.near = .1f;
		cam.far = 1000f;
		cam.update();

		keybinds = new Settings();
		gameRenderer = new GameRenderer(world);

		new SoundManager();
	}

	@Override
	public void show() {
		Gdx.input.setCursorCatched(true);
		// HUD stage is first so building-panel clicks aren't eaten by the game input.
		com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
		multiplexer.addProcessor(gameRenderer.getHudStage());
		multiplexer.addProcessor(new InputManager());
		Gdx.input.setInputProcessor(multiplexer);
	}

	@Override
	public void render(float delta) {
		// ── Camera rotation ───────────────────────────────────────────────
		if (Gdx.input.isCursorCatched()) {
			float scale = delta * CAMERA_SENSITIVITY;
			float deltaX = -Gdx.input.getDeltaX() * scale;
			float deltaY = -Gdx.input.getDeltaY() * scale;
			cam.rotate(Vector3.Y, deltaX);
			cam.rotate(cam.direction.cpy().crs(cam.up).nor(), deltaY);
		}

		// ── Camera translation ─────────────────────────────────────────────
		Vector3 flatDir = new Vector3(cam.direction).nor();
		flatDir.y = 0f;
		if (keybinds.moveForward.isPressed())
			cam.translate(flatDir.cpy().scl(CAMERA_SPEED));
		if (keybinds.moveBack.isPressed())
			cam.translate(flatDir.cpy().scl(-CAMERA_SPEED));
		Vector3 right = flatDir.cpy().crs(cam.up).nor();
		if (keybinds.moveRight.isPressed())
			cam.translate(right.cpy().scl(CAMERA_SPEED));
		if (keybinds.moveLeft.isPressed())
			cam.translate(right.cpy().scl(-CAMERA_SPEED));
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
			cam.translate(cam.up.cpy().scl(-CAMERA_SPEED));
		if (keybinds.jump.isPressed())
			cam.translate(cam.up.cpy().scl(CAMERA_SPEED));

		cam.update();

		// ── Hotbar / inventory input ───────────────────────────────────────
		if (keybinds.toggleInventory.isKeyJustPressed())
			events.post(new ToggleInventoryEvent());

		for (int i = 0; i < keybinds.hotbar.length; i++) {
			if (keybinds.hotbar[i].isKeyJustPressed())
				events.post(new HotbarIndexEvent(i));
		}

		// ── World update ──────────────────────────────────────────────────
		// Must run before click dispatch so hoveredEntity/hoveredRaftTile are current.
		world.update(delta, cam);

		// ── Click dispatch ────────────────────────────────────────────────
		if (Gdx.input.isButtonJustPressed(Buttons.LEFT))
			world.handleLeftClick();

		if (Gdx.input.isButtonJustPressed(Buttons.RIGHT))
			world.handleRightClick();

		// ── Render ────────────────────────────────────────────────────────
		gameRenderer.render(delta, cam);
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = width;
		cam.viewportHeight = height;
		cam.update();
		gameRenderer.resize(width, height);
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
		world.dispose();
		gameRenderer.dispose();
	}
}