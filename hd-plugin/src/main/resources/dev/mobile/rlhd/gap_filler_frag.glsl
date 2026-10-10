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


out vec4 FragColor;

void main() {
    // Write the smallest depth which won't round to zero for DEPTH_COMPONENT_32F
    gl_FragDepth = 1.17549435e-38;
    FragColor = vec4(0, 0, 0, 1);
}
