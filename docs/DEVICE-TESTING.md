# Device compatibility and testing

## What has been tried

| Device | Available observations |
|---|---|
| Pixel 10a | Working HD and touch controls reported during development; earlier observations ranged roughly 19–30 FPS with different settings |
| Lenovo Tab M10 (3rd Gen), Unisoc T610 / Mali-G52 | Heavy HD presets were very slow. Earlier Efficient settings reached roughly 8–13 FPS; walking could fall to about 9 FPS |

These are informal maintainer reports from earlier builds, not controlled benchmarks or measurements of every current preset. Only this small device sample has hands-on reports. v36.4 needs build-specific fresh-install and update checks, including gameplay notifications and fast camera drags. No device is certified, and no minimum FPS is promised.

Other models in the graphics guide are provisional starting recommendations based on chip specifications, not tested compatibility results. New content, locations, RuneLite/gamepack changes, plugins, thermal throttling and drivers can change behaviour.

## Test a build

1. Record app version, exact device model, Android version/API, OS ABI (not just CPU), memory page size, RAM, GPU/driver information, preset and manual settings. Startup logs include API, ABIs, page size and JVM heap budget.
2. On a fresh install, check Help → import → Login ready → Play. Cancel the file picker once; reject an invalid file; verify the next launch starts automatically.
3. On an update, check that the account and plugin settings remain. Test account replacement and clear-login, then reopen. Never share the file.
4. Check walking, repeated full camera turns in both directions, overhead tilt, pinch zoom, item drags and hold-slide-release menu selection.
5. Check sidebar scrolling, search keyboard, Backspace/Enter, manual keyboard, manual enlargement and Back-to-normal.
6. Check inventory/minimap placement, both landscape orientations, camera-cutout areas, app switching, screen lock and reopening. Check circular and rounded-square launcher icons.
7. Visit an outdoor wooded area, water, a dense town, a dark interior and an instance. Look for invisible terrain, missing textures, incorrect transparency and broken lighting/shadows.
8. Warm the device for 10 minutes. Record standing and walking performance for another five minutes at the same location/camera. Note charging, battery saver and heat.
9. Compare profiles without changing the viewport layout. Test plugins separately before combining them. Confirm direct Surface logs or record which fallback is active.
10. Test Android and RuneLite notifications, then trigger the configured Idle Notifier conditions while playing. Check permissions, silent alerts and background behaviour separately.
11. Inspect exported logs/screenshots for private information, then submit a device report.

Desktop tests cannot replace these checks. Describe a result as working only for the tested build, device, settings and scene; do not turn a successful install into a universal support claim.

## Compatibility test matrix

Test Android 8–10 and Android 11+ fullscreen separately, including gesture navigation, keyboard, cutouts and app switching. On a 16 KB device confirm the current pack produces the explanatory message without booting the VM. A future rebuilt 16 KB pack needs real VM/rendering tests with compatibility mode disabled. Test 2 / 3–4 / 6+ GiB RAM devices with representative plugins and watch for out-of-memory failures as well as FPS. Only test other ABIs after providing their complete runtime; a successful cross-compile alone is insufficient. See [Android compatibility](ANDROID-COMPATIBILITY.md).
