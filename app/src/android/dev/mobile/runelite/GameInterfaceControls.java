package dev.mobile.runelite;
import android.app.*;
import android.os.*;
import org.json.*;
import java.util.concurrent.*;
/** Game stretching controls only; no native plugin/Hub browser. */
final class GameInterfaceControls {
 private final Activity activity;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler ui=new Handler(Looper.getMainLooper());
 GameInterfaceControls(Activity activity) {this.activity=activity;}
 void show() {
  String[] labels={"Normal","Large (115%)","Larger (130%)","Largest (145%)"};int[] amounts={0,15,30,45};
  new AlertDialog.Builder(activity).setTitle("Interface size").setItems(labels,(d,which)->apply(amounts[which])).setNegativeButton("Cancel",null).show();
 }
 void showLayout() {
  new AlertDialog.Builder(activity).setTitle("Screen layout").setItems(new String[]{"Roomy — more room for game panels","Compact — lower graphics load"},(d,which)-> {
   activity.getSharedPreferences("startup",Activity.MODE_PRIVATE).edit().putInt("layoutHeight",which==0?720:540).apply();
   new AlertDialog.Builder(activity).setTitle("Layout saved").setMessage("Close and reopen to apply. Roomy can increase graphics load.").setPositiveButton("OK",null).show();
  }).setNegativeButton("Cancel",null).show();
 }
 private void apply(int amount) {
  worker.execute(()-> {
   String message;
   try {
    JSONObject c=new JSONObject();c.put("op","gameScale");c.put("amount",amount);
    JSONObject result=new JSONObject(NativeBridge.settings(c.toString()));
    if(result.has("error"))throw new Exception(result.getString("error"));
    int requested=(int)Math.round(result.optDouble("requested",1)*100),effective=(int)Math.round(result.optDouble("effective",1)*100);
    message="Interface size set to about "+effective+"%.";
    if(!result.optBoolean("resizable"))message+=" Choose a resizable game layout to enlarge the interface.";
    else if(effective<requested)message+=" This size is limited to keep game panels from overlapping. For more room, use Back → Help & advanced → Screen layout → Roomy, then reopen.";
   } catch(Exception e) {message=e.getMessage();}
   final String shown=message;ui.post(()-> {if(!activity.isFinishing())new AlertDialog.Builder(activity).setTitle("Game interface size").setMessage(shown).setPositiveButton("OK",null).show();});
  });
 }
}
