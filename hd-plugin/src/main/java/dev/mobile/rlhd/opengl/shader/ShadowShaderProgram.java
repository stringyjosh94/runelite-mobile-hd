package dev.mobile.rlhd.opengl.shader;

import java.io.IOException;
import dev.mobile.rlhd.config.ShadowMode;

import static dev.mobile.rlhd.mobile.MobileGL.*;
import static dev.mobile.rlhd.HdPlugin.TEXTURE_UNIT_GAME;
import static dev.mobile.rlhd.renderer.zone.ZoneRenderer.TEXTURE_UNIT_TEXTURED_FACES;

public abstract class ShadowShaderProgram extends ShaderProgram {
	protected final UniformTexture uniTextureArray = addUniformTexture("textureArray");
	protected final UniformTexture uniTextureFaces = addUniformTexture("textureFaces");

	protected ShadowMode mode;

	ShadowShaderProgram() {
		super(t -> t
			.add(GL_VERTEX_SHADER, "shadow_vert.glsl")
			.add(GL_FRAGMENT_SHADER, "shadow_frag.glsl"));
	}

	@Override
	protected void initialize() {
		uniTextureArray.set(TEXTURE_UNIT_GAME);
		uniTextureFaces.set(TEXTURE_UNIT_TEXTURED_FACES);
	}

	@Override
	public void compile(ShaderIncludes includes) throws ShaderException, IOException {
		super.compile(includes.copy().define("SHADOW_MODE", mode));
	}

	public static class Fast extends ShadowShaderProgram {
		public Fast() {
			mode = ShadowMode.FAST;
			uniTextureArray.ignoreMissing = true;
		}
	}

	public static class Detailed extends ShadowShaderProgram {
		public Detailed() {
			mode = ShadowMode.DETAILED;
		}
	}

	public static class Legacy extends ShadowShaderProgram {
		public Legacy setMode(ShadowMode mode) {
			this.mode = mode;
			uniTextureArray.ignoreMissing = mode != ShadowMode.DETAILED;
			return this;
		}

		@Override
		public void compile(ShaderIncludes includes) throws ShaderException, IOException {
			if (mode == ShadowMode.DETAILED) {
				shaderTemplate.add(GL_GEOMETRY_SHADER, "shadow_geom.glsl");
			} else {
				shaderTemplate.remove(GL_GEOMETRY_SHADER);
			}
			super.compile(includes);
		}
	}
}
