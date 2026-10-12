# Android architectures and versions

## Current support

v36.4 keeps the native GLES 3.2 renderer, direct Android Surface presentation, Low / Medium / High profiles and touch controls. It does not add CPU emulation, Termux or a Linux guest system. The graphics settings remain unchanged. No FPS improvement is claimed without phone measurements.

Only the **ARM64** dependency set is currently supplied. Build options for other CPUs are preparation for future runtime ports, not working releases of those ports.

| Android architecture | Build option | Dependency availability |
|---|---|---|
| 64-bit ARM | `--abi arm64-v8a` | Supplied v35 runtime pack; v36.4 APK builds locally |
| 64-bit Intel/AMD | `--abi x86_64` | Complete matching runtime and native dependencies still needed |
| 32-bit ARM | `--abi armeabi-v7a` | Complete matching runtime and native dependencies still needed; limited address space needs testing |
| 32-bit Intel | `--abi x86` | Complete matching runtime and native dependencies still needed |

A 64-bit CPU running a 32-bit Android installation cannot use the ARM64 APK. These four ABIs are the current NDK choices; obsolete MIPS/ARMv5 and future architectures are outside this work. See [Android's ABI guide](https://developer.android.com/ndk/guides/abis).

## Android versions

- Android 8 / API 26 remains the minimum. Native libraries are compiled against API 26.
- Android 8–10 retain the existing fullscreen flags.
- Android 11 / API 30 and newer use WindowInsetsController for immersive fullscreen, including returning from dialogs or switching apps.
- Android versions and GPU drivers still need device tests. An OS version alone does not establish compatibility.
- The target SDK remains 28. This is a sideloaded compatibility client, not a modern Google Play submission. Raising the target requires a separate audit of embedded-JVM loading, storage, input, window insets and background behaviour.

## Memory sizing

The embedded JVM budgets the heap from Android's reported total RAM and low-memory classification:

| Device condition | JVM maximum heap |
|---|---|
| 32-bit process, Android low-RAM classification, or up to 2 GiB total RAM | 384 MiB |
| More than 2 GiB and up to 4 GiB total RAM | 512 MiB |
| More than 4 GiB, or unknown total RAM on a normal 64-bit device | 768 MiB |

This is a maximum, not an allocation at launch. ART, GPU textures, the embedded VM, native libraries and frame buffers need memory outside that heap. A smaller heap can reduce memory pressure but can also increase garbage collection or run out with many plugins. Test representative Plugin Hub workloads before release. The policy does not change render resolution, texture quality, draw distance or the chosen profile.

## 16 KB memory pages

Android 15 and newer can run on devices using 16 KB pages. The build now inspects every packaged ELF library for the selected CPU and checks load-segment alignment. The project's rebuilt bridge libraries already request 16 KB alignment, but **many supplied OpenJDK, LWJGL and Mesa libraries are still 4 KB aligned**.

The APK records the audit result. On an incompatible page-size configuration it displays an explanation before loading the native bridge instead of attempting to boot HotSpot. This deliberately does not depend on Android's optional page-size compatibility mode. Devices using 4 KB pages can continue with the supplied runtime. `--require-16k` makes packaging fail if any included library is unaligned.

A full fix requires rebuilding/replacing every affected upstream binary and testing HotSpot and native code on a real 16 KB environment. Changing an APK flag or padding ELF headers is insufficient. See [Android's page-size guidance](https://developer.android.com/guide/practices/page-sizes).

## Adding an architecture

Supply a separate directory outside the tracked source tree:

```text
/path/to/runtime/
  jre/
    release
    lib/modules
    native-layout.json
    ...remaining runtime configuration, fonts and legal files...
  lib/
    x86_64/
      libjvm.so
      libjava.so
      ...all Java, JNA, LWJGL and shader translator dependencies...
```

The Java 17 runtime must match Android's libc, CPU and API level, include java.desktop, and work with the Cacio bridge. A desktop Linux JDK is not a substitute. Do not copy ARM64 `lib/modules` into a different runtime: architecture-specific Java configuration also matters. All corresponding licences and source obligations apply to new dependencies.

```sh
python3 tools/build.py --abi x86_64 --runtime-root /path/to/runtime --doctor
python3 tools/build.py --abi x86_64 --runtime-root /path/to/runtime --rebuild-native
```

The native translator is also required by the doctor check; build it first if the new dependency set lacks it:

```sh
ANDROID_HOME=/path/to/Android/Sdk JAVA_HOME=/path/to/jdk17 \
  python3 hd-plugin/scripts/build.py --native --abi x86_64 --runtime-root /path/to/runtime
```

The builder rejects missing libraries, mismatched runtime OS_ARCH and wrong ELF CPU types before packaging. It creates one APK per ABI. A universal APK is intentionally not generated: the Java runtime assets must also be selected by architecture, not just the `lib/` directory. Existing ARM64 client/Cacio jars are shared only where their Java APIs remain compatible.

These checks verify packaging, not dynamic linking completeness, GLES capability, runtime execution or plugin compatibility. Other ABIs must pass native linking and real-device/emulator tests before being advertised as supported.
