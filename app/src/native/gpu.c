/* Android RLAWT ABI adapter. SPDX-License-Identifier: BSD-2-Clause
 * Native GLES can present directly through an Android EGL window surface.
 * Mesa OSMesa/Zink and GLES fallback frames use the Swing compositor.
 * No X11 or GLX is used. */
#include <jni.h>
#include <dlfcn.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <GLES3/gl3.h>
#include <time.h>
#define OSMESA_RGBA 0x1908
#define OSMESA_FORMAT 0x22
#define OSMESA_DEPTH_BITS 0x30
#define OSMESA_STENCIL_BITS 0x31
#define OSMESA_PROFILE 0x33
#define OSMESA_CORE_PROFILE 0x34
#define OSMESA_COMPAT_PROFILE 0x35
#define OSMESA_CONTEXT_MAJOR_VERSION 0x36
#define OSMESA_CONTEXT_MINOR_VERSION 0x37
#define OSMESA_Y_UP 0x11

typedef struct {
    void *context;
    void *es_surface;
    int es;
    int direct,surface_published;
    unsigned long long window_generation, rejected_generation;
    void *native_window;
    jobject canvas;
    unsigned char *pixels;
    int width, height, depth, stencil;
    unsigned int frames, swaps;
    GLuint readback_buffers[2];
    GLsync readback_fences[2];
    unsigned long long readback_sequence[2], readback_next;
    int readback_width, readback_height, readback_failed;
    unsigned int readback_skipped;
    double wait_ms, copy_ms;
} Context;
static void *mesa, *vulkan;
static void *(*create_context)(const int *,void *);
static int (*make_current)(void *,void *,unsigned int,int,int);
static void (*destroy_context)(void *);
static void (*pixel_store)(int,int);
static void *(*get_proc)(const char *);
static void fail(JNIEnv *env,const char *message) {
    fprintf(stderr,"Android GPU bridge: %s\n", message); fflush(stderr);
    jclass c=(*env)->FindClass(env,"java/lang/IllegalStateException");
    if(c) (*env)->ThrowNew(env,c,message);
}
static void readback_reset(Context *c);
#include "gles_backend.h"
static int load_mesa(JNIEnv *env) {
    if(mesa) return 1;
    const char *renderer=getenv("RUNELITE_RENDERER");
    if(renderer && !strcmp(renderer,"software")) {fail(env,"Software renderer selected; leave GPU plugins disabled");return 0;}
        vulkan=dlopen("libvulkan.so",RTLD_NOW|RTLD_LOCAL);
        if(!vulkan) { fail(env,"Android Vulkan loader unavailable"); return 0; }
        char handle[64]; snprintf(handle,sizeof(handle),"%lx",(unsigned long)(uintptr_t)vulkan);
        setenv("VULKAN_PTR",handle,1);
        setenv("GALLIUM_DRIVER","zink",1);
        setenv("MESA_LOADER_DRIVER_OVERRIDE","zink",1);
        setenv("MESA_ANDROID_NO_KMS_SWRAST","1",1);
    const char *path=getenv("RUNELITE_MESA_PATH");
    if(!path) {fail(env,"RUNELITE_MESA_PATH is missing"); return 0;}
    mesa=dlopen(path,RTLD_NOW|RTLD_LOCAL);
    if(!mesa) {fail(env,dlerror());return 0;}
    create_context=dlsym(mesa,"OSMesaCreateContextAttribs");
    make_current=dlsym(mesa,"OSMesaMakeCurrent");
    destroy_context=dlsym(mesa,"OSMesaDestroyContext");
    pixel_store=dlsym(mesa,"OSMesaPixelStore");
    get_proc=dlsym(mesa,"OSMesaGetProcAddress");
    if(!create_context || !make_current || !destroy_context || !pixel_store || !get_proc) {
        fail(env,"Mesa OSMesa API incomplete"); mesa=NULL; return 0;
    }
    return 1;
}
#include "shader_adapter.h"
#include "async_readback.h"
static Context *get(JNIEnv *env,jobject obj) {
    jclass cls=(*env)->GetObjectClass(env,obj);
    jfieldID f=(*env)->GetFieldID(env,cls,"instance","J");
    return (Context *)(uintptr_t)(*env)->GetLongField(env,obj,f);
}
static double millis(void) {
    struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec*1000.0+t.tv_nsec/1000000.0;
}
static int resize(JNIEnv *env,Context *c) {
    jclass cls=(*env)->GetObjectClass(env,c->canvas);
    int w=(*env)->CallIntMethod(env,c->canvas,(*env)->GetMethodID(env,cls,"getWidth","()I"));
    int h=(*env)->CallIntMethod(env,c->canvas,(*env)->GetMethodID(env,cls,"getHeight","()I"));
    (*env)->DeleteLocalRef(env,cls);
    if(w<=0 || h<=0 || w>8192 || h>8192) {fail(env,"Canvas has invalid dimensions");return 0;}
    if(c->es) return es_resize(env,c,w,h);
    if(w!=c->width || h!=c->height) {
        unsigned char *p=calloc((size_t)w*h,4);
        if(!p) {fail(env,"GPU frame allocation failed");return 0;}
        if(!make_current(c->context,p,GL_UNSIGNED_BYTE,w,h)) {
            free(p);fail(env,"OSMesaMakeCurrent failed on resize");return 0;
        }
        free(c->pixels); c->pixels=p;c->width=w;c->height=h;
    } else {
        void *(*current)(void)=get_proc("OSMesaGetCurrentContext");
        if (current && current()==c->context) return 1;
        if(!make_current(c->context,c->pixels,GL_UNSIGNED_BYTE,w,h)) {
            fail(env,"OSMesaMakeCurrent failed");return 0;
        }
    }
    pixel_store(OSMESA_Y_UP,0);
    return 1;
}
JNIEXPORT jlong JNICALL Java_net_runelite_rlawt_AWTContext_create0(JNIEnv *env,jclass cls,jobject canvas) {
    Context *c=calloc(1,sizeof(Context));
    if(!c) {fail(env,"Context allocation failed");return 0;}
    c->canvas=(*env)->NewGlobalRef(env,canvas);
    return (jlong)(uintptr_t)c;
}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_configureInsets(JNIEnv *e,jobject o,jint x,jint y) {}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_configurePixelFormat(JNIEnv *e,jobject o,jint a,jint d,jint s) {
    Context *c=get(e,o); if(c) {c->depth=d;c->stencil=s;}
}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_configureMultisamples(JNIEnv *e,jobject o,jint count) {}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_createGLContext(JNIEnv *env,jobject obj) {
    Context *c=get(env,obj);if(!c) return;
    if(is_gles()) {
        if(!es_create(env,c) || !resize(env,c)) return;
        const unsigned char *(*string)(unsigned int)=get_proc("glGetString");
        const char *es_renderer=(const char *)string(GL_RENDERER);
        fprintf(stderr,"Android native GLES: GL=%s; renderer=%s; vendor=%s; GLSL=%s\n",string(GL_VERSION),es_renderer,string(GL_VENDOR),string(GL_SHADING_LANGUAGE_VERSION));fflush(stderr);
#ifdef __ANDROID__
        if(!es_renderer || strstr(es_renderer,"llvmpipe") || strstr(es_renderer,"SwiftShader") || strstr(es_renderer,"softpipe")) {
            fail(env,"Native GLES hardware renderer unavailable; refusing software fallback");return;
        }
#endif
        jclass support=(*env)->FindClass(env,"dev/mobile/runelite/GpuSupport");
        if(support) (*env)->CallStaticVoidMethod(env,support,(*env)->GetStaticMethodID(env,support,"initializeFunctions","()V"));
        return;
    }
    if(!load_mesa(env)) return;
    /* Modern RuneLite shaders use core GL. Probe progressively lower genuine
     * profiles to distinguish a compatibility-profile restriction from a
     * device capability limit. Never override the advertised GL version. */
    const int requests[][3]={{4,3,OSMESA_CORE_PROFILE},{3,3,OSMESA_CORE_PROFILE},{3,1,OSMESA_CORE_PROFILE},{2,1,OSMESA_COMPAT_PROFILE}};
    for(unsigned int i=0;i<sizeof(requests)/sizeof(requests[0]);i++) {
        int attributes[]={OSMESA_FORMAT,OSMESA_RGBA,OSMESA_DEPTH_BITS,c->depth,
            OSMESA_STENCIL_BITS,c->stencil,OSMESA_PROFILE,requests[i][2],
            OSMESA_CONTEXT_MAJOR_VERSION,requests[i][0],OSMESA_CONTEXT_MINOR_VERSION,requests[i][1],0};
        c->context=create_context(attributes,NULL);
        fprintf(stderr,"Android GPU context request: %d.%d %s => %s\n",
            requests[i][0],requests[i][1],requests[i][2]==OSMESA_CORE_PROFILE?"core":"compatibility",
            c->context?"created":"unavailable"); fflush(stderr);
        if(c->context) break;
    }
    if(!c->context) {fail(env,"Zink cannot create even a diagnostic OpenGL context on this driver");return;}
    if(!resize(env,c)) return;
    const unsigned char *(*get_string)(unsigned int)=get_proc("glGetString");
    if(!get_string) {fail(env,"glGetString unavailable");return;}
    const char *version=(const char *)get_string(GL_VERSION);
    const char *renderer=(const char *)get_string(GL_RENDERER);
    fprintf(stderr,"Android GPU bridge: GL=%s; renderer=%s\n",version?version:"unknown",renderer?renderer:"unknown");fflush(stderr);
    if(!renderer || !strstr(renderer,"zink") || strstr(renderer,"llvmpipe") || strstr(renderer,"lavapipe") || strstr(renderer,"SwiftShader") || strstr(renderer,"softpipe")) {
        fail(env,"Hardware Zink renderer was not established; refusing software fallback");return;
    }
    jclass support=(*env)->FindClass(env,"dev/mobile/runelite/GpuSupport");
    if(support) (*env)->CallStaticVoidMethod(env,support,(*env)->GetStaticMethodID(env,support,"initializeFunctions","()V"));
}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_makeCurrent(JNIEnv *e,jobject o) {
    Context *c=get(e,o); if(c && c->context) resize(e,c);
}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_detachCurrent(JNIEnv *e,jobject o) {
    if(is_gles()) eglMakeCurrent(es_display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
    else if(make_current) make_current(NULL,NULL,GL_UNSIGNED_BYTE,0,0);
}
JNIEXPORT jint JNICALL Java_net_runelite_rlawt_AWTContext_getFramebuffer(JNIEnv *e,jobject o,jboolean front) {
    Context *c=get(e,o); if(!front && c && c->context) resize(e,c); return 0;
}
JNIEXPORT jint JNICALL Java_net_runelite_rlawt_AWTContext_setSwapInterval(JNIEnv *e,jobject o,jint interval) { return 0; }
JNIEXPORT jlong JNICALL Java_net_runelite_rlawt_AWTContext_getGLContext(JNIEnv *e,jobject o) {
    Context *c=get(e,o);return c?(jlong)(uintptr_t)c->context:0;
}
JNIEXPORT jlong JNICALL Java_net_runelite_rlawt_AWTContext_getGLXDisplay(JNIEnv *e,jobject o) {return 0;}
JNIEXPORT jlong JNICALL Java_net_runelite_rlawt_AWTContext_getWGLHDC(JNIEnv *e,jobject o) {return 0;}
JNIEXPORT jlong JNICALL Java_net_runelite_rlawt_AWTContext_getCGLShareGroup(JNIEnv *e,jobject o) {return 0;}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_swapBuffers(JNIEnv *env,jobject obj) {
    Context *c=get(env,obj);if(!c || !c->context || (!c->pixels && !c->direct)) return;
    double begin=millis();c->swaps++;
    int ready=1;
    if(c->es && c->direct) {
        if(eglSwapBuffers(es_display,(EGLSurface)c->es_surface)) {
            if(!c->surface_published) {es_notify(env,1);c->surface_published=1;}c->frames++;
            if(c->frames==30 || c->frames%120==0) {fprintf(stderr,"GLES direct Surface: frames=%u %dx%d; no game readback; swap=%.2fms\n",c->frames,c->width,c->height,millis()-begin);fflush(stderr);}
        } else {
            fprintf(stderr,"GLES direct Surface: swap failed EGL=0x%x; switching to copied presentation until Surface recreation\n",eglGetError());fflush(stderr);
            c->rejected_generation=c->window_generation;c->surface_published=0;es_notify(env,0);
        }
        return;
    }
    if(c->es) {
        ready=async_readback(c);
        if(ready<0) {
            synchronous_gles_readback(c);
            ready=1;
        }
    } else {void (*finish)(void)=get_proc("glFinish");if(finish)finish();}
    double finished=millis();c->wait_ms+=finished-begin;
    if(!ready) {presentation_stats(c);return;}
    jsize n=c->width*c->height;
    jclass host=(*env)->FindClass(env,"dev/mobile/runelite/HostMain");if(!host)return;
    jmethodID acquire=(*env)->GetStaticMethodID(env,host,"acquireGpuFrame","(II)[I");
    jintArray frame=acquire?(*env)->CallStaticObjectMethod(env,host,acquire,c->width,c->height):NULL;
    (*env)->DeleteLocalRef(env,host);if(!frame)return;
    jint *out=(*env)->GetIntArrayElements(env,frame,NULL);if(!out) {(*env)->DeleteLocalRef(env,frame);return;}
    for(int i=0;i<n;i++) {unsigned char *p=c->pixels+(c->es ? ((c->height-1-i/c->width)*c->width+i%c->width)*4 : i*4);out[i]=(jint)(0xff000000u | p[0]<<16 | p[1]<<8 | p[2]);}
    (*env)->ReleaseIntArrayElements(env,frame,out,0);
    jclass support=(*env)->FindClass(env,"dev/mobile/runelite/GpuSupport");
    if(support) {(*env)->CallStaticVoidMethod(env,support,(*env)->GetStaticMethodID(env,support,"present","([III)V"),frame,c->width,c->height);(*env)->DeleteLocalRef(env,support);}
    (*env)->DeleteLocalRef(env,frame);
    c->copy_ms+=millis()-finished;c->frames++;
    presentation_stats(c);
}
JNIEXPORT void JNICALL Java_net_runelite_rlawt_AWTContext_destroy(JNIEnv *env,jobject obj) {
    Context *c=get(env,obj);if(!c) return;
    jclass support=(*env)->FindClass(env,"dev/mobile/runelite/GpuSupport");
    if(support) {(*env)->CallStaticVoidMethod(env,support,(*env)->GetStaticMethodID(env,support,"stopped","()V"));(*env)->DeleteLocalRef(env,support);}
    if(c->es) {
        es_notify(env,0);
        if(c->context && c->es_surface)eglMakeCurrent(es_display,c->es_surface,c->es_surface,c->context);
        readback_reset(c);
        eglMakeCurrent(es_display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
        if(c->es_surface) eglDestroySurface(es_display,(EGLSurface)c->es_surface);
        if(c->context) eglDestroyContext(es_display,(EGLContext)c->context);
        es_release_window(c);
    } else if(c->context && destroy_context) destroy_context(c->context);
    (*env)->DeleteGlobalRef(env,c->canvas);free(c->pixels);free(c);
    jclass cls=(*env)->GetObjectClass(env,obj);
    (*env)->SetLongField(env,obj,(*env)->GetFieldID(env,cls,"instance","J"),0);
}
JNIEXPORT jlong JNICALL Java_dev_mobile_runelite_GpuSupport_address(JNIEnv *env,jclass cls,jstring name) {
    const char *str=(*env)->GetStringUTFChars(env,name,NULL);
    void *p=get_proc?get_proc(str):NULL;
    const char *adapt=getenv("RUNELITE_SHADER_ADAPT");
    if(get_proc && adapt && !strcmp(adapt,"1") && !strcmp(str,"glShaderSource")) p=(void *)adapted_shader_source;
    (*env)->ReleaseStringUTFChars(env,name,str);return (jlong)(uintptr_t)p;
}
