package dev.mobile.runelite;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** One-finger scrolling for Swing panels; editing controls keep normal dragging. */
final class MobileSidebarInput {
    private JScrollPane pane;
    private int startY,initialValue;
    boolean begin(Component target,int y) {
        pane=null;
        for(Component c=target;c!=null;c=c.getParent()) {
            if(c instanceof Canvas || c instanceof JSlider || c instanceof JScrollBar || c instanceof JTextComponent)
                return false;
            if(c instanceof JScrollPane) {
                JScrollPane candidate=(JScrollPane)c;
                JScrollBar bar=candidate.getVerticalScrollBar();
                if(bar.isVisible() && bar.getMaximum()-bar.getVisibleAmount()>bar.getMinimum()) {
                    pane=candidate;startY=y;initialValue=bar.getValue();return true;
                }
            }
        }
        return false;
    }
    boolean move(int y) {
        if(pane==null)return false;
        pane.getVerticalScrollBar().setValue(initialValue+startY-y);return true;
    }
    void end() {pane=null;}
}
