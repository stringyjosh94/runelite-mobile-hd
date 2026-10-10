package dev.mobile.runelite;
import java.awt.*;import java.util.Arrays;import javax.swing.*;
/** Verify the native game opening, Swing popup ordering and copied fallback. */
public class DirectSurfaceCompositionSmoke {
 public static void main(String[] args)throws Exception {
  JFrame[] frame={null};Canvas[] canvas={null};JPanel[] popup={null},higher={null};
  SwingUtilities.invokeAndWait(()-> {
   JFrame f=new JFrame();f.setUndecorated(true);f.setBounds(20,30,500,300);
   JPanel content=new JPanel(null);content.setBackground(Color.RED);f.setContentPane(content);
   Canvas c=new Canvas();c.setBounds(50,40,300,200);content.add(c);
   JPanel p=new JPanel();p.setBackground(Color.GREEN);p.setBounds(100,90,60,40);f.getLayeredPane().add(p,JLayeredPane.POPUP_LAYER);
   JPanel high=new JPanel();high.setBackground(Color.YELLOW);high.setBounds(110,100,30,20);f.getLayeredPane().add(high,JLayeredPane.DRAG_LAYER);higher[0]=high;
   f.setVisible(true);frame[0]=f;canvas[0]=c;popup[0]=p;
  });
  int width=Toolkit.getDefaultToolkit().getScreenSize().width;
  HostMain.directSurface(true);int[] pixels=HostMain.pixels(),layout=HostMain.presentation();
  Point[] at={null},pop={null};SwingUtilities.invokeAndWait(()-> {at[0]=canvas[0].getLocationOnScreen();pop[0]=popup[0].getLocationOnScreen();});
  if(layout[0]!=1 || layout[1]!=at[0].x || layout[2]!=at[0].y || layout[3]!=300 || layout[4]!=200)throw new AssertionError("Canvas geometry mismatch: "+Arrays.toString(layout));
  if(pixels[(at[0].y+5)*width+at[0].x+5]!=0)throw new AssertionError("Game opening is not transparent");
  if(pixels[(pop[0].y+5)*width+pop[0].x+5]!=Color.GREEN.getRGB())throw new AssertionError("Swing popup covered/cleared");
  if(pixels[(pop[0].y+15)*width+pop[0].x+15]!=Color.YELLOW.getRGB())throw new AssertionError("Popup z-order lost");
  if(pixels[35*width+25]!=Color.RED.getRGB())throw new AssertionError("Sidebar/window background lost");
  layout[0]=0;if(HostMain.presentation()[0]!=1)throw new AssertionError("Borrowed mutable geometry");
  SwingUtilities.invokeAndWait(()->{popup[0].setVisible(false);higher[0].setVisible(false);});
  HostMain.directSurface(false);int[] game=HostMain.acquireGpuFrame(300,200);Arrays.fill(game,Color.BLUE.getRGB());HostMain.gpuFrame(game,300,200);
  pixels=HostMain.pixels();if(HostMain.presentation()[0]!=0 || pixels[(at[0].y+5)*width+at[0].x+5]!=Color.BLUE.getRGB())throw new AssertionError("Copied fallback failed");
  HostMain.directSurface(true);HostMain.gpuStopped();HostMain.pixels();if(HostMain.presentation()[0]!=0)throw new AssertionError("Stopped renderer leaves hole");
  SwingUtilities.invokeAndWait(()->frame[0].dispose());
  System.out.println("DIRECT_SURFACE_GEOMETRY_TRANSPARENCY_POPUPS_FALLBACK_AND_STOP_OK");System.exit(0);
 }
}
