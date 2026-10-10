# Plugin Hub compatibility

The app uses RuneLite's existing Plugin Hub loader. Install ordinary plugins from the sidebar as usual; many can use their existing builds when they use supported RuneLite APIs, but compatibility must be checked per plugin. Installing a plugin does not automatically enable it.

## Shared HD settings

The bundled GLES renderer advertises the plugin name `117 HD`, with its distinct `MobileHdGles` configuration identity and `dev.mobile.rlhd` implementation. This supports plugins such as 3D Weather that identify the active HD renderer by its display name. Do not install desktop 117HD as a replacement for the bundled renderer.

A startup bridge exposes these stock `hd` settings and synchronizes changes in both directions with the mobile `mobilehd` settings:

- defaultSkyColor and overrideSky
- screenBrightness and fContrast
- shadowMode
- seasonalTheme

The current mobile settings win at startup. Missing settings get the renderer's defaults. Existing profiles, controls and manual mobile settings are preserved. Backend-specific switches are deliberately excluded: plugins cannot enable unsupported desktop buffer/compute paths through this bridge.

Source inspection of Divine Skies shows it writes these stock-HD visual settings and uses Client.setSkyboxColor. The mobile renderer already supports RUNELITE sky colors. Source inspection of 3D Weather shows it creates ordinary RuneLiteObject models, checks the renderer's display name, and reads stock-HD seasons. These integrations now have the expected settings/name surface without modifying downloaded Hub JARs.

## What is tested

Desktop tests check startup precedence, bidirectional settings changes, removal, prevention of feedback loops, and isolation of backend settings. Existing input/composition/profile tests also run. This does not establish successful phone rendering for Divine Skies, 3D Weather, or the whole Hub catalogue.

On a phone, test each plugin separately, then together. For Divine Skies, test sky changes, brightness, night shadows and restoration when disabled. For 3D Weather, start with low particle density; test rain/snow and 117HD seasonal synchronization. Report the plugin version, phone model, screenshots and logs if objects are missing or transparent.

Plugins using desktop OpenGL/native libraries, casting to stock rs117.hd classes, or replacing the entire renderer can still require dedicated ports. This bridge is not a general OpenGL translator. Weather particle density can affect FPS.
