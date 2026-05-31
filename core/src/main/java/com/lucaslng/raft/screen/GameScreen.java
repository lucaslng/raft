package com.lucaslng.raft.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.lucaslng.raft.input.Keybinds;
import com.lucaslng.raft.rendering.GameRenderer;
import com.lucaslng.raft.world.World;
import com.lucaslng.raft.input.InputManager;

public class GameScreen implements Screen {

	private World world;
	private GameRenderer gameRenderer;
	private PerspectiveCamera cam;
	private Keybinds keybinds;

	public GameScreen() {
		world = new World();
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		cam.position.set(0, 1f, 0f);
		cam.lookAt(0, 0, 1);
		cam.near = .1f;
		cam.far = 1000f;
		cam.update();
		Gdx.input.setInputProcessor(new InputManager());
		keybinds = new Keybinds();
		gameRenderer = new GameRenderer();
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
			cam.translate(camDir.nor().scl(-speed));
		cam.update();

		world.update(delta);
		gameRenderer.render(delta, cam, world);
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
		gameRenderer.dispose();
	}

}
