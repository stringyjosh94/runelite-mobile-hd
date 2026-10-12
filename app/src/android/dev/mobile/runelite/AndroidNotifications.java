package dev.mobile.runelite;

import android.app.*;
import android.content.*;
import android.os.Handler;
import android.os.Looper;

/** Android channels own alert sound and privacy settings. */
public final class AndroidNotifications {
    private final Context context;
    private final NotificationManager manager;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private volatile boolean foreground=true;
    private boolean stopped;
    public AndroidNotifications(Context context) {
        this.context=context.getApplicationContext();
        manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel alerts=new NotificationChannel("runelite_alerts","RuneLite alerts",NotificationManager.IMPORTANCE_DEFAULT);
        alerts.setDescription("Idle Notifier and other RuneLite plugin notifications");
        manager.createNotificationChannel(alerts);
        NotificationChannel silent=new NotificationChannel("runelite_silent","RuneLite silent alerts",NotificationManager.IMPORTANCE_LOW);
        silent.setSound(null,null);manager.createNotificationChannel(silent);
        handler.post(poll);
    }
    public void foreground(boolean value) {foreground=value;}
    public void stop() {stopped=true;handler.removeCallbacks(poll);}
    private final Runnable poll=new Runnable() {public void run() {
        if(stopped)return;
        try {
            if(NativeBridge.running) for(int i=0;i<32;i++) {
                String entry=NativeBridge.notification();if(entry==null)break;
                if(entry.length()>2 && (entry.charAt(0)=='1' || !foreground))
                    send(entry.substring(2),entry.charAt(1)=='1');
            }
        } catch(Exception | LinkageError error) {android.util.Log.e("RuneLite","Notification bridge failed",error);}
        handler.postDelayed(this,1000);
    }};
    public void send(String message,boolean sound) {
        if(!manager.areNotificationsEnabled())return;
        Intent launch=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open=PendingIntent.getActivity(context,0,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(context,sound?"runelite_alerts":"runelite_silent")
            .setSmallIcon(context.getResources().getIdentifier("ic_notification","drawable",context.getPackageName()))
            .setContentTitle("RuneLite Mobile (HD)").setContentText(message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(false)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build();
        try {manager.notify(1001,notification);} catch(SecurityException denied) {
            android.util.Log.w("RuneLite","Android notifications are disabled",denied);
        }
    }
    public void settings() {
        context.startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
