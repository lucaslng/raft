package com.lucaslng.raft.util;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;

public final class Util {

	// static private Vector3 tempVec3 = new Vector3();
	static private BoundingBox tempBounds = new BoundingBox();

	public static Texture generateTexture(Color color, int height) {
		Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
		pixmap.setColor(color);
		pixmap.fill();
		Texture texture = new Texture(pixmap);
		pixmap.dispose();
		return texture;
	}

	public static Texture generateTexture(Color color) {
		return generateTexture(color, 1);
	}

	public static void scaleModelInstance(ModelInstance instance, float scale) {
		for (Node node : instance.nodes)
			node.scale.set(scale, scale, scale);
		instance.calculateTransforms();
	}

	public static void scaleModel(Model model, float scale) {
		for (Node node : model.nodes)
			node.scale.set(scale, scale, scale);
		model.calculateTransforms();
	}

	public static Vector3 centerModelInstance(ModelInstance instance) {
		Vector3 center = new Vector3();
		instance.calculateBoundingBox(tempBounds);
		tempBounds.getCenter(center);
		instance.transform.translate(center.scl(-1f));
		instance.calculateTransforms();
		return center.scl(-1f);
	}

	public static btConvexHullShape buildConvexHullShape(ModelInstance instance) {
		btConvexHullShape shape = new btConvexHullShape();
		
		for (Mesh mesh : instance.model.meshes) {
			int vertexSize = mesh.getVertexSize() / 4; // stride in floats
			float[] vertices = new float[mesh.getNumVertices() * vertexSize];
			mesh.getVertices(vertices);

			// give convex hull xyz points
			int posOffset = getPositionOffset(mesh);
			for (int i = 0; i < mesh.getNumVertices(); i++) {
				int base = i * vertexSize + posOffset;
				shape.addPoint(new Vector3(vertices[base], vertices[base + 1], vertices[base + 2]));
			}
		}

		shape.recalcLocalAabb();
		return shape;
	}

	// Gets the float-index offset of the position attribute in the vertex array
	private static int getPositionOffset(Mesh mesh) {
		for (VertexAttribute attr : mesh.getVertexAttributes()) {
			if (attr.usage == VertexAttributes.Usage.Position) {
				return attr.offset / 4; // bytes → floats
			}
		}
		throw new RuntimeException("No position attribute found in mesh");
	}

	public static Vector3 getDimensions(ModelInstance instance) {
		instance.calculateBoundingBox(tempBounds);
		Vector3 dimensions = new Vector3();
		tempBounds.getDimensions(dimensions);
		dimensions.scl(.5f); // get half extents
		return dimensions;
	}

}
