package com.lucaslng.raft.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import com.lucaslng.raft.assets.Assets;
import com.lucaslng.raft.entity.OceanTrash;
import com.lucaslng.raft.event.EventBus;
import com.lucaslng.raft.world.World;

public class GameRenderer implements Disposable {

	private final Environment environment = new Environment();
	private final Environment outlineEnvironment = new Environment();
	private final SkyboxRenderer skybox = new SkyboxRenderer();
	private final HUDRenderer hud;
	private final OceanRenderer ocean;
	private final UnderwaterRenderer underwaterRenderer = new UnderwaterRenderer();
	private final ModelBatch modelBatch = new ModelBatch();
	private final ModelBatch outlineModelBatch = new ModelBatch();
	private final DebugDrawer debugDraw = new DebugDrawer();
	private final World world;
	private final DirectionalLight sun = new DirectionalLight().setDirection(.54f, -.76f, -.36f);

	private static final Color SUN_NIGHT_COLOR = new Color(0.05f, 0.05f, 0.2f, 1f);
	private static final Color SUN_DAY_COLOR = new Color(1f, 1f, 0.95f, 1f);

	private final List<ModelInstance> opaque = new ArrayList<>();

	private boolean isDebug = true;

	public GameRenderer(Assets assets, World world, EventBus events) {
		this.world = world;

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(sun);
		outlineEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.WHITE));

		hud = new HUDRenderer(assets, events, world.getBuildingRegistry());
		ocean = new OceanRenderer(42L);

		debugDraw.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
		world.getPhysics().setDebugDraw(debugDraw);
	}

	public void render(float delta, Camera camera) {
		Gdx.gl.glViewport(0, 0,
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		float daylight = MathUtils.sin(world.getTime() * MathUtils.PI2 - MathUtils.PI / 2f);
		daylight = MathUtils.clamp((daylight + 1f) * 0.5f, 0f, 1f);
		daylight = (float) Math.pow(daylight, 3f);

		sun.color.set(SUN_NIGHT_COLOR).lerp(SUN_DAY_COLOR, daylight);

		skybox.render(camera, daylight);
		ocean.render(camera, delta);

		// ── Collect opaque instances ──────────────────────────────────────
		opaque.clear();
		opaque.addAll(world.getEntitySystem().getInstances());
		opaque.addAll(world.getRaftSystem().getInstances());

		ModelInstance hovered = world.getHoveredOutlineInstance();

		modelBatch.begin(camera);
		modelBatch.render(opaque, environment);

		for (OceanTrash t : world.getTrash())
			modelBatch.render(t.getInstance(), environment);

		world.getPlacementGhost().render(modelBatch, environment);
		modelBatch.end();

		// ── Hovered entity outline ────────────────────────────────────────
		ModelInstance outlineTarget = world.getHoveredOutlineInstance();
		if (outlineTarget != null) {
			renderOutline(camera, outlineTarget);
		}

		// ── Debug draw ────────────────────────────────────────────────────
		if (isDebug) {
			debugDraw.begin(camera);
			world.getPhysics().debugDrawWorld();
			debugDraw.end();
		}

		// ── Underwater tint ───────────────────────────────────────────────
		underwaterRenderer.renderIfSubmerged(camera.position.y);

		hud.render(world, delta);
	}

	public void resize(int width, int height) {
		hud.resize(width, height);
	}

	/**
	 * Returns the HUD's Scene2D Stage so {@code GameScreen} can add it to an
	 * {@link com.badlogic.gdx.InputMultiplexer}, giving the building UI panels
	 * priority over game input.
	 */
	public com.badlogic.gdx.scenes.scene2d.Stage getHudStage() {
		return hud.getStage();
	}

	@Override
	public void dispose() {
		debugDraw.dispose();
		skybox.dispose();
		ocean.dispose();
		hud.dispose();
		underwaterRenderer.dispose();
		modelBatch.dispose();
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private void renderOutline(Camera camera, ModelInstance mi) {
		Matrix4 original = new Matrix4(mi.transform);

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

		try {
			mi.transform.scale(1.03f, 1.03f, 1.03f);
			outlineModelBatch.begin(camera);
			outlineModelBatch.render(mi, outlineEnvironment);
			outlineModelBatch.end();
		} finally {
			mi.transform.set(original);
		}

		Gdx.gl.glStencilMask(0xFF);
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
	}
}