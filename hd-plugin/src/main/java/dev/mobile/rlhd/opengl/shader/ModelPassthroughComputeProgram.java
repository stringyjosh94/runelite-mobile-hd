package dev.mobile.rlhd.opengl.shader;

import dev.mobile.rlhd.mobile.*;

public class ModelPassthroughComputeProgram extends ShaderProgram {
	public ModelPassthroughComputeProgram() {
		super(t -> t.add(MobileGL.GL_COMPUTE_SHADER, "comp_unordered.glsl"));
	}
}
