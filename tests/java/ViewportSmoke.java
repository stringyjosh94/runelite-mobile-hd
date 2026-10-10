import dev.mobile.runelite.ViewportSizing;
public class ViewportSmoke {
 public static void main(String[] args) {
  for(int[] screen:new int[][]{{1920,1200},{1280,800},{2424,1080},{1024,768}})for(boolean low:new boolean[]{false,true}) {
   int[] v=ViewportSizing.calculate(screen[0],screen[1],low);
   if(Math.abs((double)v[0]/v[1]-(double)screen[0]/screen[1])>.005)throw new AssertionError("Aspect mismatch");
   if(v[1]<540)throw new AssertionError("Client minimum height");
   if(v[0]<1100)throw new AssertionError("Client minimum width");
  }
  int[] tablet=ViewportSizing.calculate(1920,1200,false);
  if(tablet[0]!=1100 || tablet[1]<680)throw new AssertionError("Sidebar space/aspect");
  int[] roomy=ViewportSizing.calculate(2424,1080,false,720);
  if(roomy[1]!=720 || Math.abs((double)roomy[0]/roomy[1]-2424.0/1080)>.005)throw new AssertionError("Roomy aspect/height");
  if(Math.abs(ViewportSizing.sidebarZoom(720)/720-ViewportSizing.sidebarZoom(540)/540)>.00001)throw new AssertionError("Roomy sidebar magnification mismatch");
  System.out.println("TABLET_PHONE_ASPECT_AND_CLIENT_MINIMUM_OK");
 }
}
