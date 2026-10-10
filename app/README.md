# Android application and bridge

`src/android` runs on Android and handles setup, menus, input, keyboard and the app window. `src/host` runs inside the embedded Java VM and connects RuneLite's Swing UI to Android. `src/native` provides JNI, EGL/GLES and display transfer. `res` contains the launcher icon/resources. `licenses` and `DEPENDENCIES.json` record dependency notices/provenance.

Build from the repository root using [docs/BUILDING.md](../docs/BUILDING.md). The restored `assets` and `lib` folders are ignored dependencies, not files to upload to Git. Never copy a device's home/runtime directory into this folder.
