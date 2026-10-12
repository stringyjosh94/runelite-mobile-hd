package dev.mobile.runelite;

import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Runs the real upstream client inside the embedded JVM, not Android ART. */
public final class HostMain {
    private static volatile Image completedGameFrame;
    private static volatile boolean gpuPresentation;
    private static volatile boolean directSurface;
    private static volatile int[] presentation=new int[5];
    public static int[] presentation() {return presentation.clone();}
    public static void directSurface(boolean active) {
        synchronized(gpuFrameLock) {directSurface=active;if(active)gpuPresentation=true;}
    }
    private static final Object gpuFrameLock=new Object();
    private static final class GpuFrame {
        final int[] pixels;
        final java.awt.image.BufferedImage image;
        boolean leased;
        GpuFrame(int width,int height) {
            pixels=new int[Math.multiplyExact(width,height)];
            int[] masks={0x00ff0000,0x0000ff00,0x000000ff,0xff000000};
            java.awt.image.DataBufferInt data=new java.awt.image.DataBufferInt(pixels,pixels.length);
            java.awt.image.WritableRaster raster=java.awt.image.Raster.createPackedRaster(data,width,height,width,masks,null);
            java.awt.image.DirectColorModel color=new java.awt.image.DirectColorModel(32,masks[0],masks[1],masks[2],masks[3]);
            image=new java.awt.image.BufferedImage(color,raster,false,null);
        }
    }
    private static GpuFrame[] gpuFrames=new GpuFrame[0];
    public static void gpuStopped() {synchronized(gpuFrameLock) {gpuPresentation=false;directSurface=false;gpuFrames=new GpuFrame[0];}}
    /** Borrow an unpublished buffer. Native code must finish writing before gpuFrame. */
    public static int[] acquireGpuFrame(int width,int height) {
        if(width<=0 || height<=0 || width>8192 || height>8192)throw new IllegalArgumentException("Invalid GPU frame dimensions");
        synchronized(gpuFrameLock) {
            if(gpuFrames.length==0 || gpuFrames[0].image.getWidth()!=width || gpuFrames[0].image.getHeight()!=height)
                gpuFrames=new GpuFrame[]{new GpuFrame(width,height),new GpuFrame(width,height),new GpuFrame(width,height)};
            for(GpuFrame frame:gpuFrames)if(frame.image!=completedGameFrame && !frame.leased) {frame.leased=true;return frame.pixels;}
            throw new IllegalStateException("GPU frame pool exhausted");
        }
    }
    public static void gpuFrame(int[] pixels,int width,int height) {
        synchronized(gpuFrameLock) {
            for(GpuFrame frame:gpuFrames)if(frame.pixels==pixels) {
                frame.leased=false;gpuPresentation=true;completedGameFrame=frame.image;return;
            }
            // Compatibility for external producers that still supply immutable arrays.
            int[] masks={0x00ff0000,0x0000ff00,0x000000ff,0xff000000};
            java.awt.image.DataBufferInt data=new java.awt.image.DataBufferInt(pixels,pixels.length);
            java.awt.image.WritableRaster raster=java.awt.image.Raster.createPackedRaster(data,width,height,width,masks,null);
            java.awt.image.DirectColorModel color=new java.awt.image.DirectColorModel(32,masks[0],masks[1],masks[2],masks[3]);
            gpuPresentation=true;completedGameFrame=new java.awt.image.BufferedImage(color,raster,false,null);
        }
    }
    private static java.awt.image.BufferedImage composition;
    private static int[] compositionPixels;
    private static boolean displayConfigured;
    private static final java.util.concurrent.atomic.AtomicBoolean capturePending = new java.util.concurrent.atomic.AtomicBoolean();

    // Render a fresh off-screen composition instead of sampling Cacio's
    // shared backing store while peers clear and repaint it.
    public static boolean swingUiAt(int x,int y) throws Exception {
        final boolean[] hit={false};
        SwingUtilities.invokeAndWait(()-> {
            Component target=MobilePointerInput.targetAt(x,y);
            hit[0]=target!=null && !(target instanceof Canvas);
        });return hit[0];
    }

    public static boolean gameCanvasAt(int x, int y) throws Exception {
        final boolean[] hit = {false};
        SwingUtilities.invokeAndWait(() -> {
            Component target = MobilePointerInput.targetAt(x, y);
            if (target instanceof Canvas) {
                Point at = target.getLocationOnScreen();
                hit[0] = !MobileWidgetHitTest.blocksCamera(x-at.x,y-at.y,target.getWidth(),target.getHeight());
            }
        });
        return hit[0];
    }

    public static boolean textInputFocused() throws Exception {
        final boolean[] text={false};
        SwingUtilities.invokeAndWait(()-> {
            text[0]=MobilePointerInput.textInputFocused();
        });return text[0];
    }
    public static int[] pixels() throws Exception {
        final int[][] result = new int[1][];
        SwingUtilities.invokeAndWait(() -> {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            if(composition==null || composition.getWidth()!=size.width || composition.getHeight()!=size.height) {
                composition=new java.awt.image.BufferedImage(size.width,size.height,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                compositionPixels=((java.awt.image.DataBufferInt)composition.getRaster().getDataBuffer()).getData();
            }
            presentation=new int[5];
            Graphics2D graphics = composition.createGraphics();
            graphics.setColor(Color.BLACK); graphics.fillRect(0, 0, size.width, size.height);
            RepaintManager manager = RepaintManager.currentManager((Component)null);
            boolean buffered = manager.isDoubleBufferingEnabled();
            manager.setDoubleBufferingEnabled(false);
            try {
                // Paint frames before their owned dialogs/popups. Print the root
                // pane directly, so heavyweight peers cannot redirect drawing
                // back into the shared Cacio screen buffer.
                for (Window window : Window.getWindows())
                    if (window.isShowing() && window.getOwner() == null) paintWindow(window, graphics);
                for (Window window : Window.getWindows())
                    if (window.isShowing() && window.getOwner() != null) paintWindow(window, graphics);
            } finally {
                manager.setDoubleBufferingEnabled(buffered); graphics.dispose();
            }
            // Borrowed until the next pixels() call. The single Android render worker
            // synchronously copies this through JNI before requesting another frame.
            result[0] = compositionPixels;
        });
        return result[0];
    }

    private static void paintWindow(Window window, Graphics2D target) {
        // Cacio also exposes internal proxy Windows whose peer.print is unsupported.
        if (!(window instanceof RootPaneContainer)) return;
        Graphics2D graphics = (Graphics2D)target.create();
        try {
            graphics.translate(window.getX(), window.getY());
            graphics.clipRect(0, 0, window.getWidth(), window.getHeight());
            if (window instanceof RootPaneContainer) {
                JRootPane root = ((RootPaneContainer)window).getRootPane();
                Graphics2D ui = (Graphics2D)graphics.create(root.getX(), root.getY(), root.getWidth(), root.getHeight());
                root.printAll(ui); ui.dispose();
            } else window.printAll(graphics);
            Canvas canvas = findCanvas(window);
            synchronized(gpuFrameLock) {
                Image game=completedGameFrame;
                if(canvas!=null && directSurface) {
                    Point at=SwingUtilities.convertPoint(canvas,0,0,window);
                    Graphics2D hole=(Graphics2D)graphics.create();
                    hole.setComposite(AlphaComposite.Clear);hole.fillRect(at.x,at.y,canvas.getWidth(),canvas.getHeight());hole.dispose();
                    presentation=new int[]{1,window.getX()+at.x,window.getY()+at.y,canvas.getWidth(),canvas.getHeight()};
                    // Lightweight Swing popups/glass panes must remain above the game opening.
                    JRootPane root=((RootPaneContainer)window).getRootPane();
                    JLayeredPane layers=root.getLayeredPane();
                    Component[] children=layers.getComponents();
                    for(int i=children.length-1;i>=0;i--) {Component child=children[i];
                        if(!child.isVisible() || layers.getLayer(child)<=JLayeredPane.DEFAULT_LAYER)continue;
                        Point pos=SwingUtilities.convertPoint(child,0,0,window);
                        Graphics2D popup=(Graphics2D)graphics.create(pos.x,pos.y,child.getWidth(),child.getHeight());child.printAll(popup);popup.dispose();
                    }
                    Component glass=root.getGlassPane();
                    if(glass.isVisible()) {Point pos=SwingUtilities.convertPoint(glass,0,0,window);Graphics2D overlay=(Graphics2D)graphics.create(pos.x,pos.y,glass.getWidth(),glass.getHeight());glass.printAll(overlay);overlay.dispose();}
                } else if(canvas!=null && game!=null) {
                    Point at=SwingUtilities.convertPoint(canvas,0,0,window);
                    graphics.setClip(at.x,at.y,canvas.getWidth(),canvas.getHeight());
                    graphics.drawImage(game,at.x,at.y,canvas.getWidth(),canvas.getHeight(),null);
                }
            }
        } finally { graphics.dispose(); }
    }

    private static Canvas findCanvas(Component component) {
        if (component instanceof Canvas && component.isShowing()) return (Canvas) component;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) {
            Canvas found = findCanvas(child); if (found != null) return found;
        }
        return null;
    }

    private static void configureDisplay(Dimension size) {
        if (displayConfigured) return;
        try {
            Object injector = Class.forName("net.runelite.client.RuneLite").getMethod("getInjector").invoke(null);
            if (injector == null) return;
            Method instance = Class.forName("com.google.inject.Injector").getMethod("getInstance", Class.class);
            Class<?> configType = Class.forName("net.runelite.client.config.ConfigManager");
            Object config = instance.invoke(injector, configType);
            if (configType.getMethod("getProfile").invoke(config) == null) return;
            Method set = configType.getMethod("setConfiguration", String.class, String.class, String.class);
            Method get = configType.getMethod("getConfiguration", String.class, String.class);
            if (System.getProperty("runelite.android.renderer") != null && !"13".equals(get.invoke(config, "runelite", "mobileHdProfile"))) {
                String[][] preset = {
                    {"experimentalStorageBuffers", "OFF"}, {"experimentalIndirectDraw", "OFF"},
                    {"antiAliasingMode", "DISABLED"}, {"shadowMode", "OFF"},
                    {"dynamicLights", "NONE"}, {"tiledLighting", "false"},
                    {"drawDistance", "24"}, {"detailDistance", "25"},
                    {"expandedMapLoadingChunks", "0"}, {"sceneResolutionScale", "75"},
                    {"anisotropicFilteringLevel", "2"}
                };
                for (String[] setting : preset) {
                    System.out.println("Mobile HD profile: " + setting[0] + "=" + setting[1]
                        + "; previous=" + get.invoke(config, "hd", setting[0]));
                    set.invoke(config, "hd", setting[0], setting[1]);
                }
                set.invoke(config, "runelite", "mobileHdProfile", "13");
            }
            set.invoke(config, "runelite", "automaticResizeType", "KEEP_WINDOW_SIZE");
            set.invoke(config, "runelite", "rememberScreenBounds", "false");
            set.invoke(config, "runelite", "gameSize", (size.width - 40) + "x" + (size.height - 35));
            Class<?> drawType = Class.forName("net.runelite.client.ui.DrawManager");
            Object draw = instance.invoke(injector, drawType);
            Method request = drawType.getMethod("requestNextFrameListener", java.util.function.Consumer.class);
            java.util.function.Consumer<Image> capture = image -> {
                if (!gpuPresentation) completedGameFrame = image;
                capturePending.set(false);
            };
            Runnable everyFrame = () -> {
                // GPU swapBuffers already supplies a completed frame. Requesting
                // another screenshot forces a second GPU readback per game frame.
                if (gpuPresentation) return;
                if (capturePending.compareAndSet(false, true)) try { request.invoke(draw, capture); }
                catch (Exception error) { capturePending.set(false); error.printStackTrace(); }
            };
            drawType.getMethod("registerEveryFrameListener", Runnable.class).invoke(draw, everyFrame);
            displayConfigured = true;
            System.out.println("Mobile display configured: " + size.width + "x" + size.height + "; completed-frame capture enabled");
        } catch (Exception error) { error.printStackTrace(); }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("runelite.launcher.version", "android-example");
        System.setProperty("jagex.disableBouncyCastle", "true");
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> report(error));
        SwingUtilities.invokeLater(() -> new Timer(500, event -> fitWindows()).start());
        // Diagnostic context creation/readback is unnecessary on every launch.
        // The HD renderer checks the real GLES capabilities when it starts.
        if (Boolean.getBoolean("runelite.android.startupGpuProbe")) GpuSupport.probe();
        System.out.println("MOBILE_CLIENT_START: launching RuneLite");
        MobileNotifications.start();
        GlesPluginBootstrap.start();
        String capture = System.getProperty("example.capture");
        if (capture != null) {
            Thread snapshot = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        Thread.sleep(30000);
                        Class<?> screen = Class.forName("com.github.caciocavallosilano.cacio.ctc.CTCScreen");
                        int[] pixels = (int[]) screen.getMethod("getCurrentScreenRGB").invoke(null);
                        BufferedImageHelper.save(pixels, capture);
                        System.out.println("CAPTURE_SAVED " + capture);
                        if (Boolean.getBoolean("example.verifyControls") && i == 0) {
                            SwingUtilities.invokeAndWait(() -> {
                                for (Frame frame : Frame.getFrames()) clickToolTip(frame, "Configuration");
                            });
                        }
                        if (Boolean.getBoolean("example.verifyControls") && i == 1) {
                            SwingUtilities.invokeAndWait(() -> {
                                for (Frame frame : Frame.getFrames()) clickToolTip(frame, "Plugin Hub");
                            });
                        }
                    }
                } catch (Throwable error) { error.printStackTrace(); }
            }, "verification-capture");
            snapshot.setDaemon(true);
            snapshot.start();
        }
        try {
            Class.forName("net.runelite.client.RuneLite").getMethod("main", String[].class)
                .invoke(null, (Object) new String[]{"--disable-telemetry"});
        } catch (InvocationTargetException error) { report(error.getCause()); }
    }

    private static boolean clickToolTip(Component component, String tooltip) {
        if (component.isShowing() && component instanceof JComponent && tooltip.equals(((JComponent) component).getToolTipText())) {
            try {
                Point location = component.getLocationOnScreen();
                Method receive = Class.forName("com.github.caciocavallosilano.cacio.ctc.CTCAndroidInput")
                    .getMethod("receiveData", int.class, int.class, int.class, int.class, int.class);
                receive.invoke(null, 1003, location.x + component.getWidth()/2, location.y + component.getHeight()/2, 0, 0);
                receive.invoke(null, 1006, 1024, 1, 0, 0);
                receive.invoke(null, 1006, 1024, 0, 0, 0);
                System.out.println("CONTROL_CLICK_SENT " + tooltip); return true;
            } catch (Exception error) { error.printStackTrace(); }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) if (clickToolTip(child, tooltip)) return true;
        }
        return false;
    }

    private static void fitWindows() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        configureDisplay(screen);
        for (Frame frame : Frame.getFrames()) {
            if (!frame.isVisible()) continue;
            boolean main = frame.getClass().getName().equals("net.runelite.client.ui.ContainableFrame");
            int width = main ? screen.width : Math.min(frame.getWidth(), screen.width);
            int height = main ? screen.height : Math.min(frame.getHeight(), screen.height);
            int x = main ? 0 : Math.max(0, Math.min(frame.getX(), screen.width-width));
            int y = main ? 0 : Math.max(0, Math.min(frame.getY(), screen.height-height));
            if (x != frame.getX() || y != frame.getY() || width != frame.getWidth() || height != frame.getHeight())
                frame.setBounds(x, y, width, height);
        }
    }

    private static void report(Throwable error) {
        error.printStackTrace();
        try {
            SwingUtilities.invokeLater(() -> {
                JTextArea details = new JTextArea(error.toString(), 15, 70);
                details.setEditable(false);
                JFrame frame = new JFrame("RuneLite startup error");
                frame.add(new JScrollPane(details));
                frame.pack(); frame.setVisible(true);
            });
        } catch (Throwable secondary) { secondary.printStackTrace(); }
    }

    private static final class BufferedImageHelper {
        static void save(int[] pixels, String target) throws IOException {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size.width, size.height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, size.width, size.height, pixels, 0, size.width);
            ImageIO.write(image, "png", new File(target));
        }
    }
}
