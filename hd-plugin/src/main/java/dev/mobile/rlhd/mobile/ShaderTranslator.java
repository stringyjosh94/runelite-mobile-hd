package dev.mobile.rlhd.mobile;
/** Link desktop shader interfaces before emitting native GLES source with explicit conversions. */
public final class ShaderTranslator {
 static { System.loadLibrary("rlhd_shader"); }
 private ShaderTranslator() {}
 public static native String[] translate0(String[] sources,int[] stages);
}
