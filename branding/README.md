# Mobile HD launcher icon

Original reference: https://runelite.net/img/runelite_logo_transparent_small.webp (downloaded 2026-10-10). RuneLite's original branding belongs to its respective owners. This is a modified icon for this unofficial mobile project; it does not imply official RuneLite or Jagex endorsement.

`runelite-original.webp` preserves the supplied reference. `runelite-mobile-hd-icon.png` is the transparent generated master, edited with the built-in image generation tool. It is copied unchanged into `app/res/drawable-nodpi/hd_mobile_mark.png`. Android draws it over a charcoal background with a 14% foreground inset for adaptive masks. The manifest uses the adaptive icon for both icon and roundIcon. Minimum Android API is 26.

## Editing instructions

The image editor was asked to preserve RuneLite's silver sword/R and fiery L, add a restrained phone outline and a gold-edged HD badge, and produce a square transparent foreground with no other lettering. The generated result used more space than requested, so Android adds a 14% foreground inset for launcher masks. The master and its packaged source copy are preserved without creative post-processing.

This artwork is a derivative branding experiment, not a claim of ownership or endorsement. Logo permission is separate from code licensing; see [Legal disclaimer](../LEGAL.md). Resource/alpha checks do not replace checking actual home-screen masks on devices.
