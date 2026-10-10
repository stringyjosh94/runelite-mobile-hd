/* Deterministic EGL/window doubles: tests lifecycle logic, not a device driver. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <dlfcn.h>
#include <EGL/egl.h>
#include <android/native_window.h>
typedef struct {
 void *context,*es_surface,*native_window;int es,direct,surface_published,width,height;
 unsigned long long window_generation,rejected_generation;
 unsigned char *pixels;jobject canvas;
} Context;
static int showing=1,notifications,create_failure,make_failure,errors;
static ANativeWindow first={1},second={1},*available=&first;
static uint64_t serial=1;
static ANativeWindow *surfaces[256];static int next_surface=1;
static void fail(JNIEnv *env,const char *message){fprintf(stderr,"Unexpected failure: %s\n",message);errors++;}
static void *(*get_proc)(const char *);
static void readback_reset(Context *c){}
static jclass fake_class(JNIEnv *e,const char *s){return (jclass)1;}
static jclass fake_object(JNIEnv *e,jobject o){return (jclass)1;}
static jmethodID fake_method(JNIEnv *e,jclass c,const char *n,const char *s){return (jmethodID)1;}
static jboolean fake_boolean(JNIEnv *e,jobject c,jmethodID m,...){return showing;}
static void fake_void(JNIEnv *e,jclass c,jmethodID m,...){notifications++;}
static void fake_delete(JNIEnv *e,jobject o){}
static const struct JNINativeInterface_ functions={.FindClass=fake_class,.GetObjectClass=fake_object,.GetMethodID=fake_method,.GetStaticMethodID=fake_method,.CallBooleanMethod=fake_boolean,.CallStaticVoidMethod=fake_void,.DeleteLocalRef=fake_delete};
void ANativeWindow_acquire(ANativeWindow *w){w->references++;}
void ANativeWindow_release(ANativeWindow *w){assert(w->references>1);w->references--;}
int32_t ANativeWindow_setBuffersGeometry(ANativeWindow *w,int32_t x,int32_t y,int32_t f){w->width=x;w->height=y;return 0;}
static ANativeWindow *acquire(uint64_t *generation){*generation=serial;if(available)ANativeWindow_acquire(available);return available;}
EGLDisplay eglGetDisplay(EGLNativeDisplayType d){return (EGLDisplay)1;}
EGLBoolean eglInitialize(EGLDisplay d,EGLint *a,EGLint *b){return EGL_TRUE;}
EGLBoolean eglBindAPI(EGLenum e){return EGL_TRUE;}
EGLBoolean eglChooseConfig(EGLDisplay d,const EGLint *a,EGLConfig *c,EGLint n,EGLint *count){*c=(EGLConfig)1;*count=1;return EGL_TRUE;}
EGLBoolean eglGetConfigAttrib(EGLDisplay d,EGLConfig c,EGLint a,EGLint *v){*v=1;return EGL_TRUE;}
EGLContext eglCreateContext(EGLDisplay d,EGLConfig c,EGLContext share,const EGLint *a){return (EGLContext)99;}
EGLSurface eglCreatePbufferSurface(EGLDisplay d,EGLConfig c,const EGLint *a){return (EGLSurface)(uintptr_t)next_surface++;}
EGLSurface eglCreateWindowSurface(EGLDisplay d,EGLConfig c,EGLNativeWindowType w,const EGLint *a){
 if(create_failure){create_failure=0;return EGL_NO_SURFACE;}
 assert(!w->connected);w->connected=1;int id=next_surface++;surfaces[id]=w;return (EGLSurface)(uintptr_t)id;
}
EGLBoolean eglDestroySurface(EGLDisplay d,EGLSurface s){int id=(int)(uintptr_t)s;if(surfaces[id]){assert(surfaces[id]->connected);surfaces[id]->connected=0;surfaces[id]=NULL;}return EGL_TRUE;}
EGLBoolean eglMakeCurrent(EGLDisplay d,EGLSurface draw,EGLSurface read,EGLContext c){if(draw && make_failure){make_failure=0;return EGL_FALSE;}return EGL_TRUE;}
EGLBoolean eglSwapInterval(EGLDisplay d,EGLint i){return EGL_TRUE;}
EGLint eglGetError(void){return EGL_BAD_NATIVE_WINDOW;}
__eglMustCastToProperFunctionPointerType eglGetProcAddress(const char *n){return NULL;}
#include "../../app/src/native/gles_backend.h"
int main(void){
 JNIEnv jni=&functions;JNIEnv *env=&jni;Context c={.canvas=(jobject)1};
 char callback[64];snprintf(callback,sizeof(callback),"%llx",(unsigned long long)(uintptr_t)&acquire);setenv("RUNELITE_SURFACE_ACQUIRE",callback,1);setenv("RUNELITE_RENDERER","gles",1);
 assert(es_create(env,&c));void *context=c.context;
 assert(es_resize(env,&c,300,200) && c.direct && !c.pixels && first.connected && first.references==2);
 void *surface=c.es_surface;assert(es_resize(env,&c,300,200) && c.es_surface==surface && first.references==2);
 // Resize the same window: old EGL producer must disconnect first.
 assert(es_resize(env,&c,400,250) && c.direct && first.width==400 && first.references==2 && c.context==context);
 available=NULL;serial++;assert(es_resize(env,&c,400,250) && !c.direct && c.pixels && !first.connected && first.references==1);
 available=&second;serial++;assert(es_resize(env,&c,400,250) && c.direct && second.connected);
 // Simulate swap failure marking the current generation rejected.
 c.rejected_generation=c.window_generation;assert(es_resize(env,&c,400,250) && !c.direct && second.references==1);
 assert(es_resize(env,&c,400,250) && !c.direct); // No repeated retry on bad Surface.
 serial++;create_failure=1;assert(es_resize(env,&c,400,250) && !c.direct && second.references==1);
 serial++;assert(es_resize(env,&c,400,250) && c.direct);
 serial++;make_failure=1;assert(es_resize(env,&c,400,250) && !c.direct && second.references==1);
 serial++;assert(es_resize(env,&c,400,250) && c.direct);
 make_failure=1;assert(es_resize(env,&c,400,250) && !c.direct && second.references==1); // Lost current Surface.
 serial++;assert(es_resize(env,&c,400,250) && c.direct);
 showing=0;assert(es_resize(env,&c,64,64) && !c.direct && second.references==1); // Probe/hidden Canvas cannot take game Surface.
 showing=1;setenv("RUNELITE_GLES_SURFACE","copy",1);assert(es_resize(env,&c,400,250) && !c.direct);
 eglDestroySurface(es_display,c.es_surface);es_release_window(&c);free(c.pixels);
 assert(!errors && notifications>0 && first.references==1 && second.references==1 && !first.connected && !second.connected);
 puts("SURFACE_LIFECYCLE_RESIZE_LOSS_RECREATE_REJECTION_FALLBACK_AND_REFERENCES_OK");
}
