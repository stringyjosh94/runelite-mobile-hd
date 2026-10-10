package dev.mobile.rlhd.opengl.shader;

import java.io.IOException;
import dev.mobile.rlhd.mobile.*;

import static dev.mobile.rlhd.HdPlugin.TEXTURE_UNIT_TILE_HEIGHT_MAP;

public class ModelSortingComputeProgram extends ShaderProgram {
	public final int threadCount, facesPerThread;

	private final UniformTexture uniTileHeightMap = addUniformTexture("tileHeightMap");

	public ModelSortingComputeProgram(int threadCount, int facesPerThread) {
		super(t -> t.add(MobileGL.GL_COMPUTE_SHADER, "comp.glsl"));
		this.threadCount = threadCount;
		this.facesPerThread = facesPerThread;
	}

	@Override
	protected void initialize() {
		uniTileHeightMap.set(TEXTURE_UNIT_TILE_HEIGHT_MAP);
	}

	@Override
	public void compile(ShaderIncludes includes) throws ShaderException, IOException {
		super.compile(includes
			.define("THREAD_COUNT", threadCount)
			.define("FACES_PER_THREAD", facesPerThread));
	}
}
