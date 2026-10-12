package dev.mobile.runelite;

import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.config.NotificationSound;
import java.util.concurrent.ArrayBlockingQueue;

/** Bounded, in-process delivery to Android; no credentials or network service. */
public final class MobileNotifications {
    private static final ArrayBlockingQueue<String> pending=new ArrayBlockingQueue<>(32);
    private static final MobileNotifications INSTANCE=new MobileNotifications();
    public static void start() {
        Thread thread=new Thread(()-> {
            try {
                for(int i=0;i<720;i++) {
                    if(net.runelite.client.RuneLite.getInjector()!=null) {
                        net.runelite.client.RuneLite.getInjector().getInstance(net.runelite.client.eventbus.EventBus.class).register(INSTANCE);
                        System.out.println("MOBILE_NOTIFICATIONS_READY");return;
                    }
                    Thread.sleep(500);
                }
                System.err.println("Mobile notification registration timed out");
            } catch(Exception error) {error.printStackTrace();}
        },"mobile-notifications");thread.setDaemon(true);thread.start();
    }
    @Subscribe public void onNotificationFired(NotificationFired event) {
        if(!event.getNotification().isEnabled()) return;
        String message=net.runelite.client.util.Text.removeTags(event.getMessage());
        if(message==null || message.isEmpty()) return;
        // Android alerts remain enabled while playing; desktop focus/tray controls
        // describe desktop presentation, not mobile delivery.
        String entry="1"
            +(event.getNotification().getSound()!=NotificationSound.OFF?"1":"0")+message;
        if(!pending.offer(entry)) {pending.poll();pending.offer(entry);}
    }
    public static String poll() {return pending.poll();}
}
