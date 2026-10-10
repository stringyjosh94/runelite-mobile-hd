package dev.mobile.runelite;

import com.google.gson.*;
import net.runelite.client.RuneLite;
import net.runelite.client.plugins.*;
import net.runelite.client.config.*;
import javax.swing.SwingUtilities;
import java.util.*;

/** Game stretching only; the native plugin browser has been removed. */
public final class MobileSettings {
 public static String request(String input) {
  final String[] result={"{\"error\":\"Client is not ready\"}"};
  Runnable action=()-> {try {result[0]=handle(new JsonParser().parse(input).getAsJsonObject()).toString();}
   catch(Throwable e) {e.printStackTrace();JsonObject error=new JsonObject();error.addProperty("error",e.getMessage()==null?e.toString():e.getMessage());result[0]=error.toString();}};
  try {if(SwingUtilities.isEventDispatchThread())action.run();else SwingUtilities.invokeAndWait(action);}
  catch(Exception e) {return "{\"error\":\"Settings request failed\"}";}
  return result[0];
 }
 private static JsonObject handle(JsonObject request)throws Exception {
  if(RuneLite.getInjector()==null)throw new IllegalStateException("Wait for RuneLite to finish loading");
  PluginManager manager=RuneLite.getInjector().getInstance(PluginManager.class);
  ConfigManager config=RuneLite.getInjector().getInstance(ConfigManager.class);
  String op=request.get("op").getAsString();JsonObject out=new JsonObject();
  if(op.equals("gameScale")) {
   int amount=request.get("amount").getAsInt();
   if(!Arrays.asList(0,15,30,45).contains(amount))throw new IllegalArgumentException("Unsupported UI scale");
   Plugin stretch=manager.getPlugins().stream().filter(p->p.getClass().getName().equals("net.runelite.client.plugins.stretchedmode.StretchedModePlugin")).findFirst().orElseThrow(()->new IllegalStateException("Stretched Mode is not loaded yet"));
   net.runelite.api.Client client=RuneLite.getInjector().getInstance(net.runelite.api.Client.class);
   java.awt.Container parent=client.getCanvas().getParent();
   int applied=parent==null?0:allowedAmount(parent.getWidth(),parent.getHeight(),amount);
   if(applied==0) {manager.setPluginEnabled(stretch,false);manager.stopPlugin(stretch);}
   else {
    config.setConfiguration("stretchedmode","scalingFactor",Integer.toString(applied));
    config.setConfiguration("stretchedmode","keepAspectRatio","true");
    config.setConfiguration("stretchedmode","integerScaling","false");
    config.setConfiguration("stretchedmode","increasedPerformance","false");
    manager.setPluginEnabled(stretch,true);manager.startPlugin(stretch);
    if(!manager.isPluginActive(stretch))throw new IllegalStateException("Stretched Mode did not start");
   }
   double maximum=parent==null?1:Math.min(parent.getWidth()/765.0,parent.getHeight()/503.0);
   out.addProperty("requested",amount==0?1:1+amount/100.0);
   out.addProperty("effective",amount==0?1:Math.max(1,Math.min(1+applied/100.0,maximum)));
   out.addProperty("resizable",client.isResized());
   out.addProperty("ok",true);return out;
  }
  throw new IllegalArgumentException("Unknown interface command");
 }

 static int allowedAmount(int width,int height,int requested) {
  // Modern side panels need more vertical space than the engine's 503px minimum.
  int maximum=(int)Math.floor((Math.min(width/765.0,height/560.0)-1)*100);
  return Math.max(0,Math.min(requested,maximum));
 }
}
