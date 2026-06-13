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
import com.lucaslng.raft.entity.Entity;
import com.lucaslng.raft.entity.OceanTrash;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.world.World;


public class GameRenderer implements Disposable {

	private final Environment   environment        = new Environment();
	private final Environment   outlineEnvironment = new Environment();
	private final SkyboxRenderer skybox             = new SkyboxRenderer();
	private final HUDRenderer   hud;
	private final OceanRenderer ocean;
	private final ModelBatch    modelBatch         = new ModelBatch();
	private final DebugDrawer   debugDraw          = new DebugDrawer();
	private final World         world;

	private boolean isDebug = false;

	public GameRenderer(Assets assets, World world, EventBus events) {
		this.world = world;

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, .54f, -.76f, -.36f));
		outlineEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.WHITE));

		hud   = new HUDRenderer(assets, events, world.getBuildingRegistry());
		ocean = new OceanRenderer(42L);

		debugDraw.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
		world.getPhysics().setDebugDraw(debugDraw);
	}

	public void render(float delta, Camera camera) {
		Gdx.gl.glViewport(0, 0,
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		skybox.render(camera);
		ocean.render(camera, delta);

		// ── Collect opaque instances ──────────────────────────────────────
		List<ModelInstance> opaque = new ArrayList<>();

		// All registered entities (player, shark, etc.)
		opaque.addAll(world.getEntitySystem().getInstances());

		// Raft tiles + buildings (cached list, no allocation unless dirty)
		opaque.addAll(world.getRaftSystem().getInstances());

		Entity hovered = world.getHoveredEntity();

		modelBatch.begin(camera);
		modelBatch.render(opaque, environment);

		// Trash — skip the hovered one (rendered separately for outline)
		for (OceanTrash t : world.getTrash())
			if (t != hovered)
				modelBatch.render(t.getInstance(), environment);

		// Ghost preview
		world.getPlacementGhost().render(modelBatch, environment);
		modelBatch.end();

		// ── Hovered entity outline ────────────────────────────────────────
		if (hovered != null) {
			renderOutline(camera, hovered);
		}

		// ── Debug draw ────────────────────────────────────────────────────
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

	@Override
	public void dispose() {
		debugDraw.dispose();
		skybox.dispose();
		ocean.dispose();
		hud.dispose();
		modelBatch.dispose();
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private void renderOutline(Camera camera, Entity entity) {
		ModelInstance mi = entity.getInstance();

		Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
		Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
		Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);
		Gdx.gl.glStencilMask(0xFF);
		Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);

		modelBatch.begin(camera);
		modelBatch.render(mi, environment);
		modelBatch.end();

		Gdx.gl.glStencilFunc(GL20.GL_NOTEQUAL, 1, 0xFF);
		Gdx.gl.glStencilMask(0x00);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

		Matrix4 original = new Matrix4(mi.transform);
		mi.transform.scale(1.05f, 1.05f, 1.05f);
		modelBatch.begin(camera);
		modelBatch.render(mi, outlineEnvironment);
		modelBatch.end();
		mi.transform.set(original);

		Gdx.gl.glStencilMask(0xFF);
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
	}
}