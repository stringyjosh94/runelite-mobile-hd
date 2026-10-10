package dev.mobile.runelite;

import net.runelite.client.RuneLite;
import net.runelite.client.plugins.*;
import net.runelite.client.config.ConfigManager;
import javax.swing.SwingUtilities;
import java.util.*;

/** Register the bundled fork only when the user selected the GLES backend. */
public final class GlesPluginBootstrap {
    private GlesPluginBootstrap() {}
    public static void start() {
        if (!"gles".equals(System.getProperty("runelite.android.renderer"))) return;
        Thread worker=new Thread(() -> {
            try {
                for(int i=0;i<360;i++) {
                    if(RuneLite.getInjector()!=null) {
                        PluginManager manager=RuneLite.getInjector().getInstance(PluginManager.class);
                        if(RuneLite.getInjector().getInstance(net.runelite.api.Client.class).getGameState().getState()>=10 &&
                            RuneLite.getInjector().getInstance(ConfigManager.class).getProfile()!=null &&
                            manager.getPlugins().stream().anyMatch(p->p.getClass().getName().equals("net.runelite.client.plugins.gpu.GpuPlugin"))) {
                            System.out.println("MOBILE_HD_READY: client login state, profile and core plugins available");
                            SwingUtilities.invokeAndWait(() -> register(manager));return;
                        }
                    }
                    Thread.sleep(500);
                }
                System.err.println("117HD GLES registration timed out");
            } catch(Throwable error) { error.printStackTrace(); }
        },"gles-plugin-registration");worker.setDaemon(true);worker.start();
    }
    private static void register(PluginManager manager) {
        try {
            ConfigManager config=RuneLite.getInjector().getInstance(ConfigManager.class);
            RuneLite.getInjector().getInstance(net.runelite.client.eventbus.EventBus.class)
                .register(new MobileWidgetHitTest(RuneLite.getInjector().getInstance(net.runelite.api.Client.class)));
            if(!"2".equals(config.getConfiguration("mobilehd","initialProfile"))) {
                Map<String,String> settings=new LinkedHashMap<>();
                settings.put("initialProfile","2");settings.put("legacyRenderer2","false");
                settings.put("experimentalStorageBuffers","OFF");settings.put("experimentalIndirectDraw","OFF");
                settings.put("antiAliasingMode","DISABLED");settings.put("shadowMode","FAST");
                settings.put("shadowResolution","RES_1024");settings.put("shadowDistance","DISTANCE_20");settings.put("shadowFiltering","SMOOTH_LOW");settings.put("dynamicLights","FEW");
                settings.put("tiledLighting","false");settings.put("drawDistance","24");
                settings.put("sceneResolutionScale","75");settings.put("parallaxOcclusionMappingToggle","false");
                for(Map.Entry<String,String> entry:settings.entrySet()) config.setConfiguration("mobilehd",entry.getKey(),entry.getValue());
            }
            if (!"1".equals(config.getConfiguration("mobilehd","viewDistanceV17"))) {
                String distance=config.getConfiguration("mobilehd","drawDistance");
                if (distance==null || "24".equals(distance)) config.setConfiguration("mobilehd","drawDistance","32");
                config.setConfiguration("mobilehd","viewDistanceV17","1");
            }
            if(!"1".equals(config.getConfiguration("mobilehd","daylightV17"))) {
                String cycle=config.getConfiguration("mobilehd","daylightCycle");
                if(cycle==null || "DEFAULT".equals(cycle)) config.setConfiguration("mobilehd","daylightCycle","DAY");
                config.setConfiguration("mobilehd","daylightV17","1");
            }
            String preset=MobileHdProfiles.normalize(System.getProperty("runelite.android.hdProfile","low"));
            String request=System.getProperty("runelite.android.hdProfileRequest",preset);
            if(!request.equals(config.getConfiguration("mobilehd","appliedGraphicsRequest"))) {
                for(Map.Entry<String,String> value:MobileHdProfiles.settings(preset).entrySet())
                    config.setConfiguration("mobilehd",value.getKey(),value.getValue());
                config.setConfiguration("mobilehd","appliedGraphicsRequest",request);
                System.out.println("Mobile HD graphics preset applied: "+preset);
            }
            net.runelite.client.eventbus.EventBus bus=RuneLite.getInjector().getInstance(net.runelite.client.eventbus.EventBus.class);
            MobileHdCompatibility compatibility=new MobileHdCompatibility(config);
            compatibility.initialize();
            bus.register(compatibility);
            for (Plugin old : new ArrayList<>(manager.getPlugins())) {
                String name=old.getClass().getName();
                if(name.equals("net.runelite.client.plugins.gpu.GpuPlugin") || name.equals("rs117.hd.HdPlugin")) {
                    manager.setPluginEnabled(old,false);manager.stopPlugin(old);
                }
            }
            List<Plugin> plugins=manager.loadPlugins(Collections.singletonList(Class.forName("dev.mobile.rlhd.HdPlugin")),null);
            manager.loadDefaultPluginConfiguration(plugins);
            for(Plugin plugin:plugins) { manager.setPluginEnabled(plugin,true); manager.startPlugin(plugin); }
            System.out.println("117HD GLES mobile: automatic startup; direct AWT touch input enabled");
        } catch(Throwable error) { error.printStackTrace(); }
    }
}
