package dev.mobile.runelite;
import java.awt.image.BufferedImage;import java.lang.reflect.Field;import java.util.*;
public class GpuFramePoolSmoke {
 public static void main(String[] args)throws Exception {
  Field field=HostMain.class.getDeclaredField("completedGameFrame");field.setAccessible(true);
  Set<int[]> identities=Collections.newSetFromMap(new IdentityHashMap<>());
  BufferedImage previous=null;
  for(int n=0;n<500;n++) {
   int[] frame=HostMain.acquireGpuFrame(64,32);identities.add(frame);
   int old=previous==null?0:previous.getRGB(0,0);
   Arrays.fill(frame,(n%2==0)?0xffff0000:0xff0000ff);
   if(previous!=null && previous.getRGB(0,0)!=old)throw new AssertionError("Published buffer overwritten");
   HostMain.gpuFrame(frame,64,32);previous=(BufferedImage)field.get(null);
   if(previous.getRGB(63,31)!=frame[0])throw new AssertionError("Publication mismatch");
  }
  if(identities.size()>3)throw new AssertionError("Unbounded allocation");
  int[] resized=HostMain.acquireGpuFrame(5,3);Arrays.fill(resized,0xff00ff00);HostMain.gpuFrame(resized,5,3);
  BufferedImage image=(BufferedImage)field.get(null);
  if(image.getWidth()!=5 || image.getHeight()!=3 || image.getRGB(4,2)!=0xff00ff00)throw new AssertionError("Resize");
  HostMain.gpuStopped();
  System.out.println("GPU_FRAME_POOL_REUSE_PUBLICATION_ISOLATION_AND_RESIZE_OK");System.exit(0);
 }
}
