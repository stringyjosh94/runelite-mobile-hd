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


#include <uniforms/global.glsl>
#include <uniforms/sky.glsl>

out vec2 fScreenPos;

void main() {
    // Fullscreen triangle - generates vertices at (-1,-1), (3,-1), (-1,3)
    vec2 pos = vec2((gl_VertexID & 1) * 4.0 - 1.0, (gl_VertexID & 2) * 2.0 - 1.0);
    gl_Position = vec4(pos, -1.0, 1.0); // At the far plane

    // Pass screen position to fragment shader for per-pixel ray calculation
    fScreenPos = pos;
}
