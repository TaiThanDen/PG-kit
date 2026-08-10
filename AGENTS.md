# Hướng dẫn bảo trì PG-kit

Đọc file này trước khi sửa code. Mục tiêu của dự án là mô phỏng camera Timemark trong khi **toàn bộ giao diện ứng dụng luôn khóa dọc**. Khi người dùng xoay điện thoại, chỉ camera, watermark và ảnh JPEG đầu ra thay đổi theo hướng cầm máy; không tạo layout ngang và không xoay `Activity`.

## Bản đồ source code

| File | Trách nhiệm | Sửa file này khi |
| --- | --- | --- |
| `MainActivity.java` | Điều phối lifecycle, quyền runtime và callback giữa các thành phần | Thay đổi luồng khởi động, quyền hoặc hành động cấp Activity |
| `CameraScreen.java` | Tạo toàn bộ UI dọc và chuyển thao tác người dùng qua `Actions` | Thay đổi kích thước, màu, nút hoặc bố cục; không thêm layout landscape |
| `CameraEngine.java` | Bind CameraX, flash/đổi camera, chụp, sửa EXIF, render và lưu JPEG | Thay đổi preview, capture, chất lượng ảnh hoặc MediaStore |
| `DeviceOrientation.java` | Nguồn duy nhất ánh xạ cảm biến sang `Surface.ROTATION_*` và góc xoay watermark | Sửa quy tắc cho 4 hướng cầm điện thoại |
| `WatermarkOverlayView.java` | Cập nhật đồng hồ realtime và vẽ watermark lên preview | Thay đổi cách watermark preview phản ứng với orientation |
| `WatermarkRenderer.java` | Style watermark dùng chung cho preview và JPEG cuối | Thay đổi font, cỡ chữ, khoảng cách, nội dung hoặc logo |
| `LocationController.java` | GPS, vị trí gần nhất và reverse geocoding | Thay đổi cách lấy tọa độ/địa chỉ |
| `VerificationCodeStore.java` | Tạo và lưu mã xác thực cục bộ ổn định | Thay đổi quy tắc mã xác thực |

Tài liệu reverse engineering của app mẫu nằm trong `docs/timemark_reference/`. Những dòng ghi **CHƯA XÁC MINH** không được coi là hành vi đã chứng minh.

## Quy tắc orientation bắt buộc

Luồng đúng:

```text
OrientationEventListener
        |
        v
DeviceOrientation.fromSensorDegrees()
        |------------------------------|
        v                              v
ImageCapture target rotation     WatermarkOverlayView
        |
        v
EXIF correction -> landscape size when needed -> WatermarkRenderer.drawSaved()
```

- `AndroidManifest.xml` phải tiếp tục dùng `android:screenOrientation="portrait"`.
- `Preview` phải dùng rotation của `PreviewView.getDisplay()`. Không cập nhật target rotation của `Preview` bằng cảm biến khi Activity đang khóa dọc; làm vậy khiến khung camera dọc bị đặt nằm ngang.
- `ImageCapture` phải dùng rotation vật lý do `DeviceOrientation` trả về để JPEG có tỷ lệ portrait/landscape đúng với cách cầm máy.
- `WatermarkOverlayView` nhận cùng rotation vật lý. Mọi ánh xạ 0/90/180/270 phải sửa tập trung trong `DeviceOrientation`, không nhân bản `switch` ở file khác.
- Hướng dựng ngược phải xoay watermark 180 độ; không được xử lý chung như portrait dựng đứng.
- Thời gian luôn lấy từ thiết bị bằng `System.currentTimeMillis()`/`new Date()` tại lúc hiển thị hoặc bấm chụp. Không đưa thời gian thử nghiệm hoặc ngày cố định vào production code.

Ánh xạ hiện tại của cảm biến:

| Góc `OrientationEventListener` | Rotation CameraX |
| --- | --- |
| 315–359 hoặc 0–44 | `ROTATION_0` |
| 45–134 | `ROTATION_270` |
| 135–224 | `ROTATION_180` |
| 225–314 | `ROTATION_90` |

## Luồng ảnh đã lưu

`CameraEngine.capture()` chốt thời gian, địa chỉ và rotation ngay tại lúc bấm chụp. Callback nền chạy theo thứ tự:

1. CameraX ghi JPEG tạm trong cache.
2. `applyExifRotation()` chuẩn hóa pixel theo EXIF.
3. `ensureLandscape()` bảo đảm ảnh ngang có `width >= height` khi thiết bị nằm ngang.
4. `WatermarkRenderer.drawSaved()` ghi watermark trực tiếp vào bitmap.
5. `saveBitmap()` lưu JPEG vào `Pictures/TimemarkCamera` qua MediaStore.

Không vẽ watermark JPEG dựa trên kích thước `PreviewView`; preview có crop riêng và không cùng hệ tọa độ với bitmap camera.

## Kiểm tra trước khi bàn giao

Build bằng JDK 17–21:

```powershell
.\gradlew.bat clean assembleDebug
```

APK nằm tại `app/build/outputs/apk/debug/app-debug.apk`. Khi ADB hoạt động:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop com.pgkit.timemarkcamera
adb shell monkey -p com.pgkit.timemarkcamera 1
```

Kiểm tra trên điện thoại thật cả bốn trạng thái: dọc, ngang trái, ngang phải và dọc ngược. Với mỗi trạng thái phải kiểm tra cả preview lẫn JPEG sau khi chụp. Emulator chỉ xác minh build/start/camera không crash; cảm biến orientation giả lập không thay thế kiểm tra bằng điện thoại thật.

## Kỷ luật thay đổi

- Mỗi nhóm thay đổi logic độc lập phải có một commit riêng, tên commit mô tả kết quả.
- Không commit file trong `build/`, ảnh chụp kiểm thử, APK sinh tạm hoặc dữ liệu thiết bị.
- Trước commit chạy `git diff --check` và build debug.
- Không sửa đồng thời UI, orientation và renderer trong một commit nếu chúng không phụ thuộc trực tiếp.
- Nếu thử nghiệm orientation thất bại, quay lại commit tốt gần nhất bằng commit revert; không xóa lịch sử bằng `reset --hard`.
