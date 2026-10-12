# Build an APK from source

Building is for contributors. Players only need the APK. The scripts currently target a **Linux x86-64 host**; Windows contributors can use a suitable WSL/Linux environment. Android Studio's Run button is not configured.

## Install tools

- Python 3.10 or newer, Git and a C compiler for desktop tests.
- JDK 17.
- Android SDK Platform 34, Build Tools 34.0.0 and NDK 25.2.9519653.
- The matching `runelite-mobile-hd-runtime-v35.zip` release asset.
- CMake 3.22+ and Ninja only when rebuilding the shader translator.

Set `JAVA_HOME` and `ANDROID_SDK_ROOT` if their locations differ from the defaults, or supply `--jdk` / `--sdk`. The default JDK is `/usr/lib/jvm/java-17-openjdk`; the default SDK is `$HOME/Android/Sdk`.

## First build

Open a terminal in the repository root (the folder containing README.md and tools):

```sh
python3 tools/build.py --dependencies /path/to/runelite-mobile-hd-runtime-v35.zip --doctor
python3 tools/build.py
```

Replace the pack path with the download's real location. The first command verifies/restores the matching pack and checks required files. It does not compile the app. The second compiles the HD Java fork, host bridge, Android code, native presentation/input bindings and Android resources, then signs the APK.

Output: `dist/runelite-mobile-hd-v36.4-touch-arm64-v8a.apk`. v36.4 reuses the v35 dependency pack; no new runtime download is required.

The pack supplies pinned OpenJDK/Cacio/RuneLite/LWJGL dependencies, ARM64 libraries, font/legal files and Lombok. It contains no credentials/signing key and does not fetch arbitrary newer client versions. Generated host/HD jars and presentation bindings are rebuilt from this repository. Restore a matching pack into a fresh folder; do not use a phone's app-data directory.

To rebuild the bundled native shader translator too:

```sh
python3 tools/build.py --rebuild-native
```

This compiles vendored glslang/SPIRV-Cross and can take longer. The normal build uses the pinned prebuilt translator from the dependency pack.

## Architectures and newer Android devices

The default is ARM64. `--abi` accepts the four current NDK architectures, but only ARM64 runtime dependencies are supplied. For another ABI use `--runtime-root` with a complete matching Android Java 17/native dependency set. The build checks runtime metadata and ELF machine types before packaging. `--require-16k` rejects any library without 16 KB alignment; the supplied pack does not pass this check yet. See [Android compatibility](ANDROID-COMPATIBILITY.md) for directory layout, commands and limitations.

`--output` changes the output file path. Per-ABI native build directories avoid mixing compiler outputs. ARM64 retains the existing `build/android` signing key location; other architectures use their own build directories and keys.

## Check changes

```sh
python3 tools/test.py
python3 tools/package-source.py --check
```

If this folder came from a source ZIP rather than a Git clone, run `git init -b main` before the source-packaging check.

Desktop tests exercise bridge/input/composition and profile logic; they do not establish Android login, gestures, GPU speed or visual correctness. Follow [Device testing](DEVICE-TESTING.md). Optional real desktop EGL tests are available through `tools/test-native-gles.py --help`; they need Linux EGL headers/libraries and matching LWJGL Linux natives, which are not in the ARM64 pack.

## Signing

The default build creates `build/android/debug.keystore`, a private local development key. Keep it for subsequent local updates and never commit it. Android debugging is disabled in the app manifest even though the key file/alias has a historical debug name.

A fresh checkout creates a different key and normally cannot update the supplied APK. Use a separate test device/profile or back up your settings before replacing an installation; uninstalling removes app data. The supplied APK retains the maintainer's existing signing identity, which is not included here. Establish and protect a stable release signing process before ongoing public distribution.

## Updating versions or dependencies

Update the manifest version, `tools/build.py` output name and release notes together. Keep the Android package ID and signing identity when update compatibility is required. The native startup diagnostic label is historical and is not the APK version.

Dependency updates require a new runtime snapshot/hash manifest and tests against the changed client APIs/gamepack. Do not silently mix runtime packs or assume pinned game files remain usable after upstream updates. Read [Architecture](ARCHITECTURE.md) and [Third-party components](../THIRD-PARTY.md).
