#ifdef GL_ES
precision highp float;
#endif

uniform vec3 u_cameraPos;
uniform vec3 u_lightDir; // toward light
uniform vec3 u_deepColor;
uniform vec3 u_shallowColor;
uniform vec3 u_fogColor;
uniform float u_fogDensity;

varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec2 v_uv;
varying float v_viewDist;

float fresnelSchlick(float cosTheta, float F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

void main() {
    vec3 N = normalize(v_normal);
    vec3 V = normalize(u_cameraPos - v_worldPos);
    vec3 L = normalize(u_lightDir);

    // Diffuse lighting
    float diff = max(dot(N, L), 0.0);
    vec3 bodyColor = mix(u_deepColor, u_shallowColor, diff * 0.8);

    float fresnel = fresnelSchlick(max(dot(N, V), 0.0), 0.02);
    vec3 reflection = vec3(0.72, 0.85, 0.95); // sky color

    vec3 oceanColor = mix(bodyColor, reflection, fresnel);

    // fog
    float fogAmt = 1.0 - exp(-v_viewDist * u_fogDensity);
    oceanColor = mix(oceanColor, u_fogColor, clamp(fogAmt, 0.0, 1.0));

    gl_FragColor = vec4(oceanColor, 1.0);
}