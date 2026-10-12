# Android notifications

v36.4 uses the v36 embedded Java runtime and renderer. RuneLite notification events are forwarded locally to Android notifications. No push server, login token or external notification service is used.

## Setup

1. Allow Android notifications when prompted. If no prompt appears, use Android Settings → Apps → Runelite Mobile (HD) → Notifications.
2. In the app, press Back and open Help & advanced. Use **Test Android notification** to check Android delivery. After RuneLite loads, **Test RuneLite notification** checks its event bus, embedded Java queue and Android bridge together. **Android notification settings** opens system controls.
3. Enable RuneLite's **Idle Notifier** plugin and choose the idle, inventory or other alerts you want.
4. Set the relevant thresholds in Idle Notifier. Some alerts fire only when a threshold is crossed or an activity becomes idle, rather than immediately when enabled.

The bridge uses RuneLite's existing detection and forwards enabled notification events. Other plugins using RuneLite's Notifier can also send Android alerts. Disabled alerts are not forwarded. Desktop tray and focused-window preferences no longer suppress Android alerts: enabled events are delivered while playing too. Android channels control presentation; they do not detect game conditions. Registration is independent of HD renderer startup. Sound OFF uses a silent Android channel; other sound options use the phone's notification sound rather than desktop custom sound files. Android channel settings and Do Not Disturb remain authoritative. Tapping an alert opens the client. The latest alert replaces the previous one to avoid filling the notification drawer.

## Limits and verification

Alerts require the client process and game connection to remain alive. Android can suspend or stop the app when backgrounded or the screen locks; this build does not add a foreground service or promise screen-off notifications. Closing the app stops delivery. Delivery is polled approximately once per second while Android executes the app.

Desktop tests cover notification event routing, disabled alerts, desktop tray/focus independence, message cleanup and queue bounds. APK compilation and signature verification are checked. Android alert delivery has been reported during development. Permission prompts, individual gameplay triggers and background behaviour still require device checks for each build.

The supplied runtime requires 4 KB memory pages. This release does not establish 16 KB device support.
