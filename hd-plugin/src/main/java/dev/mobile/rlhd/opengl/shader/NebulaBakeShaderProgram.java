package dev.mobile.rlhd.opengl.shader;

import static dev.mobile.rlhd.mobile.MobileGL.*;

public class NebulaBakeShaderProgram extends ShaderProgram {
	public final Uniform1i uniCubeFace = addUniform1i("cubeFace");

	public NebulaBakeShaderProgram() {
		super(t -> t
			.add(GL_VERTEX_SHADER, "sky_nebula_bake_vert.glsl")
			.add(GL_FRAGMENT_SHADER, "sky_nebula_bake_frag.glsl"));
	}
}
