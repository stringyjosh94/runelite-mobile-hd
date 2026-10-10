package dev.mobile.runelite;
import java.awt.*;
import java.lang.reflect.*;
import net.runelite.api.Client;
public class StretchSizingSmoke {
 public static void main(String[] args)throws Exception {
  Class<?> engine=Class.forName("client"),unsafeType=Class.forName("sun.misc.Unsafe");Field uf=unsafeType.getDeclaredField("theUnsafe");uf.setAccessible(true);Object unsafe=uf.get(null);
  Client client=(Client)unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,engine);
  Field resized=engine.getDeclaredField("eu");resized.setAccessible(true);resized.setBoolean(null,true);
  Canvas canvas=new Canvas();Panel parent=new Panel();parent.add(canvas);parent.setSize(1100,505);
  Field field=engine.getSuperclass().getDeclaredField("sk");field.setAccessible(true);field.set(client,canvas);
  client.setStretchedEnabled(true);client.setScalingFactor(30);client.invalidateStretching(false);
  Dimension compact=client.getRealDimensions();
  if(compact.height!=503)throw new AssertionError("Actual engine clamp changed: "+compact);
  if(MobileSettings.allowedAmount(1100,505,30)!=0)throw new AssertionError("Compact layout would crowd panels");
  parent.setSize(1500,685);int amount=MobileSettings.allowedAmount(1500,685,30);
  client.setScalingFactor(amount);client.invalidateStretching(false);Dimension roomy=client.getRealDimensions();
  if(amount<=0||roomy.height<560)throw new AssertionError("Modern layout room not preserved: "+roomy);
  client.setScalingFactor(15);Field scale=engine.getDeclaredField("be");scale.setAccessible(true);
  if(Math.abs(scale.getDouble(null)-1.15)>.0001)throw new AssertionError("Scaling percentage label incorrect");
  System.out.println("ACTUAL_STRETCH_CANVAS_CLAMP_MODERN_PANEL_ROOM_AND_PERCENTAGE_OK");
  System.exit(0);
 }
}
