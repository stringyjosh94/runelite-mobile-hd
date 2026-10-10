# Graphics profiles

The launcher offers exactly three profiles. All start the native GLES HD fork. The bundled HD renderer is selected automatically; competing stock GPU/HD renderers are disabled when it starts.

| Setting | Low | Medium | High |
|---|---|---|---|
| Shading | Efficient | Efficient | Rich |
| 3D resolution | 100% | 100% | 100% |
| Scene scaling | Nearest | Bilinear | Bilinear |
| Draw distance | 24 tiles | 32 tiles | 48 tiles |
| Shadow map | 512 px | 1024 px | 2048 px |
| Shadow distance | 12 tiles | 20 tiles | 20 tiles |
| Textures | 128 px | 256 px | 256 px |
| Nearby light limit | 2 | 4 | 8 |

All profiles explicitly disable wind/character displacement and expanded shadow draws, including after importing desktop settings. All profiles retain sunlight, ambient lighting and Fast object shadows; MSAA and unported GL features stay disabled. Low/Medium omit the separate terrain-shadow pass and transparent shadows. High restores normal maps, terrain shadows, transparent shadows and richer material/water shading. See [Architecture](ARCHITECTURE.md) for shader differences. Game/Swing interface resolution is separate from 3D scene resolution.

Low uses 100% scene resolution, Nearest, a 24-tile view distance and modest shadows to improve clarity and visibility. Medium uses 32 tiles. These exact presets have not been measured on a device; the longer distance can reduce FPS. Earlier observations are listed in [Device testing](DEVICE-TESTING.md); they are not a minimum-FPS guarantee.

## Existing installations

A preset applies once when selected or when its policy identifier changes. Later manual edits remain until another preset is selected. Unknown saved selections fall back to Low.

Use Back → Graphics quality → Low, Medium or High, then exit and reopen. Switching profiles sets the same collection of keys to avoid leftover Low/High values leaking across tiers. The profile policy is shared Java source compiled into both ART and the embedded JVM.

## Sidebar

Sidebar enlargement is manual. Manual Back → Enlarge sidebar, touch scrolling, pinch/pan in the enlarged view and Back-to-normal are retained.

Desktop regression tests cover profile migration, switching values and valid configuration enums, plus existing gestures/composition and Surface-lifecycle checks. Android build/signature verification passed. Device layout, FPS and visual checks remain manual.

## Device starting points and desktop comparison

These are engineering estimates for this fork, **not certified Android requirements or measured FPS tiers**. Chip specifications establish hardware identity/API support; they do not establish RuneLite speed. There is no valid desktop-to-phone performance conversion from GPU names, RAM or theoretical arithmetic throughput. The Java client, CPU scene submission, driver and sustained thermal behavior matter too.

Upstream's [settings guide](https://github.com/117HD/RLHD/blob/master/settings-guide.md) uses a Ryzen 5 3600/GTX 1660 Ti desktop as its 60 FPS reference. Its suggested 90-tile view, MSAA, large shadow maps and many lights are desktop examples, not mobile minimum specifications. The current [configuration source](https://github.com/117HD/RLHD/blob/master/src/main/java/rs117/hd/HdPluginConfig.java) explains the costs of resolution, shadow quality, light count and normal/POM shading. The mobile profiles budget those costs separately.

| Starting tier | Representative Android hardware | Memory guidance (total device RAM) | Reason |
|---|---|---|---|
| Low | Lenovo Tab M10 3rd Gen / Unisoc T610 / Mali-G52 2-core | 4 GB preferred; 3 GB experimental | Weak GPU and older CPU; retain Efficient shading and short shadow range |
| Low | Helio G99 / Mali-G57; Snapdragon 695 / Adreno 619 | 4 GB or more | Budget hardware: establish stable sustained behavior before raising tier |
| Medium | Snapdragon 778G; Exynos 1380 / Mali-G68 | 6 GB preferred | Efficient shading, four nearby lights, larger textures/shadows |
| Medium | Dimensity 8200 / Mali-G610 MC6 | 6 GB preferred | Conservative starting point; try High after a sustained test |
| Medium | Pixel 10a (limited hands-on reports) | Based on actual available RAM | Limited hands-on reports suggest trying Medium; exact GPU and current-release behaviour remain unverified |
| High | Snapdragon 8 Gen 2 / 8 Gen 3; Dimensity 9200 / Immortalis-G715 | 8 GB preferred | Rich shading and normal maps; still limit lights to eight and shadow range to 20 tiles |

The RAM figures are provisional headroom guidance, not measured allocation thresholds. More RAM does not make a weak GPU faster. A flagship may still need Medium if hot, using many plugins, or running a larger logical viewport. Unknown hardware starts on Low; the app does not override a saved choice based on model-name guesses. The launcher remains exactly Low / Medium / High.

Hardware references (checked 2026-10-10): [Lenovo M10](https://psref.lenovo.com/Product/LenovoTablets/Tab_M10_3rd_Gen), [Helio G99](https://www.mediatek.com/products/smartphones/mediatek-helio-g99), [Snapdragon 695](https://www.qualcomm.com/smartphones/products/6-series/snapdragon-695-5g-mobile-platform), [Snapdragon 778G](https://www.qualcomm.com/smartphones/products/7-series/snapdragon-778g-5g-mobile-platform), [Exynos 1380](https://semiconductor.samsung.com/kr/processor/mobile-processor/exynos-1380/), [Dimensity 8200](https://www.mediatek.com/products/smartphones/mediatek-dimensity-8200), [Snapdragon 8 Gen 2](https://www.qualcomm.com/smartphones/products/8-series/snapdragon-8-gen-2-mobile-platform), [Snapdragon 8 Gen 3](https://docs.qualcomm.com/bundle/publicresource/87-71408-1_REV_E_Snapdragon_8_gen_3_Mobile_Platform_Product_Brief.pdf), [Dimensity 9200](https://www.mediatek.com/products/smartphones/mediatek-dimensity-9200).

### Technical requirements and verification

The current APK needs Android 8/API 26+, a 64-bit ARM Android userspace and a working native GLES 3.2 driver. GLES version is a compatibility gate, not a speed rating. This build uses the installed vendor GLES driver; it does not install different drivers for Qualcomm, Mali or Samsung devices. Hardware ray tracing is not used. Direct Surface presentation is the default; changing the presentation fallback does not change the preset.

100% means the full game-canvas resolution inside the logical viewport, not automatically the device's native display resolution. Low retains 128 px textures; it is still a reduced-material renderer, not desktop visual parity. Mobile light limits are configurable under 117HD and are clamped to 0–25, additionally respecting Dynamic lights. Changing the limit restarts the renderer so selection buffers are rebuilt.

For cross-device checks, use the same account/location, viewport layout, camera angle and plugins; test outdoor trees/water, a busy town and an indoor lit area. Warm each device for 10 minutes, then record walking/standing FPS, CPU/GPU diagnostics, RAM and heat for another 5 minutes. Compare Low/Medium/High without changing viewport resolution. Only raise a tier if sustained performance and visual correctness remain acceptable. No controlled current-release phone measurements are available. Earlier Lenovo results were obtained with different settings and are not a current-release FPS guarantee.
