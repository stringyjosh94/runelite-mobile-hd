package dev.mobile.runelite;

import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import java.awt.Rectangle;
import java.util.*;

/** Snapshot widget geometry on the client thread; never access widgets from Android or AWT threads. */
public final class MobileWidgetHitTest {
    private final Client client;
    private long refreshed;
    private static volatile Rectangle[] widgets=new Rectangle[0];
    private static volatile int logicalWidth,logicalHeight;
    private static volatile Rectangle menu;
    public MobileWidgetHitTest(Client client) {this.client=client;}
    @Subscribe public void onBeforeRender(BeforeRender ignored) {
        MobileCameraInput.apply(client);
        menu=client.isMenuOpen()?new Rectangle(client.getMenuX(),client.getMenuY(),client.getMenuWidth(),client.getMenuHeight()):null;
        long now=System.nanoTime();if(now-refreshed<100_000_000L)return;refreshed=now;
        ArrayList<Rectangle> next=new ArrayList<>();
        Set<Widget> visited=Collections.newSetFromMap(new IdentityHashMap<>());
        logicalWidth=client.getCanvasWidth();logicalHeight=client.getCanvasHeight();
        Widget[] roots;
        try { roots=client.getWidgetRoots(); }
        catch (NullPointerException loading) { widgets=new Rectangle[0]; return; } // Client interface table is absent during early startup.
        collect(roots,next,visited);
        if(client.isMenuOpen()) next.add(new Rectangle(client.getMenuX(),client.getMenuY(),client.getMenuWidth(),client.getMenuHeight()));
        widgets=next.toArray(new Rectangle[0]);
    }
    private static boolean hasActions(String[] actions) {
        if(actions!=null) for(String action:actions) if(action!=null && !action.isEmpty()) return true;
        return false;
    }
    private void collect(Widget[] array,List<Rectangle> out,Set<Widget> visited) {
        if(array==null)return;
        for(Widget w:array) {
            if(w==null || !visited.add(w) || w.isSelfHidden()) continue;
            Rectangle bounds=w.getBounds();
            if(bounds!=null && bounds.width>0 && bounds.height>0 && !(bounds.width>=logicalWidth-10 && bounds.height>=logicalHeight-10)) {
                boolean scroll=w.getScrollHeight()>w.getHeight();
                boolean interactive=w.getType()==0 ? scroll||w.getNoClickThrough() :
                    scroll||w.getNoClickThrough()||w.getClickMask()!=0||w.getDragParent()!=null||hasActions(w.getActions())||w.hasListener();
                if(interactive) out.add(new Rectangle(bounds));
            }
            collect(w.getDynamicChildren(),out,visited);collect(w.getStaticChildren(),out,visited);collect(w.getNestedChildren(),out,visited);
        }
    }
    static boolean menuEntryAt(int x,int y,int width,int height) {
        Rectangle rect=menu;
        if(rect==null || width<=0 || height<=0)return false;
        int lx=x*logicalWidth/width,ly=y*logicalHeight/height;
        return rect.contains(lx,ly) && ly>=rect.y+19;
    }
    static boolean blocksCamera(int x,int y,int canvasWidth,int canvasHeight) {
        if(canvasWidth<=0 || canvasHeight<=0)return true;
        int lx=logicalWidth>0?x*logicalWidth/canvasWidth:x,ly=logicalHeight>0?y*logicalHeight/canvasHeight:y;
        for(Rectangle rect:widgets)if(rect.contains(lx,ly))return true;
        return false;
    }
}
