package dev.mobile.runelite;

/** Keep native/GPU/ART memory outside HotSpot's heap budget. No GPU brand assumptions. */
public final class DeviceRuntimePolicy {
    private DeviceRuntimePolicy() {}
    public static int heapMiB(long totalBytes, boolean lowRam, boolean process64Bit) {
        if (!process64Bit || lowRam || (totalBytes > 0 && totalBytes <= 2L*1024*1024*1024)) return 384;
        if (totalBytes > 0 && totalBytes <= 4L*1024*1024*1024) return 512;
        return 768;
    }
}
