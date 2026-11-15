# Timemark camera controls: source evidence and implementation

Scope: exact Timemark v10.0.311 APK set; production camera UI, focus, exposure, zoom and filter routing. Retained sources and Evidence IDs are listed in [evidence-ledger.md](evidence-ledger.md). The APK was inspected using REA/JADX, with additional original-app screenshots on Xiaomi `DEVICE_SERIAL_REDACTED`. Packaged SDK/sample classes alone do not establish a production route.

## Focus and exposure

Production `CameraWidget` holds `CameraGestureCaptureView`, `CameraGLSurfaceView` and `CameraScaleAdjustView`. The gesture view's constructor, `c`, `d`, `onDraw`, `onTouchEvent`, and its listener `vc0.l` establish these static facts:

| Property | APK observation |
| --- | --- |
| Focus target | White outer circle, radius 35 dp, stroke 2 dp; inner circle, radius 5.5 dp, stroke 1.5 dp. Additional thin dark edge circles improve contrast. |
| Exposure sun | `cam_exposure_adjustbar`, drawn at 25 dp. Resource ID `0x7f0803ab`, packaged 72×72 WebP in xxhdpi. |
| Exposure rail | 40×130 dp region, 1 dp rail. Sun center is 60 dp to the right for a focus in the left half, or 60 dp to the left in the right half. |
| Drag hitbox | Relative x bounds −78/+152 dp in the left half, mirrored in the right half; y ±115 dp. Dragging is not restricted to the thin visible rail. |
| Drag | Requires vertical distance greater than three times horizontal distance. Moves the sun by 0.15× vertical delta, clamps its travel and converts it to progress 0–100. |
| New focus | Resets progress to 50 and calls listener `u(50)` before starting the target animation. |
| Pinch | Uses two-pointer distance deltas, with a 2-pixel threshold, and marks the gesture so it cannot also trigger focus. |
| Long press | `vc0.l.onLongPress` has an empty body. No AF/AE lock is established here. |
| Dismiss | Target animation lasts 1500 ms; a confirmed tap selects a 1200 ms dismissal delay; `a()` fades alpha 0.6→0 over 300 ms. Callback timing and animation phase come from static code; no measured frame parity is claimed. |

Original runtime captures `camera-ui/original-focus.png` and `original-exposure-up.png` were taken after checking the focused production Activity under the shared runtime lock. The latter was already outside the target's visible interval, so it does **not** establish a brightness-response measurement. The user-supplied original and clone JPEG comparison also differs in scene/AE state; its color difference does not establish a selected filter.

PG-kit replaces the yellow square and rotated platform SeekBar with a white circular target and drawn sun/rail. The enlarged transparent hitbox forwards taps and multi-touch to PreviewView and cancels the pending tap when a vertical exposure drag begins. CameraGestureController suppresses single-touch focus after any multi-pointer gesture. CameraX still owns metering coordinates, AF+AE, the existing three-second auto-cancel, zoom clamping and exposure bounds.

The reconstructed target uses a short settling scale, then alpha 0.6 and a 300 ms fade. It does not yet reproduce every intermediate radius/alpha phase of the original 1500 ms animation. Exposure UI currently emits discrete steps through the existing `onExposureChanged` callback; 16 dp of finger travel requests one device exposure index. This is not yet the original absolute 0–100 mapping. Shared-engine integration is explicitly proposed below.

## Zoom and camera icons

`CameraScaleAdjustView` contains a device-dependent preset array, scale state, macro gates, 42 dp spacing and separate selected/unselected text paints. `j` draws small dark circles and text. The observed phone displays `2` and selected `1.0x`; it does not display a `0.5` preset in that captured state. Static preset presence does not prove ultra-wide camera availability.

PG-kit uses compact lower-right circles, an actual-ratio label after pinch and 44 dp touch targets. A `setZoomRange` screen hook hides unsupported presets when supplied actual engine capabilities. The proposed shared patch supplies those capabilities; until it is integrated, the existing `0.5` button remains visible and CameraEngine clamps it to the supported minimum, as required by AGENTS.md.

Edit and location controls now use outline vector paths. Flash has a distinct off icon. Menu, camera-switch and verification symbols were reconstructed as vectors from the reference appearance; they are approximations, not copies of original raster pixels. Relevant packaged resources include `flash_off_light` (`0x7f0804a7`) and `img_camera_switch_light` (`0x7f0806be`). The header spacing was adjusted using the supplied reference. Bottom template editing and watermark renderers were not changed.

## Production filter route

There is an actual production effect route, beyond library presence:

1. `b60.s.invokeSuspend` restores `com.xhey.xcamera.data.pref.d.m0()` and passes the selected `FilterInfo` to `CameraWidget.N`, then restores beauty state through `D(false)`.
2. `CameraWidget.N(FilterInfo)` converts ordinary filters into a `tz.a` model with type `lut`, category `LUT_FILTER`, a path, numeric ID and `filer_intensity`; it calls `CameraGLSurfaceView.t`.
3. The beauty-model branch maps IDs 1–8 to effect IDs 103/101/201/202/203/204/205/603.
4. `CameraGLSurfaceView.t` retains the models and intensities and forwards them to `render.b.d`. The video investigation separately traced the production frame processing stage; see [video production pipeline](../video/production-pipeline.md).

The base APK contains 19 `assets/filters/*.png` images. `res_original.png` and `res_landscape_1.png` are 512×512. A local numeric check is consistent with an 8×8 tiled 64³ LUT: the original image differs from an ideal identity cube by mean 0.246 channel values, max 0.477, consistent with 8-bit quantization. This is an analyst inference about the packaged image layout, not proof of runtime sampling, interpolation, color space or the active LUT.

The original phone's selected filter, intensity, beauty gates and native effect implementation remain **CHƯA XÁC MINH**. No arbitrary brightness/color filter or speculative filter picker was added. A faithful clone needs the selected UI route, shader/LUT sampling and matched preview/JPEG output measurements. Presence of `libnative-filters.so` is not evidence that it was active in the compared screenshot.

## Shared-file integration and remaining limits

[shared-camera-hooks.patch](shared-camera-hooks.patch) is a small proposed patch for CameraEngine and MainActivity. It resets exposure when focusing and supplies actual zoom bounds. It has not been applied because those files are owned concurrently. `git apply --check` succeeded against the shared checkout when prepared. Exact absolute exposure progress, asynchronous engine request state and a shared preview/JPEG/video filter implementation need further integration, rather than UI-only approximations.

Activity portrait lock, Preview display rotation, ImageCapture DeviceOrientation rotation, capture snapshots and production time sources remain unchanged. Four-direction preview/JPEG tests were not repeated for this UI-only work; prior orientation evidence does not establish filter or video parity.
