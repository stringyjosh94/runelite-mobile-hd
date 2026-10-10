package dev.mobile.runelite;

import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL43.*;
import net.runelite.rlawt.AWTContext;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Desktop GL entry points supplied by Android Mesa rather than GLX or desktop libGL. */
public final class GpuSupport {
    private GpuSupport() {}
    private static native long address(String name);
    public static synchronized void initializeFunctions() {
        if ("gles".equals(System.getProperty("runelite.android.renderer"))) {
            if (org.lwjgl.opengles.GLES.getFunctionProvider() == null)
                org.lwjgl.opengles.GLES.create(name -> address(functionName(name)));
            return;
        }
        if (GL.getFunctionProvider() == null)
            GL.create(name -> address(functionName(name)));
    }
    public static String functionName(ByteBuffer name) {
        ByteBuffer copy = name.duplicate();
        int start = copy.position(), end = start;
        while (end < copy.limit() && copy.get(end) != 0) end++;
        byte[] bytes = new byte[end - start]; copy.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    /** Exercise the Android native bindings before the user enables a GPU plugin. */
    public static void probe() {
        if ("gles".equals(System.getProperty("runelite.android.renderer"))) { GlesProbe.run(); return; }
        if (System.getProperty("runelite.rlawtpath") == null || "software".equals(System.getProperty("runelite.android.renderer"))) {
            System.out.println("ANDROID_SOFTWARE_MODE: GPU probe skipped; disable GPU and 117 HD plugins"); return;
        }
        AWTContext context = null;
        boolean capabilitiesCreated = false;
        try {
            System.out.println("Android GPU probe: starting; JNA=" + com.sun.jna.Native.VERSION
                + "; native pointer size=" + com.sun.jna.Native.POINTER_SIZE);
            AWTContext.loadNatives();
            java.awt.Canvas canvas = new java.awt.Canvas(); canvas.setSize(64, 64);
            context = new AWTContext(canvas);
            context.configurePixelFormat(0, 0, 0);
            context.createGLContext();
            org.lwjgl.opengl.GLCapabilities caps = GL.createCapabilities();
            capabilitiesCreated = true;
            System.out.println("Android GPU binding flags (not conformance): GL33=" + caps.OpenGL33 + "; GL43=" + caps.OpenGL43
                + "; renderer=" + glGetString(GL_RENDERER) + "; version=" + glGetString(GL_VERSION));
            glBindFramebuffer(GL_FRAMEBUFFER, 0); glReadBuffer(GL_FRONT); glDrawBuffer(GL_FRONT);
            glViewport(0, 0, 64, 64);
            glClearColor(0.25f, 0.5f, 0.75f, 1f); glClear(GL_COLOR_BUFFER_BIT); glFinish();
            java.nio.ByteBuffer pixel = org.lwjgl.BufferUtils.createByteBuffer(4);
            glReadPixels(32, 32, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            if (Math.abs((pixel.get(0) & 255) - 64) > 1 || Math.abs((pixel.get(1) & 255) - 128) > 1)
                throw new IllegalStateException("GPU framebuffer readback failed: RGB=" + (pixel.get(0)&255) + "," + (pixel.get(1)&255) + "," + (pixel.get(2)&255) + "; GL error=" + glGetError());
            System.out.println("ANDROID_GPU_FRAMEBUFFER_OK: hardware context framebuffer readback verified");
            System.out.println("Android shader language: " + glGetString(GL_SHADING_LANGUAGE_VERSION)
                + "; experimental compiler override=" + Boolean.getBoolean("runelite.glslExperiment"));
            shaderDrawTest(pixel);
            if (!caps.OpenGL43) {
                System.out.println("ANDROID_GPU_PROBE_OK: framebuffer and GLSL 330 triangle draw verified; not a full OpenGL 3.3 conformance or 117HD test; GL 4.3 compute unavailable");
                return;
            }
            int shader = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(shader, "#version 430\nlayout(local_size_x=1) in; layout(std430,binding=0) buffer Result { int value; }; void main(){value=42;}");
            glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0)
                throw new IllegalStateException("Compute shader: " + glGetShaderInfoLog(shader));
            int program = glCreateProgram(); glAttachShader(program, shader); glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0)
                throw new IllegalStateException("Compute program: " + glGetProgramInfoLog(program));
            int buffer = glGenBuffers(); glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer);
            glBufferData(GL_SHADER_STORAGE_BUFFER, new int[]{0}, GL_DYNAMIC_READ);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, buffer); glUseProgram(program);
            glDispatchCompute(1, 1, 1); glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);
            int[] result = new int[1]; glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, result);
            if (result[0] != 42) throw new IllegalStateException("GPU compute returned " + result[0]);
            glDeleteBuffers(buffer); glDeleteProgram(program); glDeleteShader(shader);
            System.out.println("ANDROID_GPU_PROBE_OK: hardware renderer, experimental GL 4.3, framebuffer readback and compute shader verified");
        } catch (Throwable error) {
            System.err.println("ANDROID_GPU_PROBE_FAILED: " + error);
            error.printStackTrace();
        } finally {
            if (context != null) context.destroy();
            if (capabilitiesCreated) GL.setCapabilities(null);
        }
    }
    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source); glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String detail = glGetShaderInfoLog(shader); glDeleteShader(shader);
            throw new IllegalStateException("GLSL 330 shader failed: " + detail);
        }
        return shader;
    }
    private static void shaderDrawTest(ByteBuffer pixel) {
        int vertex = 0, fragment = 0, program = 0, vao = 0;
        try {
            vertex = compileShader(GL_VERTEX_SHADER, "#version 330\nconst vec2 p[3]=vec2[3](vec2(-1,-1),vec2(3,-1),vec2(-1,3)); void main(){gl_Position=vec4(p[gl_VertexID],0,1);}");
            fragment = compileShader(GL_FRAGMENT_SHADER, "#version 330\nlayout(location=0) out vec4 color; void main(){color=vec4(0,1,0,1);}");
            program = glCreateProgram(); glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0)
                throw new IllegalStateException("GLSL 330 link failed: " + glGetProgramInfoLog(program));
            glViewport(0, 0, 64, 64); glClearColor(0, 0, 0, 1); glClear(GL_COLOR_BUFFER_BIT);
            vao = glGenVertexArrays(); glBindVertexArray(vao); glUseProgram(program);
            glDrawArrays(GL_TRIANGLES, 0, 3); glFinish();
            pixel.clear(); glReadPixels(32, 32, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            if ((pixel.get(0) & 255) > 1 || (pixel.get(1) & 255) < 254 || (pixel.get(2) & 255) > 1)
                throw new IllegalStateException("GLSL 330 triangle did not produce the expected green pixel");
            System.out.println("ANDROID_GPU_GLSL330_DRAW_OK: compiled, linked, drew and read back a requested GLSL 330 triangle; Zink may adapt it to GLSL 150");
        } finally {
            glUseProgram(0); glBindVertexArray(0);
            if (vao != 0) glDeleteVertexArrays(vao);
            if (program != 0) glDeleteProgram(program);
            if (vertex != 0) glDeleteShader(vertex);
            if (fragment != 0) glDeleteShader(fragment);
        }
    }
    public static void stopped() { HostMain.gpuStopped(); }
    public static void present(int[] pixels, int width, int height) {
        HostMain.gpuFrame(pixels, width, height);
    }
}
