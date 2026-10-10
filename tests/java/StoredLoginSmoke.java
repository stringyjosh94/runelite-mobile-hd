import dev.mobile.runelite.StoredLogin;import java.nio.file.*;import java.io.*;
public class StoredLoginSmoke {
 public static void main(String[] args)throws Exception {
  Path home=Files.createTempDirectory("login-clear-test"),rl=Files.createDirectories(home.resolve(".runelite"));
  for(String suffix:new String[]{"",".bak",".new",".tmp"})Files.writeString(rl.resolve("credentials.properties"+suffix),"test fixture");
  Files.writeString(rl.resolve("settings.properties"),"keep");StoredLogin.clear(home.toFile());StoredLogin.clear(home.toFile());
  for(String suffix:new String[]{"",".bak",".new",".tmp"})if(Files.exists(rl.resolve("credentials.properties"+suffix)))throw new AssertionError("Login remains");
  if(!Files.readString(rl.resolve("settings.properties")).equals("keep"))throw new AssertionError("Settings removed");
  Files.delete(rl.resolve("settings.properties"));Files.delete(rl);Files.delete(home);
  System.out.println("CLEAR_LOGIN_AND_RECOVERY_FILES_IDEMPOTENT_SETTINGS_KEPT_OK");
 }
}
