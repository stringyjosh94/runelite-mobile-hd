package dev.mobile.runelite;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.api.hooks.Callbacks;
public class RealCameraWheelSmoke {
 public static void main(String[] args)throws Exception {
  Class<?> engine=Class.forName("client"),angle=Class.forName("uy");
  Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field uf=unsafeType.getDeclaredField("theUnsafe");uf.setAccessible(true);Object unsafe=uf.get(null);
  Client client=(Client)unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,engine);
  for(String name:new String[]{"je","jh"}) {Field f=engine.getDeclaredField(name);f.setAccessible(true);f.set(null,angle.getConstructor(int.class).newInstance(0));}
  client.setCameraYawTarget(14000);client.setCameraPitchTarget(1600);
  MobileCameraInput.add(300,100);MobileCameraInput.apply(client);
  if(client.getCameraYawTarget()!=11600 || client.getCameraPitchTarget()!=2400)throw new AssertionError("Actual angle scale mismatch");
  Method sync=engine.getMethod("pk",int.class);sync.invoke(null,0);
  if(client.getCameraYawTarget()!=11600 || client.getCameraPitchTarget()!=2400)throw new AssertionError("Float sync snaps back");
  MobileCameraInput.add(1600,1000);MobileCameraInput.apply(client);sync.invoke(null,0);
  if(client.getCameraYawTarget()!=15184 || client.getCameraPitchTarget()!=4096)throw new AssertionError("Full wrap or overhead pitch failed");
  Field fp=engine.getDeclaredField("bs");fp.setAccessible(true);
  if(Math.abs(fp.getFloat(null)-(float)Math.PI/2)>0.001f)throw new AssertionError("Not overhead radians");
  MobileCameraInput.add(-1600,-1000);MobileCameraInput.apply(client);sync.invoke(null,0);
  if(client.getCameraYawTarget()!=11600 || client.getCameraPitchTarget()!=1024)throw new AssertionError("Reverse rotation/pitch clamp");
  MobileCameraInput.apply(client);sync.invoke(null,0);if(client.getCameraYawTarget()!=11600)throw new AssertionError("Repeated delta");
  System.out.println("ACTUAL_CLIENT_16384_CAMERA_FLOAT_SYNC_WRAP_AND_OVERHEAD_OK");
  MouseWheelEvent[] observed={null};
  engine.getField("kz").set(client,Proxy.newProxyInstance(Callbacks.class.getClassLoader(),new Class[]{Callbacks.class},(p,m,a)->{
   if(m.getName().equals("mouseWheelMoved")){observed[0]=(MouseWheelEvent)a[0];return a[0];}
   if(m.getReturnType()==boolean.class)return false;return null;
  }));Field cw=Class.forName("fz").getDeclaredField("cw");cw.setAccessible(true);cw.set(null,client);
  Class<?> wheelType=Class.forName("ts");Constructor<?> constructor=wheelType.getDeclaredConstructor();constructor.setAccessible(true);Object wheel=constructor.newInstance();
  Method poll=wheelType.getMethod("aq");JFrame[] frame={null};Point[] origin={null};
  SwingUtilities.invokeAndWait(()->{
   JFrame f=new JFrame();Canvas c=new Canvas();c.addMouseWheelListener((MouseWheelListener)wheel);f.add(c);f.setBounds(0,0,800,500);f.setVisible(true);frame[0]=f;origin[0]=c.getLocationOnScreen();
  });
  MobilePointerInput.receiveData(1011,-3,origin[0].x+40,origin[0].y+60,0);SwingUtilities.invokeAndWait(()->{});
  if(!poll.invoke(wheel).equals(-3) || observed[0]==null || observed[0].getX()!=40 || observed[0].getY()!=60)throw new AssertionError("Pinch wheel not delivered at midpoint");
  MobilePointerInput.receiveData(1011,2,origin[0].x+60,origin[0].y+80,0);SwingUtilities.invokeAndWait(()->{});
  if(!poll.invoke(wheel).equals(2))throw new AssertionError("Reverse pinch not delivered");
  SwingUtilities.invokeAndWait(()->frame[0].dispose());System.out.println("ACTUAL_GAME_WHEEL_HANDLER_PINCH_BOTH_DIRECTIONS_AND_MIDPOINT_OK");System.exit(0);
 }
}
