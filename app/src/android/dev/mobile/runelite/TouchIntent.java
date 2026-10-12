package dev.mobile.runelite;

/** Screen-space drag intent: crossing the slop is sticky until the next touch. */
public final class TouchIntent {
    private float x,y,slopSquared;
    private boolean dragged;
    public void begin(float x,float y,float slop) {
        this.x=x;this.y=y;slopSquared=slop*slop;dragged=false;
    }
    public void sample(float x,float y) {
        float dx=x-this.x,dy=y-this.y;
        if(dx*dx+dy*dy>slopSquared)dragged=true;
    }
    public boolean dragged() {return dragged;}
}
