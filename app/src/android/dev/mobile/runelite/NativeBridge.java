package dev.mobile.runelite;

import android.content.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

public final class NativeBridge {
    public static volatile boolean running;
    private static Context context;
    static { System.loadLibrary("pojavexec_awt"); }
    public static void init(Context value) { context = value; }
    public static native int launch(String nativeDir, String runtimeDir, String home, String log,
                                    String[] options);
    public static native int[] pixels();
    public static native String notification();
    public static native int[] presentation();
    public static native void surface(android.view.Surface surface);
    public static native String settings(String request);
    public static native boolean textInputFocused();
    public static native boolean gameCanvasAt(int x, int y);
    public static native boolean swingUiAt(int x,int y);
    public static native void input(int type, int a, int b, int c, int d);
    public static native void clipboard(String data, String mime);
    public static native void environment(String key, String value);

    public static void openLink(String value) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try { context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (Exception ignored) { }
        });
    }
    public static void queryClipboard() {
        new Handler(Looper.getMainLooper()).post(() -> {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = manager.getPrimaryClip();
            clipboard(clip != null && clip.getItemCount() > 0 ?
                clip.getItemAt(0).coerceToText(context).toString() : "", "text/plain");
        });
    }
    public static void putClipboard(String value, String mime) {
        new Handler(Looper.getMainLooper()).post(() -> {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("RuneLite", value));
        });
    }
}
