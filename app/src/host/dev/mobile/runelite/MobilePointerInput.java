package dev.mobile.runelite;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Deliver real AWT pointer events, with button ordinals and modern held-button masks. */
public final class MobilePointerInput {
    private static int x,y,held,pressX,pressY;
    private static final MobileSidebarInput sidebar=new MobileSidebarInput();
    private static boolean sidebarScrolling;
    private static Component capture,hover,inputTarget;
    private static Object cursorPeer;
    private static java.lang.reflect.Method updateCursor;
    static {
        try {
            Class<?> type=Class.forName("com.github.caciocavallosilano.cacio.peer.CacioMouseInfoPeer");
            cursorPeer=type.getMethod("getInstance").invoke(null);
            updateCursor=type.getMethod("setMouseScreenCoordinates",int.class,int.class);
        } catch(ReflectiveOperationException ignored) { }
    }
    private MobilePointerInput() {}
    static Component targetAt(int x,int y) {
        Window[] windows=Window.getWindows();
        for(int i=windows.length-1;i>=0;i--) {
            Window w=windows[i];
            if(!w.isShowing() || !(w instanceof RootPaneContainer)) continue;
            Point p=w.getLocationOnScreen();
            if(w.contains(x-p.x,y-p.y)) return SwingUtilities.getDeepestComponentAt(w,x-p.x,y-p.y);
        }
        return null;
    }
    public static void receiveData(int type,int a,int b,int c,int d) {
        if(type==1012 || type==1013 || type==1014) {
            SwingUtilities.invokeLater(()-> {
                if(type==1012) {
                    dispatch(1003,a,b);sidebarScrolling=sidebar.begin(targetAt(a,b),b);
                    if(!sidebarScrolling)dispatch(1006,1024,1);
                } else if(type==1013) {
                    if(sidebarScrolling)sidebar.move(b);else dispatch(1003,a,b);
                } else {
                    if(!sidebarScrolling && held!=0)dispatch(1006,1024,0);
                    sidebar.end();sidebarScrolling=false;
                }
            });return;
        }
        if(type==1011) {
            SwingUtilities.invokeLater(()-> {dispatch(1003,b,c);dispatch(1007,a,0);});return;
        }
        if(type==1008) { MobileCameraInput.add(a,b); return; }
        if(type==1000 || type==1005) {SwingUtilities.invokeLater(()->keyboard(type,a,b,c));return;}
        SwingUtilities.invokeLater(()-> dispatch(type,a,b));
    }
    static boolean textInputFocused() {
        Component owner=inputTarget!=null?inputTarget:KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return owner instanceof javax.swing.text.JTextComponent && owner.isShowing() && owner.isEnabled()
            && ((javax.swing.text.JTextComponent)owner).isEditable();
    }
    private static void keyboard(int type,int character,int code,int pressed) {
        Component target=inputTarget!=null?inputTarget:KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if(target==null || !target.isShowing() || !target.isEnabled())return;
        int id=type==1000?KeyEvent.KEY_TYPED:pressed==1?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED;
        char ch=type==1000?(char)character:code==10?'\n':code==8?'\b':KeyEvent.CHAR_UNDEFINED;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(target,
            new KeyEvent(target,id,System.currentTimeMillis(),0,type==1000?KeyEvent.VK_UNDEFINED:code,ch));
    }
    private static void send(Component target,int id,int button,int modifiers,boolean popup,int clicks) {
        if(target==null || !target.isShowing()) return;
        Point p=target.getLocationOnScreen();
        target.dispatchEvent(new MouseEvent(target,id,System.currentTimeMillis(),modifiers,
            x-p.x,y-p.y,x,y,clicks,popup,button));
    }
    private static int button(int mask) {
        return (mask&4096)!=0?MouseEvent.BUTTON3:(mask&2048)!=0?MouseEvent.BUTTON2:MouseEvent.BUTTON1;
    }
    private static void dispatch(int type,int a,int b) {
        if(type==1003) {
            x=a;y=b;
            if(updateCursor!=null) try {updateCursor.invoke(cursorPeer,x,y);} catch(ReflectiveOperationException ignored) { }
            Component target=capture!=null?capture:targetAt(x,y);
            if(capture==null && target!=hover) {
                send(hover,MouseEvent.MOUSE_EXITED,0,held,false,0);
                hover=target;send(hover,MouseEvent.MOUSE_ENTERED,0,held,false,0);
            }
            send(target,held==0?MouseEvent.MOUSE_MOVED:MouseEvent.MOUSE_DRAGGED,0,held,false,0);
        } else if(type==1006) {
            int mask=a& (1024|2048|4096),which=button(mask);
            if(b==1) {
                if(capture==null) capture=targetAt(x,y);
                held|=mask;pressX=x;pressY=y;
                if(capture!=null) {inputTarget=capture;capture.requestFocus();}
                send(capture,MouseEvent.MOUSE_PRESSED,which,held,false,1);
            } else {
                Component target=capture;
                held&=~mask;
                send(target,MouseEvent.MOUSE_RELEASED,which,held,which==MouseEvent.BUTTON3,1);
                if(Math.abs(x-pressX)<=4 && Math.abs(y-pressY)<=4)
                    send(target,MouseEvent.MOUSE_CLICKED,which,held,false,1);
                if(held==0) capture=null;
            }
        } else if(type==1009) {
            Component target=targetAt(x,y);
            if(target instanceof Canvas) {
                Point p=target.getLocationOnScreen();
                if(MobileWidgetHitTest.menuEntryAt(x-p.x,y-p.y,target.getWidth(),target.getHeight())) {
                    dispatch(1006,1024,1);dispatch(1006,1024,0);
                }
            }
        } else if(type==1007) {
            Component target=targetAt(x,y);
            if(target!=null && target.isShowing()) {
                Point p=target.getLocationOnScreen();
                target.dispatchEvent(new MouseWheelEvent(target,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,
                    x-p.x,y-p.y,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,a));
            }
        }
    }
}
