package dev.mobile.rlhd.opengl.shader;

import static dev.mobile.rlhd.mobile.MobileGL.*;
import static dev.mobile.rlhd.HdPlugin.TEXTURE_UNIT_UI;

public class UIShaderProgram extends ShaderProgram {
	private final UniformTexture uniUiTexture = addUniformTexture("uiTexture");

	public UIShaderProgram() {
		super(t -> t
			.add(GL_VERTEX_SHADER, "ui_vert.glsl")
			.add(GL_FRAGMENT_SHADER, "ui_frag.glsl"));
	}

	@Override
	protected void initialize() {
		uniUiTexture.set(TEXTURE_UNIT_UI);
	}
}
