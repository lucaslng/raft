#version 330 core

const vec3  waterColour    = vec3(0.604, 0.867, 0.851);
const float fresnelPower   = 0.5;
const float edgeSoftness   = 1.0;
const float minBlueness    = 0.4;
const float maxBlueness    = 0.8;
const float murkyDepth     = 14.0;

in vec4 pass_clipSpaceGrid;
in vec4 pass_clipSpaceReal;
in vec3 pass_normal;
in vec3 pass_toCameraVector;
in vec3 pass_specular;
in vec3 pass_diffuse;

out vec4 out_colour;

uniform sampler2D u_reflectTex;
uniform sampler2D u_refractTex;
uniform sampler2D u_depthTex;
uniform vec2      u_nearFar;    // x = near plane, y = far plane

// ----- Helpers (identical to original) -----------------------------------

float toLinearDepth(float zDepth) {
    float near = u_nearFar.x, far = u_nearFar.y;
    return 2.0 * near * far / (far + near - (2.0 * zDepth - 1.0) * (far - near));
}

float waterDepth(vec2 texCoords) {
    float floor_z    = toLinearDepth(texture(u_depthTex, texCoords).r);
    float surface_z  = toLinearDepth(gl_FragCoord.z);
    return floor_z - surface_z;
}

vec2 clipToTexCoords(vec4 clip) {
    vec2 ndc = clip.xy / clip.w;
    return clamp(ndc * 0.5 + 0.5, 0.002, 0.998);
}

float fresnel() {
    float factor = dot(normalize(pass_toCameraVector), normalize(pass_normal));
    return clamp(pow(factor, fresnelPower), 0.0, 1.0);
}

vec3 applyMurkiness(vec3 refract, float depth) {
    float t = clamp(depth / murkyDepth, 0.0, 1.0);
    return mix(refract, waterColour, minBlueness + t * (maxBlueness - minBlueness));
}

// -------------------------------------------------------------------------

void main(void) {
    vec2 gridCoords = clipToTexCoords(pass_clipSpaceGrid);
    vec2 realCoords = clipToTexCoords(pass_clipSpaceReal);

    vec2  refractCoords = gridCoords;
    vec2  reflectCoords = vec2(gridCoords.x, 1.0 - gridCoords.y);
    float depth         = waterDepth(realCoords);

    vec3 refractCol = applyMurkiness(texture(u_refractTex, refractCoords).rgb, depth);
    vec3 reflectCol = mix(texture(u_reflectTex, reflectCoords).rgb, waterColour, minBlueness);

    vec3 colour = mix(reflectCol, refractCol, fresnel());
    colour      = colour * pass_diffuse + pass_specular;

    out_colour   = vec4(colour, 1.0);
    out_colour.a = clamp(depth / edgeSoftness, 0.0, 1.0);  // soft edges near shore
}