# Việt/Anh theo ngôn ngữ hệ thống — 2026-11-16

## Phạm vi

- Android string resources: `values` là fallback tiếng Anh, `values-en` là tiếng Anh rõ ràng, `values-vi` là tiếng Việt. Có bộ tiếng Anh riêng để ưu tiên đúng khi danh sách hệ thống đặt Anh trước Việt.
- Camera, nút và accessibility, sheet độ phân giải/tỷ lệ/vị trí/thời gian, danh sách/sửa mẫu watermark, thư viện, chọn nhiều, chia sẻ/xác nhận xóa, thông tin ảnh/video và điều khiển video dùng resources.
- Số lượng media và số mục đã chọn dùng Android plurals. Ngày/giờ UI dùng locale của resources và lựa chọn 12/24 giờ hệ thống. Lịch dùng tên tháng/thứ và ngày đầu tuần theo locale. Tốc độ phát dùng dấu thập phân theo locale.
- Không ép Locale hoặc thêm lựa chọn ngôn ngữ trong production. Activity tiếp tục portrait; hệ thống đổi ngôn ngữ sẽ tạo lại Activity bằng cơ chế Android.
- Không dịch địa chỉ hoặc nội dung trường watermark. Không đổi màu, font, style, thời gian hay pipeline capture/orientation để dịch UI. Các renderer, `WatermarkTemplate`, `DeviceOrientation`, `WatermarkClock` byte-identical với đầu tác vụ. Chữ Việt trong bitmap watermark là nội dung được giữ nguyên, không phải nhãn giao diện chưa dịch.

## Kiểm thử

JDK `/home/developer/.local/opt/jdk-17`; unset `LD_LIBRARY_PATH`, `PYTHONHOME`, `PYTHONPATH` trước khi chạy shell.

- `clean assembleDebug testDebugUnitTest`: thành công; 28 tests, 0 failures/errors.
- Localization lint: thành công với MissingTranslation, ExtraTranslation, StringFormatInvalid, StringFormatMatches, StringFormatCount, ImpliedQuantity, MissingQuantity, PluralsCandidate. Dùng init script kiểm thử tạm, không sửa cấu hình lint dự án.
- Full `lintDebug`: còn 5 lỗi có sẵn: 3 WrongConstant tại `NativeTextWatermarkProbe`/`TimeLocationWatermark`, 2 UnsafeOptInUsageError tại Camera2Interop trong `CameraEngine`. Không sửa ngoài phạm vi dịch.
- `LocalizationInstrumentation` chạy bằng ADB trên Android: PASS. Kiểm tra resource Việt/Anh, fallback Pháp sang Anh, ưu tiên Anh/Việt và Việt/Anh, toàn bộ string resources đã compile, plural 1/2, accessibility có tham số và ngày UI theo locale.
- Ba bộ tài nguyên có cùng key; bộ default và English giống nhau.
- `git diff --check` với các file localization: PASS. Lệnh toàn working tree còn báo whitespace có sẵn ở `gradlew.bat` và tài liệu reference; không chỉnh các thay đổi đó.

Lệnh chạy instrumentation:

```bash
adb shell am instrument -w \
  com.pgkit.timemarkcamera/.LocalizationInstrumentation
```

Emulator `pgkit_watermark_qa`, Android 35, 1080×2400, khoảng 384 dp: dùng ADB cài APK, thao tác, chụp màn hình và dump UI tree. Kiểm tra 19 màn hình ở cả hệ thống `en-US` và `vi-VN`: camera, độ phân giải, tỷ lệ, giờ, lịch, vị trí, chọn mẫu, sửa mẫu, màu, xem ảnh, thông tin ảnh, xác nhận xóa, menu thêm, lưới thư viện, chọn nhiều, đang quay, xem video, tốc độ phát và thông tin video. Có chụp ảnh và quay video thực trên camera giả lập. Xác nhận xóa chỉ mở rồi hủy. Nhãn UI English không còn tiếng Việt trong các cây UI đã kiểm tra; kiểm tra ảnh màn hình không thấy chữ bị cắt.

Đã kiểm tra thêm modal nhập giờ bằng bàn phím, tháng/năm và danh sách mẫu sau khi cuộn ở cả hai ngôn ngữ.

Đã khôi phục emulator về locale runtime `en-US`, property `persist.sys.locale` rỗng và `system_locales` không có giá trị như ban đầu; adbd trở về không root. Resource instrumentation trên APK cuối tiếp tục PASS; logcat AndroidRuntime không có crash trong phiên kiểm thử.

Ảnh và UI dump kiểm thử nằm tạm tại `/tmp/pg-locale-qa/`; không commit media, APK hoặc dữ liệu thiết bị.

## Giới hạn

- ADB chỉ thấy emulator, không thấy LG `DEVICE_SERIAL_REDACTED`. Chưa cài APK lên LG, chưa xác minh UI trên LG hoặc bốn hướng cầm máy thật/preview/JPEG. Không thay đổi cấu hình hoặc dữ liệu LG.
- Chưa kiểm thử TalkBack bằng âm thanh, mọi nhánh từ chối quyền/lỗi phần cứng/lỗi lưu, chia sẻ tới ứng dụng bên ngoài, màn hình thư viện trống hoặc mọi mức font lớn. Không xóa dữ liệu hay media để tạo các tình huống này.
- Quyền hệ thống do Android hiển thị theo ngôn ngữ hệ thống. Dialog microphone tiếng Anh đã quan sát; không revoke quyền để ép lại toàn bộ các dialog.

## Bàn giao Git

Commit localization chỉ gồm resources, helper ngày/giờ, instrumentation và các file shared UI/media vốn sạch trước tác vụ. Bản dịch trong `CameraScreen`, `MainActivity`, `CameraEngine`, `WatermarkTemplateSheet`, `AuxiliaryCamera`, `WatermarkTimeSheet` giữ trong working tree vì các file đó đã có sửa/thêm sẵn của người dùng. Không stage gộp các thay đổi camera/ratio/clock/capture trước đó, không reset/revert dữ liệu người dùng.

APK: `app/build/outputs/apk/debug/app-debug.apk`.
SHA-256: `16fc98d95ddbed2aa8a9fc601e8f3ddc63344a4f060853b113b27aea750e812c`.
