package dev.mobile.runelite;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import java.util.*;
import java.util.function.BiFunction;

/** Stock-HD configuration API for Hub integrations; never forwards backend switches. */
public final class MobileHdCompatibility {
    private static final Map<String,String> DEFAULTS;
    static {
        Map<String,String> values=new LinkedHashMap<>();
        values.put("defaultSkyColor","DEFAULT");values.put("overrideSky","false");
        values.put("screenBrightness","100");values.put("fContrast","100");
        values.put("shadowMode","FAST");values.put("seasonalTheme","AUTOMATIC");
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    private final BiFunction<String,String,String> read;
    private final Writer write;
    @FunctionalInterface public interface Writer { void set(String group,String key,String value); }
    public MobileHdCompatibility(ConfigManager config) {
        this(config::getConfiguration,(group,key,value)-> {
            if(value==null) config.unsetConfiguration(group,key);
            else config.setConfiguration(group,key,value);
        });
    }
    public MobileHdCompatibility(BiFunction<String,String,String> read,Writer write) {
        this.read=read;this.write=write;
    }
    /** Mobile settings win at startup, including users' presets and manual edits. */
    public void initialize() {
        DEFAULTS.forEach((key,fallback)-> {
            String value=read.apply("mobilehd",key);
            if(value==null) value=fallback;
            copy("hd",key,value);
        });
    }
    private void copy(String group,String key,String value) {
        if(!Objects.equals(value,read.apply(group,key))) write.set(group,key,value);
    }
    @Subscribe public void onConfigChanged(ConfigChanged event) {
        changed(event.getGroup(),event.getKey(),event.getNewValue());
    }
    public void changed(String group,String key,String value) {
        if(!DEFAULTS.containsKey(key)) return;
        if("hd".equals(group)) copy("mobilehd",key,value);
        else if("mobilehd".equals(group)) copy("hd",key,value);
    }
}
