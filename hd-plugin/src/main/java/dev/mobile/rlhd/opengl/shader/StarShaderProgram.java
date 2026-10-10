package dev.mobile.rlhd.opengl.shader;

import static dev.mobile.rlhd.mobile.MobileGL.*;

public class StarShaderProgram extends ShaderProgram {
	public StarShaderProgram() {
		super(t -> t
			.add(GL_VERTEX_SHADER, "star_vert.glsl")
			.add(GL_FRAGMENT_SHADER, "star_frag.glsl"));
	}
}
