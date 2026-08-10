# Camera and orientation: extracted facts

## Manifest facts

- Camera permission is declared. **MANIFEST**
- Microphone permission is declared. **MANIFEST**
- The APK declares optional `camerax-vendor-extensions.jar` and `androidx.camera.extensions.impl`. **MANIFEST**
- Camera-related layouts include `activity_camera_preview`, `activity_cs_camera`, `layout_view_stub_camera_surface`, `layout_camera_scale_widget`, `view_preview_bottom_camera_action_global`, and `view_preview_bottom_camera_video_action_global`. **RESOURCE**

## DEX fact: CameraX sample implementation

`classes2.dex` contains `com.xhey.xcamerasdk.ui.SampleCameraActivity`. The disassembly at offsets `0x796258`–`0x79629e` shows:

1. a `ProcessCameraProvider` is obtained;
2. `androidx.camera.core.Preview.Builder.build()` is called;
3. the resulting preview receives `SampleCameraActivity.f0`, whose type is `androidx.camera.view.PreviewView`, through `PreviewView.getSurfaceProvider()` and `Preview.setSurfaceProvider(...)`;
4. an `androidx.camera.core.ImageCapture.Builder` is created;
5. `ImageCapture.Builder.setTargetAspectRatio(int)` is invoked before further configuration.

This is direct bytecode evidence for that SDK sample activity. **DEX**

## What the extracted source does not yet establish

- Whether `PreviewActivity` itself uses this exact `SampleCameraActivity` path. **CHƯA XÁC MINH**
- The formula used by the production activity for sensor orientation, display rotation, preview crop, EXIF rotation, mirror mode or landscape placement. **CHƯA XÁC MINH**
- The production photo/video encoder/output pipeline. **CHƯA XÁC MINH**

## Screenshot observation only

Supplied portrait reference screenshots show an upright camera scene and a watermark overlay. They do not prove the code path or rotation algorithm. **SCREENSHOT**

