package dev.mobile.runelite;
import java.awt.Canvas;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import net.runelite.rlawt.AWTContext;
import org.lwjgl.opengles.GLES;
import static org.lwjgl.opengles.GLES30.*;
/** Runs against actual desktop EGL/GLES and the same C presentation source as Android. */
public class NativeGlesPresentationSmoke {
 static BufferedImage image()throws Exception {Field f=HostMain.class.getDeclaredField("completedGameFrame");f.setAccessible(true);return (BufferedImage)f.get(null);}
 static void draw(int w,int h) {
  glViewport(0,0,w,h);glEnable(GL_SCISSOR_TEST);
  glScissor(0,0,w,1);glClearColor(1,0,0,1);glClear(GL_COLOR_BUFFER_BIT);
  glScissor(0,1,w,h-1);glClearColor(0,0,1,1);glClear(GL_COLOR_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);
 }
 static void check(int w,int h)throws Exception {
  BufferedImage image=image();
  if(image==null || image.getWidth()!=w || image.getHeight()!=h)throw new AssertionError("Missing/resized frame");
  if(image.getRGB(0,0)!=0xff0000ff || image.getRGB(w-1,h-1)!=0xffff0000)throw new AssertionError("RGBA/orientation corruption");
 }
 public static void main(String[] args)throws Exception {
  AWTContext.loadNatives();Canvas canvas=new Canvas();canvas.setSize(3,2);AWTContext context=new AWTContext(canvas);
  context.configurePixelFormat(0,0,0);context.createGLContext();GLES.createCapabilities();
  int buffer=glGenBuffers(),framebuffer=glGenFramebuffers();glBindBuffer(GL_PIXEL_PACK_BUFFER,buffer);glBufferData(GL_PIXEL_PACK_BUFFER,1024,GL_STREAM_READ);
  glBindFramebuffer(GL_READ_FRAMEBUFFER,framebuffer);glPixelStorei(GL_PACK_ALIGNMENT,8);glPixelStorei(GL_PACK_ROW_LENGTH,13);glPixelStorei(GL_PACK_SKIP_ROWS,2);glPixelStorei(GL_PACK_SKIP_PIXELS,1);
  draw(3,2);
  for(int i=0;i<5;i++){context.swapBuffers();glFinish();}
  check(3,2);
  if(glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING)!=buffer || glGetInteger(GL_READ_FRAMEBUFFER_BINDING)!=framebuffer || glGetInteger(GL_PACK_ALIGNMENT)!=8 || glGetInteger(GL_PACK_ROW_LENGTH)!=13 || glGetInteger(GL_PACK_SKIP_ROWS)!=2 || glGetInteger(GL_PACK_SKIP_PIXELS)!=1)throw new AssertionError("Renderer pack/FBO state changed");
  // Exercise the busy-slot/backpressure branch without explicit test finishes.
  for(int i=0;i<125;i++){draw(3,2);context.swapBuffers();}
  glFinish();context.swapBuffers();check(3,2);
  canvas.setSize(5,3);context.makeCurrent();draw(5,3);
  for(int i=0;i<5;i++){context.swapBuffers();glFinish();}check(5,3);
  if(glGetError()!=GL_NO_ERROR)throw new AssertionError("GL error after presentation/resize");
  glBindBuffer(GL_PIXEL_PACK_BUFFER,0);glDeleteBuffers(buffer);glBindFramebuffer(GL_READ_FRAMEBUFFER,0);glDeleteFramebuffers(framebuffer);
  context.destroy();GLES.setCapabilities(null);
  System.out.println("NATIVE_GLES_READBACK_RGBA_ORIENTATION_STATE_RESIZE_AND_CLEANUP_OK");System.exit(0);
 }
}
