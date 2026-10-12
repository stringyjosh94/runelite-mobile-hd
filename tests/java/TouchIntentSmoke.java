import dev.mobile.runelite.TouchIntent;
public class TouchIntentSmoke {
 public static void main(String[] args) {
  TouchIntent g=new TouchIntent();
  g.begin(100,100,8);g.sample(103,102);g.sample(101,101);
  if(g.dragged())throw new AssertionError("Tap jitter became a drag");
  g.begin(100,100,8);g.sample(130,100);
  if(!g.dragged())throw new AssertionError("Release-only swipe became a tap");
  g.begin(100,100,8);g.sample(120,100);g.sample(100,100);
  if(!g.dragged())throw new AssertionError("Batched out-and-back swipe became a tap");
  g.begin(100,100,8);g.sample(100,109);
  if(!g.dragged())throw new AssertionError("Vertical drag missed");
  g.begin(200,200,24);g.sample(212,201);
  if(g.dragged())throw new AssertionError("High-density jitter");
  g.sample(225,200);if(!g.dragged())throw new AssertionError("High-density drag");
  g.begin(0,0,8);if(g.dragged())throw new AssertionError("Gesture reset");
  System.out.println("TOUCH_TAP_FAST_SWIPE_HISTORY_AND_DENSITY_OK");
 }
}
