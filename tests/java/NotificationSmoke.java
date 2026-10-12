import dev.mobile.runelite.MobileNotifications;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.events.NotificationFired;
import java.awt.TrayIcon;
public class NotificationSmoke {
 public static void main(String[] args) {
  MobileNotifications bridge=new MobileNotifications();
  net.runelite.client.eventbus.EventBus bus=new net.runelite.client.eventbus.EventBus();bus.register(bridge);
  Notification n=new Notification().withEnabled(true).withTray(true).withSendWhenFocused(false).withSound(NotificationSound.OFF);
  bus.post(new NotificationFired(n,"<col=ff0000>You are idle.</col>",TrayIcon.MessageType.NONE));
  if(!"10You are idle.".equals(MobileNotifications.poll()))throw new AssertionError("Message/flags");
  bus.post(new NotificationFired(n.withEnabled(false),"disabled",TrayIcon.MessageType.NONE));
  if(MobileNotifications.poll()!=null)throw new AssertionError("Disabled alert forwarded");
  bus.post(new NotificationFired(n.withTray(false),"You have low run energy!",TrayIcon.MessageType.NONE));
  if(!"10You have low run energy!".equals(MobileNotifications.poll()))throw new AssertionError("Desktop tray/focus suppressed Android alert");
  if(MobileNotifications.poll()!=null)throw new AssertionError("Disabled alert forwarded");
  for(int i=0;i<100;i++)bus.post(new NotificationFired(n,"message "+i,TrayIcon.MessageType.NONE));
  int count=0;while(MobileNotifications.poll()!=null)count++;
  if(count!=32)throw new AssertionError("Unbounded queue");
  System.out.println("NOTIFICATION_EVENT_ROUTING_OK");
 }
}
