# Camera UI verification checkpoint

Verified 2026-11-15. This checkpoint covers the camera controls and gesture reconstruction, not complete Timemark parity.

## Builds and isolation

JDK17, build-tools35, Gradle offline debug build passed. The regular test APK is retained in ignored `reference_app/v10.0.311/camera-ui/camera-ui-debug.apk`; it uses `.codexcamera`, separate from the existing clone and other chats' test packages. Builds were serialized using `/tmp/pgkit-build.lock`. The temporary source-set/manifest instrumentation is outside production source directories and is omitted from the regular APK.

The previous `emulator-5580` was no longer connected. This chat started a separate read-only instance of the same AVD on `emulator-5582`, without saving its state. A SystemUI ANR during that emulator's startup was cleared on this separate emulator; it was not an application crash. No other emulator was stopped or reconfigured.

Real-phone installation of the earlier camera test APK was canceled by Android with `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`. It was not retried unchanged. Original-app focus screenshots were acquired separately under the shared runtime lock after foreground checks. No final new-build phone test is claimed.

## Meaningful gesture probe

An isolated Instrumentation class exercised the actual CameraGestureController and FocusExposureView with Android MotionEvents. Result:

```text
INSTRUMENTATION_RESULT: pinchScale=1.6666669
INSTRUMENTATION_RESULT: status=PASS: tap, pinch, no ghost focus, next tap, exposure up/down, near-focus hitbox, preview forwarding
INSTRUMENTATION_CODE: 0
```

The probe checks a single tap, multi-pointer zoom, absence of an unwanted focus after lifting pinch fingers, the next valid tap, upward/downward exposure drags, taps inside the larger focus region, and rejection of unrelated preview touches. Source and full output are retained in ignored `camera-ui/probe/` and `probe-result.txt`.

## Running camera flow

The actual `.codexcamera` Activity was launched on API35 at 1080×2400, density450. The screen was recorded while tapping focus, dragging vertically near the focus point, selecting 2x, tapping near the edge and waiting for dismissal. Frames were extracted and visually inspected. Native camera dumps report:

| State | Exposure index | Camera zoom ratio |
| --- | --- | --- |
| After upward drag | 6 | 1.00000000 |
| After downward drag | 0 | 1.00000000 |
| After selecting 2x | 0 | 2.00000000 |

Retained evidence: `final-flow.mp4`, `final-motion-sheet.png`, `final-*.png`, `final-camera-values.json` and full camera dumps under ignored `reference_app/v10.0.311/camera-ui/`. `final-runtime.log` contains no AndroidRuntime errors. The recorded emulator feed is synthetic and is not a camera color-fidelity measurement. No release-device 60 FPS claim is made.

Scoped `git diff --check` passed for owned Java, vectors and docs. The whole checkout has unrelated pre-existing CRLF/trailing-whitespace changes in old evidence and `gradlew.bat`; those files were preserved. `git apply --check docs/timemark_reference/camera-ui/shared-camera-hooks.patch` passed. The shared patch remains a proposal.

## Known differences

- Filter selection, shader sampling, beauty behavior and matched preview/JPEG colors remain unverified. The production LUT route is documented, but no speculative transform was added.
- Exact absolute exposure mapping and reset-on-focus require the shared-engine integration. The current control uses the existing bounded exposure-step API.
- The zoom capability hook needs the proposed MainActivity/CameraEngine patch. Until then, `0.5` remains visible and is clamped by CameraEngine where unsupported.
- Target geometry and fade improved, but the complete original pulse choreography and all vector pixels are not identical.
- Default font scale and portrait UI were inspected. Four physical orientations/JPEG capture, large-font layouts, front camera, video and template editing are outside this change's validation.

Owned commit preparation excludes all pre-existing video-mode hunks from CameraScreen, preserving those changes in the working checkout for their owner.
