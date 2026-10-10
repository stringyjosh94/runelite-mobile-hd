package dev.mobile.rlhd.mobile;
import org.lwjgl.opengles.GLESCapabilities;
/** Internal renderer feature adapter; does not change driver version or extensions. */
public final class MobileCaps {
 public final boolean OpenGL31, OpenGL33, OpenGL40, OpenGL43;
 public final boolean GL_ARB_copy_buffer, GL_ARB_map_buffer_range, GL_ARB_shader_image_load_store;
 public final boolean GL_ARB_buffer_storage=false, GL_ARB_multi_draw_indirect=false;
 public final boolean GL_ARB_draw_indirect, GL_EXT_texture_filter_anisotropic;
 public final boolean forwardCompatible=true;
 public final long glDebugMessageControl=0, glTexStorage3D;
 public MobileCaps(GLESCapabilities c) {
  OpenGL31=c.GLES30; OpenGL33=c.GLES30; OpenGL40=c.GLES31; OpenGL43=c.GLES31;
  GL_ARB_copy_buffer=c.GLES30; GL_ARB_map_buffer_range=c.GLES30;
  GL_ARB_shader_image_load_store=c.GLES31; GL_ARB_draw_indirect=c.GLES31;
  GL_EXT_texture_filter_anisotropic=c.GL_EXT_texture_filter_anisotropic;
  glTexStorage3D=c.glTexStorage3D;
 }
}
