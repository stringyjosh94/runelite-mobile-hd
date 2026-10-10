/* Bounded GLES3 PBO readback. Poll fences first; backpressure when both slots are busy. BSD-2-Clause. */
static PFNGLGENBUFFERSPROC rb_gen;
static PFNGLDELETEBUFFERSPROC rb_delete;
static PFNGLBINDBUFFERPROC rb_bind;
static PFNGLBUFFERDATAPROC rb_data;
static PFNGLMAPBUFFERRANGEPROC rb_map;
static PFNGLUNMAPBUFFERPROC rb_unmap;
static PFNGLFENCESYNCPROC rb_fence;
static PFNGLCLIENTWAITSYNCPROC rb_wait;
static PFNGLDELETESYNCPROC rb_delete_sync;
static PFNGLGETINTEGERVPROC rb_get;
static PFNGLREADPIXELSPROC rb_read;
static PFNGLBINDFRAMEBUFFERPROC rb_framebuffer;
static PFNGLPIXELSTOREIPROC rb_pack;
static PFNGLFLUSHPROC rb_flush;
static PFNGLGETERRORPROC rb_error;
static void readback_reset(Context *c) {
    if(rb_delete_sync)for(int i=0;i<2;i++)if(c->readback_fences[i])rb_delete_sync(c->readback_fences[i]);
    if(rb_delete && c->readback_buffers[0])rb_delete(2,c->readback_buffers);
    memset(c->readback_buffers,0,sizeof(c->readback_buffers));
    memset(c->readback_fences,0,sizeof(c->readback_fences));
    c->readback_width=c->readback_height=0;
}
static int readback_fail(Context *c,const char *reason) {
    fprintf(stderr,"GLES async readback disabled: %s; using synchronous readback\n",reason);fflush(stderr);
    readback_reset(c);c->readback_failed=1;return -1;
}
static int readback_init(Context *c) {
#define RB_LOAD(variable,type,name) variable=(type)get_proc(name);if(!variable)return readback_fail(c,name " unavailable")
    RB_LOAD(rb_gen,PFNGLGENBUFFERSPROC,"glGenBuffers");
    RB_LOAD(rb_delete,PFNGLDELETEBUFFERSPROC,"glDeleteBuffers");
    RB_LOAD(rb_bind,PFNGLBINDBUFFERPROC,"glBindBuffer");
    RB_LOAD(rb_data,PFNGLBUFFERDATAPROC,"glBufferData");
    RB_LOAD(rb_map,PFNGLMAPBUFFERRANGEPROC,"glMapBufferRange");
    RB_LOAD(rb_unmap,PFNGLUNMAPBUFFERPROC,"glUnmapBuffer");
    RB_LOAD(rb_fence,PFNGLFENCESYNCPROC,"glFenceSync");
    RB_LOAD(rb_wait,PFNGLCLIENTWAITSYNCPROC,"glClientWaitSync");
    RB_LOAD(rb_delete_sync,PFNGLDELETESYNCPROC,"glDeleteSync");
    RB_LOAD(rb_get,PFNGLGETINTEGERVPROC,"glGetIntegerv");
    RB_LOAD(rb_read,PFNGLREADPIXELSPROC,"glReadPixels");
    RB_LOAD(rb_framebuffer,PFNGLBINDFRAMEBUFFERPROC,"glBindFramebuffer");
    RB_LOAD(rb_pack,PFNGLPIXELSTOREIPROC,"glPixelStorei");
    RB_LOAD(rb_flush,PFNGLFLUSHPROC,"glFlush");
    RB_LOAD(rb_error,PFNGLGETERRORPROC,"glGetError");
#undef RB_LOAD
    readback_reset(c);
    GLint previous=0;rb_get(GL_PIXEL_PACK_BUFFER_BINDING,&previous);
    rb_gen(2,c->readback_buffers);
    for(int i=0;i<2;i++) {rb_bind(GL_PIXEL_PACK_BUFFER,c->readback_buffers[i]);rb_data(GL_PIXEL_PACK_BUFFER,(GLsizeiptr)c->width*c->height*4,NULL,GL_STREAM_READ);}
    rb_bind(GL_PIXEL_PACK_BUFFER,(GLuint)previous);
    if(rb_error()!=GL_NO_ERROR)return readback_fail(c,"PBO allocation failed or prior GL error");
    c->readback_width=c->width;c->readback_height=c->height;
    fprintf(stderr,"GLES async readback: two bounded PBOs, poll-first fences with backpressure, %dx%d\n",c->width,c->height);fflush(stderr);
    return 1;
}
static int async_readback(Context *c) {
    if(c->readback_failed)return -1;
    const char *mode=getenv("RUNELITE_GLES_READBACK");
    if(mode && !strcmp(mode,"sync"))return readback_fail(c,"compatibility mode selected");
    if(c->readback_width!=c->width || c->readback_height!=c->height)if(readback_init(c)<0)return -1;
    GLint previous=0;rb_get(GL_PIXEL_PACK_BUFFER_BINDING,&previous);
    int latest=-1;
    for(int i=0;i<2;i++)if(c->readback_fences[i]) {
        GLenum status=rb_wait(c->readback_fences[i],0,0);
        if(status==GL_WAIT_FAILED)return readback_fail(c,"fence poll failed");
        if(status==GL_ALREADY_SIGNALED || status==GL_CONDITION_SATISFIED) {
            rb_delete_sync(c->readback_fences[i]);c->readback_fences[i]=NULL;
            if(latest<0 || c->readback_sequence[i]>c->readback_sequence[latest])latest=i;
        }
    }
    // Pbuffer rendering has no window-swap pacing. Do not let removing glFinish
    // create an ever-growing GPU command queue on a slow device.
    if(latest<0 && c->readback_fences[0] && c->readback_fences[1]) {
        int oldest=c->readback_sequence[0]<c->readback_sequence[1]?0:1;
        GLenum status=rb_wait(c->readback_fences[oldest],GL_SYNC_FLUSH_COMMANDS_BIT,1000000000ULL);
        if(status==GL_WAIT_FAILED)return readback_fail(c,"backpressure fence failed");
        if(status==GL_ALREADY_SIGNALED || status==GL_CONDITION_SATISFIED) {
            rb_delete_sync(c->readback_fences[oldest]);c->readback_fences[oldest]=NULL;latest=oldest;
        }
    }
    if(latest>=0) {
        rb_bind(GL_PIXEL_PACK_BUFFER,c->readback_buffers[latest]);
        void *data=rb_map(GL_PIXEL_PACK_BUFFER,0,(GLsizeiptr)c->width*c->height*4,GL_MAP_READ_BIT);
        if(!data) {rb_bind(GL_PIXEL_PACK_BUFFER,previous);return readback_fail(c,"completed PBO map failed");}
        memcpy(c->pixels,data,(size_t)c->width*c->height*4);
        GLboolean valid=rb_unmap(GL_PIXEL_PACK_BUFFER);rb_bind(GL_PIXEL_PACK_BUFFER,previous);
        if(!valid)return readback_fail(c,"PBO content invalidated");
    }
    int slot=-1;for(int i=0;i<2;i++)if(!c->readback_fences[i]) {slot=i;break;}
    if(slot>=0) {
        GLint framebuffer,alignment,row_length,skip_rows,skip_pixels;
        rb_get(GL_READ_FRAMEBUFFER_BINDING,&framebuffer);rb_get(GL_PACK_ALIGNMENT,&alignment);
        rb_get(GL_PACK_ROW_LENGTH,&row_length);rb_get(GL_PACK_SKIP_ROWS,&skip_rows);rb_get(GL_PACK_SKIP_PIXELS,&skip_pixels);
        rb_framebuffer(GL_READ_FRAMEBUFFER,0);rb_bind(GL_PIXEL_PACK_BUFFER,c->readback_buffers[slot]);
        rb_pack(GL_PACK_ALIGNMENT,4);rb_pack(GL_PACK_ROW_LENGTH,0);rb_pack(GL_PACK_SKIP_ROWS,0);rb_pack(GL_PACK_SKIP_PIXELS,0);
        rb_read(0,0,c->width,c->height,GL_RGBA,GL_UNSIGNED_BYTE,0);
        c->readback_fences[slot]=rb_fence(GL_SYNC_GPU_COMMANDS_COMPLETE,0);
        c->readback_sequence[slot]=++c->readback_next;
        rb_bind(GL_PIXEL_PACK_BUFFER,previous);rb_framebuffer(GL_READ_FRAMEBUFFER,framebuffer);
        rb_pack(GL_PACK_ALIGNMENT,alignment);rb_pack(GL_PACK_ROW_LENGTH,row_length);rb_pack(GL_PACK_SKIP_ROWS,skip_rows);rb_pack(GL_PACK_SKIP_PIXELS,skip_pixels);
        if(!c->readback_fences[slot] || rb_error()!=GL_NO_ERROR)return readback_fail(c,"readback submission failed or prior GL error");
        rb_flush();
    } else c->readback_skipped++;
    return latest>=0;
}
static void presentation_stats(Context *c) {
    if(c->swaps==30 || c->swaps%120==0) {
        fprintf(stderr,"GPU presentation: swaps=%u published=%u %dx%d; CPU readback/poll=%.2fms, frame conversion/publish=%.2fms; pending skips=%u; mode=%s\n",c->swaps,c->frames,c->width,c->height,c->wait_ms/c->swaps,c->frames?c->copy_ms/c->frames:0,c->readback_skipped,c->es&&!c->readback_failed?"async GLES":"synchronous");fflush(stderr);
    }
}
static void synchronous_gles_readback(Context *c) {
    PFNGLGETINTEGERVPROC get=(PFNGLGETINTEGERVPROC)get_proc("glGetIntegerv");
    PFNGLBINDBUFFERPROC buffer=(PFNGLBINDBUFFERPROC)get_proc("glBindBuffer");
    PFNGLBINDFRAMEBUFFERPROC framebuffer=(PFNGLBINDFRAMEBUFFERPROC)get_proc("glBindFramebuffer");
    PFNGLPIXELSTOREIPROC pack=(PFNGLPIXELSTOREIPROC)get_proc("glPixelStorei");
    PFNGLREADPIXELSPROC read=(PFNGLREADPIXELSPROC)get_proc("glReadPixels");
    GLint old_buffer,old_framebuffer,alignment,row_length,skip_rows,skip_pixels;
    get(GL_PIXEL_PACK_BUFFER_BINDING,&old_buffer);get(GL_READ_FRAMEBUFFER_BINDING,&old_framebuffer);
    get(GL_PACK_ALIGNMENT,&alignment);get(GL_PACK_ROW_LENGTH,&row_length);get(GL_PACK_SKIP_ROWS,&skip_rows);get(GL_PACK_SKIP_PIXELS,&skip_pixels);
    buffer(GL_PIXEL_PACK_BUFFER,0);framebuffer(GL_READ_FRAMEBUFFER,0);
    pack(GL_PACK_ALIGNMENT,4);pack(GL_PACK_ROW_LENGTH,0);pack(GL_PACK_SKIP_ROWS,0);pack(GL_PACK_SKIP_PIXELS,0);
    read(0,0,c->width,c->height,GL_RGBA,GL_UNSIGNED_BYTE,c->pixels);
    buffer(GL_PIXEL_PACK_BUFFER,old_buffer);framebuffer(GL_READ_FRAMEBUFFER,old_framebuffer);
    pack(GL_PACK_ALIGNMENT,alignment);pack(GL_PACK_ROW_LENGTH,row_length);pack(GL_PACK_SKIP_ROWS,skip_rows);pack(GL_PACK_SKIP_PIXELS,skip_pixels);
}
