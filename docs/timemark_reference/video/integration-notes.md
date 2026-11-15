# Video integration ownership and checks

`VideoWatermarkRenderer.java` is the new owned helper. `camera-engine-video.patch` contains only video changes relative to the exact pre-existing dirty engine recorded before analysis. It is a review artifact: those changes are already present in the shared working tree. Do not apply it twice. This zero-context patch avoids trailing-whitespace context rows in the documentation artifact; an explicit future apply requires `git apply --unidiff-zero` after verifying the recorded baseline.

The baseline included CameraX video dependencies, manifest audio permission, recorder fields/callbacks, the video mode UI, MainActivity integration and recording drawable. Those are pre-existing changes and must not be attributed to or staged by the video chat. The new patch relies on that baseline. A commit containing only the patch against older HEAD would be incomplete; retain the patch until the baseline owner integrates it.

Source files owned by the photo/UI chats were not edited for this work. The MainActivity callback already holds preview orientation during recording and rejects template editing while recording. It still lets a location edit change preview while the recording address is a snapshot; compare that behavior with the original before changing it. Video preview remains the existing3:4 UI viewport, although the recorded MP4 is9:16. The original observed video preview extends to9:16 and overlays controls. A CameraScreen/PortraitPreviewFrame change should be integrated by its UI owner after reviewing the saved MP4 flow.

The video renderer uses the existing live template renderer; all non-34 templates retain their prototype fidelity. The right brand is measured for the default-34 reference. The fixed-date instrumentation exists only in the ignored video/probe source tree added through the local test init script. It is excluded from normal production source sets. Production recording time always uses System.currentTimeMillis().

Validation artifacts remain ignored under reference_app/v10.0.311/video: exact original MP4, frames/metadata, REA bundles, baseline/diff, APK hash, renderer probe and build log. Build/test package is com.pgkit.timemarkcamera.codexvideo. Do not overwrite other test packages or install over the user's differently signed original clone.

Use flock --close with all shared lock commands. A daemon inherited a runtime lock during automatic ADB startup; recovery left the server running and quarantined that stale inode. The current canonical runtime lock is usable.

## Current checkpoint

Owned commits: `b8b41b2` (production video analysis, ledger and engine review patch) and `9e8e3b7` (new raster/cache helper). A fresh `clean assembleDebug` passed with35 tasks executed. CameraEngine changes are already present but intentionally remain uncommitted with the pre-existing shared baseline; the review patch excludes later photo-save edits. No UI, photo-save or orientation-mapping change from another chat was staged.

Runtime blocker: controlled portrait MP4 ID1000011481 finalized on Xiaomi, but the USB device disappeared before transfer. The device was still absent from both ADB and USB after the user reported reconnecting; only LG LM_V510N and the UI chat's emulator5582 were visible. No substitute device was used. The runtime lock is released. Next: retrieve that exact MP4 when Xiaomi reappears, inspect decoded frames and metadata, then ask for landscape top-left/top-right and inverted portrait holds one at a time. Do not mark four-direction verification complete from the earlier photo tests or emulator orientation.
