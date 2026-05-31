#version 330 core

const float PI             = 3.14159265358979;
const float waveLength     = 4.0;
const float waveAmplitude  = 0.2;
const float specReflect    = 0.4;
const float shineDamper    = 20.0;

in vec2 in_position;    // (x, z) grid coordinates
in vec4 in_indicators;  // offsets to the other two verts in this triangle

out vec4 pass_clipSpaceGrid;
out vec4 pass_clipSpaceReal;
out vec3 pass_normal;
out vec3 pass_toCameraVector;
out vec3 pass_specular;
out vec3 pass_diffuse;

uniform mat4  u_projView;
uniform float u_height;
uniform vec3  u_cameraPos;
uniform float u_waveTime;
uniform vec2  u_offset;      // XZ world-space shift so the mesh follows the camera
uniform vec3  u_lightDir;
uniform vec3  u_lightColour;
uniform vec2  u_lightBias;   // x = ambient, y = diffuse

// ----- Lighting helpers (identical to original) --------------------------

vec3 specularLighting(vec3 toCam, vec3 toLight, vec3 normal) {
    vec3  reflected = reflect(-toLight, normal);
    float factor    = pow(max(dot(reflected, toCam), 0.0), shineDamper);
    return factor * specReflect * u_lightColour;
}

vec3 diffuseLighting(vec3 toLight, vec3 normal) {
    float brightness = max(dot(toLight, normal), 0.0);
    return (u_lightColour * u_lightBias.x) + (brightness * u_lightColour * u_lightBias.y);
}

vec3 calcNormal(vec3 v0, vec3 v1, vec3 v2) {
    return normalize(cross(v1 - v0, v2 - v0));
}

// ----- Wave distortion (identical to original) ---------------------------

float generateOffset(float x, float z, float val1, float val2) {
    float radiansX = ((mod(x + z * x * val1, waveLength) / waveLength)
                      + u_waveTime * mod(x * 0.8 + z, 1.5)) * 2.0 * PI;
    float radiansZ = ((mod(val2 * (z * x + x * z), waveLength) / waveLength)
                      + u_waveTime * 2.0 * mod(x, 2.0)) * 2.0 * PI;
    return waveAmplitude * 0.5 * (sin(radiansZ) + cos(radiansX));
}

vec3 applyDistortion(vec3 v) {
    return v + vec3(
        generateOffset(v.x, v.z, 0.2, 0.1),
        generateOffset(v.x, v.z, 0.1, 0.3),
        generateOffset(v.x, v.z, 0.15, 0.2)
    );
}

// -------------------------------------------------------------------------

void main(void) {

    // Reconstruct all 3 world-space positions for this triangle.
    // u_offset shifts the whole grid to follow the camera each frame.
    vec3 worldPos = vec3(in_position.x + u_offset.x, u_height, in_position.y + u_offset.y);
    vec3 v1       = worldPos + vec3(in_indicators.x, 0.0, in_indicators.y);
    vec3 v2       = worldPos + vec3(in_indicators.z, 0.0, in_indicators.w);

    // Clip-space of the undistorted grid position — used for texture coord lookup.
    pass_clipSpaceGrid = u_projView * vec4(worldPos, 1.0);

    // Apply wave distortion to all three verts so the normal stays accurate.
    worldPos = applyDistortion(worldPos);
    v1       = applyDistortion(v1);
    v2       = applyDistortion(v2);

    pass_normal        = calcNormal(worldPos, v1, v2);
    pass_clipSpaceReal = u_projView * vec4(worldPos, 1.0);
    gl_Position        = pass_clipSpaceReal;

    pass_toCameraVector = normalize(u_cameraPos - worldPos);

    vec3 toLight  = -normalize(u_lightDir);
    pass_specular = specularLighting(pass_toCameraVector, toLight, pass_normal);
    pass_diffuse  = diffuseLighting(toLight, pass_normal);
}