# Modified 117HD GLES renderer

This folder adapts [117HD](https://github.com/117HD/RLHD) at commit `6430e99d76bd4d59993591e98bd65211a4b6986c` for the app's Android GLES renderer. Original copyright notices and licences are retained.

Java and shader sources are in `src/main`; compiler support is in `compile-support`; the native shader translator and vendored compiler dependencies are in `native`. `tools` and `build` are ignored restored/generated files.

Use the root [build guide](../docs/BUILDING.md), [architecture guide](../docs/ARCHITECTURE.md) and [graphics profiles](../docs/GRAPHICS-PROFILES.md). This fork is not the upstream desktop plugin and should not be described as upstream-certified mobile support.
