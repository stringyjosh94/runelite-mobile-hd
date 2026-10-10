# v35 release candidate

This is an experimental ARM64 Android client. Install `runelite-mobile-hd-v35.apk`; developers also need `runelite-mobile-hd-runtime-v35.zip`. The source-only archive is `runelite-mobile-hd-source-v35.zip`.

## Included

- Embedded Java/Swing RuneLite client with a modified native GLES 3.2 HD renderer.
- Tap, camera drag, pinch zoom, item drag and hold-slide-release menu controls.
- Low / Medium / High profiles with 100% scene resolution and 2 / 4 / 8 nearby lights.
- Direct Android Surface presentation with copied-frame fallbacks.
- One-time credential import, desktop export help, account replacement and clear-login.
- Simplified Back menu, manual sidebar enlargement and touch scrolling.
- Adaptive Mobile HD launcher icon; Android debugging and backups disabled.

The documentation/source packaging was cleaned for publication without changing the v35 APK. Old build-by-build experiment notes are not part of the public source guides.

## Known limits

Testing covers a small Pixel/Lenovo sample using earlier builds. Current profiles, setup flows and launcher masks need further device checks. Game/Plugin Hub updates can require code changes. Desktop renderer plugins are not universally compatible. Low retains reduced textures/material shading and does not promise desktop image quality or a minimum FPS.

The supplied APK uses the existing private development signing identity to preserve updates. The key is not distributed. A source build creates its own development key. See [Building](BUILDING.md), [Legal](../LEGAL.md) and [Device testing](DEVICE-TESTING.md).
