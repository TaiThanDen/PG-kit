# Display ratio and date/time sheet check — 2026-11-17

Test device: LG V50S LM-V510N, density 401 dpi. Android currently reports a base 1440×2560 display and initially had a 1080×1920 override. Neither should be mistaken for the hardware specification: LG lists 1080×2340 (portrait 9:19.5): https://www.lge.co.kr/product/smartphones/lm-v510n?modelId=MD08044800&pdpType=PURCHASE . Both 1080×1920 and 1080×2340 were exercised; the initial override was restored afterward.

## Preview measurement

At 1080×1920, the old 200 dp panel reservation reduced the standard 3:4 preview to about 967×1289, horizontally inset 56 px. Timemark uses a 1080×1440 preview at this setting. The clone therefore normalized its preview text against a roughly 10.5% narrower viewport.

The camera viewport now retains full width and allows the bottom controls to overlap its lower edge when needed. The existing watermark/control avoidance also applies to this compact standard-ratio state. Preview rotation still comes from the display; Activity remains portrait. No sensor mapping or saved renderer was changed.

After correction, measured solid ink (all RGB channels >230) on the same day:

| Preview region | Timemark | Clone |
| --- | --- | --- |
| Date | 279×39 px | 277×39 px |
| Weekday | 109×31 px | 108×31 px |
| Yellow divider | 5×109 px | 4×108 px |

The clone anchors above its own taller capture panel, so absolute screen Y differs. Time strings differed between captures; date and weekday were equal. These measurements establish that the previous ~10% shrink is removed, not full pixel parity. Address content differed because the apps returned different GPS/address results.

## Saved JPEG measurement

Actual photos from both apps at 1080×1920 were retrieved. Both files are 1536×2048. Solid foreground threshold >235, with no resizing:

| Saved region | Timemark | Clone |
| --- | --- | --- |
| Date | 395×55 px | 392×55 px |
| Weekday | 154×43 px | 153×44 px |

The JPEG path retains its own output geometry, independently of the cropped/occluded preview. Small raster/placement differences remain; clock text, GPS text and camera backgrounds differ, so no whole-image IoU or exact parity claim is made. This check covers the default watermark and portrait JPEG, not every template or physical orientation.

## Sheet and build checks

Removed the numeric-input shortcut and its unused keyboard editor. Draft date/time now appears below the active wheels/calendar with explicit 8 dp spacing. The window measures the actual scroll content rather than allocating a taller transparent window; only top corners are rounded. On LG, time/calendar tabs, back dismissal and bottom placement were exercised at both display sizes. A recorded native-size flow and screenshots are retained locally outside Git under `/tmp/pgkit-qa/`. The bottom white surface meets the system navigation area; buttons retain bottom padding. Android navigation chrome remains system-owned.

Production debug/release builds, all 30 unit tests and release lint passed. Existing Camera2 interop usage now declares its opt-in; the API-23 Layout constant aliases carry a scoped lint explanation for the API-35 builder annotation. These annotation-only changes do not change runtime rendering. Release certificate matches beta.1/beta.2 and the release manifest contains neither debuggable=true nor instrumentation. Full four-direction preview/JPEG/video validation was not repeated. Raw screenshots, device data, APKs and recordings are excluded from commits.
