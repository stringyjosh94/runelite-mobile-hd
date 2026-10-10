# How the app works

## Main parts

Android owns the app window, touch input, file picker and keyboard. An embedded OpenJDK runtime runs the desktop RuneLite Java client. Caciocavallo supplies the AWT/Swing toolkit that RuneLite's desktop interface expects. A native bridge carries input and display information between Android and the embedded Java runtime.

The modified 117HD plugin renders the game with Android's native EGL/OpenGL ES 3.2 driver. It remains a modified desktop renderer, not a complete rewrite of RuneLite into Android widgets. No Termux, proot or separate Linux userspace is required.

## Display paths

**Direct Surface** is the default. Android creates a SurfaceView; the renderer obtains a retained native window and presents with EGL. Ordinary game frames avoid full-scene CPU readback. Swing settings and dialogs still use a CPU-rendered bitmap above the game. Game widgets and RuneLite overlays continue through the renderer's game/UI path.

The Swing compositor leaves a transparent opening over the game canvas, then paints popups and dialogs above it. Android uses the same destination rectangle for the Surface, composition and touch mapping. The usable activity view is filled; a changed aspect ratio can stretch the original logical viewport rather than crop it.

Two copied-frame modes provide fallback. Pipelined mode uses two GPU pixel buffers/fences and a bounded reusable Java frame pool. Synchronous mode reads a frame directly. They preserve graphics quality but can change latency and transfer cost. Select them under Back → Help & advanced → Rendering fallback, then reopen.

Surface creation, destruction, resizing and failures are handled on the render thread with window-generation checks. A failed direct Surface generation can fall back to copied frames. Screenshots still use on-demand readback. Direct presentation does not remove scene, shadow or lighting costs.

## HD fork and shaders

The upstream base is [117HD commit 6430e99](https://github.com/117HD/RLHD/tree/6430e99d76bd4d59993591e98bd65211a4b6986c). The fork uses `dev.mobile.rlhd` and configuration group `mobilehd`, keeping it separate from desktop 117HD. Startup loads the bundled fork and disables competing stock GPU/HD renderers.

Shader sources are compiled through glslang 15.1.0 to SPIR-V, then SPIRV-Cross `vulkan-sdk-1.3.290.0` emits GLES shader source. The compiler runs inside the app. This handles shader-language conversion; it is not a universal translator for arbitrary desktop OpenGL plugins. Unsupported persistent storage, indirect-draw and other desktop paths remain disabled or use explicit fallbacks.

Low and Medium use Efficient shading: diffuse textures/blending, sunlight, ambient light, nearby lights, fog and simple water. They omit expensive material maps, local specular shading and extra terrain/transparency shadow work. High retains richer materials and normal maps. All tiers use bounded local-light counts, no MSAA/POM and disabled wind/character displacement. See [Graphics profiles](GRAPHICS-PROFILES.md).

## Input and configuration

Touch gestures become mouse, wheel and keyboard events understood by the desktop client. Widget hit testing separates camera drags from inventory/interface interaction. Holding and sliding selects a context-menu item on release. Sidebar scrolling and manual magnification assist with desktop controls.

Interface size uses the existing Stretched Mode plugin and clamps enlargement to leave room for game panels. Roomy/Compact under advanced settings changes the logical viewport after reopening. Neither setting is the same as the HD scene-resolution percentage.

The profile policy is shared Java source compiled for Android and the embedded JVM. A selected preset is applied once; later manual edits remain until another preset is selected or a deliberate profile migration occurs. UI-only release v35 retains the v33 profile request identifier.

## Diagnostics

`GLES direct Surface attached` and `no game readback` indicate direct presentation. `Mobile HD timings` reports CPU wall-time preparation/submission/presentation and optional sparse GPU timestamps. CPU wall time can include waits. GPU timestamps are collected without blocking when supported and discarded after disjoint events. These values are diagnostic samples, not an FPS guarantee.

See [Verification](VERIFICATION.md) for what desktop tests establish and [Device testing](DEVICE-TESTING.md) for checks that require real Android hardware.
