package dev.mobile.runelite;

import net.runelite.api.Client;

/** Relative touch deltas in legacy 2048-angle units; this client uses 16384-angle targets.
 * Apply through the public setters to update both integer and floating-point camera state.
 */
public final class MobileCameraInput {
    private static int yaw, pitch;
    private MobileCameraInput() {}
    public static synchronized void add(int dx,int dy) { yaw-=dx*8; pitch+=dy*8; }
    static void apply(Client client) {
        int x,y;
        synchronized(MobileCameraInput.class) { x=yaw;y=pitch;yaw=pitch=0; }
        if(x!=0) client.setCameraYawTarget((client.getCameraYawTarget()+x)&16383);
        if(y!=0) {
            client.setCameraPitchRelaxerEnabled(true);
            client.setCameraPitchTarget(Math.max(1024,Math.min(4096,client.getCameraPitchTarget()+y)));
        }
    }
}
