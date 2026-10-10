package dev.mobile.runelite;
import java.awt.Canvas;import java.nio.file.*;import java.util.*;import java.util.regex.*;
import net.runelite.rlawt.AWTContext;
import dev.mobile.rlhd.mobile.*;import dev.mobile.rlhd.opengl.shader.*;import dev.mobile.rlhd.utils.ResourcePath;
import static org.lwjgl.opengles.GLES32.*;
/** Real shader translation and driver compilation with fixture material/world-view counts. No game-scene/FPS claim. */
public class EfficientShaderSmoke {
 static ShaderIncludes includes(boolean efficient,boolean zone,Path root)throws Exception {
  ShaderIncludes i=new ShaderIncludes().addIncludePath(ResourcePath.path(root.toString()));
  String constants=Files.readString(root.resolve("utils/constants.glsl"));
  Matcher m=Pattern.compile("#include ([A-Z_]+)").matcher(constants);while(m.find())i.define(m.group(1),0);
  i.define("MOBILE_EFFICIENT",efficient).define("ZONE_RENDERER",zone).define("LEGACY_RENDERER",!zone)
   .define("SHADOW_MODE",1).define("SHADOW_FILTERING",0).define("SHADOW_FILTERING_KERNEL",efficient?1:2)
   .define("DYNAMIC_LIGHTS",true).define("MAX_LIGHT_COUNT",25).define("MATERIAL_COUNT",4).define("WATER_TYPE_COUNT",2)
   .define("MAX_SIMULTANEOUS_WORLD_VIEWS",4).define("NORMAL_MAPPING",!efficient).define("PARALLAX_OCCLUSION_MAPPING",false)
   .define("NEBULA_CLUSTER_COUNT",4).define("NEBULAE",false).define("STAR_MODE",0)
   .define("COLOR_BLINDNESS",0).define("APPLY_COLOR_FILTER",false).define("TILED_LIGHTING_TILE_SIZE",16)
   .define("MAX_CHARACTER_POSITION_COUNT",1).define("WIND_DISPLACEMENT_NOISE_RESOLUTION",16)
   .addInclude("MATERIAL_GETTER","#define getMaterial(i) uboMaterials.Array[i]")
   .addInclude("WATER_TYPE_GETTER","#define getWaterType(i) uboWaterTypes.Array[i]")
   .addInclude("WORLD_VIEW_GETTER","#define getWorldView(i) uboWorldViews.Array[i]");
  Set<String> names=new TreeSet<>();
  try(var paths=Files.walk(root)){for(Path path:paths.filter(p->p.toString().endsWith(".glsl")).toList()){
   Matcher match=Pattern.compile("\\bMAT_[A-Z0-9_]+\\b").matcher(Files.readString(path));while(match.find())names.add(match.group());
  }}
  StringBuilder material=new StringBuilder();for(String name:names)material.append("#define ").append(name).append(" getMaterial(0)\n");
  i.addInclude("MATERIAL_CONSTANTS",material.toString());return i;
 }
 public static void main(String[] args)throws Exception {
  AWTContext.loadNatives();Canvas canvas=new Canvas();canvas.setSize(16,16);AWTContext context=new AWTContext(canvas);context.configurePixelFormat(0,0,0);context.createGLContext();org.lwjgl.opengles.GLES.createCapabilities();
  Path root=Paths.get(System.getProperty("mobile.shader.resources"));
  for(boolean zone:new boolean[]{true,false})for(boolean efficient:new boolean[]{true,false}) {
   ShaderTemplate template=new ShaderTemplate().add(GL_VERTEX_SHADER,"scene_vert.glsl").add(GL_FRAGMENT_SHADER,"scene_frag.glsl");
   if(!zone)template.add(GL_GEOMETRY_SHADER,"scene_geom.glsl");
   int program=template.compile(includes(efficient,zone,root));glDeleteProgram(program);
   System.out.println("MOBILE_SCENE_SHADER_TRANSLATED_AND_LINKED efficient="+efficient+" zone="+zone);
  }
  MobileRenderDiagnostics diagnostics=new MobileRenderDiagnostics();
  for(int n=0;n<35;n++){diagnostics.prepare();diagnostics.begin();diagnostics.stamp(1);diagnostics.stamp(2);diagnostics.beforeSwap();glFlush();diagnostics.end(true,new int[]{16,16});}
  glFinish(); // Test-only completion; production diagnostics never wait for GPU results.
  diagnostics.prepare();diagnostics.begin();diagnostics.stamp(1);diagnostics.stamp(2);diagnostics.beforeSwap();diagnostics.end(true,new int[]{16,16});
  var supported=MobileRenderDiagnostics.class.getDeclaredField("supported");supported.setAccessible(true);
  var measured=MobileRenderDiagnostics.class.getDeclaredField("gpu");measured.setAccessible(true);
  if(supported.getBoolean(diagnostics) && !measured.get(diagnostics).toString().contains("shadow/tiled="))throw new AssertionError("GPU results not collected asynchronously");
  diagnostics.destroy();
  if(glGetError()!=GL_NO_ERROR)throw new AssertionError("GL error after shader/diagnostic checks");context.destroy();
  System.out.println("EFFICIENT_AND_RICH_SCENE_SHADER_VARIANTS_AND_DIAGNOSTICS_OK");System.exit(0);
 }
}
