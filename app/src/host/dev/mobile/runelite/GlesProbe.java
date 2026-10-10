package dev.mobile.runelite;

import net.runelite.rlawt.AWTContext;
import org.lwjgl.opengles.GLES;
import static org.lwjgl.opengles.GLES32.*;

/** Tests real GLES entry points and framebuffer output before loading the experimental fork. */
public final class GlesProbe {
    private GlesProbe() {}
    public static void run() {
        AWTContext context = null;
        try {
            AWTContext.loadNatives();
            java.awt.Canvas canvas = new java.awt.Canvas(); canvas.setSize(64, 64);
            context = new AWTContext(canvas); context.configurePixelFormat(0,24,8); context.createGLContext();
            if (!GLES.createCapabilities().GLES32) throw new IllegalStateException("Native GLES 3.2 is required");
            System.out.println("GLES driver: " + glGetString(GL_RENDERER) + "; " + glGetString(GL_VERSION));
            glBindFramebuffer(GL_FRAMEBUFFER,0); glViewport(0,0,64,64);
            glClearColor(0.25f,0.5f,0.75f,1); glClear(GL_COLOR_BUFFER_BIT);
            java.nio.ByteBuffer pixel=org.lwjgl.BufferUtils.createByteBuffer(4);
            glReadPixels(32,32,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixel);
            if (Math.abs((pixel.get(0)&255)-64)>1 || Math.abs((pixel.get(1)&255)-128)>1)
                throw new IllegalStateException("GLES framebuffer readback failed");
            int vs=compile(GL_VERTEX_SHADER,"#version 320 es\nvoid main(){vec2 p[3]=vec2[3](vec2(-1,-1),vec2(3,-1),vec2(-1,3));gl_Position=vec4(p[gl_VertexID],0,1);}");
            int fs=compile(GL_FRAGMENT_SHADER,"#version 320 es\nprecision highp float;layout(location=0) out vec4 color;void main(){color=vec4(0,1,0,1);}");
            int program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program);
            if(glGetProgrami(program,GL_LINK_STATUS)==0) throw new IllegalStateException(glGetProgramInfoLog(program));
            int vao=glGenVertexArrays();glBindVertexArray(vao);glUseProgram(program);glDrawArrays(GL_TRIANGLES,0,3);
            glReadPixels(32,32,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixel);
            if((pixel.get(1)&255)<250 || (pixel.get(0)&255)>2 || glGetError()!=GL_NO_ERROR)
                throw new IllegalStateException("GLES triangle draw/readback failed");
            glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vs);glDeleteShader(fs);
            System.out.println("ANDROID_GLES_PROBE_OK: native ES3.2 framebuffer and shader triangle; not an HD scene test");
        } catch (Throwable error) {
            System.err.println("ANDROID_GLES_PROBE_FAILED");error.printStackTrace();
        } finally {
            if (context!=null) context.destroy();
            GLES.setCapabilities(null);
        }
    }
    private static int compile(int type,String source) {
        int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==0) throw new IllegalStateException(glGetShaderInfoLog(shader));
        return shader;
    }
}
