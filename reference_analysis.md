# Reference analysis

## Inspected reference build

- Package: `com.oceangalaxy.camera.new` (`Timemark`).
- Version: `v10.0.189` / `versionCode 100018900`.
- SDKs: `minSdk 26`, `targetSdk 35`, `compileSdk 35`.
- Launch activity: `com.xhey.xcamera.ui.camera.picNew.PreviewActivity`.
- Installed splits: `base.apk`, `split_config.arm64_v8a.apk`, `split_config.vi.apk`, and `split_config.xxhdpi.apk`.
- Device used for inspection: 1080 × 2400 px at 450 dpi.
- `reference_app/` holds the pulled APK splits solely for local inspection at the user's request. No reference source code has been copied into the clone.

## Screenshot sources

- Full saved-photo reference: 1920 x 2560 px (`codex-clipboard-eed6e4be-bae2-43f5-ae20-150b2c327254.png`).
- Live camera references: 1080 x 2400 px. They establish the live-preview placement and camera-screen controls, but the saved-photo image is the typography source of truth because it contains the cleanest watermark.

## Watermark observations

| Element | Observed layout / typography | Color | Source |
| --- | --- | --- | --- |
| Time | Left aligned near lower-left; very narrow bold sans; approximately 154 px on the 1920 px saved image; 07:45 baseline near y=2300 | White with dark edge/shadow | SCREENSHOT ESTIMATE |
| Date | Starts just right of divider; approximately 54 px, bold narrow sans; baseline about y=2170 | White with dark edge/shadow | SCREENSHOT ESTIMATE |
| Weekday | Aligned to date; approximately 54 px; baseline about y=2320 | White with dark edge/shadow | SCREENSHOT ESTIMATE |
| Divider | Vertical line between time and details; x≈503, y≈2110–2320; width≈8 px | `#FFB82B` estimate | SCREENSHOT ESTIMATE |
| Address | Left aligned below the time/detail row; approximately 55 px; wraps to two lines when needed | White with dark edge/shadow | SCREENSHOT ESTIMATE |
| Branding | Bottom-right `Timemark` plus `100% Chân thực`; not required for the live overlay but added to saved image | Yellow / light gray | SCREENSHOT ESTIMATE |
| Verification text | Rotated along right side of saved image | White with dark edge/shadow | SCREENSHOT ESTIMATE |

## APK-resource findings

- The default `Time&Location` template has `base_id=34`, time style `132`, address style `300`, and a theme text color of `#FFFFFF` with accent `#FFC233`. **SOURCE = APK RESOURCE**.
- The APK bundles `BebasDaka`, `BigShouldersText-Medium`, `Montserrat-Black`, `PTMono`, `RobotoCondensed` (Regular/Medium/Bold), and `XiaoHeiNumber` under `assets/fonts/global/`; it also has `res/font/roboto_medium_numbers.ttf`. **SOURCE = APK RESOURCE**.
- Internal identifier strings include `RobotoCondensedBold`, `RobotoCondensedRegular`, and `BigShouldersTextMedium`, but the resource metadata inspected does not prove which one style `132` selects. **SOURCE = APK RESOURCE; FONT BINDING = UNVERIFIED**.
- The reference declares Camera/Location capabilities and CameraX extension libraries. This clone uses the Android Camera2 API to keep the minimal build dependency-free; it does not reproduce the reference's business logic. **SOURCE = APK MANIFEST**.

## Implementation mapping

- `WatermarkRenderer` is the single style/geometry authority. `WatermarkOverlayView` invokes `drawLive`; `CameraEngine` invokes `drawSaved` after camera capture and before MediaStore write.
- Typeface is Android's public `sans-serif-condensed` in bold weight. It is a legal platform fallback selected from screenshot shape; the reference APK contains but does not prove use of `RobotoCondensed` for this exact template. No bundled reference font is reused.
- The implementation uses white fill, a dark outline and subtle shadow for legibility over arbitrary camera scenes. The divider uses `#FFB82B`.
- The format is now `HH:mm`, `dd Tháng M,yyyy`, and lowercase Vietnamese weekday names, matching the supplied reference's punctuation/casing.

## Test status

- The fresh debug APK builds successfully with JDK 21.
- The debug APK was installed successfully on the connected 1080 × 2400 device with Camera and precise Location permission granted. `MainActivity` was confirmed as foreground.
- A physical-camera test photo was captured and saved as `Pictures/TimemarkCamera/TIMEMARK_20260809_081059.jpg` (2256 × 4000 px). The pulled verification copy is `captures/device-v1.3-watermarked.jpg`; it visibly contains the time, date, weekday, address, Timemark branding, and rotated verification text burned into JPEG pixels.
- The device preview screenshot is `captures/device-v1.3-live.png`; the reference live screenshot/UI dump are `captures/reference-live-device-20260809.*`. These files provide the final direct-device comparison artifacts.
