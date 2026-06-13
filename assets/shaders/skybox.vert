#ifdef GL_ES
precision highp float;
#endif

attribute vec3 a_position;
uniform mat4 u_projViewTrans; // projection * view (with no translation)
varying vec3 v_texCoords;
varying vec3 v_direction;     // world-space direction ray (un-normalised is fine)

void main() {
  v_texCoords = a_position; // direction vector into the cube
  v_direction = a_position;
  vec4 pos = u_projViewTrans * vec4(a_position, 1.0);
  gl_Position = pos.xyww; // trick: set z = w so depth = 1.0 (always behind everything)
}