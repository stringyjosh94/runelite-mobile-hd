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


uniform sampler2D shadowMap;

in vec2 fUv;

out vec4 FragColor;

void main() {
    FragColor = vec4(fUv, 0, 1);
}
