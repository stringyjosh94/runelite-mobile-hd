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


// One-time bake of the procedural nebula into a cubemap face. The nebula is a
// pure function of view direction (no animation), so evaluating its multi-octave
// fBm once per texel and sampling it thereafter removes the per-frame per-pixel
// noise cost from the sky shader entirely.

// NEBULA_BAKE tells starfield.glsl to expose the procedural nebula path rather
// than declaring/sampling the (not-yet-existing) nebula cubemap.
#define NEBULA_BAKE
#include <utils/starfield.glsl>

in vec3 fFaceDirection;

out vec4 FragColor;

void main() {
    FragColor = vec4(proceduralNebula(normalize(fFaceDirection)), 1.0);
}
