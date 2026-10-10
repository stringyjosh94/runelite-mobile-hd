package dev.mobile.runelite;
import javax.swing.*;
import java.awt.*;
public class SidebarScrollSmoke {
 public static void main(String[] args)throws Exception {
  SwingUtilities.invokeAndWait(()-> {
   JPanel content=new JPanel();content.setPreferredSize(new Dimension(240,1200));
   JButton setting=new JButton("Setting");JSlider slider=new JSlider();JTextField text=new JTextField("Search");
   content.add(setting);content.add(slider);content.add(text);
   JScrollPane pane=new JScrollPane(content);pane.setSize(260,300);pane.doLayout();
   pane.getVerticalScrollBar().setValues(100,300,0,1200);
   int[] clicks={0};setting.addActionListener(e->clicks[0]++);
   MobileSidebarInput touch=new MobileSidebarInput();
   if(!touch.begin(setting,200))throw new AssertionError("Setting row not scrollable");
   touch.move(120);if(pane.getVerticalScrollBar().getValue()!=180)throw new AssertionError("Drag direction/distance");
   touch.move(250);if(pane.getVerticalScrollBar().getValue()!=50)throw new AssertionError("Reverse drag");
   touch.move(-5000);if(pane.getVerticalScrollBar().getValue()!=pane.getVerticalScrollBar().getMaximum()-pane.getVerticalScrollBar().getVisibleAmount())throw new AssertionError("End clamp");
   touch.end();if(touch.move(0))throw new AssertionError("Ended gesture retained");
   if(touch.begin(slider,0)||touch.begin(text,0)||touch.begin(pane.getVerticalScrollBar(),0))throw new AssertionError("Editing control intercepted");
   if(touch.begin(new Canvas(),0))throw new AssertionError("Game canvas intercepted");
   if(clicks[0]!=0)throw new AssertionError("Scrolling clicked a setting");
  });
  System.out.println("SIDEBAR_DRAG_DIRECTION_CLAMP_NO_CLICK_AND_EDITING_CONTROLS_OK");
 }
}
