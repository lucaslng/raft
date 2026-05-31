package com.lucaslng.raft.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Random;

/**
 * FFT-based "infinite" ocean renderer for a first-person libGDX game.
 *
 * The ocean is rendered as a large grid of tiles centred on the camera's XZ
 * position every frame, so it appears infinite as the player walks around.
 *
 * Wave simulation follows Tessendorf's "Simulating Ocean Water" (SIGGRAPH 2004):
 *   1. Build a Phillips spectrum + Gaussian noise → h₀(k)  (once, on init)
 *   2. Each frame: evolve h(k,t) via the deep-water dispersion relation
 *   3. Run a 2-D IFFT (row-column via Apache Commons Math) to get:
 *        - height field          → Y displacement
 *        - choppy X/Z fields    → horizontal displacement (Gerstner-style peaks)
 *        - gradient fields       → surface normals
 *   4. Upload the resulting float[] into a dynamic libGDX Mesh
 *   5. Draw TILES_DIM × TILES_DIM copies of the mesh centred on the camera
 *
 * Dependencies (add to your build.gradle core module):
 *   implementation 'org.apache.commons:commons-math3:3.6.1'
 *
 * Shader files are embedded as static strings below — see OceanShaders.java.
 *
 * Usage:
 *   OceanRenderer ocean = new OceanRenderer();
 *   // in render():
 *   ocean.render(camera, deltaTime);
 *   // in dispose():
 *   ocean.dispose();
 */
public class OceanRenderer implements Disposable {

    // -------------------------------------------------------------------------
    // Simulation parameters — tune these for different ocean moods
    // -------------------------------------------------------------------------

    /** FFT grid resolution — must be a power of 2. 128 is a good balance. */
    private static final int N = 256;

    /** World-space size of one ocean tile in metres. */
    private static final float PATCH_SIZE = 200f;

    /** Wind speed in m/s — controls wave height and frequency. */
    private static final float WIND_SPEED = 30f;

    /** Wind direction (will be normalised). */
    private static final float WIND_DIR_X = 1f;
    private static final float WIND_DIR_Z = 0.8f;

    /** Phillips constant — overall wave amplitude scale. */
    // private static final float PHILLIPS_CONSTANT = 0.0006f;
    private static final float PHILLIPS_CONSTANT = 800f;

    /** Choppiness factor — scales horizontal XZ displacement. */
    private static final float CHOPPINESS = 1.5f;

    /** Wave simulation speed multiplier. */
    private static final float TIME_SCALE = 0.8f;

    /**
     * Number of tiles along each axis. Must be odd so the centre tile sits
     * exactly under the camera.  3 = 3×3 grid of tiles (fine for most cases).
     */
    private static final int TILES_DIM = 7;

    // -------------------------------------------------------------------------
    // Rendering parameters
    // -------------------------------------------------------------------------

    /** Ocean surface colour in the deep/back-scattering look. */
    private static final float DEEP_R = 0.04f, DEEP_G = 0.16f, DEEP_B = 0.35f;

    /** Colour of the wave crests / shallow-angle view. */
    private static final float SHALLOW_R = 0.08f, SHALLOW_G = 0.42f, SHALLOW_B = 0.60f;

    /** Sun / directional light direction (points TOWARD the light). */
    private static final float SUN_X = -.54f, SUN_Y = .76f, SUN_Z = .36f;

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    private final FastFourierTransformer fft =
            new FastFourierTransformer(DftNormalization.STANDARD);

    // Initial spectrum h₀(k) and its conjugate mirror h₀(-k)
    private final float[][] h0Re = new float[N][N];
    private final float[][] h0Im = new float[N][N];
    private final float[][] h0NegRe = new float[N][N];
    private final float[][] h0NegIm = new float[N][N];

    // Per-frame frequency-domain arrays (height + choppiness X/Z + gradients)
    private final Complex[][] freqHeight = new Complex[N][N];
    private final Complex[][] freqChopX  = new Complex[N][N];
    private final Complex[][] freqChopZ  = new Complex[N][N];
    private final Complex[][] freqGradX  = new Complex[N][N];
    private final Complex[][] freqGradZ  = new Complex[N][N];

    // Spatial-domain output arrays (flat row-major, length N*N)
    private final float[] heights = new float[N * N];
    private final float[] chopX   = new float[N * N];
    private final float[] chopZ   = new float[N * N];
    private final float[] gradX   = new float[N * N];
    private final float[] gradZ   = new float[N * N];

    // Vertex buffer: position(3) + normal(3) + uv(2) = 8 floats per vertex
    private static final int VERTEX_STRIDE = 8;
    private final float[] vertices = new float[N * N * VERTEX_STRIDE];

    // libGDX objects
    private Mesh mesh;
    private ShaderProgram shader;

    // Reusable matrices / vectors
    private final Matrix4 worldTransform = new Matrix4();
    private final Matrix3 normalMatrix   = new Matrix3();
    private final Vector3 lightDir       = new Vector3(SUN_X, SUN_Y, SUN_Z).nor();

    private float elapsedTime = 0f;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public OceanRenderer() {
        ShaderProgram.pedantic = false;
        buildInitialSpectrum();
        buildMesh();
        buildShader();
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Call once per frame inside your ApplicationListener.render().
     *
     * @param camera    The active camera (position used for tile centering).
     * @param deltaTime Gdx.graphics.getDeltaTime() or equivalent.
     */
    public void render(Camera camera, float deltaTime) {
        elapsedTime += deltaTime * TIME_SCALE;

        // 1. Evolve spectrum and run IFFT on background fields
        evolveSpectrum(elapsedTime);
        runIFFT2D(freqHeight, heights);
        runIFFT2D(freqChopX,  chopX);
        runIFFT2D(freqChopZ,  chopZ);
        runIFFT2D(freqGradX,  gradX);
        runIFFT2D(freqGradZ,  gradZ);

        // 2. Build vertex buffer from displacement fields
        buildVertices();

        // 3. Upload to GPU
        mesh.setVertices(vertices);

        // 4. Render tiles centred on camera XZ
        shader.bind();
        setGlobalUniforms(camera);

        // Snap camera XZ to tile grid so tiling is seamless
        float snapX = (float) Math.floor(camera.position.x / PATCH_SIZE) * PATCH_SIZE;
        float snapZ = (float) Math.floor(camera.position.z / PATCH_SIZE) * PATCH_SIZE;

        int half = TILES_DIM / 2;
        for (int tz = -half; tz <= half; tz++) {
            for (int tx = -half; tx <= half; tx++) {
                float offsetX = snapX + tx * PATCH_SIZE;
                float offsetZ = snapZ + tz * PATCH_SIZE;
                drawTile(offsetX, offsetZ);
            }
        }
    }

    @Override
    public void dispose() {
        if (mesh   != null) mesh.dispose();
        if (shader != null) shader.dispose();
    }

    // =========================================================================
    // Initialisation helpers
    // =========================================================================

    /**
     * Builds the initial Phillips spectrum h₀(k) for every wave vector k.
     * This only runs once — the result is reused every frame.
     */
    private void buildInitialSpectrum() {
        Random rng = new Random(42L); // fixed seed for determinism

        // Normalise wind direction
        float wLen = (float) Math.sqrt(WIND_DIR_X * WIND_DIR_X + WIND_DIR_Z * WIND_DIR_Z);
        float windX = WIND_DIR_X / wLen;
        float windZ = WIND_DIR_Z / wLen;

        float g = 9.81f;
        float L  = WIND_SPEED * WIND_SPEED / g; // largest wave from wind

        for (int m = 0; m < N; m++) {
            for (int n = 0; n < N; n++) {
                // Wave vector k for grid point (n, m), centred
                float kx = (float) (2.0 * Math.PI * (n - N / 2.0) / PATCH_SIZE);
                float kz = (float) (2.0 * Math.PI * (m - N / 2.0) / PATCH_SIZE);
                float kLen = (float) Math.sqrt(kx * kx + kz * kz);

                float phillips = 0f;
                if (kLen > 1e-6f) {
                    float kL  = kLen * L;
                    float kL2 = kL * kL;
                    // Dot product of k-hat with wind direction
                    float kdotw = (kx / kLen) * windX + (kz / kLen) * windZ;
                    // Suppress waves moving against the wind
                    if (kdotw < 0f) kdotw *= 0.07f;

                    // Small-wave suppression — removes ripples smaller than 1 cm
                    float ell    = 0.01f;
                    float dampK2 = kLen * kLen * ell * ell;

                    phillips = PHILLIPS_CONSTANT
                            * (float) Math.exp(-1.0 / kL2)
                            / (kLen * kLen * kLen * kLen)
                            * (kdotw * kdotw)
                            * (float) Math.exp(-dampK2);
                }

                float amplitude = (float) (Math.sqrt(phillips / 2.0));

                // Gaussian random complex number (Box-Muller)
                float[] g1 = gaussianRandom(rng);
                float[] g2 = gaussianRandom(rng);

                h0Re[m][n]    = g1[0] * amplitude;
                h0Im[m][n]    = g1[1] * amplitude;
                // Mirror — use separate Gaussian so they are independent
                h0NegRe[m][n] = g2[0] * amplitude;
                h0NegIm[m][n] = g2[1] * amplitude;
            }
        }
    }

    /** Creates the dynamic libGDX Mesh with position, normal, and UV attributes. */
    private void buildMesh() {
        // Build a triangle grid: (N-1)×(N-1) quads, 2 triangles each, 3 indices each
        int numVertices = N * N;
        int numIndices  = (N - 1) * (N - 1) * 6;
        short[] indices = new short[numIndices];

        int idx = 0;
        for (int m = 0; m < N - 1; m++) {
            for (int n = 0; n < N - 1; n++) {
                short bl = (short) (m * N + n);
                short br = (short) (m * N + n + 1);
                short tl = (short) ((m + 1) * N + n);
                short tr = (short) ((m + 1) * N + n + 1);
                // Triangle 1
                indices[idx++] = bl;
                indices[idx++] = br;
                indices[idx++] = tl;
                // Triangle 2
                indices[idx++] = br;
                indices[idx++] = tr;
                indices[idx++] = tl;
            }
        }

        // isStatic=false because we update vertices every frame
        mesh = new Mesh(false, numVertices, numIndices,
                new VertexAttribute(VertexAttributes.Usage.Position, 3,
                        ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3,
                        ShaderProgram.NORMAL_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2,
                        ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        mesh.setIndices(indices);
    }

    /** Compiles the GLSL shaders and creates the ShaderProgram. */
    private void buildShader() {
        shader = new ShaderProgram(Gdx.files.internal("shaders/ocean.vert"), Gdx.files.internal("shaders/ocean.frag"));
        if (!shader.isCompiled()) {
            throw new RuntimeException("Ocean shader failed to compile:\n" + shader.getLog());
        }
    }

    // =========================================================================
    // Per-frame simulation
    // =========================================================================

    /**
     * Evolves each frequency component h(k, t) from the stored h₀(k) using the
     * deep-water dispersion relation  ω(k) = √(g·|k|).
     *
     * Also fills the choppiness and gradient frequency arrays so their IFFTs
     * yield horizontal displacement and surface normals respectively.
     */
    private void evolveSpectrum(float t) {
        float g = 9.81f;

        for (int m = 0; m < N; m++) {
            for (int n = 0; n < N; n++) {
                float kx = (float) (2.0 * Math.PI * (n - N / 2.0) / PATCH_SIZE);
                float kz = (float) (2.0 * Math.PI * (m - N / 2.0) / PATCH_SIZE);
                float kLen = (float) Math.sqrt(kx * kx + kz * kz);
                float omega = (kLen < 1e-6f) ? 0f : (float) Math.sqrt(g * kLen);

                // e^(iωt) = cos(ωt) + i·sin(ωt)
                float cosOt = MathUtils.cos(omega * t);
                float sinOt = MathUtils.sin(omega * t);

                // h(k,t) = h₀(k)·e^(iωt) + conj(h₀(-k))·e^(-iωt)
                float re =  h0Re[m][n] * cosOt - h0Im[m][n] * sinOt
                          + h0NegRe[m][n] * cosOt + h0NegIm[m][n] * sinOt;
                float im =  h0Re[m][n] * sinOt + h0Im[m][n] * cosOt
                          + h0NegRe[m][n] * (-sinOt) - h0NegIm[m][n] * cosOt;

                freqHeight[m][n] = new Complex(re, im);

                if (kLen > 1e-6f) {
                    float kxN = kx / kLen;
                    float kzN = kz / kLen;

                    // Choppy X: multiply h(k,t) by i·kx/|k| → shift by 90°
                    freqChopX[m][n] = new Complex(-im * kxN,  re * kxN);
                    freqChopZ[m][n] = new Complex(-im * kzN,  re * kzN);

                    // Gradient for normals: multiply by i·k
                    freqGradX[m][n] = new Complex(-im * kx,   re * kx);
                    freqGradZ[m][n] = new Complex(-im * kz,   re * kz);
                } else {
                    freqChopX[m][n] = Complex.ZERO;
                    freqChopZ[m][n] = Complex.ZERO;
                    freqGradX[m][n] = Complex.ZERO;
                    freqGradZ[m][n] = Complex.ZERO;
                }
            }
        }
    }

    /**
     * Performs a 2-D IFFT using the row-column decomposition.
     * Apache Commons Math only provides a 1-D transform, so we apply it
     * to each row, then to each column of the intermediate result.
     *
     * The real part of the output is written into {@code out[]}.
     */
    private void runIFFT2D(Complex[][] spectrum, float[] out) {
        Complex[][] temp = new Complex[N][N];

        // IFFT each row
        for (int m = 0; m < N; m++) {
            temp[m] = fft.transform(spectrum[m], TransformType.INVERSE);
        }

        // IFFT each column, collect real part
        Complex[] col = new Complex[N];
        for (int n = 0; n < N; n++) {
            for (int m = 0; m < N; m++) col[m] = temp[m][n];
            Complex[] result = fft.transform(col, TransformType.INVERSE);
            for (int m = 0; m < N; m++) {
                out[m * N + n] = (float) result[m].getReal();
            }
        }
    }

    /**
     * Packs the IFFT outputs into the vertex buffer.
     *
     * Each vertex gets:
     *  - Position: (baseX + chopX·CHOPPINESS, height, baseZ + chopZ·CHOPPINESS)
     *  - Normal:   normalised(-gradX, 1, -gradZ)  — pointing upward
     *  - UV:       (n/N, m/N)  — repeating 0..1 across the patch
     *
     * A sign-alternating (checkerboard) correction is applied because the IFFT
     * of a spectrum that was *not* pre-shifted produces values that alternate in
     * sign — multiplying by (-1)^(m+n) un-shifts the result.
     */
    private void buildVertices() {
        int vi = 0;
        for (int m = 0; m < N; m++) {
            for (int n = 0; n < N; n++) {
                int i   = m * N + n;
                float sign = ((m + n) % 2 == 0) ? 1f : -1f;

                float h  = heights[i] * sign;
                float cx = chopX[i]   * sign * CHOPPINESS;
                float cz = chopZ[i]   * sign * CHOPPINESS;
                float gx = gradX[i]   * sign;
                float gz = gradZ[i]   * sign;

                // Base grid position centred on origin (0..PATCH_SIZE, offset by -half)
                float baseX = (n - N / 2f) / N * PATCH_SIZE;
                float baseZ = (m - N / 2f) / N * PATCH_SIZE;

                // Position
                vertices[vi++] = baseX + cx;
                vertices[vi++] = h;
                vertices[vi++] = baseZ + cz;

                // Normal — unnormalized; GPU normalises in vertex shader
                vertices[vi++] = -gx;
                vertices[vi++] = 1f;
                vertices[vi++] = -gz;

                // UV (wraps 0→1 across the tile; GLSL repeats for foam/detail)
                vertices[vi++] = (float) n / N;
                vertices[vi++] = (float) m / N;
            }
        }
    }

    // =========================================================================
    // Rendering helpers
    // =========================================================================

    /** Uploads uniforms that are the same for every tile in a frame. */
    private void setGlobalUniforms(Camera camera) {
        shader.setUniformMatrix("u_projView", camera.combined);
        shader.setUniformf("u_cameraPos",
                camera.position.x, camera.position.y, camera.position.z);
        shader.setUniformf("u_lightDir", lightDir.x, lightDir.y, lightDir.z);
        shader.setUniformf("u_deepColor",    DEEP_R,    DEEP_G,    DEEP_B);
        shader.setUniformf("u_shallowColor", SHALLOW_R, SHALLOW_G, SHALLOW_B);
        shader.setUniformf("u_time", elapsedTime);
    }

    /**
     * Positions the world transform for a single tile and draws it.
     * The tile is simply translated — no rotation or scale needed because
     * the geometry already covers [0, PATCH_SIZE] in XZ.
     */
    private void drawTile(float worldOffsetX, float worldOffsetZ) {
        worldTransform.setToTranslation(worldOffsetX, 0f, worldOffsetZ);
        normalMatrix.set(worldTransform).inv().transpose();

        shader.setUniformMatrix("u_world",  worldTransform);
        shader.setUniformMatrix("u_normalMatrix", normalMatrix);

        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Generates a pair of independent Gaussian random values using the
     * Box-Muller transform.  Mean = 0, std dev = 1.
     */
    private float[] gaussianRandom(Random rng) {
        float u1 = Math.max(rng.nextFloat(), 1e-7f); // avoid log(0)
        float u2 = rng.nextFloat();
        float mag = (float) Math.sqrt(-2.0 * Math.log(u1));
        return new float[]{
                mag * MathUtils.cos(MathUtils.PI2 * u2),
                mag * MathUtils.sin(MathUtils.PI2 * u2)
        };
    }
}