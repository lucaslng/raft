package com.lucaslng.raft.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

class SkyboxRenderer implements Disposable {

	private final Cubemap dayCube, nightCube;
	private final ShaderProgram shader;
	private final Mesh mesh = createSkyboxMesh();

	protected SkyboxRenderer() {
		dayCube = new Cubemap(Gdx.files.internal("textures/skybox/day/px.png"),
				Gdx.files.internal("textures/skybox/day/nx.png"), Gdx.files.internal("textures/skybox/day/py.png"),
				Gdx.files.internal("textures/skybox/day/ny.png"), Gdx.files.internal("textures/skybox/day/pz.png"),
				Gdx.files.internal("textures/skybox/day/nz.png"), true);
		nightCube = new Cubemap(Gdx.files.internal("textures/skybox/night/px.png"),
				Gdx.files.internal("textures/skybox/night/nx.png"), Gdx.files.internal("textures/skybox/night/py.png"),
				Gdx.files.internal("textures/skybox/night/ny.png"), Gdx.files.internal("textures/skybox/night/pz.png"),
				Gdx.files.internal("textures/skybox/night/nz.png"), true);

		shader = new ShaderProgram(Gdx.files.internal("shaders/skybox.vert"),
				Gdx.files.internal("shaders/skybox.frag"));
	}

	protected void render(Camera camera, float daylight) {
		Matrix4 viewNoTranslation = new Matrix4(camera.view);
		viewNoTranslation.val[Matrix4.M03] = 0;
		viewNoTranslation.val[Matrix4.M13] = 0;
		viewNoTranslation.val[Matrix4.M23] = 0;

		Matrix4 projView = new Matrix4(camera.projection).mul(viewNoTranslation);

		Gdx.gl.glDepthMask(false);
		shader.bind();
		shader.setUniformMatrix("u_projViewTrans", projView);
		dayCube.bind(0);
		nightCube.bind(1);
		shader.setUniformi("u_dayCubemap", 0);
		shader.setUniformi("u_nightCubemap", 1);
		shader.setUniformf("u_daylight", daylight); // blend day and night based on this value

		shader.setUniformf("u_fogColor", OceanRenderer.FOG_R, OceanRenderer.FOG_G, OceanRenderer.FOG_B);
		shader.setUniformf("u_fogSunColor", OceanRenderer.FOG_SUN_R, OceanRenderer.FOG_SUN_G, OceanRenderer.FOG_SUN_B);
		shader.setUniformf("u_sunDir", OceanRenderer.SUN_X, OceanRenderer.SUN_Y, OceanRenderer.SUN_Z);
		shader.setUniformf("u_horizonFogStart", -0.05f);
		shader.setUniformf("u_horizonFogEnd", 0.12f);

		mesh.render(shader, GL20.GL_TRIANGLES);
		Gdx.gl.glDepthMask(true);
	}

	// Create a cube, I copied this from some guide but then I formatted the file and now it looks ugly
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
		dayCube.dispose();
		nightCube.dispose();
		shader.dispose();
	}

}