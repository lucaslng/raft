package com.lucaslng.raft.rendering;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_2D;

class OceanRenderer implements Disposable {

  private static final int N = 128; // resolution, power of 2
  private static final float PATCH_SIZE = 200f; // tile size
  private static final float WIND_SPEED = 15f; // wave height & freq
  private static final float WIND_DIR_X = 1f; // wind direction
  private static final float WIND_DIR_Z = 0.8f;
  private static final float PHILLIPS_CONSTANT = 100f; // wave amp
  private static final int TILES_DIM = 5; // tiles dimensions
  // water colours
  private static final float DEEP_R = 0.04f, DEEP_G = 0.16f, DEEP_B = 0.35f;
  private static final float SHALLOW_R = 0.08f, SHALLOW_G = 0.42f, SHALLOW_B = 0.60f;

  // sun direction
  static final float SUN_X = -.54f, SUN_Y = .76f, SUN_Z = .36f;

  // fog settings
  static final float FOG_R = 0.72f, FOG_G = 0.85f, FOG_B = 0.95f;
  static final float FOG_SUN_R = 1.0f, FOG_SUN_G = 0.90f, FOG_SUN_B = 0.65f;
  static final float FOG_DENSITY = 0.015f;

  private final Random random;

  private final FloatFFT_2D fft2d = new FloatFFT_2D(N, N);
  private final float[] complexBuffer = new float[N * N * 2]; // reused

  // Initial spectrum h₀(k)
  private final float[][] h0Re = new float[N][N];
  private final float[][] h0Im = new float[N][N];

  // Frequency‑domain arrays (updated each frame)
  private final float[][] freqHeightRe = new float[N][N];
  private final float[][] freqHeightIm = new float[N][N];
  private final float[][] freqGradXRe = new float[N][N];
  private final float[][] freqGradXIm = new float[N][N];
  private final float[][] freqGradZRe = new float[N][N];
  private final float[][] freqGradZIm = new float[N][N];

  // Spatial‑domain outputs (row‑major)
  private final float[] heights = new float[N * N];
  private final float[] gradX = new float[N * N];
  private final float[] gradZ = new float[N * N];
  private final float[] baseX = new float[N * N];
  private final float[] baseZ = new float[N * N];

  // Vertex buffer: position (3) + normal (3) + uv (2) = 8 floats per vertex
  private static final int VERTEX_STRIDE = 8;
  private final float[] vertices;

  private Mesh mesh;
  private ShaderProgram shader;

  private final Matrix4 worldTransform;
  private final Vector3 lightDir;

  private float elapsedTime;

  protected OceanRenderer(long seed) {
    random = new Random(seed);

    vertices = new float[N * N * VERTEX_STRIDE];

    worldTransform = new Matrix4();
    lightDir = new Vector3(SUN_X, SUN_Y, SUN_Z).nor();

    elapsedTime = 0f;

    ShaderProgram.pedantic = false;

    for (int m = 0; m < N; m++) {
      for (int n = 0; n < N; n++) {
        int i = m * N + n;
        float u = (float) n / (N - 1) - 0.5f;
        float v = (float) m / (N - 1) - 0.5f;
        baseX[i] = u * PATCH_SIZE;
        baseZ[i] = v * PATCH_SIZE;
      }
    }

    buildInitialSpectrum();
    buildMesh();
    shader = new ShaderProgram(
        Gdx.files.internal("shaders/ocean.vert"),
        Gdx.files.internal("shaders/ocean.frag"));
  }

  protected void render(Camera camera, float delta) {
    elapsedTime += delta;

    // Evolve spectrum and perform 2D IFFTs to get spatial fields
    evolveSpectrum(elapsedTime);
    runIFFT2D(freqHeightRe, freqHeightIm, heights);
    runIFFT2D(freqGradXRe, freqGradXIm, gradX);
    runIFFT2D(freqGradZRe, freqGradZIm, gradZ);

    buildVertices();
    mesh.setVertices(vertices);
    shader.bind();
    setGlobalUniforms(camera);

    // Snap camera position to tile grid for seamless tiling
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
    if (mesh != null)
      mesh.dispose();
    if (shader != null)
      shader.dispose();
  }

  private void buildInitialSpectrum() {
    float wLen = (float) Math.sqrt(WIND_DIR_X * WIND_DIR_X + WIND_DIR_Z * WIND_DIR_Z);
    float windX = WIND_DIR_X / wLen;
    float windZ = WIND_DIR_Z / wLen;

    float g = 9.81f;
    float L = WIND_SPEED * WIND_SPEED / g;

    for (int m = 0; m < N; m++) {
      for (int n = 0; n < N; n++) {
        float kx = (float) (2.0 * Math.PI * (n - N / 2.0) / PATCH_SIZE);
        float kz = (float) (2.0 * Math.PI * (m - N / 2.0) / PATCH_SIZE);
        float kLen = (float) Math.sqrt(kx * kx + kz * kz);

        float phillips = 0f;
        if (kLen > 1e-6f) {
          float kL = kLen * L;
          float kL2 = kL * kL;
          float kdotw = (kx / kLen) * windX + (kz / kLen) * windZ;
          if (kdotw < 0f)
            kdotw *= 0.07f; // suppress waves moving against wind

          float ell = 0.01f;
          float dampK2 = kLen * kLen * ell * ell;

          phillips = PHILLIPS_CONSTANT
              * (float) Math.exp(-1.0 / kL2)
              / (kLen * kLen * kLen * kLen)
              * (kdotw * kdotw)
              * (float) Math.exp(-dampK2);
        }

        float amplitude = (float) Math.sqrt(phillips / 2.0);

        // Generate a single complex random number for h₀(k)
        h0Re[m][n] = (float) (random.nextGaussian() * amplitude);
        h0Im[m][n] = (float) (random.nextGaussian() * amplitude);
      }
    }
  }

  private void buildMesh() {
    int numVertices = N * N;
    int numIndices = (N - 1) * (N - 1) * 6;
    short[] indices = new short[numIndices];

    int idx = 0;
    for (int m = 0; m < N - 1; m++) {
      for (int n = 0; n < N - 1; n++) {
        short bl = (short) (m * N + n);
        short br = (short) (m * N + n + 1);
        short tl = (short) ((m + 1) * N + n);
        short tr = (short) ((m + 1) * N + n + 1);

        indices[idx++] = bl;
        indices[idx++] = br;
        indices[idx++] = tl;
        indices[idx++] = br;
        indices[idx++] = tr;
        indices[idx++] = tl;
      }
    }

    mesh = new Mesh(false, numVertices, numIndices,
        new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
        new VertexAttribute(VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
    mesh.setIndices(indices);
  }

  private void evolveSpectrum(float t) {
    float g = 9.81f;

    for (int m = 0; m < N; m++) {
      for (int n = 0; n < N; n++) {
        float kx = (float) (2.0 * Math.PI * (n - N / 2.0) / PATCH_SIZE);
        float kz = (float) (2.0 * Math.PI * (m - N / 2.0) / PATCH_SIZE);
        float kLen = (float) Math.sqrt(kx * kx + kz * kz);
        float omega = (kLen < 1e-6f) ? 0f : (float) Math.sqrt(g * kLen);

        float cosOt = MathUtils.cos(omega * t);
        float sinOt = MathUtils.sin(omega * t);

        // Positive wave vector term: h₀(k) * e^(iωt)
        float posRe = h0Re[m][n] * cosOt - h0Im[m][n] * sinOt;
        float posIm = h0Re[m][n] * sinOt + h0Im[m][n] * cosOt;

        // Negative wave vector index: (-k) corresponds to (N-m)%N and (N-n)%N
        int mNeg = (N - m) % N;
        int nNeg = (N - n) % N;

        // Conjugate of h₀(-k) * e^(-iωt) → h₀(-k)* * (cos - i sin)
        // = (h0Re[mNeg][nNeg] - i h0Im[mNeg][nNeg]) * (cosOt - i sinOt)
        // Expand and add the complex conjugate contribution:
        float negRe = h0Re[mNeg][nNeg] * cosOt + h0Im[mNeg][nNeg] * sinOt;
        float negIm = -h0Re[mNeg][nNeg] * sinOt + h0Im[mNeg][nNeg] * cosOt;

        // Total h(k,t) = pos + neg
        float re = posRe + negRe;
        float im = posIm + negIm;

        freqHeightRe[m][n] = re;
        freqHeightIm[m][n] = im;

        if (kLen > 1e-6f) {
          // Gradient i*k*h: real = -im*k, imag = re*k
          freqGradXRe[m][n] = -im * kx;
          freqGradXIm[m][n] = re * kx;
          freqGradZRe[m][n] = -im * kz;
          freqGradZIm[m][n] = re * kz;
        } else {
          freqGradXRe[m][n] = freqGradXIm[m][n] = 0f;
          freqGradZRe[m][n] = freqGradZIm[m][n] = 0f;
        }
      }
    }
  }

  private void runIFFT2D(float[][] re, float[][] im, float[] out) {
    int half = N / 2;

    for (int m = 0; m < N; m++) {
      for (int n = 0; n < N; n++) {
        int sm = (m + half) % N; // shifted row index
        int sn = (n + half) % N; // shifted column index
        int idx = (m * N + n) * 2;
        complexBuffer[idx] = re[sm][sn];
        complexBuffer[idx + 1] = im[sm][sn];
      }
    }

    fft2d.complexInverse(complexBuffer, true);
    // Copy real parts into out
    for (int i = 0; i < N * N; i++) {
      out[i] = complexBuffer[i * 2];
    }
  }

  private void buildVertices() {
    int vi = 0;
    for (int m = 0; m < N; m++) {
      for (int n = 0; n < N; n++) {
        int i = m * N + n;

        float h = heights[i];
        float gx = gradX[i];
        float gz = gradZ[i];

        // Position
        vertices[vi++] = baseX[i];
        vertices[vi++] = h;
        vertices[vi++] = baseZ[i];

        // Normal
        vertices[vi++] = -gx;
        vertices[vi++] = 1f;
        vertices[vi++] = -gz;

        // UV
        vertices[vi++] = (float) n / N;
        vertices[vi++] = (float) m / N;
      }
    }
  }

  private void setGlobalUniforms(Camera camera) {
    shader.setUniformMatrix("u_projView", camera.combined);
    shader.setUniformf("u_cameraPos", camera.position.x, camera.position.y, camera.position.z);
    shader.setUniformf("u_lightDir", lightDir.x, lightDir.y, lightDir.z);
    shader.setUniformf("u_deepColor", DEEP_R, DEEP_G, DEEP_B);
    shader.setUniformf("u_shallowColor", SHALLOW_R, SHALLOW_G, SHALLOW_B);
    shader.setUniformf("u_fogColor", FOG_R, FOG_G, FOG_B);
    shader.setUniformf("u_fogDensity", FOG_DENSITY);
  }

  private void drawTile(float worldOffsetX, float worldOffsetZ) {
    worldTransform.setToTranslation(worldOffsetX, 0f, worldOffsetZ);

    shader.setUniformMatrix("u_world", worldTransform);

    mesh.render(shader, GL20.GL_TRIANGLES);
  }
}