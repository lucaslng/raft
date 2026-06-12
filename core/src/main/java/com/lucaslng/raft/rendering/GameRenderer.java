package com.lucaslng.raft.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.entity.OceanTrash;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.world.World;

public class GameRenderer implements Disposable {

	private final Assets assets;
	private final Environment environment = new Environment();
	private final Environment outlineEnvironment = new Environment();
	private final SkyboxRenderer skybox = new SkyboxRenderer();
	private final HUDRenderer hud;
	private final OceanRenderer ocean;
	private final ModelBatch modelBatch = new ModelBatch();
	private final List<ModelInstance> instances = new ArrayList<>();
	private final DebugDrawer debugDraw = new DebugDrawer();
	private final World world;
	private final Iterable<OceanTrash> trash;

	private boolean isDebug = false;

	public GameRenderer(Assets assets, World world, EventBus events) {
		this.assets = assets;
		this.world = world;
		this.trash = world.getTrash();

		instances.add(world.getRaft().getInstance());
		instances.add(world.getPlayer().getInstance());
		instances.add(world.getShark().getInstance());

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, .54f, -.76f, -.36f));
		outlineEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.WHITE));

		hud = new HUDRenderer(assets, events);
		ocean = new OceanRenderer(42L);

		debugDraw.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
		world.getPhysics().setDebugDraw(debugDraw);
	}

	public void render(float delta, Camera camera) {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		skybox.render(camera);
		ocean.render(camera, delta);

		modelBatch.begin(camera);
		modelBatch.render(instances, environment);
		for (OceanTrash t : trash)
			if (t != world.getHoveredEntity())
				modelBatch.render(t.getInstance(), environment);
		modelBatch.end();

		if (world.getHoveredEntity() != null) {
			ModelInstance hoveredInstance = world.getHoveredEntity().getInstance();

			// draw original with stencil
			Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
			Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
			Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);
			Gdx.gl.glStencilMask(0xFF);
			Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);
			modelBatch.begin(camera);
			modelBatch.render(hoveredInstance, environment);
			modelBatch.end();

			// draw outline using scaled up model
			Gdx.gl.glStencilFunc(GL20.GL_NOTEQUAL, 1, 0xFF);
			Gdx.gl.glStencilMask(0x00);
			Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
			Matrix4 original = new Matrix4(hoveredInstance.transform);
			hoveredInstance.transform.scale(1.05f, 1.05f, 1.05f);
			modelBatch.begin(camera);
			modelBatch.render(hoveredInstance, outlineEnvironment);
			modelBatch.end();

			hoveredInstance.transform.set(original); // restore transform

			Gdx.gl.glStencilMask(0xFF);
			Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
			Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
		}

		if (isDebug) {
			debugDraw.begin(camera);
			world.getPhysics().debugDrawWorld();
			debugDraw.end();
		}

		hud.render(world, delta);
	}

	public void resize(int width, int height) {
		hud.resize(width, height);
	}

	public void dispose() {
		debugDraw.dispose();
		skybox.dispose();
		ocean.dispose();
		hud.dispose();
		assets.dispose();
	}
}
