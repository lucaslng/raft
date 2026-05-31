package com.lucaslng.raft.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.world.World;

public class GameRenderer implements Disposable {

	private boolean loading;
	private Assets assets;
	private Environment environment;
	private SkyboxRenderer skybox;
	private ShadowRenderer shadow;
	private HUDRenderer hud;
	private OceanRenderer ocean;
	private ModelBatch modelBatch;
	private List<ModelInstance> instances;

	public GameRenderer() {
		loading = true;
		assets = new Assets();
		instances = new ArrayList<>();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, .54f, -.76f, -.36f));

		skybox = new SkyboxRenderer();
		shadow = new ShadowRenderer();
		hud = new HUDRenderer(assets);
		ocean = new OceanRenderer();
		modelBatch = new ModelBatch();

	}

	private void doneLoading() {
		ModelInstance instance = new ModelInstance(assets.get("platform.g3db", Model.class));
		instances.add(instance);
		loading = false;
	}

	public void render(float delta, Camera camera, World world) {
		if (loading && assets.update()) {
			doneLoading();
		}

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		skybox.render(camera);
		ocean.render(camera, delta);

		modelBatch.begin(camera);
		modelBatch.render(instances);

		modelBatch.end();

		hud.render(world);
	}

	public void dispose() {
		skybox.dispose();
		ocean.dispose();
		hud.dispose();
		assets.dispose();
	}
}
