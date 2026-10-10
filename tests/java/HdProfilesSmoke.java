import dev.mobile.runelite.MobileHdProfiles;import java.util.*;
public class HdProfilesSmoke {
 public static void main(String[] args)throws Exception {
  Map<String,String> low=MobileHdProfiles.settings("low"),medium=MobileHdProfiles.settings("medium"),high=MobileHdProfiles.settings("high");
  if(!"100".equals(low.get("sceneResolutionScale")) || !"NEAREST".equals(low.get("sceneScalingMode")) || !"24".equals(low.get("drawDistance")) || !"RES_512".equals(low.get("shadowResolution")))throw new AssertionError("Low policy");
  if(!"true".equals(low.get("mobileEfficient")) || !"true".equals(medium.get("mobileEfficient")) || !"false".equals(high.get("mobileEfficient")))throw new AssertionError("Shader tiers");
  if(!"100".equals(medium.get("sceneResolutionScale")) || !"32".equals(medium.get("drawDistance")) || !"48".equals(high.get("drawDistance")))throw new AssertionError("Detail tiers");
  if(!"2".equals(low.get("mobileLightLimit")) || !"4".equals(medium.get("mobileLightLimit")) || !"8".equals(high.get("mobileLightLimit")))throw new AssertionError("Mobile light budgets");
  if(!"DISTANCE_20".equals(high.get("shadowDistance")))throw new AssertionError("Mobile high shadow coverage");
  for(Map<String,String> tier:Arrays.asList(low,medium,high))for(String key:Arrays.asList("windDisplacement","characterDisplacement","expandShadowDraw"))if(!"false".equals(tier.get(key)))throw new AssertionError("Desktop cost inherited: "+key);
  for(String name:Arrays.asList("efficient","ultra","light","standard","software","unknown"))if(!"low".equals(MobileHdProfiles.normalize(name)))throw new AssertionError("Legacy migration: "+name);
  for(String name:Arrays.asList("keep","balanced","medium"))if(!"medium".equals(MobileHdProfiles.normalize(name)))throw new AssertionError("Medium migration");
  if(!"high".equals(MobileHdProfiles.normalize("high")))throw new AssertionError("High migration");
  if(!low.keySet().equals(medium.keySet()) || !low.keySet().equals(high.keySet()))throw new AssertionError("Preset switch leaves tier settings behind");
  for(Map<String,String> v:Arrays.asList(low,medium,high)) {
   for(String[] pair:new String[][]{{"ShadowMode","shadowMode"},{"ShadowResolution","shadowResolution"},{"ShadowDistance","shadowDistance"},{"ShadowFiltering","shadowFiltering"},{"DynamicLights","dynamicLights"},{"TextureResolution","textureResolution"},{"SceneScalingMode","sceneScalingMode"}})check(pair[0],v.get(pair[1]));
   if(!"FAST".equals(v.get("shadowMode")) || !"DISABLED".equals(v.get("antiAliasingMode")) || !"false".equals(v.get("tiledLighting")))throw new AssertionError("Shadows or GLES limits lost");
  }
  for(String name:Arrays.asList("standard","software","keep","efficient","unknown"))try{MobileHdProfiles.settings(name);throw new AssertionError("Removed profile accepted");}catch(IllegalArgumentException expected){}
  // Exercise the actual renderer limit with synthetic configuration values.
  java.lang.reflect.Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);
  sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
  dev.mobile.rlhd.HdPlugin plugin=(dev.mobile.rlhd.HdPlugin)unsafe.allocateInstance(dev.mobile.rlhd.HdPlugin.class);
  java.lang.reflect.Field config=dev.mobile.rlhd.HdPlugin.class.getDeclaredField("config");config.setAccessible(true);
  for(int limit:new int[]{-5,0,2,4,8,25,99}) {
   Object value=java.lang.reflect.Proxy.newProxyInstance(dev.mobile.rlhd.HdPluginConfig.class.getClassLoader(),new Class[]{dev.mobile.rlhd.HdPluginConfig.class},(proxy,method,a)->method.getName().equals("mobileLightLimit")?limit:null);
   config.set(plugin,value);
   for(int requested:new int[]{0,1,25,50})if(plugin.mobileLightLimit(requested)!=Math.min(requested,Math.max(0,Math.min(25,limit))))throw new AssertionError("Renderer light cap");
  }
  System.out.println("THREE_HD_TIERS_LEGACY_MIGRATION_AND_COMPLETE_PRESET_SWITCHING_OK");
 }
 @SuppressWarnings({"unchecked","rawtypes"}) static void check(String type,String value)throws Exception {Enum.valueOf((Class)Class.forName("dev.mobile.rlhd.config."+type),value);}
}
