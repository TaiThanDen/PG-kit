# Filter mặc định Timemark

## Kết luận và phạm vi

REA xác nhận cấu hình filter mặc định được đóng gói trong Timemark **v10.0.311 và v10.0.316** là **Original**: ID `-10000`, asset `filters/res_original.png`. Renderer ép cường độ LUT thành **0** khi gặp ID này. Giá trị preference `0.65` không phải cường độ hiệu dụng của Original. Uniform `toneIntensity` cũng được gán `0`.

Vì vậy app áp dụng profile màu trung tính cho preview và ảnh chụp: `CONTROL_EFFECT_MODE_OFF`, không thêm LUT hoặc phép chỉnh màu vào bitmap JPEG. Đây là tái tạo hành vi filter Original, không phải bằng chứng mọi pixel camera của hai app giống nhau. ISP, AE/AWB, chọn camera, độ phân giải, crop và xử lý JPEG vẫn có thể tạo khác biệt.

## Chuỗi bằng chứng

| Bước | Đối tượng đã kiểm tra | Kết quả |
| --- | --- | --- |
| Giá trị mặc định | `com.xhey.xcamera.b.<clinit>` ở cả hai APK | ID `-10000`, đường dẫn Original |
| Nạp preference | `h10.b.<init>` ở cả hai APK | Dùng các giá trị trên khi chưa lưu lựa chọn; intensity fallback `0.65` |
| Đường camera sản phẩm | v10.0.316 `CameraWidget.N` | Truyền filter qua `CameraGLSurfaceView` tới renderer LUT |
| Bỏ qua LUT | `com.xhey.xcamerasdk.render.b.d` ở cả hai APK | ID `-10000` buộc intensity về `0` |
| Shader | v10.0.311 `sc0.a.h/j`, v10.0.316 `tc0.a.h/j` | Trộn LUT bằng intensity; tone intensity luôn `0` |
| Camera trước/sau | v10.0.311 `CameraGLSurfaceView.onDrawFrame`, `render.b.c` | Camera sau dùng mode 1; beauty xử lý ở nhánh mode 3; mode sau reset beauty |
| Camera2 đang chạy | Xiaomi, Timemark v10.0.316 | Capture result: effect OFF, AE compensation 0, AWB AUTO |

Scan tĩnh toàn bộ 8 DEX v10.0.316 tìm ghi vào các field mặc định `b.f/g/h` chỉ thấy ba phép ghi trong `<clinit>`. Kết quả lưu trong `reference_app/v10.0.316/default-filter/default-field-writes.json`. Kiểm tra này không bao phủ ghi bằng reflection/native hoặc cấu hình tải từ server.

Danh sách Evidence ID và vị trí wrapper đầy đủ: [evidence-ledger.md](evidence-ledger.md). Evidence runtime chỉ chứng minh trạng thái Camera2 công khai tại thời điểm thu thập; không chứng minh lựa chọn filter riêng tư hoặc toàn bộ GL pipeline. Không đọc dữ liệu riêng tư của app gốc.

## Tích hợp

`TimemarkDefaultFilter.apply()` đặt hardware color effect OFF trên hai builder `Preview` và `ImageCapture`. Hai lời gọi nằm trong `CameraEngine.bindUseCases()`. Patch nhỏ đã áp dụng được lưu tại [engine-integration.patch](engine-integration.patch).

Preview tiếp tục dùng rotation của display; ImageCapture dùng rotation vật lý hiện có. Helper không ghi đè exposure, white balance, chất lượng hay orientation. Luồng EXIF → bitmap → watermark → MediaStore giữ nguyên.

## Kiểm tra

- Build debug thành công với JDK 17, CameraX 1.4.1 và build-tools 35, chế độ offline. Dùng init script cục bộ tạo package kiểm thử `.codexfilter`; không thay package app đang cài.
- Instrumentation trên Android tạo builder thật với effect SEPIA/MONO trước khi áp dụng Original: cả hai được thay bằng OFF; target rotation, capture quality được giữ; không xuất hiện override AE compensation/AWB. Kết quả PASS trong `reference_app/v10.0.316/default-filter/probe-result.txt`. Probe và manifest thử nghiệm chỉ nằm trong thư mục evidence bị ignore, không thuộc source production.
- `git diff --check` cho phần thay đổi filter sạch. Kiểm tra toàn checkout còn báo whitespace CRLF trong các file `evidence/latest` đã sửa từ trước bởi công việc khác; không đưa chúng vào commit này.
- Đã cập nhật package clone trên LG và chụp hai JPEG mỗi app trong cùng cảnh. Cài/chụp/lưu PASS nhưng đầu ra chưa tương đương 1:1: khác độ phân giải và độ sáng/nhiễu. Xem [bài kiểm tra LG ngày 2026-11-15](lg-pair-test-2026-11-15.md) và script tái lập phép đo.

## Chưa xác minh

- Beauty camera trước dùng SenseTime và cấu hình riêng; chưa tái tạo backend native này. Không coi profile Original là clone beauty.
- Chưa chứng minh màu sắc giống từng pixel trên cảnh cố định, cùng thiết bị và cùng AE/AWB.
- Thay đổi filter này không thay thế kiểm tra preview/JPEG bốn hướng trên điện thoại thật theo AGENTS.md.
