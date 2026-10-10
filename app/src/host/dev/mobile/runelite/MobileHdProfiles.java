package dev.mobile.runelite;
import java.util.*;
/** Shared by ART and the embedded JVM: three explicit, portable HD presets. */
public final class MobileHdProfiles {
    private MobileHdProfiles() {}
    public static String normalize(String name) {
        if("high".equals(name))return "high";
        if("medium".equals(name)||"balanced".equals(name)||"keep".equals(name))return "medium";
        return "low"; // Includes efficient/light/ultra and removed standard/software selections.
    }
    public static Map<String,String> settings(String name) {
        if(!Arrays.asList("low","medium","high").contains(name))throw new IllegalArgumentException("Unknown HD preset");
        boolean low="low".equals(name),high="high".equals(name);
        Map<String,String> v=new LinkedHashMap<>();
        v.put("mobileEfficient",Boolean.toString(!high));v.put("legacyRenderer2","false");
        v.put("experimentalStorageBuffers","OFF");v.put("experimentalIndirectDraw","OFF");
        v.put("antiAliasingMode","DISABLED");v.put("tiledLighting","false");
        v.put("parallaxOcclusionMappingToggle","false");v.put("normalMapping",Boolean.toString(high));
        v.put("textureResolution",low?"RES_128":"RES_256");
        v.put("terrainShadows",Boolean.toString(high));v.put("enableShadowTransparency",Boolean.toString(high));
        v.put("shadowMode","FAST");v.put("shadowFiltering",low?"NEAREST":"SMOOTH_LOW");
        v.put("shadowResolution",low?"RES_512":high?"RES_2048":"RES_1024");
        v.put("shadowDistance",low?"DISTANCE_12":high?"DISTANCE_20":"DISTANCE_20");
        v.put("dynamicLights","FEW");
        v.put("mobileLightLimit",low?"2":high?"8":"4"); // Avoid desktop 25-light budgets.
        v.put("drawDistance",low?"24":high?"48":"32");
        v.put("detailDistance",low?"24":high?"48":"32");
        v.put("sceneResolutionScale","100");
        v.put("sceneScalingMode",low?"NEAREST":"LINEAR");
        v.put("anisotropicFilteringLevel",low?"0":high?"4":"2");
        v.put("expandedMapLoadingChunks","0");
        // Keep all tiers on predictable mobile paths, including after desktop imports.
        v.put("windDisplacement","false");v.put("characterDisplacement","false");
        v.put("expandShadowDraw","false");
        return v;
    }
}
