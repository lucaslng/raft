package com.lucaslng.raft.rendering.water;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Renders a single water mesh that follows the player camera, creating the
 * illusion of an infinite ocean.
 *
 * Usage each frame:
 *   1. renderer.beginReflection()  → render scene with Y-flipped camera → renderer.endReflection()
 *   2. renderer.beginRefraction()  → render scene normally (below water) → renderer.endRefraction()
 *   3. renderer.render(camera, ...)
 */
public class WaterRenderer {

    private static final float WAVE_SPEED = 0.002f;

    private static final int UNIT_REFLECT = 0;
    private static final int UNIT_REFRACT = 1;
    private static final int UNIT_DEPTH   = 2;

    private final WaterMesh    water;
    private final ShaderProgram shader;
    private final FrameBuffer  reflectionFbo;
    private final FrameBuffer  refractionFbo;  // colour + depth texture attachments

    private final int   gridCount;
    private float       waveTime = 0f;

    /**
     * @param gridCount    Grid squares per axis. 200 gives a wide ocean at low cost.
     * @param waterHeight  World Y of the water surface.
     * @param fboWidth     Reflection / refraction texture width  (e.g. 1280).
     * @param fboHeight    Reflection / refraction texture height (e.g. 720).
     */
    public WaterRenderer(int gridCount, float waterHeight, int fboWidth, int fboHeight) {
        this.gridCount = gridCount;
        this.water     = new WaterMesh(gridCount, waterHeight);
        this.shader    = loadShader();

        // Standard FBO for reflection (colour only)
        reflectionFbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, true);

        // Refraction FBO needs a *texture* depth attachment so the shader can
        // read depth values (for edge softness and murkiness calculations).
        FrameBufferBuilder builder = new FrameBufferBuilder(fboWidth, fboHeight);
        builder.addColorTextureAttachment(GL30.GL_RGBA8, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE);
        builder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL20.GL_FLOAT);
        refractionFbo = builder.build();
    }

    // ------------------------------------------------------------------ //
    //  FBO helpers                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Begin rendering into the reflection buffer.
     * Before calling this, flip your camera's Y position and pitch:
     *   camera.position.y = waterY * 2 - camera.position.y;
     *   camera.direction.y = -camera.direction.y;
     *   camera.update();
     * Also enable a clipping plane so only above-water geometry is drawn.
     */
    public void beginReflection() {
        reflectionFbo.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    public void endReflection() { reflectionFbo.end(); }

    /**
     * Begin rendering into the refraction buffer.
     * Use the normal camera orientation, but clip geometry above the water
     * surface so only underwater objects contribute.
     */
    public void beginRefraction() {
        refractionFbo.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    public void endRefraction() { refractionFbo.end(); }

    // ------------------------------------------------------------------ //
    //  Main render call                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Renders the water. Call after endRefraction() and before swapping buffers.
     *
     * @param camera         Active scene camera (water grid will auto-follow it).
     * @param lightDirection Normalised direction vector toward the light source.
     * @param lightColour    RGB colour of the light source.
     * @param lightBias      x = ambient factor, y = diffuse factor (e.g. 0.3, 0.8).
     */
    public void render(Camera camera,
                       Vector3 lightDirection,
                       Vector3 lightColour,
                       Vector2 lightBias) {
        waveTime += WAVE_SPEED;

        bindTextures();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        loadUniforms(camera, lightDirection, lightColour, lightBias);
        water.getMesh().render(shader, GL20.GL_TRIANGLES, 0, water.getVertexCount());

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        water.dispose();
        shader.dispose();
        reflectionFbo.dispose();
        refractionFbo.dispose();
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private void bindTextures() {
        // Reflection colour
        reflectionFbo.getColorBufferTexture().bind(UNIT_REFLECT);

        // Refraction colour  (attachment index 0)
        refractionFbo.getTextureAttachments().get(0).bind(UNIT_REFRACT);

        // Refraction depth   (attachment index 1, added via FrameBufferBuilder)
        refractionFbo.getTextureAttachments().get(1).bind(UNIT_DEPTH);
    }

    private void loadUniforms(Camera cam, Vector3 lightDir, Vector3 lightCol, Vector2 bias) {
        // Centre the grid on the camera's XZ position so it follows the player.
        float offsetX = cam.position.x - gridCount * 0.5f;
        float offsetZ = cam.position.z - gridCount * 0.5f;

        shader.setUniformf("u_offset",          offsetX, offsetZ);
        shader.setUniformMatrix("u_projView",   cam.combined);
        shader.setUniformf("u_cameraPos",       cam.position);
        shader.setUniformf("u_nearFar",         cam.near, cam.far);
        shader.setUniformf("u_waveTime",        waveTime);
        shader.setUniformf("u_height",          water.getHeight());
        shader.setUniformf("u_lightDir",        lightDir);
        shader.setUniformf("u_lightColour",     lightCol);
        shader.setUniformf("u_lightBias",       bias);
        shader.setUniformi("u_reflectTex",      UNIT_REFLECT);
        shader.setUniformi("u_refractTex",      UNIT_REFRACT);
        shader.setUniformi("u_depthTex",        UNIT_DEPTH);
    }

    private ShaderProgram loadShader() {
        ShaderProgram.pedantic = false;   // don't crash on unused uniforms
        ShaderProgram prog = new ShaderProgram(
            Gdx.files.internal("shaders/water.vert"),
            Gdx.files.internal("shaders/water.frag")
        );
        if (!prog.isCompiled())
            throw new RuntimeException("Water shader compile error:\n" + prog.getLog());
        return prog;
    }
}