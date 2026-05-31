#ifdef GL_ES
precision highp float;
#endif

// --- Uniforms ---
uniform vec3 u_cameraPos;
uniform vec3 u_lightDir;      // unit vector TOWARD the light source

uniform vec3 u_deepColor;     // ocean body colour (deep / back-scatter)
uniform vec3 u_shallowColor;  // wave-crest / shallow scatter colour

uniform float u_time;

// --- Varyings ---
varying vec3 v_worldPos;
varying vec3 v_normal;
varying vec2 v_uv;
varying vec2 v_detailUv;
varying float v_viewDist;

// ---------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------

/**
	* Schlick Fresnel approximation.
	*  cosTheta — dot(view, normal), clamped to [0,1]
	*  F0       — reflectance at normal incidence (~0.02 for water)
	*/
float fresnelSchlick(float cosTheta, float F0) {
		return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

/**
	* Blinn-Phong specular highlight.
	*  N — surface normal (unit)
	*  L — light direction (unit, toward light)
	*  V — view direction (unit, toward camera)
	*/
float specular(vec3 N, vec3 L, vec3 V, float shininess) {
		vec3 H = normalize(L + V);
		return pow(max(dot(N, H), 0.0), shininess);
}

/**
	* Procedural foam / whitecap based on wave steepness.
	* We use the magnitude of the XZ normal components as a proxy for
	* the wave Jacobian — steep regions get bright foam.
	*/
float foamAmount(vec3 N) {
		// When the normal tilts strongly from vertical, we have a steep crest.
		float steepness = 1.0 - abs(N.y);  // 0 = flat, 1 = vertical (breaking)
		float foam = smoothstep(0.35, 0.75, steepness);

		// Animate foam texture procedurally so it shimmers
		// A cheap hash-based pattern using UV and time
		vec2 foamUv = v_detailUv * 1.3 + vec2(sin(u_time * 0.3), cos(u_time * 0.2));
		float noise = fract(sin(dot(floor(foamUv), vec2(127.1, 311.7))) * 43758.5453);
		float softNoise = smoothstep(0.3, 0.8, noise);

		return foam * softNoise;
}

/**
	* Approximates the sky colour at the horizon for reflection blending.
	* In a full game, replace this with a cube-map texture lookup.
	*/
vec3 skyColor(vec3 reflectDir) {
		float t = clamp(reflectDir.y * 0.5 + 0.5, 0.0, 1.0);
		vec3 horizonColor = vec3(0.72, 0.85, 0.95);
		vec3 zenithColor  = vec3(0.25, 0.55, 0.90);
		return mix(horizonColor, zenithColor, t * t);
}

// ---------------------------------------------------------------
// Main
// ---------------------------------------------------------------

void main() {
		vec3 N = normalize(v_normal);
		vec3 V = normalize(u_cameraPos - v_worldPos);
		vec3 L = normalize(u_lightDir);

		// Fresnel — water reflectance F0 ≈ 0.02
		float cosTheta = max(dot(N, V), 0.0);
		float fresnel   = fresnelSchlick(cosTheta, 0.02);

		// ---------- Reflection ----------
		vec3 R          = reflect(-V, N);
		vec3 skyRefl    = skyColor(R);

		// Sun disc: sharp specular right on the sun reflection ray
		float sunSpec   = specular(N, L, V, 512.0);
		vec3 sunColor   = vec3(1.0, 0.95, 0.80);
		vec3 reflection = skyRefl + sunColor * sunSpec * 2.5;

		// ---------- Refraction / body colour ----------
		// Sub-surface scattering approximation: light transmitted upward
		// through the wave crests appears more saturated and bright.
		float sss = pow(max(dot(L, N), 0.0), 2.0) * 0.6;
		vec3 bodyColor = mix(u_deepColor, u_shallowColor, sss);

		// Diffuse term for gentle fill lighting on wave faces
		float diff = max(dot(N, L), 0.0);
		bodyColor += u_deepColor * diff * 0.15;

		// ---------- Combine ----------
		vec3 oceanColor = mix(bodyColor, reflection, fresnel);

		// ---------- Foam / whitecaps ----------
		float foam = foamAmount(N);
		// Foam is bright white, slightly warm from sunlight
		vec3 foamColor = vec3(0.95, 0.96, 1.0);
		oceanColor = mix(oceanColor, foamColor, foam);

		// ---------- Horizon fog / distance fade ----------
		// Fade to a hazy sky colour at the horizon so tile edges
		// are never visible as hard seams.
		float fogStart  = 300.0;
		float fogEnd    = 600.0;
		float fogFactor = clamp((v_viewDist - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
		vec3 fogColor   = vec3(0.75, 0.88, 0.95);
		oceanColor      = mix(oceanColor, fogColor, fogFactor * fogFactor);

		// ---------- Output ----------
		gl_FragColor = vec4(oceanColor, 1.0);
}