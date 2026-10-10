/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2020 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.mobile.rlhd.opengl.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import dev.mobile.rlhd.mobile.*;

import static dev.mobile.rlhd.mobile.MobileGL.*;
import static dev.mobile.rlhd.opengl.shader.ShaderIncludes.SHADER_DUMP_PATH;

@Slf4j
public class ShaderTemplate
{
	private final static String SHADER_TYPE_DEFINE =
		"#define VERTEX_SHADER " + GL_VERTEX_SHADER + '\n' +
		"#define FRAGMENT_SHADER " + GL_FRAGMENT_SHADER + '\n' +
		"#define GEOMETRY_SHADER " + GL_GEOMETRY_SHADER + '\n' +
		"#define COMPUTE_SHADER " + MobileGL.GL_COMPUTE_SHADER + '\n' +
		"#define SHADER_TYPE ";

	private final Map<Integer, String> shaderTypePaths = new HashMap<>();

	public ShaderTemplate add(int type, String name) {
		shaderTypePaths.put(type, name);
		return this;
	}

	public ShaderTemplate remove(int type) {
		shaderTypePaths.remove(type);
		return this;
	}

	public int compile(ShaderIncludes includes) throws ShaderException, IOException {
		int program = glCreateProgram();
		int[] shaders = new int[shaderTypePaths.size()];
		int i = 0;
		boolean ok = false;

		try
		{
			var entries = new java.util.ArrayList<>(shaderTypePaths.entrySet());
            String[] sources = new String[entries.size()];
            int[] stages = new int[entries.size()];
            for (int stageIndex = 0; stageIndex < entries.size(); stageIndex++) {
                var stageEntry = entries.get(stageIndex);
                stages[stageIndex] = stageEntry.getKey();
                sources[stageIndex] = includes.addInclude("SHADER_TYPE", SHADER_TYPE_DEFINE + stageEntry.getKey())
                    .loadFile(stageEntry.getValue());
            }
            String[] translated = dev.mobile.rlhd.mobile.ShaderTranslator.translate0(sources, stages);
            int stageIndex = 0;
            for (var entry : entries) {
                int shader = glCreateShader(entry.getKey());
                if (shader == 0) throw new ShaderException("Unable to create GLES shader");
                String source = translated[stageIndex++];
                java.nio.file.Path dump = java.nio.file.Paths.get(System.getProperty("user.home"), ".runelite", "117hd-gles-shaders", entry.getValue());
                java.nio.file.Files.createDirectories(dump.getParent());
                java.nio.file.Files.writeString(dump, source);

				glShaderSource(shader, source);
				glCompileShader(shader);

				if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
					String error = glGetShaderInfoLog(shader);
					glDeleteShader(shader);
					throw ShaderException.compileError(includes, source, error, entry.getValue());
				}

				glAttachShader(program, shader);
				shaders[i++] = shader;
			}

			glLinkProgram(program);

			String[] paths = shaderTypePaths.values().toArray(String[]::new);
			String combinedName = String.join(" + ", paths);
			if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
				throw ShaderException.compileError(
					includes,
					"// Linking " + combinedName,
					glGetProgramInfoLog(program),
					paths
				);
			}

			ok = true;

			if (SHADER_DUMP_PATH != null) {
				int[] numFormats = { 0 };
				glGetIntegerv(MobileGL.GL_NUM_PROGRAM_BINARY_FORMATS, numFormats);
				if (numFormats[0] < 1) {
					log.error("OpenGL driver does not support any binary formats");
				} else {
					int[] size = { 0 };
					glGetProgramiv(program, MobileGL.GL_PROGRAM_BINARY_LENGTH, size);

					int[] format = { 0 };
					ByteBuffer binary = BufferUtils.createByteBuffer(size[0]);
					MobileGL.glGetProgramBinary(program, size, format, binary);

					SHADER_DUMP_PATH.resolve("binaries", combinedName + ".bin").mkdirs().writeByteBuffer(binary);
				}
			}
		} finally {
			while (i > 0) {
				int shader = shaders[--i];
				glDetachShader(program, shader);
				glDeleteShader(shader);
			}

			if (!ok)
				glDeleteProgram(program);
		}

		return program;
	}
}
