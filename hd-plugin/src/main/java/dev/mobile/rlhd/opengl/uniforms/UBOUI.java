package dev.mobile.rlhd.opengl.uniforms;

import dev.mobile.rlhd.utils.buffer.GLBuffer;

import static dev.mobile.rlhd.mobile.MobileGL.*;

public class UBOUI extends UniformBuffer<GLBuffer> {
	public UBOUI() {
		super(GL_DYNAMIC_DRAW);
	}

	public Property sourceDimensions = addProperty(PropertyType.IVec2, "sourceDimensions");
	public Property targetDimensions = addProperty(PropertyType.IVec2, "targetDimensions");
	public Property alphaOverlay = addProperty(PropertyType.FVec4, "alphaOverlay");
}
