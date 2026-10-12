# v36.4 — Touch controls and Android notifications

APK: `runelite-mobile-hd-v36.4-touch-arm64-v8a.apk`
Android version: `0.36.4-mobile-hd` · Version code: `42`

## Changes

- Improved drag detection prevents fast camera swipes from becoming walk clicks. Movement recorded at release or in Android's batched touch events is included.
- Camera rotation retains the start and end of a drag. Tap, pinch zoom and hold-slide-release menu selection remain available.
- Enabled Idle Notifier alerts and other RuneLite notification events are forwarded to the phone's notification drawer, including while playing.
- Added separate Android and RuneLite notification tests and a shortcut to Android notification settings.
- Fixed fullscreen startup when Android's window controls are not yet attached.

The build retains the v36 Java runtime and native GLES HD renderer, three graphics profiles, credential import and manual sidebar enlargement. Source builds use the unchanged `runelite-mobile-hd-runtime-v35.zip` dependency pack.

## Requirements and limits

Android 8+, ARM64, 4 KB memory pages and OpenGL ES 3.2 are required by the supplied build. Other architectures and 16 KB devices are not supported by the supplied runtime. Device performance and graphics-plugin compatibility vary.

Enable Idle Notifier and configure its alert switches and thresholds. Android notification permissions, channel settings and Do Not Disturb control delivery and presentation. Alerts require the client to remain running and connected; screen-off delivery is not guaranteed. See [Notifications](NOTIFICATIONS.md).

APK compilation, signing/alignment checks and desktop regression tests passed. Physical touch behaviour, notification delivery and performance still require build-specific device checks. Earlier Pixel and Lenovo reports are not universal compatibility results. See [Verification](VERIFICATION.md) and [Device testing](DEVICE-TESTING.md).
