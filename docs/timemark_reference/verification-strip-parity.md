# Verification strip parity checkpoint

Work in progress, 2026-11-15. Owner: separate verification chat. Scope is only the saved JPEG edge code, shield and `Timemark Verified` label. The code remains a local PG-kit identifier; this work does not implement Timemark server verification.

## Device coordination

The user confirmed LG is free after the initial static investigation. A dedicated `pgkit_watermark_qa` emulator was started with 1536 MB RAM, two CPU cores, port 5580. LG commands use `/tmp/pgkit-adb-DEVICE_SERIAL_REDACTED.lock`; emulator commands use `/tmp/pgkit-adb-emulator-5580.lock`. Do not take the Xiaomi runtime lock. Initial `app_process` attempts failed because Android system fonts were not initialized. A separate target-35 instrumentation APK, `com.pgkit.verificationprobe`, then ran successfully on LG. The target-28 probe was blocked by Play Protect; it was rebuilt for target 35 without changing device protection settings. A debug-only instrumentation entry was also added to the existing clone test app. Physical camera checks are being collected below. The emulator completed the initial fixture; it was no longer running at the later production-instrumentation check.

## Shipped artifact evidence

APK: `reference_app/v10.0.316/apks/base.apk`, SHA-256 `b59c50b6c84ccfa180f8648bc6864a83040b61ff6fcd70eb7e092321042a1a98`.

REA export: `reference_app/v10.0.316/verification-handoff/rea-verification-strip.json` (ignored). Static decompiler evidence establishes drawing instructions, not runtime execution or complete pixel parity.

- `AntiCodeRenderWidget.B`, `ev_949415286a575eadaf6692dfba8b95bf63bdca9730a58d714fbb8aa559ebb916`: regular PT Mono and 8x9 dp `anti_code_icon_small` for 4:3; portrait 16:9 may select bold PT Mono and a 10x12 dp icon. This implementation initially targets the supplied 4:3 JPEGs.
- `BottomToTopTextView.<init>` overload 2, `ev_089037f200562bf97e69b7c9b7d06db6891dc048d0553808815203519e2c123f`: antialiasing, centered characters, 2 native-pixel black 50% stroke, white fill.
- `BottomToTopTextView.onMeasure`, `ev_58c2e0ec46b98300cd01422812ba4c110036389b091ae3ea3a2db029636b6836`: width truncates font-metric height; height truncates sum of isolated glyph advances plus 10 native pixels.
- `BottomToTopTextView.onDraw`, `ev_606221b1cdd5f5bfaebe249daa262d003f36bdbcdb0697891230336d1154f2a3`: draw each character separately at -90 degrees, starting at view height minus font bottom minus five native pixels. Adjacent isolated glyph advances determine spacing; drawing one rotated string is different.
- `w40.k6.invoke` case 12, `ev_1aa43238e6f2b1aafab19f9339e138f521e4322e78a6466093ebdf7f0fca3743`: text scale is display width divided by 375 dp.
- `com.xhey.xcamera.util.n0.h`, `ev_318dddd366f16b6a831e6b628f7190c222d5d97352bf123ba51be2391454de47`, and `com.xhey.xcamera.earn.config.a.invoke` cases 25/27, `ev_836afbe85cea999bf0e0488fc7e74d486cd345412facf60e22836f71b893551b`: PT Mono names map to packaged `fonts/global/PTMono-Regular.ttf` and `PTMono-Bold.ttf`.
- `AntiCodeRenderWidget.c`, `ev_89da9e097e0aa77933237b6489f07442d45d03b25d8bd1b12f8d31d09f61fe34`: uses `widget_anti_code`.

REA resource decode is unavailable because apktool is missing. Existing SDK `aapt dump xmltree` reads the exact APK instead. Retained text is under the ignored handoff directory:

- `widget_anti_code.xml`: both sizes are 9 dp; label uses RobotoCondensedRegular with native offset 6; code uses ptMonoRegular with offset 8; label is above centered code with 2 dp margin; icon is below code, rotation -90 degrees, with 1 dp top/end margins.
- `activity_camera_preview.xml`: `WaterMarkAntiCode` RotateLayout aligns to parent end and centers vertically.

The original reference shield has a built-in dark outline. `watermark_shield.webp`, used by the old clone, is a different asset. PT Mono Regular and the small icon were copied byte-for-byte from the shipped .316 APK into app assets. Roboto Condensed Regular was already present. PT Mono Bold was investigated but is not shipped by this change, which is scoped to the supplied 4:3 photo state.

## Implementation and validation

`SavedVerificationRenderer` isolates the edge strip from the shared brand renderer. Android raster fixtures use the production draw function and the same four code strings as the original JPEGs. Fixed verification strings are confined to the debug instrumentation and ignored fixture harness; release code uses the captured local code. No fixed production clock was added. Build `assembleDebug testDebugUnitTest --offline` passed. Do not call the result 1:1.


## Native rounding and raster results

LG instrumentation confirmed XML `android:textSize="9dp"` becomes **23.0 px** at density **2.5062501** before the custom width/375dp scale. The first candidate omitted this integer rounding and produced a visibly shorter strip. The renderer now preserves it. Icon decoding also preserves its xxhdpi source density (480), target-device density and subsequent JPEG-layer scale, instead of tinting a different shield.

Assets copied from the exact APK:

| Asset | SHA-256 |
| --- | --- |
| `fonts/PTMono-Regular.ttf` | `51b98478119b183da968d15ed172f9d0ce491e1f89f0d62b1aa230bac35dcb46` |
| `verification/anti_code_icon_small.webp` | `60ea4536398f6ac88a5f71842d5f4143fa0acbaeed8ad0423adb0579c6a030ad` |

`VerificationStripInstrumentation` calls the production renderer using the four reference strings. PNG and JPEG-99 fixtures cover black, white and transparent backgrounds at portrait 1536x2048 and landscape 2048x1536. The LG production fixture passed its contrast assertions. The exported JPEGs retain 3055–3129 bright foreground pixels on black and 5244–5574 dark outline pixels on white in the fixed strip ROI. This checks visibility, not fidelity. Evidence: `verification-handoff/lg-production-fixture/verification-strip-probe/` and `contrast-metrics.json`.

Exact-coordinate comparisons against the four retained 09:21–09:24 reference JPEGs have label mask IoUs around 0.51–0.55 and code mask IoUs around 0.61–0.66. The strings are identical in the debug fixture. No resize or alignment is applied. The reference has a camera background and the fixture has a black background; a fixed white threshold is sensitive to antialiasing on those backgrounds. The visible geometry is much closer than those masks alone imply, but these numbers do not prove pixel parity. Label bounds differ by roughly one output pixel; code strokes and shield alpha/sampling still differ.

`scripts/compare_verification_strip.py` preserves image dimensions and uses fixed absolute edge ROIs. It saves source crops, a foreground-mask difference and metrics. It never aligns or resizes. Code IoU is disabled unless `--same-code` is explicitly supplied; different local and original codes cannot support a code-shape parity claim. Low-alpha shield whites may never exceed the foreground threshold, so an empty shield mask does not mean the shield is absent.

The three JPEGs appended to an original portrait file were inspected as possible isolated layers. They contain clean camera-background slices, not transparent watermark raster. They were not used as proof of foreground equality. The retained slice decoding is exploratory evidence only.

## Physical camera checks (in progress)

The user holds the real LG for each orientation; no simulated orientation settings are used. UI remains portrait. Preview screenshots and original-resolution JPEGs are retained under `verification-handoff/camera/`.

| Hold | Original / clone dimensions | EXIF | Result |
| --- | --- | --- | --- |
| Portrait | both 1536x2048 | both 1 | Captured 20:43; vertical edge strip visible |
| Landscape, phone top left | both 2048x1536 | both 1 | Captured 20:47; edge strip visible, bottom-to-top |
| Landscape, phone top right | pending | pending | Awaiting physical hold |
| Inverted portrait | pending | pending | Awaiting physical hold |

The first attempted landscape pair at 20:45 remained portrait in **both apps**. It is retained under `camera/landscape-top-left/` as a failed hold check and is not counted as landscape validation. The successful retry is under `camera/landscape-top-left-retry/`.

For the latest camera pairs, the identical `Timemark Verified` label has exact-coordinate white-mask IoU 0.62914 in portrait and 0.57049 in landscape-left. Different camera backgrounds, JPEG quantization and antialiasing remain material limitations. The actual verification strings differ, so code IoU is intentionally not calculated.
