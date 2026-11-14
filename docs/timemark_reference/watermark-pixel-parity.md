# Watermark pixel parity investigation — v10.0.311

Status: **analysis in progress; the clone is not pixel-identical**. The generic 16-template Canvas renderer and current picker are prototypes. Their existence does not establish template fidelity.

The requested acceptance target is the original watermark, its template picker/editor, live preview and saved JPEG. The app stays portrait-only. Device time remains real in production. Fixed strings below belong only to an isolated rendering probe.

## Artifact and retained evidence

Original package: `com.oceangalaxy.camera.new`, version `v10.0.311` / `100031100`.
Base APK SHA-256: `45ddfcecd097aa3c3b557f43cab42df9bdc2c5e3709475bdeb2ed2b621de9893`.

Local ignored evidence under `reference_app/v10.0.311/pixel/`:

- `rea-watermark-evidence-bundle.json`: 112 retained REA records at this checkpoint, including raw provider observations.
- `layouts/layout-index.json`: 36 resolved resource layouts, their member hashes and attributes.
- `layouts/*.resolved.txt`: full AAPT tree with resolved dimensions, styles, colors and resource names.
- `glyph-comparison.json`, `regions.json`, `probe/device/`: independent Android glyph-rendering experiment.
- `../evidence/01-camera.png` and `.xml`: original app capture used by the glyph experiment; not a clone screenshot.

## Two different layout paths were previously conflated

`fb0.l.a` selects `new_watermark_cover_id34` for `water_mark_des_34`. That cover declares 38 dp time, 16 dp date/weekday, 14 dp location and a 260 dp maximum address width.

`fb0.l.b` selects `watermark_cover_id1034` for the XML rendering route of that same watermark name. `WatermarkPreviewView.b` consumes the corresponding `s10.a9` binding, adjusts the location view, and attaches extra item rows. `s10.b9.executeBindings` binds the actual data and dynamic sizing. A feature-gated branch of `fb0.l.b` can instead create programmatic builder views through `ib0.b.a`; the route must be checked for each template/runtime state.

Therefore the old 38/16/14 cover values must not be treated as the live camera specification. This explains a material part of the current clone mismatch.

REA parent Evidence IDs:

| Question | Parent Evidence |
| --- | --- |
| New-cover route | `ev_0bee612af300297fc84563547911d26f07274bc3c06a502afa825a9bf6af67de` |
| Actual route / builder gate | `ev_add98629a00e77a7a109312e8e4d64552a58ec979a88f09a9430775e6a64daec` |
| Camera/editor binding | `ev_40e8139efcf7b1a5c605b51d1262c28ed01962674eb9bb549c0d81d631aec5d3` |
| Production binding and size inputs | `ev_403ff47cac901dafead5284bfe42663e56ba7c4cf3cb10e00d2a9946660c353a` |

Source identities in the exported bundle are authoritative if a future engine session assigns different aliases. Decompilation is static evidence; runtime screenshots corroborate only their observed state.

## Mẫu 34: confirmed declarations and binding behavior

| Element | Original definition | Practical consequence |
| --- | --- | --- |
| Outer margins | `waterMarkMargin`: 8 dp on four sides | Measure the actual container; do not anchor glyph bounds directly to an 8 dp baseline |
| Content column | 245 dp in the production XML, with dynamic width updates | A hard-coded 260 dp wrap rule is a different layout |
| Clock | `StrokeTextView`, 45 dp, `includeFontPadding=false` | Preserve native measurement and separate outline/fill behavior |
| Date and weekday | 14 dp; top/bottom margins 4 dp | Current 16 dp date/weekday are incorrect for this route |
| Divider | 2 dp wide, 38 dp high, centered; binding updates margin/height | Do not derive its ends from font ascent and arbitrary insets |
| Address | `AutoDirectionTextView`, 3 dp start padding; dynamic integer text size and maximum width | Its line wrapping and baseline need native layout verification |
| Clock font | Binding calls `s00.e.q`, which uses `p0.b()` | APK font `BigShouldersText-Medium.ttf` |
| Secondary font | Binding calls `s00.e.y`, which uses `p0.e()` | APK font `RobotoCondensed-Regular.ttf` |
| Font scaling | `s00.c.s`: `setTextSize(PX, round(dp * applicationDensity) * fontScale)` | Screen density alone is insufficient to infer runtime pixels |
| Shadow | Text style: dx 0, dy 1.5, radius 3.5, black alpha 0.4 | Compare antialiasing/shadow separately from the solid glyph mask |
| Outline | `StrokeTextView` has a second TextView with stroke Paint, measured/layout separately | A single Canvas text call omits part of the original rendering |

Before the native-layout implementation below, the renderer used 38 dp time, 16 dp secondary text, manual baseline offsets and greedy address wrapping. Those choices were replaced for the default template-34 live camera path.

## Measured original state at 1080 × 2400

The earlier original UIAutomator capture records:

| View | Bounds in screen pixels |
| --- | --- |
| Preview | `[0,256]–[1080,1696]` (1080 × 1440) |
| Watermark root | `[0,1385]–[751,1696]` |
| Content | `[23,1408]–[728,1673]` |
| Clock | `[23,1408]–[246,1564]` |
| Date | `[297,1419]–[571,1473]` |
| Weekday | `[297,1499]–[422,1553]` |
| Address | `[23,1564]–[728,1673]` |

The divider's yellow foreground occupies x 274–279, y 1432–1540 in that PNG. These are observations for that input/configuration, not universal production constants. Different strings and style options change the geometry.

The clone capture had a shorter viewport ending at y 1652. Comparing screen anchors without fixing the preview coordinate systems would conflate a 44 px viewport difference with a watermark error.

## Android glyph experiment

A shell-only `app_process` probe used the original packaged fonts and the phone's Android rasterizer. It rendered the exact reference strings across fractional pixel sizes, independently of camera content. Production app time was not changed.

Metric: threshold each RGB channel above 220, crop to the ink bounds, align within ±4 px, then compute foreground intersection-over-union. This verifies shape and size after alignment, **not complete pixel parity**.

| Reference string | Best candidate size | Mask IoU |
| --- | --- | --- |
| `15:32` | 130 px | 0.998640 |
| `14 Tháng 11,2026` | 39.5–39.75 px | 1.000000 |
| `Thứ Bảy` | 39.5–39.75 px | 0.980100 |
| First address line | 43–43.25 px | 0.939460 |

These candidate sizes diagnose the mismatch; they must not be hard-coded as general dp values. Address and weekday have residual differences. JPEG compression, fractional placement, Paint flags, outline and native text layout remain relevant.

## Watermark UI

The original screenshots establish a two-column template sheet, selected-card actions, and a separate editing sheet with native switches, item-specific editors, color/size controls, and preview repositioning above the sheet. The current generic Dialog with default Android buttons is visibly different and has not passed parity.

Decoded UI resources include `fragment_universal_watermark_list_global`, `dialog_edit_watermark`, `dialog_edit_full_info_watermark`, `layout_watermark_edit_header_global`, `layout_watermark_edit_all_item`, `layout_seek_bar_for_watermark_size`, `watermark_preview_layout`, and `watermark_preview_bottom_sheet`. They are evidence candidates; the active picker/editor route must be traced rather than inferred from a similar filename.

For every template, record: original picker thumbnail, selected state, edit sheet, each field editor, color/size state, preview geometry and saved JPEG. A static cover is not the camera output. Templates backed by programmatic builder views require their style/attribute code as well as XML inventory.

## Acceptance ledger

- Proven: exact original font files; original route distinction; production template-34 XML/binding behavior; the measured original state above; 36 resource layouts; same-device glyph fit.
- Partial: template-34 outline, location width/font-scale inputs, native line breaking, sheet sizing and viewport behavior.
- Not verified: complete template-34 RGBA parity, saved JPEG parity, every other template's output, all item editors, and four physical orientations for the new template system.

Do not label any template "1:1" until all required states pass. Compare like-for-like data, raster size, density, theme/scale and orientation; preserve both images and a difference image. Measure solid foreground, antialiasing, outline, shadow, placement and line breaking separately. Repeat on preview and the actual exported JPEG. Camera scene differences must be masked out of the watermark comparison.

## Reproduce resource analysis

```bash
python3 scripts/analyze_watermark_resources.py reference_app/v10.0.311/apks/base.apk \
  --aapt2 /home/developer/Android/Sdk/build-tools/36.0.0/aapt2 \
  --output reference_app/v10.0.311/pixel/layouts
```

Glyph probe source: `scripts/android/WatermarkGlyphProbe.java`. Compile it against Android 35 with JDK 17, convert using D8, then run its DEX JAR through `app_process` with an output directory containing the two font files. The probe is a test tool; it is never packaged in the app. Compare its pulled PNGs using:

```bash
python3 scripts/compare_watermark_glyphs.py \
  --reference reference_app/v10.0.311/evidence/01-camera.png \
  --regions reference_app/v10.0.311/pixel/regions.json \
  --candidates reference_app/v10.0.311/pixel/probe/device \
  --output reference_app/v10.0.311/pixel/glyph-comparison.json
```

Only APK-derived resources, REA results and original-app captures establish original behavior. The current clone screenshots and reconstructed UI are implementation artifacts, not reference evidence.

## Native template-34 implementation checkpoint — 2026-11-14

`TimeLocationWatermark` now uses Android StaticLayout, the packaged fonts, native text flags, font padding and line breaking. REA `x50.d.<init>` confirms address size Q3 defaults to 15 dp. `x50.d.D` computes `(widthPixels / density) / 375`, explaining why multiplying the nominal 45 dp clock by physical density alone was insufficient. The implementation rounds dp to pixels **before** applying this width normalization, as the original binding does. It preserves the empty seconds container's 4 dp margin and the divider's relative 6 dp margin.

Additional REA observations are retained in `pixel/rea-native34-implementation.json`. `o.C` only changes the top margin; it does not remove the address's 3 dp start padding. The clock outline width comes from `getDimensionPixelSize`, so the half-dp attribute rounds to an integer pixel. Shadow attributes are raw floats in pixels.

The shell fixture in `scripts/android/TimeLocationLayoutProbe.java` calls the actual application's layout class. With reference strings and the same phone (1080x1440 preview, 450 dpi), `scripts/compare_time_location_layout.py` measured the following **without alignment or resizing**:

| Region | Solid foreground IoU | Absolute ink bounds |
| --- | --- | --- |
| Clock | 1.0 | identical |
| Date | 1.0 | identical |
| Weekday | 1.0 | identical |
| Address (both lines) | 0.9998486 | identical |
| Yellow divider | 1.0 | identical |

Outputs: `pixel/native34-layout-comparison.json`, `original-layout.png`, `candidate-layout.png`, `difference-layout.png` (ignored local evidence). This is a significant improvement over the earlier glyph fit, which cropped and aligned glyphs and did not exercise the complete application layout. Camera background pixels, antialiasing, outline/shadow RGBA, alternate settings, JPEG output and other templates remain outside this metric. Do not convert it into a claim that the whole app is pixel-identical.

The debug APK with the native template-34 layout was installed successfully as `com.pgkit.timemarkcamera.codextest`, started on the real phone and visually inspected. A capture produced a readable 2448x3264 JPEG with the separate existing saved-watermark path; the camera-to-JPEG flow works, but that does not validate its fidelity. No production time was fixed and no orientation mapping was changed.
