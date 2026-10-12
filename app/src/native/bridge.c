#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <android/native_window_jni.h>
#include <stdint.h>

/* Shared native window only: never pass JNI objects between ART and HotSpot. */
static pthread_mutex_t surface_lock = PTHREAD_MUTEX_INITIALIZER;
static ANativeWindow *game_window;
static uint64_t surface_generation;
__attribute__((visibility("default"))) ANativeWindow *rl_surface_acquire(uint64_t *generation) {
    pthread_mutex_lock(&surface_lock);
    ANativeWindow *window=game_window;
    if(window) ANativeWindow_acquire(window);
    *generation=surface_generation;
    pthread_mutex_unlock(&surface_lock);
    return window;
}
JNIEXPORT void JNICALL Java_dev_mobile_runelite_NativeBridge_surface(JNIEnv *env,jclass cls,jobject surface) {
    ANativeWindow *next=surface?ANativeWindow_fromSurface(env,surface):NULL;
    pthread_mutex_lock(&surface_lock);
    ANativeWindow *old=game_window;game_window=next;surface_generation++;
    pthread_mutex_unlock(&surface_lock);
    if(old)ANativeWindow_release(old);
}
static JavaVM *art_vm;
static JavaVM *hotspot_vm;
static int vm_ready;
static jclass android_bridge;
static jmethodID open_link, query_clipboard, put_clipboard;
static pthread_mutex_t vm_lock = PTHREAD_MUTEX_INITIALIZER;

static JNIEnv *runtime_env(void) {
    JavaVM *vm;
    pthread_mutex_lock(&vm_lock);
    vm = vm_ready ? hotspot_vm : NULL;
    pthread_mutex_unlock(&vm_lock);
    if (!vm) return NULL;
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) == JNI_EDETACHED) {
        if ((*vm)->AttachCurrentThreadAsDaemon(vm, (void **)&env, NULL) != JNI_OK) return NULL;
    }
    return env;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    if (!art_vm) {
        art_vm = vm;
        JNIEnv *env;
        (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6);
        jclass cls = (*env)->FindClass(env, "dev/mobile/runelite/NativeBridge");
        if (!cls) return JNI_ERR;
        android_bridge = (*env)->NewGlobalRef(env, cls);
        open_link = (*env)->GetStaticMethodID(env, cls, "openLink", "(Ljava/lang/String;)V");
        query_clipboard = (*env)->GetStaticMethodID(env, cls, "queryClipboard", "()V");
        put_clipboard = (*env)->GetStaticMethodID(env, cls, "putClipboard", "(Ljava/lang/String;Ljava/lang/String;)V");
        if ((*env)->ExceptionCheck(env)) return JNI_ERR;
    } else if (vm != art_vm) {
        pthread_mutex_lock(&vm_lock); hotspot_vm = vm; pthread_mutex_unlock(&vm_lock);
    }
    return JNI_VERSION_1_6;
}

static int describe(JNIEnv *env) {
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); return 1;
    }
    return 0;
}

static double bridge_ms(void) {struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec*1000.0+t.tv_nsec/1000000.0;}

JNIEXPORT jintArray JNICALL Java_dev_mobile_runelite_NativeBridge_pixels(JNIEnv *art, jclass cls) {
    double begin=bridge_ms(),composed=begin;
    static unsigned int frames;static double compose_ms,copy_ms;
    JNIEnv *env = runtime_env();
    if (!env) return NULL;
    if ((*env)->PushLocalFrame(env, 12) != JNI_OK) return NULL;
    jclass screen = (*env)->FindClass(env, "dev/mobile/runelite/HostMain");
    jintArray result = NULL;
    if (screen && !describe(env)) {
        jmethodID method = (*env)->GetStaticMethodID(env, screen, "pixels", "()[I");
        if (method) {
            jintArray data = (jintArray)(*env)->CallStaticObjectMethod(env, screen, method);
            composed=bridge_ms();
            if (!describe(env) && data) {
                jsize count = (*env)->GetArrayLength(env, data);
                jint *buffer = (*env)->GetIntArrayElements(env, data, NULL);
                if (buffer) {
                    result = (*art)->NewIntArray(art, count);
                    if (result) (*art)->SetIntArrayRegion(art, result, 0, count, buffer);
                    (*env)->ReleaseIntArrayElements(env, data, buffer, JNI_ABORT);
                }
            }
        }
    }
    describe(env); (*env)->PopLocalFrame(env, NULL);
    if(result) {frames++;compose_ms+=composed-begin;copy_ms+=bridge_ms()-composed;
        if(frames==30 || frames%120==0) {fprintf(stderr,"Display bridge: frames=%u; JVM composition/EDT=%.2fms, JVM-to-Android copy=%.2fms\n",frames,compose_ms/frames,copy_ms/frames);fflush(stderr);}}
    return result;
}

JNIEXPORT jintArray JNICALL Java_dev_mobile_runelite_NativeBridge_presentation(JNIEnv *art,jclass cls) {
    JNIEnv *env=runtime_env();if(!env || (*env)->PushLocalFrame(env,8)!=JNI_OK)return NULL;
    jint values[5]={0};jintArray result=NULL;
    jclass host=(*env)->FindClass(env,"dev/mobile/runelite/HostMain");
    if(host && !describe(env)) {
        jmethodID method=(*env)->GetStaticMethodID(env,host,"presentation","()[I");
        jintArray data=method?(*env)->CallStaticObjectMethod(env,host,method):NULL;
        if(!describe(env) && data && (*env)->GetArrayLength(env,data)==5) {
            (*env)->GetIntArrayRegion(env,data,0,5,values);
            result=(*art)->NewIntArray(art,5);
            if(result)(*art)->SetIntArrayRegion(art,result,0,5,values);
        }
    }
    describe(env);(*env)->PopLocalFrame(env,NULL);return result;
}

JNIEXPORT jboolean JNICALL Java_dev_mobile_runelite_NativeBridge_gameCanvasAt(JNIEnv *art, jclass cls, jint x, jint y) {
    JNIEnv *env = runtime_env();
    if (!env || (*env)->PushLocalFrame(env, 8) != JNI_OK) return JNI_FALSE;
    jboolean result = JNI_FALSE;
    jclass host = (*env)->FindClass(env, "dev/mobile/runelite/HostMain");
    if (host && !describe(env)) {
        jmethodID method = (*env)->GetStaticMethodID(env, host, "gameCanvasAt", "(II)Z");
        if (method) result = (*env)->CallStaticBooleanMethod(env, host, method, x, y);
    }
    if (describe(env)) result = JNI_FALSE;
    (*env)->PopLocalFrame(env, NULL);
    return result;
}

JNIEXPORT jboolean JNICALL Java_dev_mobile_runelite_NativeBridge_swingUiAt(JNIEnv *art, jclass cls, jint x, jint y) {
    JNIEnv *env = runtime_env();
    if (!env || (*env)->PushLocalFrame(env, 8) != JNI_OK) return JNI_FALSE;
    jboolean result = JNI_FALSE;
    jclass host = (*env)->FindClass(env, "dev/mobile/runelite/HostMain");
    if (host && !describe(env)) {
        jmethodID method = (*env)->GetStaticMethodID(env, host, "swingUiAt", "(II)Z");
        if (method) result = (*env)->CallStaticBooleanMethod(env, host, method, x, y);
    }
    if (describe(env)) result = JNI_FALSE;
    (*env)->PopLocalFrame(env, NULL);
    return result;
}

JNIEXPORT jboolean JNICALL Java_dev_mobile_runelite_NativeBridge_textInputFocused(JNIEnv *art, jclass cls) {
    JNIEnv *env = runtime_env();
    if (!env || (*env)->PushLocalFrame(env, 8) != JNI_OK) return JNI_FALSE;
    jboolean result = JNI_FALSE;
    jclass host = (*env)->FindClass(env, "dev/mobile/runelite/HostMain");
    if (host && !describe(env)) {
        jmethodID method = (*env)->GetStaticMethodID(env, host, "textInputFocused", "()Z");
        if (method) result = (*env)->CallStaticBooleanMethod(env, host, method);
    }
    if (describe(env)) result = JNI_FALSE;
    (*env)->PopLocalFrame(env, NULL);
    return result;
}

JNIEXPORT void JNICALL Java_dev_mobile_runelite_NativeBridge_input(JNIEnv *art, jclass cls,
    jint type, jint a, jint b, jint c, jint d) {
    JNIEnv *env = runtime_env();
    if (!env || (*env)->PushLocalFrame(env, 8) != JNI_OK) return;
    jclass input = (*env)->FindClass(env, (type==1000 || type==1005 || type==1003 || type==1006 || type==1007 || type==1008 || type==1009 || type==1011 || type==1012 || type==1013 || type==1014) ? "dev/mobile/runelite/MobilePointerInput" : "com/github/caciocavallosilano/cacio/ctc/CTCAndroidInput");
    if (input && !describe(env)) {
        jmethodID method = (*env)->GetStaticMethodID(env, input, "receiveData", "(IIIII)V");
        if (method) (*env)->CallStaticVoidMethod(env, input, method, type, a, b, c, d);
    }
    describe(env); (*env)->PopLocalFrame(env, NULL);
}

static JNIEnv *android_env(int *detach) {
    JNIEnv *env = NULL; *detach = 0;
    if ((*art_vm)->GetEnv(art_vm, (void **)&env, JNI_VERSION_1_6) == JNI_EDETACHED) {
        if ((*art_vm)->AttachCurrentThread(art_vm, (void **)&env, NULL) != JNI_OK) return NULL;
        *detach = 1;
    }
    return env;
}

static void uri(JNIEnv *runtime, jstring value) {
    int detach; JNIEnv *art = android_env(&detach); if (!art) return;
    const char *text = (*runtime)->GetStringUTFChars(runtime, value, NULL);
    if (text) {
        jstring copy = (*art)->NewStringUTF(art, text);
        (*art)->CallStaticVoidMethod(art, android_bridge, open_link, copy);
        (*art)->DeleteLocalRef(art, copy);
        (*runtime)->ReleaseStringUTFChars(runtime, value, text);
    }
    describe(art); if (detach) (*art_vm)->DetachCurrentThread(art_vm);
}
JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCDesktopPeer_openUri(JNIEnv *env, jclass cls, jstring value) { uri(env, value); }
JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCDesktopPeer_openFile(JNIEnv *env, jclass cls, jstring value) { uri(env, value); }
JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openUri(JNIEnv *env, jclass cls, jstring value) { uri(env, value); }
JNIEXPORT void JNICALL Java_net_java_openjdk_cacio_ctc_CTCDesktopPeer_openFile(JNIEnv *env, jclass cls, jstring value) { uri(env, value); }

JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCClipboard_nQuerySystemClipboard(JNIEnv *env, jclass cls) {
    int detach; JNIEnv *art = android_env(&detach); if (!art) return;
    (*art)->CallStaticVoidMethod(art, android_bridge, query_clipboard);
    describe(art); if (detach) (*art_vm)->DetachCurrentThread(art_vm);
}
JNIEXPORT void JNICALL Java_com_github_caciocavallosilano_cacio_ctc_CTCClipboard_nPutClipboardData(JNIEnv *env, jclass cls, jstring data, jstring mime) {
    int detach; JNIEnv *art = android_env(&detach); if (!art) return;
    const char *a = (*env)->GetStringUTFChars(env, data, NULL);
    const char *b = (*env)->GetStringUTFChars(env, mime, NULL);
    if (a && b) {
        jstring ca = (*art)->NewStringUTF(art, a), cb = (*art)->NewStringUTF(art, b);
        (*art)->CallStaticVoidMethod(art, android_bridge, put_clipboard, ca, cb);
        (*art)->DeleteLocalRef(art, ca); (*art)->DeleteLocalRef(art, cb);
    }
    if (a) (*env)->ReleaseStringUTFChars(env, data, a);
    if (b) (*env)->ReleaseStringUTFChars(env, mime, b);
    describe(art); if (detach) (*art_vm)->DetachCurrentThread(art_vm);
}
JNIEXPORT void JNICALL Java_dev_mobile_runelite_NativeBridge_clipboard(JNIEnv *art, jclass cls, jstring data, jstring mime) {
    JNIEnv *env = runtime_env(); if (!env || (*env)->PushLocalFrame(env, 8) != JNI_OK) return;
    const char *a = (*art)->GetStringUTFChars(art, data, NULL), *b = (*art)->GetStringUTFChars(art, mime, NULL);
    jclass clipboard = (*env)->FindClass(env, "com/github/caciocavallosilano/cacio/ctc/CTCClipboard");
    if (clipboard && a && b) {
        jmethodID method = (*env)->GetStaticMethodID(env, clipboard, "systemClipboardDataReceived", "(Ljava/lang/String;Ljava/lang/String;)V");
        if (method) (*env)->CallStaticVoidMethod(env, clipboard, method, (*env)->NewStringUTF(env, a), (*env)->NewStringUTF(env, b));
    }
    if (a) (*art)->ReleaseStringUTFChars(art, data, a);
    if (b) (*art)->ReleaseStringUTFChars(art, mime, b);
    describe(env); (*env)->PopLocalFrame(env, NULL);
}

JNIEXPORT void JNICALL Java_dev_mobile_runelite_NativeBridge_environment(JNIEnv *env, jclass cls, jstring key, jstring value) {
    const char *k = (*env)->GetStringUTFChars(env, key, NULL), *v = (*env)->GetStringUTFChars(env, value, NULL);
    if (k && v) setenv(k, v, 1);
    if (k) (*env)->ReleaseStringUTFChars(env, key, k);
    if (v) (*env)->ReleaseStringUTFChars(env, value, v);
}

static int preload(const char *directory, const char *name) {
    char path[4096]; snprintf(path, sizeof(path), "%s/%s", directory, name);
    if (!dlopen(path, RTLD_NOW | RTLD_GLOBAL)) {
        fprintf(stderr, "Cannot load %s: %s\n", path, dlerror()); return 0;
    }
    return 1;
}

JNIEXPORT jint JNICALL Java_dev_mobile_runelite_NativeBridge_launch(JNIEnv *art, jclass cls,
    jstring native_dir, jstring runtime_dir, jstring home, jstring log, jobjectArray options) {
    char surface_callback[64];snprintf(surface_callback,sizeof(surface_callback),"%llx",(unsigned long long)(uintptr_t)&rl_surface_acquire);
    setenv("RUNELITE_SURFACE_ACQUIRE",surface_callback,1);
    const char *dir = (*art)->GetStringUTFChars(art, native_dir, NULL);
    const char *runtime = (*art)->GetStringUTFChars(art, runtime_dir, NULL);
    const char *user = (*art)->GetStringUTFChars(art, home, NULL);
    const char *logfile = (*art)->GetStringUTFChars(art, log, NULL);
    if (!dir || !runtime || !user || !logfile) return -1;
    FILE *stream = fopen(logfile, "w");
    if (stream) { fflush(NULL); dup2(fileno(stream), STDOUT_FILENO); dup2(fileno(stream), STDERR_FILENO); fclose(stream); }
    setvbuf(stdout, NULL, _IONBF, 0); setvbuf(stderr, NULL, _IONBF, 0);
    char search[8192]; snprintf(search, sizeof(search), "%s:%s/lib:%s/lib/server:/system/lib64", dir, runtime, runtime);
    setenv("LD_LIBRARY_PATH", search, 1); setenv("JAVA_HOME", runtime, 1); setenv("HOME", user, 1);
    setenv("TMPDIR", user, 1); chdir(user);
    fprintf(stderr, "Runelite Mobile (HD) v0.20: starting ARM64 HotSpot\n");
    char modules[4096], jvm_path[4096], runtime_lib[4096];
    snprintf(modules, sizeof(modules), "%s/lib/modules", runtime);
    snprintf(jvm_path, sizeof(jvm_path), "%s/lib/server/libjvm.so", runtime);
    snprintf(runtime_lib, sizeof(runtime_lib), "%s/lib", runtime);
    fprintf(stderr, "JAVA_HOME=%s\nJVM=%s\nMODULES=%s readable=%d\n", runtime,
        jvm_path, modules, access(modules, R_OK) == 0);
    int result = -2;
    if (access(modules, R_OK) != 0) {
        fprintf(stderr, "Runtime module image is missing or unreadable.\n"); goto cleanup;
    }
    // Load the actual file in lib/server first, before any DT_NEEDED resolves
    // libjvm.so from Android's flat native directory.
    void *handle = dlopen(jvm_path, RTLD_NOW | RTLD_GLOBAL);
    if (!handle) { fprintf(stderr, "Cannot load JVM: %s\n", dlerror()); goto cleanup; }
    const char *libraries[] = {"libjli.so", "libverify.so", "libjava.so", "libjimage.so", "libnet.so", "libnio.so", "libzip.so", "libawt.so", "libawt_headless.so", "libfreetype.so", "libfontmanager.so", "libawt_xawt.so", "libinstrument.so"};
    for (size_t i = 0; i < sizeof(libraries)/sizeof(libraries[0]); i++) if (!preload(runtime_lib, libraries[i])) goto cleanup;
    jint (*create)(JavaVM **, void **, void *) = dlsym(handle, "JNI_CreateJavaVM");
    if (!create) goto cleanup;
    jsize count = (*art)->GetArrayLength(art, options);
    JavaVMOption *opts = calloc(count, sizeof(JavaVMOption));
    if (!opts) goto cleanup;
    for (jsize i = 0; i < count; i++) {
        jstring item = (*art)->GetObjectArrayElement(art, options, i);
        const char *text = (*art)->GetStringUTFChars(art, item, NULL);
        opts[i].optionString = strdup(text);
        (*art)->ReleaseStringUTFChars(art, item, text); (*art)->DeleteLocalRef(art, item);
    }
    JavaVMInitArgs args = {.version=JNI_VERSION_1_8, .nOptions=count, .options=opts, .ignoreUnrecognized=JNI_FALSE};
    JNIEnv *env; JavaVM *vm = NULL;
    result = create(&vm, (void **)&env, &args);
    for (jsize i = 0; i < count; i++) free(opts[i].optionString); free(opts);
    if (result != JNI_OK) { fprintf(stderr, "JNI_CreateJavaVM failed: %d\n", result); goto cleanup; }
    pthread_mutex_lock(&vm_lock); hotspot_vm = vm; vm_ready = 1; pthread_mutex_unlock(&vm_lock);
    jclass main = (*env)->FindClass(env, "dev/mobile/runelite/HostMain");
    if (!main || describe(env)) { result = -3; goto cleanup; }
    jmethodID method = (*env)->GetStaticMethodID(env, main, "main", "([Ljava/lang/String;)V");
    jclass string = (*env)->FindClass(env, "java/lang/String");
    jobjectArray empty = (*env)->NewObjectArray(env, 0, string, NULL);
    if (method) (*env)->CallStaticVoidMethod(env, main, method, empty);
    result = describe(env) ? -4 : 0;
    fprintf(stderr, "RuneLite main returned: %d (AWT continues on its own threads)\n", result);
    (*vm)->DetachCurrentThread(vm);
cleanup:
    (*art)->ReleaseStringUTFChars(art, native_dir, dir);
    (*art)->ReleaseStringUTFChars(art, runtime_dir, runtime);
    (*art)->ReleaseStringUTFChars(art, home, user);
    (*art)->ReleaseStringUTFChars(art, log, logfile);
    return result;
}

JNIEXPORT jstring JNICALL Java_dev_mobile_runelite_NativeBridge_settings(JNIEnv *art,jclass cls,jstring request) {
    JNIEnv *env=runtime_env();
    if(!env || (*env)->PushLocalFrame(env,8)!=JNI_OK)return (*art)->NewStringUTF(art,"{\"error\":\"Client is not ready\"}");
    const char *text=(*art)->GetStringUTFChars(art,request,NULL);
    jstring input=(*env)->NewStringUTF(env,text);
    (*art)->ReleaseStringUTFChars(art,request,text);
    jstring result=NULL;
    jclass host=(*env)->FindClass(env,"dev/mobile/runelite/MobileSettings");
    if(host) {
        jmethodID method=(*env)->GetStaticMethodID(env,host,"request","(Ljava/lang/String;)Ljava/lang/String;");
        if(method)result=(*env)->CallStaticObjectMethod(env,host,method,input);
    }
    jstring output=NULL;
    if(!describe(env) && result) {
        const char *value=(*env)->GetStringUTFChars(env,result,NULL);
        output=(*art)->NewStringUTF(art,value);
        (*env)->ReleaseStringUTFChars(env,result,value);
    }
    (*env)->PopLocalFrame(env,NULL);
    return output?output:(*art)->NewStringUTF(art,"{\"error\":\"Settings bridge failed\"}");
}

/* Pull notification text across the two VMs using UTF-16 copies, never JNI refs. */
JNIEXPORT jstring JNICALL Java_dev_mobile_runelite_NativeBridge_notification(JNIEnv *art,jclass cls) {
    JNIEnv *env=runtime_env();
    if(!env || (*env)->PushLocalFrame(env,8)!=JNI_OK)return NULL;
    jstring result=NULL;
    jclass host=(*env)->FindClass(env,"dev/mobile/runelite/MobileNotifications");
    if(host && !describe(env)) {
        jmethodID method=(*env)->GetStaticMethodID(env,host,"poll","()Ljava/lang/String;");
        jstring text=method?(jstring)(*env)->CallStaticObjectMethod(env,host,method):NULL;
        if(!describe(env) && text) {
            jsize length=(*env)->GetStringLength(env,text);
            const jchar *chars=(*env)->GetStringChars(env,text,NULL);
            if(chars) {result=(*art)->NewString(art,chars,length);(*env)->ReleaseStringChars(env,text,chars);}
        }
    }
    describe(env);(*env)->PopLocalFrame(env,NULL);return result;
}
