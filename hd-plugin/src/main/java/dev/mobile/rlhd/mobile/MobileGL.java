package dev.mobile.rlhd.mobile;
import org.lwjgl.opengles.*;
import org.lwjgl.system.Callback;
public class MobileGL extends GLES32 {
 public static MobileCaps createCapabilities() { if (!GLES.createCapabilities().GLES32) throw new IllegalStateException("117HD GLES requires native OpenGL ES 3.2"); return getCapabilities(); }
 public static MobileCaps getCapabilities() { return new MobileCaps(GLES.getCapabilities()); }
 public static Callback debugCallback() { return null; }

 public static final int GL_MAP_PERSISTENT_BIT=0x40, GL_DYNAMIC_STORAGE_BIT=0x100, GL_CLIENT_STORAGE_BIT=0x200;
 public static final int GL_MULTISAMPLE=0x809D, GL_PROGRAM_POINT_SIZE=0x8642, GL_POINT_SPRITE=0x8861;
 public static final int GL_TIMESTAMP=0x8E28;
 public static void glEnable(int cap) {
  // ES multisampling is determined by framebuffer samples; point sprites and program point size are always enabled.
  if(cap!=GL_MULTISAMPLE && cap!=GL_PROGRAM_POINT_SIZE && cap!=GL_POINT_SPRITE) GLES32.glEnable(cap);
 }
 public static void glDisable(int cap) {
  if(cap!=GL_MULTISAMPLE && cap!=GL_PROGRAM_POINT_SIZE && cap!=GL_POINT_SPRITE) GLES32.glDisable(cap);
 }
 public static void glClearDepth(double depth) { glClearDepthf((float)depth); }
 public static void glDrawBuffer(int buffer) { glDrawBuffers(buffer); }
 public static java.nio.ByteBuffer glMapBuffer(int target,int access,java.nio.ByteBuffer old) {
  int flags=access==GL_READ_ONLY?GL_MAP_READ_BIT:access==GL_WRITE_ONLY?GL_MAP_WRITE_BIT:GL_MAP_READ_BIT|GL_MAP_WRITE_BIT;
  return glMapBufferRange(target,0,glGetBufferParameteri(target,GL_BUFFER_SIZE),flags,old);
 }
 public static void glBufferStorage(int target,long size,int flags) {
  throw new UnsupportedOperationException("Persistent desktop buffer storage is disabled in the GLES backend");
 }
 public static void glMultiDrawArrays(int mode,java.nio.IntBuffer first,java.nio.IntBuffer count) {
  for(int i=0;i<count.remaining();i++) glDrawArrays(mode,first.get(first.position()+i),count.get(count.position()+i));
 }
 public static void glMultiDrawArraysIndirect(int mode,long offset,int count,int stride) {
  int step=stride==0?16:stride;
  for(int i=0;i<count;i++) glDrawArraysIndirect(mode,offset+(long)i*step);
 }
 public static void glQueryCounter(int id,int target) { org.lwjgl.opengles.EXTDisjointTimerQuery.glQueryCounterEXT(id,target); }
 public static void glGetQueryObjectiv(int id,int name,int[] out) { out[0]=org.lwjgl.opengles.EXTDisjointTimerQuery.glGetQueryObjectiEXT(id,name); }
 public static long glGetQueryObjectui64(int id,int name) { return org.lwjgl.opengles.EXTDisjointTimerQuery.glGetQueryObjectui64EXT(id,name); }
}
