#ifdef GL_ES
precision mediump float;
#endif

uniform samplerCube u_cubemap;
varying vec3 v_texCoords;

void main() {
  gl_FragColor = textureCube(u_cubemap, v_texCoords);
}