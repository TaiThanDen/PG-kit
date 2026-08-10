# Timemark Camera

Ứng dụng Android native chụp ảnh kèm thời gian, ngày/thứ và vị trí lấy trực tiếp từ hệ thống điện thoại. Watermark được render vào pixel JPEG trước khi lưu, không phải lớp phủ tạm thời.

## Chạy dự án

1. Mở dự án bằng Android Studio hoặc đặt đường dẫn Android SDK trong `local.properties`.
2. Build with JDK 17–21: `gradlew.bat assembleDebug` (the system default JDK 24 is not supported by this Gradle/Android Gradle Plugin combination).
3. APK debug: `app/build/outputs/apk/debug/app-debug.apk`

Ảnh chụp được lưu tại `Pictures/TimemarkCamera` qua Android MediaStore. `WatermarkRenderer` is shared by the live overlay and final JPEG renderer, so the timestamp/address is burned into the saved image.

Preview hỗ trợ các thao tác CameraX cơ bản: chọn mức zoom, pinch-to-zoom, chạm để focus/đo sáng, bù sáng EV, flash và đổi camera trước/sau. Mọi giá trị zoom/EV đều được giới hạn theo khả năng camera thực tế của thiết bị.

## Cấu trúc

- `MainActivity`: điều phối lifecycle, quyền runtime và callback.
- `CameraScreen`: toàn bộ giao diện dọc; UI không đổi layout khi xoay điện thoại.
- `CameraEngine`: CameraX preview/capture, sửa EXIF, render watermark và lưu MediaStore.
- `CameraGestureController`: tap-to-focus và pinch-to-zoom trên preview.
- `DeviceOrientation`: ánh xạ duy nhất cho bốn hướng cầm thiết bị.
- `LocationController`: GPS và reverse geocoding.
- `WatermarkOverlayView` + `WatermarkRenderer`: watermark realtime và bản render JPEG chung style.
- `VerificationCodeStore`: tạo và lưu mã xác thực cục bộ.

Agent tiếp tục dự án phải đọc `AGENTS.md` trước khi sửa. File đó mô tả chính xác nơi cần sửa, các bất biến orientation và ma trận kiểm tra bốn hướng.

Thông số visual và mức độ xác minh reference nằm trong `watermark_spec.md` và `reference_analysis.md`.

## Quyền sử dụng

- Camera: xem trước và chụp ảnh.
- Vị trí chính xác/gần đúng: lấy GPS hệ thống và chuyển tọa độ thành địa chỉ khi Geocoder của thiết bị có dữ liệu.
- Android 9 trở xuống: quyền bộ nhớ để lưu vào thư viện ảnh.
