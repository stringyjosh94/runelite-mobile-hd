package dev.mobile.rlhd.opengl.shader;

import java.io.IOException;
import dev.mobile.rlhd.config.ShadowMode;

import static dev.mobile.rlhd.mobile.MobileGL.*;

public class TerrainShadowShaderProgram extends ShaderProgram {
	public TerrainShadowShaderProgram() {
		super(t -> t
			.add(GL_VERTEX_SHADER, "shadow_vert.glsl")
			.add(GL_FRAGMENT_SHADER, "stub_frag.glsl"));
	}

	@Override
	public void compile(ShaderIncludes includes) throws ShaderException, IOException {
		super.compile(includes.copy()
			.define("SHADOW_MODE", ShadowMode.FAST)
			.define("TERRAIN_ONLY_PASS", true)
			.define("SHADOW_TRANSPARENCY", false));
	}
}
