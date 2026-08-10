# Timemark Camera

Ứng dụng Android native chụp ảnh kèm thời gian, ngày/thứ và vị trí lấy trực tiếp từ hệ thống điện thoại. Watermark được render vào pixel JPEG trước khi lưu, không phải lớp phủ tạm thời.

## Chạy dự án

1. Mở dự án bằng Android Studio hoặc đặt đường dẫn Android SDK trong `local.properties`.
2. Build with JDK 17–21: `gradlew.bat assembleDebug` (the system default JDK 24 is not supported by this Gradle/Android Gradle Plugin combination).
3. APK debug: `app/build/outputs/apk/debug/app-debug.apk`

Ảnh chụp được lưu tại `Pictures/TimemarkCamera` qua Android MediaStore. `WatermarkRenderer` is shared by the live overlay and final JPEG renderer, so the timestamp/address is burned into the saved image.

## Cấu trúc

- `MainActivity`: giao diện, quyền runtime, vị trí và hành động chụp.
- `CameraEngine`: Camera2 preview/capture, xoay ảnh, render watermark và lưu MediaStore.
- `WatermarkOverlayView` + `WatermarkRenderer`: watermark realtime và bản render JPEG chung style.

Thông số visual và mức độ xác minh reference nằm trong `watermark_spec.md` và `reference_analysis.md`.

## Quyền sử dụng

- Camera: xem trước và chụp ảnh.
- Vị trí chính xác/gần đúng: lấy GPS hệ thống và chuyển tọa độ thành địa chỉ khi Geocoder của thiết bị có dữ liệu.
- Android 9 trở xuống: quyền bộ nhớ để lưu vào thư viện ảnh.
