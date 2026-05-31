attribute vec3 a_position;
uniform mat4 u_projViewTrans; // projection * view (with no translation)
varying vec3 v_texCoords;

void main() {
  v_texCoords = a_position; // direction vector into the cube
  vec4 pos = u_projViewTrans * vec4(a_position, 1.0);
  gl_Position = pos.xyww; // trick: set z = w so depth = 1.0 (always behind everything)
}