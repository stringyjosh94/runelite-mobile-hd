# Where to change the project

| Location | Purpose |
|---|---|
| `app/AndroidManifest.xml` | App version/name/icon, permissions and Android requirements |
| `app/res/` | Packaged Android icon and resources |
| `app/src/android/.../MainActivity.java` | Setup, app menus, gestures, keyboard and display mapping |
| `app/src/android/.../GameInterfaceControls.java` | Interface-size and screen-layout dialogs |
| `app/src/android/.../StoredLogin.java` | Remove the saved login and recovery files |
| `app/src/android/.../ViewportSizing.java` | Logical viewport size and sidebar magnification |
| `app/src/host/.../HostMain.java` | Swing composition, canvas bounds and frame ownership |
| `app/src/host/.../MobilePointerInput.java` | Input dispatch and menu selection |
| `app/src/host/.../MobileCameraInput.java` | Camera movement and angle wrapping |
| `app/src/host/.../MobileWidgetHitTest.java` | Scene versus interface hit testing |
| `app/src/host/.../MobileHdProfiles.java` | Shared Low / Medium / High settings |
| `app/src/host/.../GlesPluginBootstrap.java` | Register and start the bundled HD renderer |
| `app/src/native/bridge.c` | Android ↔ embedded JVM bridge and native window ownership |
| `app/src/native/gpu.c`, `gles_backend.h` | EGL contexts, Surface presentation and copied fallback |
| `app/src/native/lwjgl-gles/` | GLES native binding source |
| `hd-plugin/src/main/` | Modified 117HD renderer, shaders and scene resources |
| `hd-plugin/compile-support/` | Compile-time generated/API support |
| `hd-plugin/native/` | Shader translator and vendored compiler sources |
| `tests/` | Java/native desktop regression checks |
| `tools/` | Build, tests and distribution packaging |
| `branding/` | Icon master, original reference and attribution |
| `app/licenses/`, `hd-plugin/LICENSE` | Third-party licence notices; preserve them |
| `app/DEPENDENCIES.json` | Upstream dependency versions/provenance |
| `runtime-manifest.json`, `runtime-pack.sha256` | Matching external dependency pack and integrity data |

`app/assets/`, `app/lib/` and `hd-plugin/tools/` are restored dependencies or generated payloads and are ignored by Git. `build/` and `dist/` are generated output. They can exist locally without belonging in a source commit.

Read [Architecture](ARCHITECTURE.md) before changing graphics or VM integration. Do not modify vendored third-party code just to match this repository's documentation style. Its own notices and build instructions are retained.

Most files in this source tree belong to the vendored glslang/SPIRV-Cross compiler sources. They are retained so contributors can rebuild the native translator and preserve upstream notices. The small project-specific entry points are listed above.
