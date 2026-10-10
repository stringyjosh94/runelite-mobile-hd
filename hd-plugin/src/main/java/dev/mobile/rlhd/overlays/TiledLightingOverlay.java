package dev.mobile.rlhd.overlays;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayLayer;
import dev.mobile.rlhd.HdPlugin;
import dev.mobile.rlhd.config.DynamicLights;

import static dev.mobile.rlhd.mobile.MobileGL.*;
import static dev.mobile.rlhd.HdPlugin.TEXTURE_UNIT_TILED_LIGHTING_MAP;
import static dev.mobile.rlhd.utils.HDUtils.drawStringCentered;
import static dev.mobile.rlhd.utils.HDUtils.drawStringShadowed;

@Slf4j
@Singleton
public class TiledLightingOverlay extends ShaderOverlay<TiledLightingOverlay.Shader> {
	static class Shader extends ShaderOverlay.Shader {
		private final UniformTexture uniTiledLightingTextureArray = addUniformTexture("tiledLightingArray");

		public Shader() {
			super(t -> t.add(GL_FRAGMENT_SHADER, "overlays/tiled_lighting_overlay_frag.glsl"));
		}

		@Override
		protected void initialize() {
			uniTiledLightingTextureArray.set(TEXTURE_UNIT_TILED_LIGHTING_MAP);
		}
	}

	@Inject
	private HdPlugin plugin;

	public TiledLightingOverlay() {
		setLayer(OverlayLayer.ABOVE_SCENE);
		setFullscreen(true);
	}

	@Override
	public boolean isHidden() {
		return super.isHidden() || plugin.configDynamicLights == DynamicLights.NONE || !plugin.configTiledLighting;
	}

	@Override
	public Dimension render(Graphics2D g) {
		if (!super.isHidden()) {
			g.setColor(Color.YELLOW);
			boolean usingImageLoadStore = plugin.getTiledLightingImageStoreProgram().isValid();
			drawStringShadowed(g, String.format("Using GL_ARB_shader_image_load_store: %B", usingImageLoadStore), 4, 32);
			if (plugin.configDynamicLights == DynamicLights.NONE) {
				drawStringCentered(g, "Dynamic lights are disabled");
			} else if (!plugin.configTiledLighting) {
				drawStringCentered(g, "Tiled lighting is disabled");
			}
		}
		return super.render(g);
	}
}
