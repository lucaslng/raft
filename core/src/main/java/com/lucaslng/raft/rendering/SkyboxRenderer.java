package com.lucaslng.raft.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

class SkyboxRenderer implements Disposable {

	private final Cubemap cubemap;
	private final ShaderProgram shader;
	private final Mesh mesh = createSkyboxMesh();

	protected SkyboxRenderer() {
		cubemap = new Cubemap(Gdx.files.internal("textures/skybox/px.png"),
				Gdx.files.internal("textures/skybox/nx.png"), Gdx.files.internal("textures/skybox/py.png"),
				Gdx.files.internal("textures/skybox/ny.png"), Gdx.files.internal("textures/skybox/pz.png"),
				Gdx.files.internal("textures/skybox/nz.png"));
		shader = new ShaderProgram(Gdx.files.internal("shaders/skybox.vert"),
				Gdx.files.internal("shaders/skybox.frag"));
	}

	protected void render(Camera camera) {
		// Build a view matrix with NO translation (only rotation)
		Matrix4 viewNoTranslation = new Matrix4(camera.view);
		viewNoTranslation.val[Matrix4.M03] = 0;
		viewNoTranslation.val[Matrix4.M13] = 0;
		viewNoTranslation.val[Matrix4.M23] = 0;

		Matrix4 projView = new Matrix4(camera.projection).mul(viewNoTranslation);

		// Draw skybox first, disable depth writing
		Gdx.gl.glDepthMask(false);
		shader.bind();
		shader.setUniformMatrix("u_projViewTrans", projView);
		cubemap.bind(0);
		shader.setUniformi("u_cubemap", 0);

		// Atmospheric fog — values sourced from OceanRenderer so both renderers
		// converge to exactly the same colour at the horizon.
		shader.setUniformf("u_fogColor",        OceanRenderer.FOG_R,     OceanRenderer.FOG_G,     OceanRenderer.FOG_B);
		shader.setUniformf("u_fogSunColor",     OceanRenderer.FOG_SUN_R, OceanRenderer.FOG_SUN_G, OceanRenderer.FOG_SUN_B);
		// Sun direction matches the directional light in OceanRenderer
		shader.setUniformf("u_sunDir",          OceanRenderer.SUN_X, OceanRenderer.SUN_Y, OceanRenderer.SUN_Z);
		// Horizon band: fog starts just above the horizon, fully blended higher up.
		// Tune these to widen or narrow the haze band.
		shader.setUniformf("u_horizonFogStart", -0.05f);
		shader.setUniformf("u_horizonFogEnd",    0.12f);

		mesh.render(shader, GL20.GL_TRIANGLES);
		Gdx.gl.glDepthMask(true);
	}

	private Mesh createSkyboxMesh() {
		float[] vertices = {
				-1, 1, -1, -1, -1, -1, 1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1, -1,
				-1, -1, 1, -1, -1, -1, -1, 1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1,
				1, -1, -1, 1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, 1, -1, -1,
				-1, -1, 1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, 1, -1, -1, 1,
				-1, 1, -1, 1, 1, -1, 1, 1, 1, 1, 1, 1, -1, 1, 1, -1, 1, -1,
				-1, -1, -1, -1, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, -1, -1, -1, -1
		};

		Mesh mesh = new Mesh(true, 36, 0,
				new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
		mesh.setVertices(vertices);
		return mesh;
	}

	@Override
	public void dispose() {
		mesh.dispose();
		cubemap.dispose();
		shader.dispose();
	}

}