/* Embedded VirGL test server with a current Android EGL pbuffer context.
 * SPDX-License-Identifier: BSD-2-Clause */
#include <EGL/egl.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/stat.h>
static void *virgl_server;
static int (*virgl_main)(int,char **);
#include <stdatomic.h>
static atomic_int virgl_failed;
static int virgl_attempted;
static void *virgl_thread(void *unused) {
    EGLDisplay display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
    EGLint major,minor,n=0;
    const EGLint config_attrs[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,EGL_OPENGL_ES3_BIT,
        EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
    const EGLint surface_attrs[]={EGL_WIDTH,64,EGL_HEIGHT,64,EGL_NONE};
    const EGLint context_attrs[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
    EGLConfig config;
    if(display==EGL_NO_DISPLAY || !eglInitialize(display,&major,&minor) ||
        !eglBindAPI(EGL_OPENGL_ES_API) || !eglChooseConfig(display,config_attrs,&config,1,&n) || n==0) {
        fprintf(stderr,"Embedded VirGL: EGL configuration failed, error=%x\n",eglGetError());virgl_failed=1;return NULL;
    }
    EGLSurface surface=eglCreatePbufferSurface(display,config,surface_attrs);
    EGLContext context=eglCreateContext(display,config,EGL_NO_CONTEXT,context_attrs);
    if(surface==EGL_NO_SURFACE || context==EGL_NO_CONTEXT || !eglMakeCurrent(display,surface,surface,context)) {
        fprintf(stderr,"Embedded VirGL: EGL pbuffer/context failed, error=%x\n",eglGetError());virgl_failed=1;return NULL;
    }
    void *gles=dlopen("libGLESv3.so",RTLD_NOW|RTLD_LOCAL);
    const GLubyte *(*host_get_string)(GLenum)=gles?dlsym(gles,"glGetString"):NULL;
    if(!host_get_string) {fprintf(stderr,"Embedded VirGL: GLES entry points unavailable\n");virgl_failed=1;return NULL;}
    fprintf(stderr,"Embedded VirGL host: GL=%s; renderer=%s; GLSL=%s\n",host_get_string(GL_VERSION),host_get_string(GL_RENDERER),host_get_string(GL_SHADING_LANGUAGE_VERSION));fflush(stderr);
    const char *renderer=(const char *)host_get_string(GL_RENDERER);
    if(!renderer || strstr(renderer,"llvmpipe") || strstr(renderer,"SwiftShader")) {virgl_failed=1;return NULL;}
    char *argv[]={"embedded-vtest","--no-fork","--use-gles",NULL};
    int result=virgl_main(3,argv);
    fprintf(stderr,"Embedded VirGL server exited: %d\n",result);fflush(stderr);virgl_failed=1;
    eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
    eglDestroyContext(display,context);eglDestroySurface(display,surface);
    return NULL;
}
static int start_virgl(JNIEnv *env) {
    if(virgl_attempted) {
        if(virgl_failed) {fail(env,"Embedded VirGL server failed; restart the app before retrying");return 0;}
        return 1;
    }
    virgl_attempted=1;
    const char *dir=getenv("RUNELITE_NATIVE_DIR"),*socket_path=getenv("VTEST_SOCKET_NAME");
    if(!dir || !socket_path || strlen(socket_path)>100) {fail(env,"Embedded VirGL configuration missing or socket path too long");return 0;}
    char path[4096];snprintf(path,sizeof(path),"%s/libvirgl_test_server.so",dir);
    virgl_server=dlopen(path,RTLD_NOW|RTLD_LOCAL);
    if(!virgl_server) {fail(env,dlerror());return 0;}
    virgl_main=dlsym(virgl_server,"vtest_main");
    if(!virgl_main) {fail(env,"VirGL server entry point missing");return 0;}
    unlink(socket_path);
    setenv("VTEST_USE_GLES","1",1);
    pthread_t thread;
    if(pthread_create(&thread,NULL,virgl_thread,NULL)!=0) {fail(env,"VirGL server thread failed");return 0;}
    pthread_detach(thread);
    struct stat st;
    for(int i=0;i<150;i++) {
        if(virgl_failed) {fail(env,"VirGL server initialization failed; see preceding EGL diagnostics");return 0;}
        if(stat(socket_path,&st)==0 && S_ISSOCK(st.st_mode)) {
            fprintf(stderr,"Embedded VirGL: private socket ready at %s\n",socket_path);fflush(stderr);return 1;
        }
        usleep(20000);
    }
    fail(env,"VirGL socket was not ready within 3 seconds");return 0;
}
