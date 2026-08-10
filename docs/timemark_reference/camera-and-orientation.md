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

## Production runtime behavior

- `PreviewActivity` báo `requestedOrientation=SCREEN_ORIENTATION_PORTRAIT`. Khi cảm biến đề xuất `ROTATION_90`, display vẫn ở `ROTATION_0`; UI không xoay Activity. **RUNTIME**
- Preview giữ khung portrait nhưng cảnh camera theo đúng hướng cầm máy. Watermark thay đổi bốn hướng độc lập với UI. **RUNTIME**
- Chuyển hướng có hysteresis khoảng 60°, không đổi ngay khi rung quanh mốc 45°. **RUNTIME**; số đo được cung cấp từ phiên kiểm tra trước, raw sensor log chưa nằm trong `evidence/latest`.
- Bounds cho thấy watermark đổi kích thước và anchor theo hướng cầm máy, phù hợp hành vi của `RotateLayout`. Việc production thật sự dùng class/remeasure nội bộ nào vẫn là **SUY ĐOÁN** cho tới khi có DEX.

Bounds tuyệt đối trên màn hình 1080 × 2400:

| Hướng | Bounds watermark |
| --- | --- |
| Dọc, address một dòng | `[0,1436]–[751,1696]` |
| Dọc, address hai dòng | `[0,1385]–[751,1696]` |
| Ngang A | `[0,256]–[311,1007]` |
| Ngang B | `[769,945]–[1080,1696]` |

**UIAUTOMATOR**. Bounds một dòng nằm trong `captures/reference-live-device-20260809-ui.xml`, có `package="com.oceangalaxy.camera.new"`; bounds hai dòng và hai hướng ngang đến từ phiên đo runtime được cung cấp. Chiều cao watermark thay đổi theo số dòng address.

## What remains unverified

- DEX hiện chỉ chứng minh CameraX trong `SampleCameraActivity`; chưa chứng minh `PreviewActivity` production dùng đúng class/path đó. **CHƯA XÁC MINH**
- Công thức nội bộ chính xác cho sensor, preview crop và transform chưa được trích từ DEX production. Hành vi đầu ra ở trên đã được runtime chứng minh, nhưng không được gán nhãn DEX. **CHƯA XÁC MINH**
- Mirror mode của preview/video và pipeline encoder video. **CHƯA XÁC MINH**
