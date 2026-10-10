package dev.mobile.runelite;
import java.awt.Rectangle;
import java.lang.reflect.*;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
public class WidgetHitSmoke {
 public static void main(String[]args) {
  Widget item=widget(new Rectangle(600,250,150,200),true,null);
  Widget root=widget(new Rectangle(0,0,765,503),false,new Widget[]{item});
  Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class[]{Client.class},(p,m,a)->{
   switch(m.getName()) {
    case "getWidgetRoots":return new Widget[]{root};case "getCanvasWidth":return 765;case "getCanvasHeight":return 503;
    case "isMenuOpen":return true;case "getMenuX":return 300;case "getMenuY":return 300;case "getMenuWidth":return 100;case "getMenuHeight":return 100;
   }
   return zero(m.getReturnType());
  });
  new MobileWidgetHitTest(client).onBeforeRender(null);
  if(MobileWidgetHitTest.blocksCamera(100,100,765,503))throw new AssertionError("Scene blocked by transparent root");
  if(!MobileWidgetHitTest.blocksCamera(620,300,765,503))throw new AssertionError("Inventory not blocked");
  if(!MobileWidgetHitTest.blocksCamera(1240,600,1530,1006))throw new AssertionError("Stretched inventory mapping");
  if(!MobileWidgetHitTest.blocksCamera(310,310,765,503))throw new AssertionError("Context menu mapping");
  System.out.println("WIDGET_SCENE_INVENTORY_MENU_AND_STRETCH_HIT_OK");
 }
 static Widget widget(Rectangle bounds,boolean interactive,Widget[]children) {
  return (Widget)Proxy.newProxyInstance(Widget.class.getClassLoader(),new Class[]{Widget.class},(p,m,a)->{
   switch(m.getName()) {
    case "getBounds":return bounds;case "getWidth":return bounds.width;case "getHeight":return bounds.height;
    case "getActions":return interactive?new String[]{"Use"}:null;
    case "getType":return interactive?5:0;case "getDynamicChildren":return children;
   }
   return zero(m.getReturnType());
  });
 }
 static Object zero(Class<?>type) {if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;}
}
