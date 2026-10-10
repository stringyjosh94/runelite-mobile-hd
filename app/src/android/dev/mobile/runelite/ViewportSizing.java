package dev.mobile.runelite;
/** Match usable screen aspect without spending pixels on native tablet resolution. */
public final class ViewportSizing {
 public static float sidebarZoom(int virtualHeight) {return Math.max(2f,Math.min(3f,2f*virtualHeight/540f));}
 public static int[] calculate(int width,int height,boolean lowPower) {
  return calculate(width,height,lowPower,540);
 }
 public static int[] calculate(int width,int height,boolean lowPower,int baseHeight) {
  double aspect=width>0&&height>0?(double)width/height:16.0/9;
  double h=Math.min(lowPower?432:540,Math.sqrt((lowPower?400000:650000)/aspect));
  // RuneLite needs roughly 503 game pixels plus frame chrome. Reduce 3D
  // resolution inside HD instead of clipping the desktop below this height.
  h=Math.max(baseHeight,h);
  double w=h*aspect;
  // Reserve enough width for the minimum game canvas plus an open sidebar.
  if(w<1100) {w=1100;h=w/aspect;}
  return new int[]{(int)Math.round(w),(int)Math.round(h)};
 }
}
