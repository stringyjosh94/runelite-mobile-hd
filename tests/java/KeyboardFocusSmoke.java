import dev.mobile.runelite.*;
import java.awt.*;
import javax.swing.*;
import java.lang.reflect.*;
public class KeyboardFocusSmoke {
 public static void main(String[] args)throws Exception {
  JFrame[] f={null};JTextField[] text={null};Canvas[] game={null};Point[] at={null};
  SwingUtilities.invokeAndWait(()->{
   f[0]=new JFrame();text[0]=new JTextField();game[0]=new Canvas();f[0].add(text[0],BorderLayout.NORTH);f[0].add(game[0]);f[0].setBounds(0,0,800,500);f[0].setVisible(true);at[0]=text[0].getLocationOnScreen();
  });
  MobilePointerInput.receiveData(1003,at[0].x+20,at[0].y+10,0,0);MobilePointerInput.receiveData(1006,1024,1,0,0);MobilePointerInput.receiveData(1006,1024,0,0,0);
  SwingUtilities.invokeAndWait(()->{});Thread.sleep(300);
  SwingUtilities.invokeAndWait(()->System.out.println("Text bounds="+text[0].getBounds()+" origin="+at[0]+" focus="+KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()));
  if(!HostMain.textInputFocused())throw new AssertionError("Text focus not detected");
  Method send=MobilePointerInput.class.getMethod("receiveData",int.class,int.class,int.class,int.class,int.class);
  for(char c:"search".toCharArray())send.invoke(null,1000,(int)c,0,0,0);
  Thread.sleep(300);SwingUtilities.invokeAndWait(()->{});
  if(!text[0].getText().equals("search"))throw new AssertionError("Typed text not delivered: "+text[0].getText());
  send.invoke(null,1005,0,8,1,0);send.invoke(null,1005,0,8,0,0);Thread.sleep(200);SwingUtilities.invokeAndWait(()->{});
  if(!text[0].getText().equals("searc"))throw new AssertionError("Backspace not delivered");
  SwingUtilities.invokeAndWait(()->text[0].setEditable(false));if(HostMain.textInputFocused())throw new AssertionError("Read-only field requests keyboard");
  SwingUtilities.invokeAndWait(()->f[0].dispose());System.out.println("SWING_TEXT_FOCUS_TYPED_SEARCH_BACKSPACE_READONLY_OK");System.exit(0);
 }
}
