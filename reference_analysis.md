# Reference analysis (đã đối chiếu 2026-08-10)

File này là bản tổng hợp, không phải raw source of truth. Khi có mâu thuẫn, ưu tiên runtime app mẫu, APK/DEX/resource, UIAutomator/JPEG, rồi mới tới Markdown và screenshot estimate.

## Reference build

- Package `com.oceangalaxy.camera.new`, version `v10.0.189` / code `100018900`. **RUNTIME/MANIFEST**
- Main activity `com.xhey.xcamera.ui.camera.picNew.PreviewActivity`. **RUNTIME/MANIFEST**
- minSdk 26, target/compile SDK 35. **MANIFEST**
- Thiết bị kiểm tra 1080 × 2400 px, 450 dpi. **RUNTIME**
- Ảnh hoặc UI của package clone `com.pgkit.timemarkcamera` không được dùng làm bằng chứng cho app mẫu.

## Camera và orientation

- Production Activity luôn portrait; display giữ `ROTATION_0` ngay cả khi sensor đề xuất `ROTATION_90`. **RUNTIME**
- UI không có layout camera landscape. Preview/capture/watermark phản ứng với hướng cầm máy mà không xoay Activity. **RUNTIME**
- Watermark có hysteresis khoảng 60° và đổi anchor theo hướng cầm máy. **RUNTIME/UIAUTOMATOR**. Geometry phù hợp `RotateLayout`; cơ chế class/remeasure nội bộ chưa có DEX nên là **SUY ĐOÁN**.
- DEX hiện chỉ chứng minh CameraX trong `SampleCameraActivity`; chưa đủ để nói production `PreviewActivity` dùng cùng đường gọi. **DEX + CHƯA XÁC MINH**
- Clone hiện dùng CameraX, không phải Camera2 thuần hoặc dependency-free. **CODE**

## UI runtime

Trên màn hình 1080 × 2400:

| Thành phần | Bounds |
| --- | --- |
| Top bar | `[0,110]–[1080,256]` |
| Preview | `[0,256]–[1080,1696]` |
| Shutter | `[428,1876]–[653,2101]` |
| Xác thực ảnh | `[372,146]–[670,221]` |
| Plus | `[749,149]–[911,217]` |

**UIAUTOMATOR**

## Watermark

- Template `Time&Location`, base ID 34; time style 132; address style 300; accent `#FFC233`; text `#FFFFFF`. **ASSET**
- Live sizes: time 38 dp, date/weekday 16 dp, address 14 dp, address maxWidth 260 dp, margin 8 dp. **RUNTIME**; raw style dump chưa nằm trong evidence.
- Shadow runtime: dx 0, dy 1.5, radius 3.5. Layout resource không khai báo bold/stroke. **RUNTIME/RESOURCE**
- Time có khả năng cao dùng `BigShouldersText-Medium`; nội dung phụ có khả năng cao dùng `RobotoCondensed-Regular`. APK có hai font này nhưng binding style vẫn chưa được chứng minh bằng DEX. **ASSET + SUY ĐOÁN**
- Weekday reference dùng `Thứ Hai`…`Thứ Bảy`, riêng Sunday dump là `Chủ nhật`. Logo saved image tô `Time` vàng và `mark` trắng; verification dùng icon khiên. **UIAUTOMATOR/JPEG**
- Bounds live phụ thuộc address: dọc một dòng `[0,1436]–[751,1696]`, dọc hai dòng `[0,1385]–[751,1696]`; ngang A `[0,256]–[311,1007]`; ngang B `[769,945]–[1080,1696]`. **UIAUTOMATOR**

## JPEG và media

- Portrait 1920 × 2560; landscape 2560 × 1920; MediaStore orientation 0. **JPEG**
- Watermark được burn vào pixel, lưu ở `DCIM/Camera`; tên file có timestamp và một phần địa chỉ. **JPEG/RUNTIME**
- Preview crop và JPEG không cùng hệ tọa độ. **JPEG/RUNTIME**

## Clone implementation status

- `CameraScreen` dựng UI portrait bằng Java.
- `CameraEngine` dùng CameraX cho preview/capture, EXIF normalization và MediaStore.
- `DeviceOrientation` là nơi ánh xạ sensor/Surface và hysteresis.
- `WatermarkOverlayView` biến đổi live watermark theo orientation.
- `WatermarkRenderer` dùng font local `BigShouldersText-Medium.ttf` và `RobotoCondensed-Regular.ttf` cho clone; việc này không chứng minh binding của app mẫu.

Các thông số clone cụ thể nằm trong `watermark_spec.md`. Không dùng ảnh trong `captures/` làm source of truth cho app mẫu nếu package tạo ảnh không được xác nhận là `com.oceangalaxy.camera.new`.

## Evidence health

`docs/timemark_reference/evidence/latest/` đã được làm mới bằng `E:\Android\Sdk\platform-tools\adb.exe`: package/version, bốn split APK, resource/template và DEX snippets đều có raw output. DEX snippets vẫn chỉ chứng minh `SampleCameraActivity`; không tự suy rộng sang production `PreviewActivity`.
