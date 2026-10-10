package dev.mobile.runelite;
import javax.swing.*;
import java.awt.*;
public class SidebarTargetSmoke {
 public static void main(String[] args)throws Exception {
  JFrame[] frame={null};Point[] game={null},sidebar={null};
  SwingUtilities.invokeAndWait(()-> {
   JFrame f=new JFrame();Canvas canvas=new Canvas();JButton button=new JButton("Sidebar");
   f.add(canvas,BorderLayout.CENTER);f.add(button,BorderLayout.EAST);f.setBounds(0,0,1000,540);f.setVisible(true);
   game[0]=canvas.getLocationOnScreen();game[0].translate(30,30);sidebar[0]=button.getLocationOnScreen();sidebar[0].translate(10,30);frame[0]=f;
  });
  try {
   if(HostMain.swingUiAt(game[0].x,game[0].y))throw new AssertionError("Gameplay would trigger sidebar magnification");
   if(!HostMain.swingUiAt(sidebar[0].x,sidebar[0].y))throw new AssertionError("Sidebar not recognized");
   System.out.println("REAL_SWING_SIDEBAR_TARGET_EXCLUDES_GAME_CANVAS_OK");
  }finally {SwingUtilities.invokeAndWait(()->frame[0].dispose());}
 }
}
