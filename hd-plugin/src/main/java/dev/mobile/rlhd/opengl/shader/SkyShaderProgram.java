package dev.mobile.rlhd.opengl.shader;

import static dev.mobile.rlhd.mobile.MobileGL.*;
import static dev.mobile.rlhd.HdPlugin.TEXTURE_UNIT_NEBULA_MAP;

public class SkyShaderProgram extends ShaderProgram {
	protected final UniformTexture uniNebulaMap = addUniformTexture("nebulaMap");

	public SkyShaderProgram() {
		super(t -> t
			.add(GL_VERTEX_SHADER, "sky_vert.glsl")
			.add(GL_FRAGMENT_SHADER, "sky_frag.glsl"));
		uniNebulaMap.ignoreMissing = true;
	}

	@Override
	protected void initialize() {
		uniNebulaMap.set(TEXTURE_UNIT_NEBULA_MAP);
	}
}
