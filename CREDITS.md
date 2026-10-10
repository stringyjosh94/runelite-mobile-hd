# Credits and project origins

The original projects and their contributors made this experiment possible. Please use their official repositories for upstream information, licences and contributions. Problems specific to this Android adaptation should be reported here rather than assigned to upstream maintainers.

## Project-specific work

The maintainer supplied the concept, requirements, feedback and limited hands-on device testing, motivated by a love of the game. AI coding tools generated the project-specific implementation, adaptations, integration work and documentation. The launcher icon was adapted using an AI image editor. This is not a claim that the maintainer authored the upstream software or manually wrote the integration code. Neither AI generation nor successful compilation establishes correctness; review and device testing are still needed.

## Main projects

| Project | Contribution |
|---|---|
| [RuneLite](https://github.com/runelite/runelite) | Desktop client, plugin APIs, Plugin Hub integration and supporting libraries |
| [117HD / RLHD](https://github.com/117HD/RLHD) | Original HD renderer, lighting, materials, scene processing and shaders; base of the GLES fork |
| [zeruth/runelite-android](https://github.com/zeruth/runelite-android) | Reference for Android touch behaviour and desktop credential import; this app uses its own Swing bridge integration |
| [OpenJDK](https://openjdk.org/) | Java virtual machine and class libraries |
| [Caciocavallo](https://github.com/CaciocavalloSilano/caciocavallo) | Headless AWT/Swing toolkit |
| [FCL-Team/caciocavallo17](https://github.com/FCL-Team/caciocavallo17) and [PojavLauncherTeam/caciocavallo17](https://github.com/PojavLauncherTeam/caciocavallo17) | Android-adapted Java UI toolkit lineage |
| [Fold Craft Launcher](https://github.com/FCL-Team/FoldCraftLauncher) | Source of the pinned Android runtime/native dependency payload |
| [FCL Android OpenJDK build](https://github.com/FCL-Team/Android-OpenJDK-Build) | Android Java runtime build project; exact binary source matching still requires verification |
| [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) | Upstream Android Java-runtime/launcher ecosystem credited through the FCL dependency lineage |
| [LWJGL](https://github.com/LWJGL/lwjgl3) | Java/native graphics bindings |
| [RuneLite rlawt](https://github.com/runelite/rlawt) | AWT graphics-context interface used by the renderer |
| [glslang](https://github.com/KhronosGroup/glslang) | Shader compiler |
| [SPIRV-Cross](https://github.com/KhronosGroup/SPIRV-Cross) | Converts intermediate shaders to GLES shader source |
| [Mesa](https://www.mesa3d.org/) | Retained Mesa/Zink runtime dependency; the current HD renderer uses vendor GLES |
| [JNA](https://github.com/java-native-access/jna) | Java/native integration used by dependencies |
| [Project Lombok](https://projectlombok.org/) | Compile-time Java code generation |
| [Noto fonts](https://github.com/notofonts) | Bundled font; see its OFL notice |

RuneLite's additional Java dependencies retain their notices under `app/licenses/jars/`. Version and artifact details are recorded in `app/DEPENDENCIES.json` and `runtime-manifest.json`. Copyright notices in copied and modified upstream source are retained.

## Artwork and game

Old School RuneScape and its game content belong to Jagex and the relevant rights holders. The launcher artwork derives from [RuneLite's supplied logo](https://runelite.net/img/runelite_logo_transparent_small.webp). See [branding notes](branding/README.md). Credits do not imply endorsement or grant redistribution rights.
