package dev.mobile.runelite;
import java.awt.*;import javax.swing.*;
public class CompositionSmoke {
 public static void main(String[] args)throws Exception {
  JFrame[] window={null};JPanel[] panel={null};
  SwingUtilities.invokeAndWait(()->{
   JFrame f=new JFrame();JPanel p=new JPanel();p.setBackground(Color.RED);f.add(p);f.setBounds(0,0,800,500);f.setVisible(true);window[0]=f;panel[0]=p;
  });
  int width=Toolkit.getDefaultToolkit().getScreenSize().width;
  int[] first=HostMain.pixels();if(first[100*width+100]!=Color.RED.getRGB())throw new AssertionError("Initial frame");
  SwingUtilities.invokeAndWait(()->panel[0].setBackground(Color.BLUE));int[] second=HostMain.pixels();
  if(first!=second || second[100*width+100]!=Color.BLUE.getRGB())throw new AssertionError("Reuse or updated pixels");
  SwingUtilities.invokeAndWait(()->window[0].dispose());int[] third=HostMain.pixels();
  if(third[100*width+100]!=Color.BLACK.getRGB())throw new AssertionError("Stale image after window closed");
  System.out.println("COMPOSITION_BUFFER_REUSED_UPDATES_AND_CLEARS_OK");System.exit(0);
 }
}
