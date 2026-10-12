# Install and use

## Installation

1. Download `runelite-mobile-hd-v36.4-touch-arm64-v8a.apk` from this repository's Releases page.
2. Open the file and allow installation from the browser or file manager if Android asks.
3. Open **Runelite Mobile (HD)**. Choose **Import login file**, or **Help** for instructions.
4. Select your own `credentials.properties`. When **Login ready** appears, tap **Play**.

Read the [desktop login tutorial](LOGIN.md) if you do not have the file. The app does not offer a built-in Jagex browser sign-in. **Other login** opens the game's login screen; Jagex Accounts still require an imported file in this build.

After setup, later launches open the client automatically. A new installation contains no account. Updating preserves the account and settings already stored by that user. Initial setup and shader compilation can take time.

## Touch controls

| Action | Gesture |
|---|---|
| Click or walk | Tap |
| Turn or tilt camera | Drag the game scene |
| Zoom | Pinch over the scene |
| Choose a menu action | Hold, slide to an entry, release |
| Move an item | Drag the item |
| App menu | Android Back |

Back first hides the keyboard or restores an enlarged sidebar. During normal gameplay it opens the app menu. RuneLite sidebar lists support vertical dragging to scroll.

## App menu

- **Keyboard:** opens typing manually; editable RuneLite fields can open it automatically.
- **Graphics quality:** Low / Medium / High; close and reopen to apply.
- **Interface size:** adjusts RuneLite Stretched Mode. Use a resizable game layout. Enlargement is limited to prevent overlapping game panels.
- **Enlarge sidebar:** temporary magnification; pinch to resize, use two fingers to pan, Back to restore.
- **Manage login:** import a replacement or clear the stored login. See [Login](LOGIN.md).
- **Help & advanced:** login help, camera tuning, Screen layout, rendering fallback and diagnostic logs.
- **Close app / Resume:** leave the app or return to the client.

The game keeps the full activity area; the menu does not reserve a permanent toolbar. Manual changes in RuneLite can affect appearance and performance.

## If something goes wrong

Start on Low. Use Help & advanced → Rendering fallback if the game display is blank or broken, then reopen. Export a diagnostic log only after checking it for personal information. Report device, app version, settings and reproduction steps using [Device testing](DEVICE-TESTING.md). No comfortable FPS or universal device compatibility is guaranteed.

## Device requirements

The supplied build is ARM64, Android 8+, GLES 3.2 and 4 KB pages. If startup shows a compatibility message, installing another graphics profile cannot fix the runtime. See [Android compatibility](ANDROID-COMPATIBILITY.md). Device coverage is limited; check the release notes and device-testing guide before updating.

For gameplay gestures, see [Touch controls](TOUCH-CONTROLS.md). To enable phone alerts, see [Notifications](NOTIFICATIONS.md).
