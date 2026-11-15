# Verification strip parity checkpoint

Checkpoint, 2026-11-15. Owner: separate verification chat. Scope is only the saved JPEG edge code, shield and `Timemark Verified` label. The code remains a local PG-kit identifier; this work does not implement Timemark server verification.

## Device coordination

The user confirmed LG is free after the initial static investigation. A dedicated `pgkit_watermark_qa` emulator was started with 1536 MB RAM, two CPU cores, port 5580. LG commands use `/tmp/pgkit-adb-DEVICE_SERIAL_REDACTED.lock`; emulator commands use `/tmp/pgkit-adb-emulator-5580.lock`. Do not take the Xiaomi runtime lock. Initial `app_process` attempts failed because Android system fonts were not initialized. A separate target-35 instrumentation APK, `com.pgkit.verificationprobe`, then ran successfully on LG. The target-28 probe was blocked by Play Protect; it was rebuilt for target 35 without changing device protection settings. A debug-only instrumentation entry was also added to the existing clone test app. Physical camera checks are being collected below. The emulator completed the initial fixture; it was no longer running at the later production-instrumentation check.

## Shipped artifact evidence

APK: `reference_app/v10.0.316/apks/base.apk`, SHA-256 `b59c50b6c84ccfa180f8648bc6864a83040b61ff6fcd70eb7e092321042a1a98`.

REA export: `reference_app/v10.0.316/verification-handoff/rea-verification-strip-restated.json` (ignored). Static decompiler evidence establishes drawing instructions, not runtime execution or complete pixel parity.

- `AntiCodeRenderWidget.B`, `ev_9da6fcafc0730cf4bdbb613f031439028df8d22795a3536e8fad98a348def77d`: regular PT Mono and 8x9 dp `anti_code_icon_small` for 4:3; portrait 16:9 may select bold PT Mono and a 10x12 dp icon. This implementation initially targets the supplied 4:3 JPEGs.
- `BottomToTopTextView.<init>` overload 2, `ev_5ec4e2e72234e2a970961668d63fe5f8598ba3fe3b9d43ecfdec82ccd480ad55`: antialiasing, centered characters, 2 native-pixel black 50% stroke, white fill.
- `BottomToTopTextView.onMeasure`, `ev_dd06fa74346bb7c992adc0334155de6b3130e5563411420dc5bcea69781db306`: width truncates font-metric height; height truncates sum of isolated glyph advances plus 10 native pixels.
- `BottomToTopTextView.onDraw`, `ev_95a128c4ac65781ba33ad2539b5528bbe7fc1b115803d16605e26d72ea35c0bd`: draw each character separately at -90 degrees, starting at view height minus font bottom minus five native pixels. Adjacent isolated glyph advances determine spacing; drawing one rotated string is different.
- `w40.k6.invoke` case 12, `ev_26adb3a329eed008b98d78974b34ff73e0003febf47f40976ed41f69d47a9cc5`: text scale is display width divided by 375 dp.
- `com.xhey.xcamera.util.n0.h`, `ev_ca747df1cdf3c00982045262e71e131edce3b6562752b0b60f451d1964ed6328`, and `com.xhey.xcamera.earn.config.a.invoke` cases 25/27, `ev_fa6d7b3a3ac50d1611998ea1b0c442b4a7ae5ed30ed20b716a039a089c947847`: PT Mono names map to packaged `fonts/global/PTMono-Regular.ttf` and `PTMono-Bold.ttf`.
- `AntiCodeRenderWidget.c`, `ev_a747eee92711b16891632cef4365c06a5e989b7841febcc73b66306314e27dd2`: uses `widget_anti_code`.

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

## Physical camera checks

The user holds the real LG for each orientation; no simulated orientation settings are used. UI remains portrait. Preview screenshots and original-resolution JPEGs are retained under `verification-handoff/camera/`.

| Hold | Original / clone dimensions | EXIF | Result |
| --- | --- | --- | --- |
| Portrait | both 1536x2048 | both 1 | Captured 20:43; vertical edge strip visible |
| Landscape, phone top left | both 2048x1536 | both 1 | Captured 20:47; edge strip visible, bottom-to-top |
| Landscape, phone top right | both 2048x1536 | both 1 | Captured 20:52; edge strip visible, bottom-to-top |
| Inverted portrait | both 1536x2048 | both 1 | Captured 20:54; preview watermark inverted 180 degrees, UI portrait, normalized JPEG |

The first attempted landscape pair at 20:45 remained portrait in **both apps**. It is retained under `camera/landscape-top-left/` as a failed hold check and is not counted as landscape validation. The successful retry is under `camera/landscape-top-left-retry/`.

For the latest camera pairs, the identical `Timemark Verified` label has exact-coordinate white-mask IoU 0.62914 in portrait and 0.57049 in landscape-left. Different camera backgrounds, JPEG quantization and antialiasing remain material limitations. The actual verification strings differ, so code IoU is intentionally not calculated.


All four real holds are now captured. The user may return LG to normal portrait. The photo UI and orientation sources were not edited by this verification-strip change. Camera pair JPEG dimensions and normalized EXIF orientation passed; these checks do not establish complete camera-image parity. The default camera previews do not show this saved-only edge strip; the retained preview screenshots verify orientation behavior separately.

Latest label foreground IoU by actual hold: portrait 0.62914, landscape-left 0.57049, landscape-right 0.58759, inverted 0.54648. Evidence summary: `verification-handoff/camera/four-physical-summary.json`. A readable presentation board, `four-physical-strips.png`, crops the same 70-pixel edge ROI and rotates each crop 90 degrees for reading; the actual metric script performs neither alignment nor resize. Original is above clone in each row.

Source/resources/debug fixture/comparison tool were committed separately as `bbf5db0` (`Match saved verification strip fonts and shield to Timemark APK`). Unrelated UI/video/location/metadata work remains untouched. A concurrently added MatchedPhotoInstrumentation manifest entry was explicitly excluded from this commit while preserving its working-tree hunk.


## Final controlled comparison with the original DEX widget

A separate instrumentation probe now loads the **actual shipped** `BottomToTopTextView` and `widget_anti_code.xml` from the installed .316 APK using `DexClassLoader`. It creates the original Application through its inspected constructor and attaches the original resource Context; it does **not** call Application.onCreate. The probe has no INTERNET permission. This exercises original onMeasure/onDraw and the original ConstraintLayout/ImageView, rather than a second handwritten renderer.

The original widget is measured at native LG width 1440/density 401 with native layer heights 1920 or 1440. The candidate uses `SavedVerificationRenderer`. Both receive the identical code and background and are sent through the same explicitly chosen layer-to-output transform. It is a controlled replay of the widget, **not the original camera GL export pipeline**.

This comparison exposed three icon-specific differences after the first commit:

1. ConstraintLayout centers the ImageView at integer native x=9, whereas the initial clone used x=8.5.
2. Loading the WebP through the Android 13 resource path produces different 20x23 pixels than BitmapFactory density scaling. ImageDecoder with the matching target dimensions reproduces the original decoded bitmap exactly.
3. BitmapDrawable's paint has filter+dither and no bitmap antialias flag. Matching those flags removes the remaining composite differences.

The final result is **zero differing RGBA pixels, maximum channel difference zero in all 12 cases**: four reference strings/photo geometries, each on transparent, black and white. Comparison uses the complete 1536x2048 or 2048x1536 arrays, with no crop, alignment, resize or threshold between comparison inputs. Native measurement/transform construction itself is part of the declared fixture.

Evidence, all ignored:

- `verification-handoff/OriginalViewInstrumentation.java`: controlled replay source.
- `verification-handoff/original-view-runtime.txt`: actual original widget geometry and bitmap decode/paint properties.
- `verification-handoff/original-view-fixture/original-view-probe/four-geometry-rgba-metrics.json`: all 12 exact-array results.
- `original-<hold>-<background>.png` and `candidate-<hold>-<background>.png`: complete comparison arrays.

This establishes exact raster equality for the tested **widget layer** on LG, not full JPEG equality. The saved-camera pairs above still show GL positioning/sampling and JPEG/background differences. They were captured before the final icon-only density/rounding/paint refinement; orientation code and the already exact text raster did not change. No claim is made for Android 8's BitmapFactory fallback, other device densities or the 16:9 bold/icon-large state.

The initial REA server session was replaced during the longer run. Its cited methods were restated against the same immutable APK and re-exported into `rea-verification-strip-restated.json`; the IDs above reference this retained final export.


## Final integration validation

After the icon refinement, `assembleDebug testDebugUnitTest --offline` passed again. The final test APK was installed on LG as `com.pgkit.timemarkcamera.codextest`; its VerificationStripInstrumentation passed PNG and JPEG white/black contrast for all four code/geometry fixtures. Final JPEG foreground/outline counts are 3055–3129 on black and 5259–5589 on white (`final-contrast-metrics.json`). An additional real capture with the installed final APK produced a readable 2048x1536 JPEG with EXIF orientation 1, retained under `camera/final-installed/clone.jpg`. The installed test app is left available for inspection.

The diagnostic probe APK is separate from the camera app. Its source and all outputs are retained locally under ignored `reference_app`; none of the APKs, screenshots, device data or native-raster arrays are committed. `exact-widget-comparison.png` is a readability board from the controlled fixture (original above, candidate below), not a claim about complete camera JPEG equality.
