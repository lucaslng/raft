package com.lucaslng.raft.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.lucaslng.raft.input.Keybinds;
import com.lucaslng.raft.rendering.GameRenderer;
import com.lucaslng.raft.world.World;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.input.InputManager;

class GameScreen implements Screen {

	private World world;
	private GameRenderer gameRenderer;
	private PerspectiveCamera cam;
	private Keybinds keybinds;

	protected GameScreen(Assets assets, ScreenManager screenManager) {
		world = new World(assets);
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		cam.position.set(0, 1f, 0f);
		cam.lookAt(0, 0, 1);
		cam.near = .1f;
		cam.far = 1000f;
		cam.update();
		Gdx.input.setInputProcessor(new InputManager());
		keybinds = new Keybinds();
		gameRenderer = new GameRenderer(assets, world);

		Music music = assets.get("music/The Pirate's Waltz.mp3", Music.class);
		music.setVolume(.3f);
		music.play();
		music.setLooping(true);

	}

	@Override
	public void show() {
		Gdx.input.setCursorCatched(true);
	}

	@Override
	public void render(float delta) {
		float sensitivity = 4f;
		updateCameraRotation(cam, delta, sensitivity);


		float speed = .1f;
		Vector3 camDir = cam.direction.cpy();
		camDir.y = 0f;
		if (keybinds.moveRight.isPressed())
			cam.translate(camDir.cpy().crs(cam.up).nor().scl(speed));
		if (keybinds.moveLeft.isPressed())
			cam.translate(camDir.cpy().crs(cam.up).nor().scl(-speed));
		if (keybinds.moveForward.isPressed())
			cam.translate(camDir.cpy().nor().scl(speed));
		if (keybinds.moveBack.isPressed())
			cam.translate(camDir.cpy().nor().scl(-speed));

		// world.getPlayer().getPosition(cam.position);
		// world.getPlayer().setRotation(camDir,);
		cam.update();


		world.update(delta, cam);

		gameRenderer.render(delta, cam);

	}

	public void updateCameraRotation(Camera camera, float delta, float sensitivity) {
		if (!Gdx.input.isCursorCatched())
			return;

		float scale = delta * sensitivity;
		float deltaX = -Gdx.input.getDeltaX() * scale;
		float deltaY = -Gdx.input.getDeltaY() * scale;

		camera.rotate(Vector3.Y, deltaX);

		Vector3 right = camera.direction.cpy().crs(camera.up).nor();
		camera.rotate(right, deltaY);

		camera.update();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = width;
		cam.viewportHeight = height;
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
