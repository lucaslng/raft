package com.lucaslng.raft.rendering.water;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;

/**
 * Generates a flat grid mesh for the water surface.
 * No vertices are shared between triangles, so each triangle can compute
 * its own flat normal in the vertex shader via the indicator offsets.
 *
 * An ocean-follow effect is achieved purely via a u_offset uniform in the
 * shader; this mesh never needs to be rebuilt or moved on the CPU side.
 */
public class WaterMesh {

    // 2 triangles × 3 vertices per grid square
    private static final int VERTS_PER_SQUARE = 6;
    // x, z, ind0, ind1, ind2, ind3  (position + offset-to-other-two-verts)
    private static final int FLOATS_PER_VERT = 6;

    private final Mesh  mesh;
    private final int   vertexCount;
    private final float height;

    /**
     * @param gridCount  Number of grid squares along each axis (e.g. 200).
     * @param height     World-space Y of the water plane.
     */
    public WaterMesh(int gridCount, float height) {
        this.height      = height;
        this.vertexCount = gridCount * gridCount * VERTS_PER_SQUARE;

        mesh = new Mesh(
            true, vertexCount, 0,
            new VertexAttribute(Usage.Position, 2, "in_position"),   // x, z
            new VertexAttribute(Usage.Generic,  4, "in_indicators")  // offsets to the other 2 verts
        );
        mesh.setVertices(buildData(gridCount));
    }

    // ------------------------------------------------------------------ //

    private float[] buildData(int gridCount) {
        float[] data = new float[vertexCount * FLOATS_PER_VERT];
        int idx = 0;
        for (int row = 0; row < gridCount; row++)
            for (int col = 0; col < gridCount; col++)
                idx = storeSquare(col, row, data, idx);
        return data;
    }

    /**
     * Stores 6 vertices for one grid square (2 triangles, no shared verts).
     * Corner layout: 0=top-left, 1=bottom-left, 2=top-right, 3=bottom-right
     */
    private int storeSquare(int col, int row, float[] data, int idx) {
        float[] cx = { col,     col,     col + 1, col + 1 };
        float[] cz = { row,     row + 1, row,     row + 1 };

        // Left triangle:  corners 0-1-2
        idx = packVert(cx, cz, 0, 1, 2, data, idx);
        idx = packVert(cx, cz, 1, 2, 0, data, idx);
        idx = packVert(cx, cz, 2, 0, 1, data, idx);
        // Right triangle: corners 2-1-3
        idx = packVert(cx, cz, 2, 1, 3, data, idx);
        idx = packVert(cx, cz, 1, 3, 2, data, idx);
        idx = packVert(cx, cz, 3, 2, 1, data, idx);
        return idx;
    }

    /**
     * Packs one vertex: its (x,z) grid position plus the two XZ offset
     * vectors that point to its two triangle-mates. These indicators let
     * the vertex shader reconstruct all three world positions without extra
     * attributes, so it can compute the flat normal via a cross product.
     */
    private int packVert(float[] cx, float[] cz,
                         int cur, int v1, int v2,
                         float[] data, int idx) {
        data[idx++] = cx[cur];
        data[idx++] = cz[cur];
        data[idx++] = cx[v1] - cx[cur];  // X offset to vertex 1
        data[idx++] = cz[v1] - cz[cur];  // Z offset to vertex 1
        data[idx++] = cx[v2] - cx[cur];  // X offset to vertex 2
        data[idx++] = cz[v2] - cz[cur];  // Z offset to vertex 2
        return idx;
    }

    // ------------------------------------------------------------------ //

    public Mesh  getMesh()        { return mesh;        }
    public int   getVertexCount() { return vertexCount; }
    public float getHeight()      { return height;      }
    public void  dispose()        { mesh.dispose();     }
}