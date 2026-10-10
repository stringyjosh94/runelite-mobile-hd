package dev.mobile.rlhd.renderer;

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import net.runelite.api.hooks.*;
import dev.mobile.rlhd.mobile.*;
import dev.mobile.rlhd.opengl.shader.ShaderException;
import dev.mobile.rlhd.opengl.shader.ShaderIncludes;
import dev.mobile.rlhd.scene.SceneContext;

public interface Renderer extends DrawCallbacks {
	default boolean supportsGpu(MobileCaps glCaps) {
		return true;
	}
	default int gpuFlags() {
		return 0;
	}
	default void initialize() {}
	default void destroy() {}
	default void addShaderIncludes(ShaderIncludes includes) {}
	default void initializeShaders(ShaderIncludes includes) throws ShaderException, IOException {}
	default void destroyShaders() {}
	default void waitUntilIdle() {}
	default void processConfigChanges(Set<String> keys) {}
	default boolean isLoadingScene() {
		return false;
	}
	default void reloadScene() {}
	default void clearCaches() {}
	@Nullable
	default SceneContext getSceneContext() {
		return null;
	}
}
