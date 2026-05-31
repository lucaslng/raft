package com.lucaslng.raft.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

class SkyboxRenderer implements Disposable {

	private Cubemap cubemap;
	private ShaderProgram shader;
	private Mesh mesh;

	SkyboxRenderer() {
		cubemap = new Cubemap(Gdx.files.internal("textures/skybox/px.png"),
				Gdx.files.internal("textures/skybox/nx.png"), Gdx.files.internal("textures/skybox/py.png"),
				Gdx.files.internal("textures/skybox/ny.png"), Gdx.files.internal("textures/skybox/pz.png"),
				Gdx.files.internal("textures/skybox/nz.png"));
		mesh = createSkyboxMesh();
		shader = new ShaderProgram(Gdx.files.internal("shaders/skybox.vert"),
				Gdx.files.internal("shaders/skybox.frag"));
	}

	void render(Camera camera) {
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
