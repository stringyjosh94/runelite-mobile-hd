package dev.mobile.runelite;
import net.runelite.api.Client;
import java.lang.reflect.Proxy;
public final class CameraMenuSmoke {
 public static void main(String[] args) {
  int[] yaw={16320},pitch={1600};boolean[] menu={true};
  Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class[]{Client.class},(p,m,a)->{
   switch(m.getName()) {
    case "getCameraYawTarget":return yaw[0];case "setCameraYawTarget":yaw[0]=(int)a[0];return null;
    case "getCameraPitchTarget":return pitch[0];case "setCameraPitchTarget":pitch[0]=(int)a[0];return null;
    case "getCanvasWidth":return 765;case "getCanvasHeight":return 503;case "isMenuOpen":return menu[0];
    case "getMenuX":return 300;case "getMenuY":return 100;case "getMenuWidth":return 150;case "getMenuHeight":return 100;
   }
   return WidgetHitSmoke.zero(m.getReturnType());
  });
  MobileCameraInput.add(20,10); MobileCameraInput.add(-5,-3);MobileCameraInput.apply(client);
  if(yaw[0]!=16200 || pitch[0]!=1656)throw new AssertionError("Relative deltas");
  MobileCameraInput.apply(client);if(yaw[0]!=16200)throw new AssertionError("Stale deltas repeated");
  MobileCameraInput.add(-40,999);MobileCameraInput.apply(client);
  if(yaw[0]!=136 || pitch[0]!=4096)throw new AssertionError("Wrap/clamp");
  new MobileWidgetHitTest(client).onBeforeRender(null);
  if(!MobileWidgetHitTest.menuEntryAt(640,260,1530,1006))throw new AssertionError("Stretched menu entry");
  if(MobileWidgetHitTest.menuEntryAt(640,220,1530,1006))throw new AssertionError("Menu heading selectable");
  if(MobileWidgetHitTest.menuEntryAt(100,100,765,503))throw new AssertionError("Outside menu selectable");
  menu[0]=false;new MobileWidgetHitTest(client).onBeforeRender(null);
  if(MobileWidgetHitTest.menuEntryAt(320,130,765,503))throw new AssertionError("Closed menu selectable");
  System.out.println("RELATIVE_CAMERA_WRAP_CLAMP_AND_MENU_RELEASE_HIT_OK");
 }
}
