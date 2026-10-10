import dev.mobile.runelite.MobileHdCompatibility;
import java.util.*;
public class HdCompatibilitySmoke {
 public static void main(String[] args) {
  Map<String,String> values=new HashMap<>();
  values.put("mobilehd.screenBrightness","120");values.put("hd.screenBrightness","70");
  MobileHdCompatibility[] bridge=new MobileHdCompatibility[1];int[] writes={0};
  bridge[0]=new MobileHdCompatibility((g,k)->values.get(g+"."+k),(g,k,v)-> {
   if(++writes[0]>100)throw new AssertionError("Feedback loop");
   if(v==null)values.remove(g+"."+k);else values.put(g+"."+k,v);
   bridge[0].changed(g,k,v);
  });
  bridge[0].initialize();
  check(values,"hd.screenBrightness","120");check(values,"hd.defaultSkyColor","DEFAULT");
  values.put("hd.defaultSkyColor","RUNELITE");bridge[0].changed("hd","defaultSkyColor","RUNELITE");
  check(values,"mobilehd.defaultSkyColor","RUNELITE");
  bridge[0].changed("hd","experimentalStorageBuffers","ON");
  if(values.containsKey("mobilehd.experimentalStorageBuffers"))throw new AssertionError("Unsafe backend forwarded");
  values.remove("mobilehd.screenBrightness");bridge[0].changed("mobilehd","screenBrightness",null);
  if(values.containsKey("hd.screenBrightness"))throw new AssertionError("Deletion not mirrored");
  bridge[0].changed("other","shadowMode","OFF");check(values,"hd.shadowMode","FAST");
  System.out.println("HD_INTEGRATION_DEFAULTS_BIDIRECTIONAL_UPDATES_DELETION_AND_BACKEND_ISOLATION_OK");
 }
 static void check(Map<String,String> values,String key,String expected) {if(!expected.equals(values.get(key)))throw new AssertionError(key+"="+values.get(key));}
}
