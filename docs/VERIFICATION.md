# Verification limits

The current source has been built and the desktop regression suite passed during development. The v35 APK's signing identity, resource compilation, non-debuggable manifest, disabled backups, adaptive icon and absence of credential/key files were checked. These checks are not an independent security or licence audit.

## Desktop checks

`python3 tools/test.py` checks input routing, real pinned-client camera/wheel behaviour, menu selection, sidebar scrolling, text focus, stretch sizing, widget hit testing, frame ownership, Swing composition, native Surface lifecycle doubles, profile switching/light limits, settings compatibility and login-file deletion.

The optional native GLES tool checks copied-frame pixels/orientation/state/lifecycle with desktop EGL/GLES. With a matching Linux shader translator, it also compiles Efficient/rich scene-shader variants and checks diagnostic queries. Fixture scene data is used. Desktop software Mesa is not evidence of Android GPU speed or full-map correctness.

## Requires Android testing

First-run dialogs/file picker, actual account login, keyboard behaviour, physical gestures, Surface composition, home-screen masks, thermal performance, complete map coverage and Plugin Hub compatibility require real devices. Follow [Device testing](DEVICE-TESTING.md). Earlier Pixel/Lenovo observations do not establish every feature of the current release.

## Distribution checks

`python3 tools/package-source.py` exports Git-visible source files and checks for obvious private/generated files and oversized Git files. `tools/package-runtime.py` creates a separate dependency pack and records hashes. Hashes identify a snapshot; they do not prove that dependencies are secure or licensed for every intended distribution.

A clean restore and build must succeed with the published source and matching pack. A newly generated development key will differ from the supplied APK's key, so the resulting APK is not expected to have an identical hash or update the supplied installation.

## Source handover check

The cleaned v35 source archive was extracted into an empty folder, the matching v35 runtime pack was restored, the APK was rebuilt with the documented build tool, and the full desktop regression suite passed there. Every runtime-pack entry was checked against its manifest. This verifies the source/dependency handover on the current Linux toolchain; it does not add new Android device test coverage.
