#version 320 es
precision highp float;
precision highp int;
precision highp sampler2D;
precision highp sampler2DArray;
precision highp sampler2DShadow;
precision highp isamplerBuffer;
precision highp isampler3D;
precision highp samplerCube;
precision highp usampler2DArray;


// Depth-only stub. Apple's OpenGL rejects draws with no fragment shader
// attached (GL_INVALID_OPERATION), so this cannot be omitted entirely.
void main() {}
