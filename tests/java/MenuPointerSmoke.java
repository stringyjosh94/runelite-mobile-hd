import dev.mobile.runelite.*;
import net.runelite.api.Client;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Proxy;
import javax.swing.*;
public class MenuPointerSmoke {
 public static void main(String[] args)throws Exception {
  Canvas[] canvas={null};JFrame[] frame={null};Point[] origin={null};int[] left={0},right={0};
  SwingUtilities.invokeAndWait(()->{
   JFrame f=new JFrame();Canvas c=new Canvas();c.addMouseListener(new MouseAdapter(){
    public void mousePressed(MouseEvent e){if(e.getButton()==1)left[0]++;if(e.getButton()==3)right[0]++;}
   });f.add(c);f.setBounds(0,0,800,500);f.setVisible(true);canvas[0]=c;frame[0]=f;origin[0]=c.getLocationOnScreen();
  });
  Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class[]{Client.class},(p,m,a)->{
   switch(m.getName()) {
    case "getCanvasWidth":return canvas[0].getWidth();case "getCanvasHeight":return canvas[0].getHeight();
    case "isMenuOpen":return true;case "getMenuX":return 100;case "getMenuY":return 100;case "getMenuWidth":return 120;case "getMenuHeight":return 100;
   }
   Class<?> t=m.getReturnType();if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;return null;
  });
  new MobileWidgetHitTest(client).onBeforeRender(null);
  input(1003,origin[0].x+50,origin[0].y+50);input(1006,4100,1);input(1006,4100,0);
  input(1003,origin[0].x+120,origin[0].y+145);input(1009,0,0);flush();
  if(right[0]!=1 || left[0]!=1)throw new AssertionError("Hold slide release must select once");
  input(1003,origin[0].x+120,origin[0].y+110);input(1009,0,0);
  input(1003,origin[0].x+400,origin[0].y+400);input(1009,0,0);flush();
  if(left[0]!=1)throw new AssertionError("Heading/outside release must not click");
  SwingUtilities.invokeAndWait(()->frame[0].dispose());System.out.println("MENU_SLIDE_RELEASE_SELECTS_ONCE_AND_IGNORES_HEADING_OUTSIDE_OK");System.exit(0);
 }
 static void input(int t,int a,int b){MobilePointerInput.receiveData(t,a,b,0,0);}static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
}
