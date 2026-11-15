# Timemark v10.0.311: video production pipeline

Status: investigation and implementation in progress. No claim of video/JPEG pixel parity.

## Scope and retained artifacts

Exact base APK SHA-256: `45ddfcecd097aa3c3b557f43cab42df9bdc2c5e3709475bdeb2ed2b621de9893`; package `com.oceangalaxy.camera.new`, version `10.0.311` / `100031100`. Complete base/arm64/vi/xxhdpi set is retained locally. Android code below was inspected with REA/JADX 0.7.1. Static observations do not prove runtime dispatch, reflection coverage or active native libraries. Evidence IDs and source bodies are in [evidence-ledger.md](evidence-ledger.md) and ignored `reference_app/v10.0.311/video/rea-video-checkpoint*.json`.

Video ownership: new video helpers and video methods in the already-dirty `CameraEngine.java`. The pre-existing engine is preserved as `video/CameraEngine.baseline.java` and `.diff`. Shared renderer, MainActivity, CameraScreen and DeviceOrientation were not edited by this video investigation.

## Source chain

1. Production `CameraWidget.t(kc.e)` dispatches ShootStatus 3 to `J().i(confirm)` (RecordHelper `b60.t3`), status 4 to `j(stoppedByUser)`, statuses 6/7 to pause/resume helpers. The retained earlier `rea-camera-widget-t.json` is Smali fallback, not a sample activity.
2. `t3.i` allocates a session ID and output path, checks foreground/stopping state, then invokes async file/storage preparation and `b60.q3.invoke`.
3. `q3.invoke` checks session identity again. It sets each RotateLayout rotation gate to false, snapshots angle, collects background/main/QR/timer/location/brand layers, disables dragging and layout, chooses encoder mode and resolution, and fills `oc0.a` recorder state.
4. Main watermark creation uses `c0.x(flag, container, rotateLayout, false)`. It calls `y4.w` to rasterize the live view and `y4.E` to construct normalized position/size plus source alpha. The `false` branch uses scale 1; the photo high-resolution branch in `c0.x` is separately conditional. Decompiled `c0.x` has numerous JADX type warnings; only its supported branch/calls are used here.
5. `b60.o3.invoke` starts hardware mode 1 or software mode 2. A one-second reactive interval runs `b60.r3.invoke`, which rebuilds the main watermark and another dynamic layer and calls `CameraGLSurfaceView.setRecordWatermark`. The clock is therefore not frozen as one immutable start bitmap. Dragging/layout/orientation are locked while its time-dependent bitmap refreshes. Periodic rasterization can also pick up text/data updates; continuous GPS/address behavior is **CHƯA XÁC MINH**.
6. `CameraGLSurfaceView.onDrawFrame` obtains camera/processed texture H0. Preview and video consume that texture. For recording it constructs `render.r`, applies recording angle/mirror handling, blends watermark textures and sends resulting texture I0 plus frame timestamp to hardware/software sampling handlers. This method required complete Smali fallback.
7. `render.r.a` updates GL textures using `GLUtils.texImage2D`; `render.q.k` draws each WaterMark using normalized x/y/w/h, source alpha and sorted z-order. `render.r.b` applies the recording angle before the blend. Static code establishes composition every video frame, with watermark texture refresh on the separate one-second schedule.
8. `pc0.a.c` constructs MP4 muxer, AVC video encoder and optional audio encoder. `pc0.i.k` configures AVC surface input, target 24 FPS and one-second I-frame interval. The bitrate comes from selected video quality (`t3.c`, with 3,000,000 fallback), otherwise a pixel-based default. `pc0.h` creates MediaMuxer MPEG-4 with orientation hint 0: this route rotates pixels before encoding.
9. Hardware audio is AAC-LC, two channels. The captured MP4 confirms 44,100 Hz and about 96 kb/s; those observed values are not inferred merely from class presence. Software `startRecord` is called with 30 FPS, 44,100 Hz, two channels and 96,000 b/s; it forces 544x960 or 960x544. Its actual device execution has not been observed.
10. On stop, `t3.j` invalidates the session, stops selected recorder, clears recording GL state and unlocks drag/layout/rotation. `f60.d.b` forwards to `e0.w.b`, which validates the file, constructs VideoInfo and emits a result wrapped with `wrapDisableChangeWatermark`. Editing a saved video watermark is therefore explicitly disabled in this result route. Final public media publication beyond that result is not fully traced; the actual original sample was present in MediaStore/DCIM/Camera.

## Effects/filter boundary

`CameraGLSurfaceView.onDrawFrame` routes through `render.b.c` before texture H0 is used for video. `render.b.d(tz.a, strength)` retains effect models/strength; `render.b.c` has beauty and bitmap/LUT-style texture stages and can bypass effects according to state. This is evidence of an available production processing chain, not evidence that every bundled effect, filter library, Aegis branch or native library is active on this phone. The selected filter ID, LUT/color transform, beauty strength and runtime gate for the recorded sample remain **CHƯA XÁC MINH**. No arbitrary color filter was added to PG-kit based on camera color differences.

## Photo/video comparison

| Property | Evidence for video | Comparison with photo |
| --- | --- | --- |
| Template and fonts | Main bitmap is rasterized from the same live RotateLayout/view; template 34 binding uses BigShouldersText-Medium and RobotoCondensed-Regular | Shared live view is established. Same final pixel rasterization is not established. |
| Size/wrap/position | Normalized WaterMark x/y/w/h comes from current view; video uses scale-1 bitmap | Photo has an optional high-resolution raster scale. Different output dimensions and resampling can change edges, outlines and wrapping. |
| Outline/shadow | Already baked into rasterized view, then uploaded as alpha texture | Same view style is supported statically; matching post-JPEG and post-H.264 edge pixels is **CHƯA XÁC MINH**. |
| Clock | Watermark bitmap refreshed every second during recording | Photo snapshots capture time. Video must keep advancing time. |
| Template/location editing | Drag/layout/rotation locked at start; saved result disables watermark change | Shared editor before capture does not prove identical editor behavior while/after capture. |
| Orientation | Recording angle is captured at start; GL transforms pixel texture; muxer hint 0 | JPEG uses independent capture/EXIF normalization. Four physical video directions require separate checks. |
| Resolution/quality | Selected quality arrays; hardware AVC 24 FPS configuration; software fallback 30 FPS at 544x960 | Does not inherit photo aspect ratio, JPEG quality or photo size. |
| Audio | Optional hardware AAC encoder; AAC stereo in actual sample | Photo has no audio. |
| Authenticity metadata | Original MP4 contains an opaque description field | Its meaning/service validation was not decoded or cloned. PG-kit does not reproduce original trust/signature services. |

## Real original sample

Captured under the shared ADB runtime lock on real Xiaomi `DEVICE_SERIAL_REDACTED`, normal portrait, 2026-11-14 around 19:41–19:42 local. Foreground was checked before each adaptive tap. New MediaStore ID `1000011459`, filename `20261114_194156_234, Example Street.mp4`. Only this newly created MP4 was copied. Samples and screenshots remain ignored local evidence.

`original-portrait.mp4`: 19.939956 s, 720x1280 normalized pixels, H.264 High/yuv420p/BT.709, about 3.67 Mb/s, 284 frames (14.24276 FPS reported by ffprobe). AAC-LC stereo 44.1 kHz, about 96.1 kb/s, starting at 0.507 s. MP4 display-matrix rotation is 0. The configured 24 FPS must not be reported as observed 24 FPS. Low-light/thermal/camera scheduling causes are **CHƯA XÁC MINH**.

The 2-second decoded frame contains native-style template 34 at bottom-left and a Timemark brand at bottom-right. It contains neither the app's UI buttons nor the JPEG renderer's side verification text. Photo preview has a 3:4 camera viewport; video preview extends to a 9:16 camera region and overlays some controls before recording. An identical whole-screen screenshot is not an acceptance criterion across those modes.

## PG-kit change and outstanding checks

`VideoWatermarkRenderer` rasterizes the existing live renderer into a software ARGB bitmap at native layout density, caches it for the current device-clock second, and composites it for every frame. Template and address are constructor snapshots. Branding is a measured reconstruction of the default-34 clip, not proven for other brand variants. `none` suppresses video watermark layers. CameraEngine clears every effect canvas, applies crop/rotation/mirror in buffer coordinates and releases session bitmaps on the effect thread. HD is preferred with device capability fallback; 24 FPS is a target, not a guarantee.

This is a reproduction using CameraX, not a port of Timemark's recorder. Shared JPEG renderer was not changed. Other templates remain prototypes. Font/layout/bitmap matching, H.264/RGBA parity, front camera, clock boundary, four physical directions, interruption/storage errors and filter variants require validation. The proposed MainActivity/UI integration changes, if needed, will be supplied as small patches here rather than overwriting other chats' files.

Build of `.codexvideo` succeeded with JDK17/build-tools35. First real-phone install was canceled by the phone (`INSTALL_FAILED_USER_RESTRICTED`); no unchanged retry was made. Waiting for the user to unlock/accept installation while continuing static analysis.

## Coordination lock recovery

An ADB server automatically launched by an install inherited fd3 for `/tmp/pgkit-adb-runtime.lock`. `pgrep` showed only that daemon, and `lslocks` showed it was the sole lock holder; no ADB client was running. The dead inode was quarantined as `/tmp/pgkit-adb-runtime.inherited-server-34081.lock` and the canonical path was recreated. ADB server was not stopped or restarted. All subsequent scoped lock commands use `flock --close` so a subprocess/daemon cannot inherit the lock. Full checks are retained in `video/adb-lock-recovery.txt`.

## Renderer probe

An isolated debug instrumentation probe (ignored source tree, excluded from production builds) passed minute-boundary clock advancement, cached redraw equality against a fresh renderer (no accumulated pixels), and transparent `none`. At density450, 720x1280, its default-34 solid-text bounding boxes are within 0–1 pixels of the original encoded frame. Threshold-mask IoU varies by region (clock about0.94, address about0.61); this is explicitly not pixel parity because the probe PNG and original lossy H.264 frame have different edge/shadow/color histories. Measurements remain in `video/native-video-geometry.json`.

## Resume checkpoint, 2026-11-15

The `.codexvideo` APK installed successfully after explicit user readiness. A newly retrieved previous-session clip (MediaStore1000011464) decodes as H.264720x1280, about23.27 FPS, AAC stereo48kHz,9.626444s. The frame has readable bottom-left native-layout watermark and bottom-right branding, without side verification text. Physical orientation at the creation of this unsupervised clip is not attributed; the four-direction acceptance test remains pending. Its audio sample rate differs from the original44.1kHz and remains a known CameraX-profile difference.

## Controlled physical portrait, transfer pending

On2026-11-15 the user explicitly confirmed normal portrait. Foreground was corrected from the separate `.codextest` UI package to `.codexvideo`; the before/recording screenshots show upright watermark and a portrait Activity. The controlled recording finalized as MediaStoreID1000011481, filename`TIMEMARK_VIDEO_20261115_091217_44-12-to-44-kp3-example-district-ho-.mp4`,16304ms,720x1280,is_pending0. Xiaomi disconnected before the MP4 transfer. The local `controlled-portrait.mp4` is an empty failed-transfer placeholder and is not valid output evidence. Final MP4 decoding, normalized orientation and the other three physical holds remain pending. LG serialDEVICE_SERIAL_REDACTED was not substituted. Shared runtime lock was released.

A fresh JDK17 clean assembleDebug passed with35 tasks executed; the test APK was frozen under the video evidence directory. Owned-file diff checks passed. Whole-checkout diff check still reports pre-existing CRLF/trailing whitespace in old reference captures and gradlew.bat; these unrelated files were preserved.
