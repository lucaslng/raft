#ifdef GL_ES
precision highp float;
#endif

// --- Attributes supplied by libGDX Mesh ---
attribute vec3 a_position;   // world-space XYZ (displacement already applied on CPU)
attribute vec3 a_normal;     // un-normalised gradient normal from IFFT
attribute vec2 a_texCoord0;  // 0..1 UV across the tile

// --- Uniforms ---
uniform mat4 u_projView;      // camera.combined
uniform mat4 u_world;         // per-tile translation matrix
uniform mat3 u_normalMatrix;  // inv-transpose of u_world (for normals)

uniform float u_time;       // elapsed time (seconds) — for detail UV animation

// --- Varyings passed to fragment shader ---
varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec2 v_uv;          // tile UV (0..1)
varying vec2 v_detailUv;    // animated fine-detail UV for foam / normal detail
varying float v_viewDist;   // distance from camera — used for horizon fade

void main() {
	vec4 worldPos = u_world * vec4(a_position, 1.0);
	v_worldPos = worldPos.xyz;

	// Normalise the normal in the vertex shader to avoid per-fragment cost.
	// u_normalMatrix is the inverse-transpose so non-uniform scales are handled.
	v_normal = normalize(u_normalMatrix * a_normal);

	v_uv = a_texCoord0;

	// Animate a finer detail UV layer for foam scrolling
	// Scale up so one detail tile covers 1/8th of the patch, scroll slowly.
	vec2 detailScale = vec2(8.0, 8.0);
	vec2 detailScroll = vec2(u_time * 0.04, u_time * 0.02);
	v_detailUv = a_texCoord0 * detailScale + detailScroll;

	// Clip-space distance for the horizon fog / fade
	v_viewDist = length(worldPos.xyz);

	gl_Position = u_projView * worldPos;
}