#pragma once
#include <stdint.h>
typedef struct ANativeWindow {int references,width,height,connected;} ANativeWindow;
void ANativeWindow_acquire(ANativeWindow *);
void ANativeWindow_release(ANativeWindow *);
int32_t ANativeWindow_setBuffersGeometry(ANativeWindow *,int32_t,int32_t,int32_t);
