# Video brand comparison checkpoint — 2026-11-15

Status: wordmark improved; **full video watermark 1:1 remains unverified**.
Four-direction playback verification predates this renderer change and does not
verify the new APK. UI and orientation code were not changed by this patch.

## Change and provenance

`VideoWatermarkRenderer` now draws the packaged original wordmark instead of
reconstructing `Time` / `mark` with horizontally scaled font glyphs. The source
is v10.0.311 `res/drawable-xxhdpi-v4/official_wm_new.webp` (300×300), copied to
`app/src/main/assets/video/official_wm_new.webp`. SHA-256:
`789392581a94325825f52ca284dd9a7dc8784aa4d58a56608df6a34f6b87fa57`.
Only rectangle `(98,186)-(276,226)` is drawn. The bundled `Camera` subtitle is
excluded because the observed Vietnamese recording uses `100% Chân thực`.

Placement was calibrated against the .311 default-34 MP4 and checked against a
separate .316 MP4. The installed original updated during investigation; its
.316 base APK SHA-256 is
`b59c50b6c84ccfa180f8648bc6864a83040b61ff6fcd70eb7e092321042a1a98`.
The .316 sample is portrait output, but its physical hold was not independently
confirmed. It is a brand comparison sample, not a new orientation test.

Static inspection of `com.xhey.xcamera.ui.setting.j.a/c/e` also found a remote
official-logo override path. The packaged bitmap is therefore not proof of the
exact active runtime asset. Some later REA calls timed out and their local
wrapper files contain errors; those files must not be cited as successful
decompilations. No shared REA state was reset.

## Actual Android raster comparison

The shell harness invokes the production private `drawBrand` method on Android
at 1080×1920 with display density 450, then filters to 720×1280. It does not invoke
the full watermark renderer or video encoder. Its final output is retained at
`reference_app/v10.0.316/video/renderer-shell/brand-metrics-output/brand-new.png`.

`scripts/compare_video_brand.py` composites candidate RGBA over a median-15
estimate of the original camera background. It performs no alignment or resize.
These threshold-dependent IoUs are diagnostic, **not percentages of full parity**.

| Reference | Combined white/gold wordmark, before | After | Subtitle contrast, before / after |
| --- | ---: | ---: | ---: |
| .311 | 0.439446 | 0.869565 | 0.184679 / 0.184679 |
| .316 | 0.453642 | 0.875190 | 0.157468 / 0.157468 |

Reproduce from the ignored local fixtures:

```bash
python3 scripts/compare_video_brand.py \
  --reference reference_app/v10.0.311/video/original-frame-2s.png \
  --before reference_app/v10.0.311/video/native-34-720-density450.png \
  --after reference_app/v10.0.316/video/renderer-shell/brand-metrics-output/brand-new.png \
  --output reference_app/v10.0.316/video/wordmark-final-311.json
```

Repeat with `.316/video/original-frame-2s.png` for the second reference. Fixed
fixture dates belong only to test probes; production continues using device time.

Earlier comparisons that directly thresholded RGB channels of transparent PNGs
against decoded MP4 RGB were unfair. With alpha compositing, default-34 date,
weekday and address diagnostic IoUs at threshold 200 are 0.944034, 0.964135 and
0.951935; the best tested translation is (0,0). These are not evidence for moving
those elements by a pixel. The unknown backdrop, filtering and codec remain
confounders. Static glyphs in two original frames have approximately 0.992–0.996
self-consistency, so remaining errors cannot all be dismissed as compression.

## Validation and unresolved work

- Clean debug build with JDK17 / build-tools35 passed; 35 tasks executed.
- The final APK is `reference_app/v10.0.316/video/pgkit-wordmark-verified.apk`,
  SHA-256 `8f8621fddbe203bcac69ac2341ef134544bf00aa2e021af31315d1f1d2a68610`.
- Subtitle font/size experiments were reverted: direct low-resolution fits did
  not retain the improvement through the actual native raster/downsample path.
- Updated probes were repeatedly rejected by Xiaomi with
  `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`, including after USB
  installation was enabled. The older installed probe is not the new renderer.
- The original layout inventory probe needs package visibility in the new test
  APK. Earlier shell probes hit vendor drawable-cache and uninitialized font
  state errors; none produced a controlled original RGBA watermark.
- Still required: install the final APK, inspect a fresh encoded MP4, obtain a
  controlled original RGBA layer or equivalent comparison, resolve subtitle and
  text-edge differences, and verify other templates and brand variants.

Generated APKs, device captures and analysis rasters remain ignored local evidence.

## Installed APK and encoded MP4 follow-up

The user completed installation through App Manager after opening the APK from
the system Files app. Opening a shell `file://` URI failed with “Không thể lấy
thông tin gói”; Files supplied readable content instead. APK signature v2 verifies,
and the installed base APK SHA-256 matches the final build hash above. The old
installation blocker is resolved for this build.

The installed debug renderer probe passed: the clock advances across a minute,
the reused cache equals a fresh render, and template `none` remains transparent.
Its RGBA output is retained under `.316/video/installed-renderer-probe/`.
The detached original-layout probe now resolves the original package, but fails
in `StrokeTextView.g` because `ExKt.toPxInt` reaches a null framework context.
It still does not produce a controlled original RGBA reference. The original
Application was not initialized and private app data was not accessed.

A fresh encoded test clip, MediaStore ID1000011500, was recorded using the
verified APK. `verified-wordmark-recording.mp4` fully decodes with ffmpeg without
errors: H.264 720×1280, 22.461589 seconds, average 23.7294 fps, no display-matrix
rotation; AAC stereo 48kHz. The 2-second frame shows the new wordmark bottom-right
and the main watermark bottom-left. The physical hold was not newly confirmed;
this validates the encoded renderer change, not another four-direction run.
Full pixel parity, subtitle and other-template gaps remain open.

## Subtitle native-raster follow-up

A second bounded sweep rendered all candidates at the actual 1080 raster before
filtering to 720, rather than choosing from direct-720 glyphs. The retained
implementation keeps Roboto Condensed Regular, reduces its size from 32 to 24.8
in the existing `layoutShortEdge / 1152` scale, and adjusts right/bottom offsets
from 18/20 to 16.4/18.4. The shadow, color and target text width stay unchanged.
This is an empirical improvement; the active original subtitle asset is unknown.

The actual private production `drawBrand` method was then invoked with the same
display metrics and downsampling, independently of the candidate-sprite fitter.
The unchanged `compare_video_brand.py` gives subtitle contrast IoU 0.184679 →
0.509579 on .311 and 0.157468 → 0.452328 on .316. Wordmark IoUs stay unchanged.
Inputs and results are retained as `renderer-shell/subtitle-verified-output/`,
`subtitle-production-final-311.json` and `subtitle-production-final-316.json`.
The debug build passed. The installed base APK was verified against SHA-256
`8ecd2095afeb1723ed82ddcf7cfa8dcd9e71f9959e075520f3563cf5af48e171`.
The installed clock/cache/none probe passed again. Fresh MediaStore clip
ID1000011501 (`verified-subtitle-recording.mp4`) fully decodes without errors:
H.264 720×1280, 34.130356 seconds, no display-matrix rotation, AAC stereo 48kHz.
The inspected 2-second frame confirms the smaller subtitle in the encoded file.
This confirms implementation/output behavior; it does not establish full parity.

The later detached original-layout probe failed closed on an ambiguous context
slot. Raw DEX inspection explains why a generic Context injection is unsuitable:
`n.g(float)` reads `com.facebook.imageutils.c.a` typed as `TodayApplication`, not
`Context`. The probe did not initialize the original Application or access private
data. The same DEX inspection retains the official-logo query annotation
`watermark/official/query`; a runtime response or active downloaded asset has not
been obtained. `official_watermark2_vn.png` was inspected: its subtitle is “Máy
ảnh”, which differs from the observed MP4 variant.
