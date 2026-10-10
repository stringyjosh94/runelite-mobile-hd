/* Targeted GLSL 330 -> 150 adaptation. SPDX-License-Identifier: BSD-2-Clause
 * Preserve locations through actual advertised ARB extensions, rather than
 * changing GL/GLSL version strings. Compute/high-version shaders are untouched. */
static int has_extension(const char *name) {
    void (*get_integer)(GLenum,GLint *)=get_proc("glGetIntegerv");
    const GLubyte *(*get_string_i)(GLenum,GLuint)=get_proc("glGetStringi");
    if(!get_integer || !get_string_i) return 0;
    GLint count=0;get_integer(GL_NUM_EXTENSIONS,&count);
    for(GLint i=0;i<count;i++) {
        const char *extension=(const char *)get_string_i(GL_EXTENSIONS,(GLuint)i);
        if(extension && !strcmp(extension,name)) return 1;
    }
    return 0;
}
static void adapted_shader_source(GLuint shader,GLsizei count,const GLchar *const *strings,const GLint *lengths) {
    void (*original)(GLuint,GLsizei,const GLchar *const *,const GLint *)=get_proc("glShaderSource");
    if(!original) return;
    size_t total=0;
    for(GLsizei i=0;i<count;i++) {
        size_t n=lengths && lengths[i]>=0 ? (size_t)lengths[i] : strlen(strings[i]);
        if(n>16*1024*1024 || total>16*1024*1024-n) {original(shader,count,strings,lengths);return;} total+=n;
    }
    char *source=malloc(total+1);if(!source) {original(shader,count,strings,lengths);return;}
    size_t at=0;
    for(GLsizei i=0;i<count;i++) {size_t n=lengths && lengths[i]>=0 ? (size_t)lengths[i] : strlen(strings[i]);memcpy(source+at,strings[i],n);at+=n;}
    source[at]=0;
    char *version=strstr(source,"#version");
    int requested=0;
    if(!version || sscanf(version,"#version %d",&requested)!=1 || requested!=330) {
        free(source);original(shader,count,strings,lengths);return;
    }
    if(!has_extension("GL_ARB_explicit_attrib_location")) {
        fprintf(stderr,"GLSL adapter: cannot adapt shader %u: driver does not advertise GL_ARB_explicit_attrib_location\n",shader);fflush(stderr);
        free(source);original(shader,count,strings,lengths);return;
    }
    const char *end=strchr(version,'\n');if(!end) end=source+total;else end++;
    char header[2048]="#version 150\n#extension GL_ARB_explicit_attrib_location : require\n";
    const char *optional[]={"GL_ARB_shading_language_420pack","GL_ARB_shader_storage_buffer_object","GL_ARB_gpu_shader5",
        "GL_ARB_shader_bit_encoding","GL_ARB_texture_gather","GL_ARB_explicit_uniform_location"};
    for(unsigned int i=0;i<sizeof(optional)/sizeof(optional[0]);i++) if(has_extension(optional[i])) {
        strcat(header,"#extension ");strcat(header,optional[i]);strcat(header," : enable\n");
    }
    size_t prefix=(size_t)(version-source),suffix=total-(size_t)(end-source),head=strlen(header);
    char *adapted=malloc(prefix+head+suffix+1);
    if(!adapted) {free(source);original(shader,count,strings,lengths);return;}
    memcpy(adapted,source,prefix);memcpy(adapted+prefix,header,head);memcpy(adapted+prefix+head,end,suffix);adapted[prefix+head+suffix]=0;
    const GLchar *replacement=adapted;GLint size=(GLint)(prefix+head+suffix);
    fprintf(stderr,"GLSL adapter: shader %u requested 330 -> 150; advertised extensions enabled\n",shader);fflush(stderr);
    original(shader,1,&replacement,&size);
    free(adapted);free(source);
}
