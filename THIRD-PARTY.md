# Dependency provenance and licences

[CREDITS.md](CREDITS.md) links the main upstream projects and explains their roles. Existing source headers and licence files are retained. The root BSD licence does not replace any upstream terms.

| Component | Pinned provenance | Notices/source |
|---|---|---|
| RuneLite | 1.13.1; original bootstrap artifact URLs/hashes in `app/DEPENDENCIES.json` | `app/licenses/RuneLite-BSD-2-Clause.txt`; [source](https://github.com/runelite/runelite) |
| Game/injected client | Pinned RuneLite-distributed injected client; game content is distinct from RuneLite's BSD code | Jagex rights/terms apply; do not label all game content BSD |
| Modified 117HD | Base commit `6430e99d76bd4d59993591e98bd65211a4b6986c` | `hd-plugin/LICENSE`, file headers; [base source](https://github.com/117HD/RLHD/tree/6430e99d76bd4d59993591e98bd65211a4b6986c) |
| Android runtime payload | FCL 1.3.3.7, source commit `34344b9b637839c73f2856615021f7a8ee7ba9d8`; APK hash in metadata | `app/licenses/FCL-GPL-3.0.txt`; [FCL source](https://github.com/FCL-Team/FoldCraftLauncher/tree/34344b9b637839c73f2856615021f7a8ee7ba9d8) |
| OpenJDK | Embedded 17.0.10-internal from the pinned runtime payload | Restored `app/assets/jre/legal/`; [OpenJDK](https://openjdk.org/), [Android build project](https://github.com/FCL-Team/Android-OpenJDK-Build) |
| Caciocavallo | 1.19.1-SNAPSHOT payload, Android adaptation lineage | `app/licenses/Caciocavallo*-Classpath.txt`; [FCL adaptation](https://github.com/FCL-Team/caciocavallo17) |
| LWJGL | 3.3.3 Java/native bindings with platform adaptation | `app/licenses/LWJGL-GLES-LICENSE.txt`, native headers; [source](https://github.com/LWJGL/lwjgl3/tree/3.3.3) |
| glslang | 15.1.0 | Vendored `hd-plugin/native/vendor/glslang` and its licences; [source](https://github.com/KhronosGroup/glslang/tree/15.1.0) |
| SPIRV-Cross | `vulkan-sdk-1.3.290.0` | Vendored `hd-plugin/native/vendor/spirv-cross`; [source](https://github.com/KhronosGroup/SPIRV-Cross/tree/vulkan-sdk-1.3.290.0) |
| Mesa/Zink | Retained library from FCL payload | `app/licenses/Mesa-OSMesa-notice.txt`; [Mesa](https://www.mesa3d.org/) |
| JNA | 5.9.0 | Dependency metadata and `app/licenses/jars/`; [source](https://github.com/java-native-access/jna) |
| Lombok | 1.18.30, compile-time dependency | [source/licence](https://github.com/projectlombok/lombok) |
| Noto font | Bundled runtime font | `app/licenses/Noto-OFL.txt` |
| Other Java libraries | Original artifact records in metadata, actual pack hashes in `runtime-manifest.json` | `app/licenses/jars/` and JAR notices |

The Android launcher currently exposes native GLES HD only. Retained Mesa/desktop binding files do not imply a supported Standard GPU or VirGL mode. A retained VirGL notice documents dependency history; its old test server is not packaged.

## What the hashes mean

`runtime-manifest.json` identifies each file in the v35 external dependency pack. `runtime-pack.sha256` identifies the complete ZIP. Generated app/HD/bridge outputs are rebuilt from source rather than restored as dependencies. Original bootstrap metadata can mention earlier LWJGL artifacts; the actual snapshot uses 3.3.3.

## Distribution review still required

Preserving notices is necessary but may not be sufficient. Before publishing runtime binaries or APKs, verify redistribution terms and exact corresponding-source obligations for OpenJDK/Caciocavallo/FCL-derived and other native components. Matching source completeness for every prebuilt runtime binary has not been established. Do not describe upstream home-page links as a completed source offer or licence audit. Brand/game-content permissions require separate consideration. See [LEGAL.md](LEGAL.md).
