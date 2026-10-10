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

layout (location = 0) in vec2 vPos;
layout (location = 1) in vec2 vUv;

out vec2 fUv;

void main() {
    gl_Position = vec4(vPos, 0, 1);
    fUv = vUv;
}
