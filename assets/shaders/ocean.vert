#ifdef GL_ES
precision highp float;
#endif

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projView;
uniform mat4 u_world;
uniform vec3 u_cameraPos;

varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec2 v_uv;
varying float v_viewDist;

void main() {
    vec4 worldPos = u_world * vec4(a_position, 1.0);
    v_worldPos = worldPos.xyz;
    v_normal = normalize((u_world * vec4(a_normal, 0.0)).xyz);
    v_uv = a_texCoord0;
    v_viewDist = length(worldPos.xyz - u_cameraPos);
    gl_Position = u_projView * worldPos;
}