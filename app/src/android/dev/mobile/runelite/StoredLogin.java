package dev.mobile.runelite;
import java.io.File;
import java.io.IOException;

/** Delete only the imported login and its atomic-write recovery files. */
public final class StoredLogin {
    private StoredLogin() {}
    public static void clear(File home) throws IOException {
        File folder=new File(home,".runelite");
        for(String suffix:new String[]{"", ".bak", ".new", ".tmp"}) {
            File file=new File(folder,"credentials.properties"+suffix);
            if(file.exists() && !file.delete()) throw new IOException("Unable to clear stored login");
        }
    }
}
