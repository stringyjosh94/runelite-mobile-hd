# Touch controls

The target interaction pattern is tap to act, one-finger scene drag to rotate the camera, pinch to zoom and hold to open a context menu. This project implements that pattern through RuneLite's desktop input and camera APIs. It does not reproduce the official Android client's source, UI or exact gesture timing.

| Gesture | Behaviour |
| --- | --- |
| Tap with small finger jitter | Left-click on release |
| Drag on the game scene | Camera rotation; no release click |
| Drag over an item or interface | Item/interface dragging or Swing scrolling |
| Hold | Context menu |
| Hold, slide to an option, release | Select the highlighted menu option |
| Pinch | Zoom; suppress subsequent one-finger clicks |

## v36.4 correction

Drag detection now checks batched historical positions and the final release position, not only MOVE events. Once movement exceeds Android's device-scaled touch slop, that contact cannot become a tap, even if the finger returns to its starting position. Initial and final camera displacement are retained. Long press, camera speed, pinch handling and scene/widget hit testing retain their existing settings.

Regression traces cover tap jitter, release-only swipes, out-and-back historical movement, vertical drags, density-scaled thresholds and resetting for the next gesture. Existing camera, context-menu, item and Swing regression tests also run on desktop. These checks do not establish identical behaviour to Jagex's app or replace real touch testing.

## Device checks

Try a short quick scene swipe, a slow long swipe, a swipe back to its starting point, an ordinary tap, item dragging, hold-slide-release and a pinch followed by lifting either finger. Confirm that camera drags do not issue walk actions. Use automatic camera dragging for scene rotation; the alternate inventory/interface drag mode intentionally routes drags differently. Report the gesture and screen area if a problem persists.
