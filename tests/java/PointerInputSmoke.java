import dev.mobile.runelite.MobilePointerInput;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
public class PointerInputSmoke {
 public static void main(String[] args)throws Exception {
  java.util.List<MouseEvent> events=new ArrayList<>();int[] wheel={0},clicks={0};
  Point[] scene={null},side={null};JFrame[] frame={null};
  SwingUtilities.invokeAndWait(()->{
   JFrame f=new JFrame();f.setLayout(new BorderLayout());Canvas c=new Canvas();
   MouseAdapter listener=new MouseAdapter(){public void mousePressed(MouseEvent e){events.add(e);}public void mouseReleased(MouseEvent e){events.add(e);}public void mouseClicked(MouseEvent e){events.add(e);}public void mouseDragged(MouseEvent e){events.add(e);}public void mouseWheelMoved(MouseWheelEvent e){wheel[0]+=e.getWheelRotation();}};
   c.addMouseListener(listener);c.addMouseMotionListener(listener);c.addMouseWheelListener(listener);
   JButton button=new JButton("Sidebar");button.addActionListener(e->clicks[0]++);
   f.add(c,BorderLayout.CENTER);f.add(button,BorderLayout.EAST);f.setBounds(0,0,1000,500);f.setVisible(true);
   scene[0]=c.getLocationOnScreen();scene[0].translate(30,30);side[0]=button.getLocationOnScreen();side[0].translate(15,20);frame[0]=f;
  });
  move(scene[0].x,scene[0].y);input(1006,1024,1);input(1006,1024,0);flush();
  if(events.get(0).getButton()!=1 || events.get(2).getID()!=MouseEvent.MOUSE_CLICKED)throw new AssertionError("Tap sequence");events.clear();
  input(1006,2048,1);move(scene[0].x+40,scene[0].y+20);input(1006,2048,0);flush();
  if(events.get(0).getButton()!=2 || events.get(1).getID()!=MouseEvent.MOUSE_DRAGGED || (events.get(1).getModifiersEx()&2048)==0 || events.get(2).getButton()!=2)throw new AssertionError("Camera sequence");events.clear();
  input(1006,4100,1);input(1006,4100,0);input(1007,-2,0);flush();
  if(events.get(0).getButton()!=3 || !events.get(1).isPopupTrigger() || wheel[0]!=-2)throw new AssertionError("Context menu/zoom");
  move(side[0].x,side[0].y);input(1006,1024,1);input(1006,1024,0);flush();
  if(clicks[0]!=1)throw new AssertionError("Swing button click");
  SwingUtilities.invokeAndWait(()->frame[0].dispose());System.out.println("POINTER_TAP_CAMERA_CONTEXT_ZOOM_AND_SWING_OK");System.exit(0);
 }
 static void move(int x,int y){input(1003,x,y);}static void input(int t,int a,int b){MobilePointerInput.receiveData(t,a,b,0,0);}static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
}
