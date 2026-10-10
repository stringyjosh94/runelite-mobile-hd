/* SPDX-License-Identifier: BSD-2-Clause. Native Android EGL/GLES context backend. */
#include <EGL/egl.h>
#include <EGL/eglext.h>
#ifdef __ANDROID__
#include <android/native_window.h>
#endif
static void es_notify(JNIEnv *env,int direct) {
    jclass host=(*env)->FindClass(env,"dev/mobile/runelite/HostMain");
    if(host) {jmethodID method=(*env)->GetStaticMethodID(env,host,"directSurface","(Z)V");
        if(method)(*env)->CallStaticVoidMethod(env,host,method,direct?JNI_TRUE:JNI_FALSE);
        (*env)->DeleteLocalRef(env,host);}
}
static void es_release_window(Context *c) {
#ifdef __ANDROID__
    if(c->native_window)ANativeWindow_release(c->native_window);
#endif
    c->native_window=NULL;
}
static EGLDisplay es_display = EGL_NO_DISPLAY;
static EGLConfig es_config;
static void *es_library;
static int is_gles(void) {
    const char *mode=getenv("RUNELITE_RENDERER");
    return mode && !strcmp(mode,"gles");
}
static void *es_proc(const char *name) {
    void *p=(void *)eglGetProcAddress(name);
    return p ? p : (es_library ? dlsym(es_library,name) : NULL);
}
static int es_load(JNIEnv *env) {
    if(es_display!=EGL_NO_DISPLAY) return 1;
    es_library=dlopen("libGLESv3.so",RTLD_NOW|RTLD_LOCAL);
    if(!es_library) es_library=dlopen("libGLESv2.so",RTLD_NOW|RTLD_LOCAL);
    EGLDisplay display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
    EGLint major,minor,count;
    if(display==EGL_NO_DISPLAY || !eglInitialize(display,&major,&minor) || !eglBindAPI(EGL_OPENGL_ES_API)) {
        fail(env,"Android GLES: EGL initialization failed");return 0;
    }
    EGLint surface_bits=EGL_PBUFFER_BIT;
#ifdef __ANDROID__
    if(!getenv("RUNELITE_GLES_SURFACE") || strcmp(getenv("RUNELITE_GLES_SURFACE"),"copy"))surface_bits|=EGL_WINDOW_BIT;
#endif
    EGLint attrs[]={EGL_SURFACE_TYPE,surface_bits,EGL_RENDERABLE_TYPE,EGL_OPENGL_ES3_BIT_KHR,
        EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,
        EGL_DEPTH_SIZE,24,EGL_STENCIL_SIZE,8,EGL_NONE};
    if(!eglChooseConfig(display,attrs,&es_config,1,&count) || count!=1) {
        attrs[1]=EGL_PBUFFER_BIT;
        if(!eglChooseConfig(display,attrs,&es_config,1,&count) || count!=1) {
            fail(env,"Android GLES: no RGBA8/depth24 ES3 framebuffer config");return 0;
        }
        fprintf(stderr,"GLES direct Surface unavailable: no combined window/pbuffer config; using copied frames\n");
    }
    es_display=display;get_proc=es_proc;return 1;
}
static int es_resize(JNIEnv *env,Context *c,int w,int h) {
    void *window=NULL;unsigned long long generation=0;
#ifdef __ANDROID__
    const char *mode=getenv("RUNELITE_GLES_SURFACE");
    jclass canvas_class=(*env)->GetObjectClass(env,c->canvas);
    jmethodID showing=(*env)->GetMethodID(env,canvas_class,"isShowing","()Z");
    int eligible=showing && (*env)->CallBooleanMethod(env,c->canvas,showing);
    (*env)->DeleteLocalRef(env,canvas_class);
    if(eligible && (!mode || strcmp(mode,"copy"))) {
        /* Installed by this process's ART bridge before starting HotSpot.
         * A plain C callback avoids Android linker namespace/global visibility assumptions. */
        const char *callback=getenv("RUNELITE_SURFACE_ACQUIRE");
        ANativeWindow *(*acquire)(uint64_t *)=callback?(void *)(uintptr_t)strtoull(callback,NULL,16):NULL;
        uint64_t serial=0;
        if(acquire)window=acquire(&serial);
        generation=serial;
        if(window && generation==c->rejected_generation) {ANativeWindow_release(window);window=NULL;}
    }
#endif
    int direct=window!=NULL;
    int changed=w!=c->width || h!=c->height || direct!=c->direct || (direct && generation!=c->window_generation);
    if(changed) {
        // A native window may have only one EGL producer. Disconnect before
        // recreating its surface on a canvas resize, while retaining GL resources.
        es_notify(env,0);c->surface_published=0;
        eglMakeCurrent(es_display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
        if(c->es_surface)eglDestroySurface(es_display,(EGLSurface)c->es_surface);
        c->es_surface=NULL;es_release_window(c);
        EGLSurface surface=EGL_NO_SURFACE;
#ifdef __ANDROID__
        if(window) {
            EGLint format=0;eglGetConfigAttrib(es_display,es_config,EGL_NATIVE_VISUAL_ID,&format);
            if(ANativeWindow_setBuffersGeometry(window,w,h,format)==0)
                surface=eglCreateWindowSurface(es_display,es_config,window,NULL);
            if(surface==EGL_NO_SURFACE) {
                fprintf(stderr,"GLES direct Surface: window creation failed EGL=0x%x; using copied frames\n",eglGetError());fflush(stderr);
                c->rejected_generation=generation;ANativeWindow_release(window);window=NULL;direct=0;
            }
        }
#endif
        if(!direct) {EGLint attrs[]={EGL_WIDTH,w,EGL_HEIGHT,h,EGL_NONE};surface=eglCreatePbufferSurface(es_display,es_config,attrs);}
        if(surface==EGL_NO_SURFACE) {fail(env,"Android GLES surface allocation failed");return 0;}
        unsigned char *pixels=direct?NULL:calloc((size_t)w*h,4);
        int current=(direct || pixels) && eglMakeCurrent(es_display,surface,surface,(EGLContext)c->context);
#ifdef __ANDROID__
        if(!current && direct) {
            fprintf(stderr,"GLES direct Surface: make-current failed EGL=0x%x; using copied frames\n",eglGetError());fflush(stderr);
            eglDestroySurface(es_display,surface);ANativeWindow_release(window);window=NULL;direct=0;c->rejected_generation=generation;
            pixels=calloc((size_t)w*h,4);
            EGLint fallback[]={EGL_WIDTH,w,EGL_HEIGHT,h,EGL_NONE};surface=eglCreatePbufferSurface(es_display,es_config,fallback);
            current=pixels && surface!=EGL_NO_SURFACE && eglMakeCurrent(es_display,surface,surface,(EGLContext)c->context);
        }
#endif
        if(!current) {
            free(pixels);eglDestroySurface(es_display,surface);
#ifdef __ANDROID__
            if(window)ANativeWindow_release(window);
#endif
            fail(env,"Android GLES surface make-current/allocation failed");return 0;
        }
        readback_reset(c);
        free(c->pixels);
        c->pixels=pixels;c->es_surface=surface;c->width=w;c->height=h;
        c->direct=direct;c->native_window=window;c->window_generation=generation;
        if(direct) {eglSwapInterval(es_display,1);fprintf(stderr,"GLES direct Surface attached: %dx%d generation=%llu\n",w,h,generation);fflush(stderr);}
    } else {
#ifdef __ANDROID__
        if(window)ANativeWindow_release(window);
#endif
        if(!eglMakeCurrent(es_display,(EGLSurface)c->es_surface,(EGLSurface)c->es_surface,(EGLContext)c->context)) {
            if(c->direct) {
                fprintf(stderr,"GLES direct Surface: restore-current failed EGL=0x%x; using copied frames\n",eglGetError());fflush(stderr);
                c->rejected_generation=c->window_generation;
                return es_resize(env,c,w,h);
            }
            fail(env,"Android GLES restore-current failed");return 0;
        }
    }
    return 1;
}
static int es_create(JNIEnv *env,Context *c) {
    if(!es_load(env)) return 0;
    EGLint attrs[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
    c->context=eglCreateContext(es_display,es_config,EGL_NO_CONTEXT,attrs);
    if(c->context==EGL_NO_CONTEXT) {fail(env,"Android GLES context creation failed");return 0;}
    c->es=1;return 1;
}
