// SPDX-License-Identifier: BSD-2-Clause
#include <jni.h>
#include <glslang/Public/ShaderLang.h>
#include <glslang/Public/ResourceLimits.h>
#include <SPIRV/GlslangToSpv.h>
#include <spirv_glsl.hpp>
#include <memory>
#include <mutex>
#include <regex>
#include <string>
#include <vector>
#include <stdexcept>

extern "C" JNIEXPORT jobjectArray JNICALL
Java_dev_mobile_rlhd_mobile_ShaderTranslator_translate0(JNIEnv *env,jclass,jobjectArray input,jintArray kinds) {
 try {
  static std::once_flag initialized;
  std::call_once(initialized,[]{if(!glslang::InitializeProcess()) throw std::runtime_error("glslang initialization failed");});
  jsize count=env->GetArrayLength(input);
  if(count!=env->GetArrayLength(kinds)) throw std::runtime_error("Shader stage/source count mismatch");
  std::vector<jint> types(count);env->GetIntArrayRegion(kinds,0,count,types.data());
  std::vector<std::unique_ptr<glslang::TShader>> shaders;
  glslang::TProgram program;
  EShMessages messages=EShMessages(EShMsgSpvRules);
  for(int i=0;i<count;i++) {
   EShLanguage stage=types[i]==0x8B31?EShLangVertex:types[i]==0x8B30?EShLangFragment:types[i]==0x8DD9?EShLangGeometry:types[i]==0x91B9?EShLangCompute:EShLangCount;
   if(stage==EShLangCount) throw std::runtime_error("Unsupported shader stage");
   auto text=(jstring)env->GetObjectArrayElement(input,i);
   const char *chars=env->GetStringUTFChars(text,nullptr);std::string source(chars);
   env->ReleaseStringUTFChars(text,chars);env->DeleteLocalRef(text);
   source=std::regex_replace(source,std::regex("#version[ \\t]+320[ \\t]+es"),"#version 450 core");
   auto shader=std::make_unique<glslang::TShader>(stage);
   const char *str=source.c_str();shader->setStrings(&str,1);
   shader->setEnvInput(glslang::EShSourceGlsl,stage,glslang::EShClientOpenGL,450);
   shader->setEnvClient(glslang::EShClientOpenGL,glslang::EShTargetOpenGL_450);
   shader->setEnvTarget(glslang::EShTargetSpv,glslang::EShTargetSpv_1_0);
   shader->setAutoMapBindings(true);shader->setAutoMapLocations(true);
   if(!shader->parse(GetDefaultResources(),450,false,messages)) throw std::runtime_error(std::string("Desktop shader parse: ")+shader->getInfoLog());
   program.addShader(shader.get());shaders.push_back(std::move(shader));
  }
  if(!program.link(messages) || !program.mapIO()) throw std::runtime_error(std::string("Shader interface link: ")+program.getInfoLog());
  jclass stringClass=env->FindClass("java/lang/String");
  jobjectArray output=env->NewObjectArray(count,stringClass,nullptr);
  for(int i=0;i<count;i++) {
   std::vector<unsigned int> spirv;
   glslang::SpvOptions options;options.disableOptimizer=true;
   glslang::GlslangToSpv(*program.getIntermediate(shaders[i]->getStage()),spirv,&options);
   spirv_cross::CompilerGLSL compiler(std::move(spirv));
   auto resources=compiler.get_shader_resources();
   // Uniform locations are program-wide in GLES. Let its linker assign them;
   // retain explicit vertex/fragment interface locations from the linked SPIR-V.
   for(auto *list : {&resources.sampled_images,&resources.separate_images,&resources.separate_samplers,&resources.storage_images,&resources.gl_plain_uniforms,&resources.uniform_buffers,&resources.storage_buffers})
    for(const auto &resource:*list) {
     compiler.unset_decoration(resource.id,spv::DecorationLocation);
     compiler.unset_decoration(resource.id,spv::DecorationBinding);
    }
   auto opts=compiler.get_common_options();opts.version=320;opts.es=true;
   opts.fragment.default_float_precision=spirv_cross::CompilerGLSL::Options::Highp;
   opts.fragment.default_int_precision=spirv_cross::CompilerGLSL::Options::Highp;
   opts.enable_420pack_extension=false;opts.vertex.fixup_clipspace=false;
   compiler.set_common_options(opts);
   std::string result=compiler.compile();
   jstring translated=env->NewStringUTF(result.c_str());env->SetObjectArrayElement(output,i,translated);env->DeleteLocalRef(translated);
  }
  return output;
 } catch(const std::exception &error) {
  env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),error.what());return nullptr;
 }
}
