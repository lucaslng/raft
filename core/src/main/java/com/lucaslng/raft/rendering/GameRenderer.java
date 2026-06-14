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
import com.lucaslng.raft.entity.OceanTrash;
import com.lucaslng.raft.rendering.hud.HUDRenderer;
import com.lucaslng.raft.settings.Settings;
import com.lucaslng.raft.world.World;

// Orchestrates all the rendering in the game
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
	private final WinWall winWall = new WinWall();

	private static final Color SUN_NIGHT_COLOR = new Color(0.05f, 0.05f, 0.2f, 1f);
	private static final Color SUN_DAY_COLOR = new Color(1f, 1f, 0.95f, 1f);

	private final List<ModelInstance> opaque = new ArrayList<>();

	public GameRenderer(World world) {
		this.world = world;

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(sun);
		outlineEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.WHITE));

		hud = new HUDRenderer(world);
		ocean = new OceanRenderer(42L);

		debugDraw.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
		world.getPhysics().setDebugDraw(debugDraw);
	}

	public void render(float delta, Camera camera) {
		Gdx.gl.glViewport(0, 0,
				Gdx.graphics.getBackBufferWidth(),
				Gdx.graphics.getBackBufferHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		winWall.update(delta);

		float daylight = MathUtils.sin(world.getTime() * MathUtils.PI2 - MathUtils.PI / 2f);
		daylight = MathUtils.clamp((daylight + 1f) * 0.5f, 0f, 1f);
		daylight = (float) Math.pow(daylight, 3f);

		sun.color.set(SUN_NIGHT_COLOR).lerp(SUN_DAY_COLOR, daylight);

		skybox.render(camera, daylight);
		ocean.render(camera, delta);

		// collect opaque modelinstances
		opaque.clear();
		opaque.addAll(world.getEntitySystem().getInstances());
		opaque.addAll(world.getRaftSystem().getInstances());

		modelBatch.begin(camera);
		modelBatch.render(opaque, environment);

		for (OceanTrash t : world.getTrash())
			modelBatch.render(t.getInstance(), environment);

		world.getPlacementGhost().render(modelBatch, environment);
		winWall.render(modelBatch, environment);
		modelBatch.end();

		// render outline for hovered entity
		ModelInstance outlineTarget = world.getHoveredOutlineInstance();
		if (outlineTarget != null) {
			renderOutline(camera, outlineTarget);
		}

		// debug
		if (Settings.get().debug) {
			debugDraw.begin(camera);
			world.getPhysics().debugDrawWorld();
			debugDraw.end();
		}

		underwaterRenderer.renderIfSubmerged(camera.position.y);

		hud.render(delta);
	}

	public void resize(int width, int height) {
		hud.resize(width, height);
	}
	
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

	// Render outline on a model instance
	private void renderOutline(Camera camera, ModelInstance instance) {
		Matrix4 original = new Matrix4(instance.transform);

		Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
		Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
		Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);
		Gdx.gl.glStencilMask(0xFF);
		Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);

		modelBatch.begin(camera);
		modelBatch.render(instance, environment);
		modelBatch.end();

		Gdx.gl.glStencilFunc(GL20.GL_NOTEQUAL, 1, 0xFF);
		Gdx.gl.glStencilMask(0x00);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

		try {
			instance.transform.scale(1.03f, 1.03f, 1.03f);
			outlineModelBatch.begin(camera);
			outlineModelBatch.render(instance, outlineEnvironment);
			outlineModelBatch.end();
		} finally {
			instance.transform.set(original);
		}

		Gdx.gl.glStencilMask(0xFF);
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
	}
}