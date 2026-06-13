#ifdef GL_ES
precision highp float;
#endif

uniform samplerCube u_dayCubemap;
uniform samplerCube u_nightCubemap;
uniform float u_daylight;

uniform vec3  u_fogColor;          // base horizon/atmosphere colour
uniform vec3  u_fogSunColor;       // warm sun-glow colour near the sun disc
uniform vec3  u_sunDir;            // unit vector TOWARD the sun
uniform float u_horizonFogStart;   // elevation angle (0 = horizon) where fog begins
uniform float u_horizonFogEnd;     // elevation angle where fog reaches full strength

varying vec3 v_texCoords;
varying vec3 v_direction;

void main() {
  vec4 dayColor = textureCube(u_dayCubemap, v_texCoords);
  vec4 nightColor = textureCube(u_nightCubemap, v_texCoords);
  vec4 skyColor = mix(nightColor, dayColor, u_daylight);
  vec3 dir      = normalize(v_direction);

  float elevation = dir.y;
  float fogFactor = 1.0 - smoothstep(u_horizonFogStart, u_horizonFogEnd, elevation);

  // Tint the fog toward the sun colour on the side facing the sun.
  float sunAlign = max(dot(dir, u_sunDir), 0.0);
  vec3  fogColor = mix(u_fogColor, u_fogSunColor, pow(sunAlign, 6.0));

  gl_FragColor = vec4(mix(skyColor.rgb, fogColor, clamp(fogFactor, 0.0, 1.0)), 1.0);
}