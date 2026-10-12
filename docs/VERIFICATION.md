# Verification and testing limits

The v36.4 APK compiled successfully and passed signing and APK alignment checks. The desktop regression suite passed, including touch-intent and notification-routing tests. These checks do not establish Android GPU performance, universal device compatibility or an independent security review.

## Desktop checks

`python3 tools/test.py` checks camera and pointer routing, menu selection, sidebar scrolling, keyboard focus, viewport sizing, frame ownership, Swing composition, Surface lifecycle test doubles, graphics profiles and login-file deletion.

Touch traces cover tap jitter, release-only swipes, historical out-and-back movement, vertical drags, device-scaled thresholds and gesture reset. Notification tests cover enabled and disabled events, desktop tray/focus independence, message cleanup and queue bounds.

Compatibility checks cover memory budgets and mismatched native dependencies. The supplied ARM64 runtime does not pass the strict 16 KB compatibility gate. Other architecture fixtures are validation tests, not working runtime distributions.

Optional desktop GLES tests use fixture scenes. Software Mesa results do not establish Android GPU speed or complete map correctness.

## Android checks

Manual device tests are required for physical gestures, gameplay alert triggers, permissions, keyboard, account setup, app switching, screen lock, rendering, heat and performance. Notification delivery and earlier HD gameplay have been reported during development; coverage is limited to the devices and builds tested. Follow [Device testing](DEVICE-TESTING.md).

## Distribution checks

`python3 tools/package-source.py --check` checks Git-visible filenames and sizes for obvious private or generated files. Review file contents before publishing. Checksums identify downloads; they do not prove security or licensing suitability.

Source builds require the matching v35 runtime pack. Earlier source/dependency handover builds succeeded with that pack; this does not replace a fresh restore test of each published snapshot. Source builds create a separate development signing key unless a private key is supplied, so they are not expected to match the distributed APK hash or update its installation.
