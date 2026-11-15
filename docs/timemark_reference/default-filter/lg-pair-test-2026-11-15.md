# Cài đặt và đối chiếu trên LG — 2026-11-15

**Cài đặt PASS; chụp/lưu JPEG PASS; ảnh chưa đạt tương đương 1:1.**

## Bản đã cài

- LG LM-V510N, Android/LineageOS đang kết nối; cập nhật package `com.pgkit.timemarkcamera.codextest` bằng `adb install -r`, kết quả `Success`. `lastUpdateTime=2026-11-15 15:33:30`. Không gỡ app hay xóa dữ liệu.
- APK có tích hợp filter commit `0f9238a`, được build từ checkout dùng chung cùng các thay đổi watermark/video hiện có; không coi đây là bản build chỉ từ clean commit đó.
- SHA-256 APK: `53a4a24ff998081708552ccf661f8e19c1d14a0e0f00281f0f2e2236197265fa`.
- Timemark đối chiếu: package `com.oceangalaxy.camera.new`, v10.0.316 / 100031600. Giữ phiên và preferences hiện có, không reset app hoặc đọc dữ liệu riêng tư.
- Xiaomi từ chối USB install (`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`). Người dùng đã chọn chuyển sang LG; không phát lại lượt cài bị từ chối.

## Cách đo

LG giữ cố định trong cùng cảnh tối, camera sau ID 0, zoom hiển thị 1.0x, máy đang dựng ngược. UI hai app vẫn dọc; JPEG có pixel chuẩn hóa và EXIF orientation 1. Chụp lần lượt clone 15:34:26, Timemark 15:35:30, Timemark 15:35:55, clone 15:36:22. Không dùng ảnh từ hai thiết bị khác nhau làm cặp so sánh.

Hai ảnh mỗi app đều lưu thành công, đã tải về trước khi nhả khóa runtime. Sau test để clone mở trên LG. Lượt này không thay thế bài kiểm tra bốn hướng trên điện thoại thật.

Để đo màu, transpose theo EXIF rồi đổi RGB; đưa clone về kích thước Timemark 1536×2048 bằng Lanczos. Dùng vùng `(32,32)-(1460,1780)`, loại watermark dưới và mã xác thực bên phải. Không căn chỉnh ảnh, không khử nhiễu, không bù màu hoặc bù sáng. Vì chụp tuần tự và khác độ phân giải, sai số này phản ánh toàn bộ pipeline/cảnh; không phải phép đo riêng lỗi LUT.

## Kết quả

| Chỉ số | Timemark | Clone |
| --- | --- | --- |
| Kích thước JPEG cả hai lượt | 1536×2048 | 3024×4032 |
| Luminance trung bình ROI, lượt 1 (0–255) | 81.536 | 94.410 |
| Luminance trung bình ROI, lượt 2 (0–255) | 80.762 | 92.759 |
| MAE RGB giữa hai lượt cùng app (0–255) | 1.622 | 5.394 |

MAE RGB giữa hai app: **13.867** ở cặp 1, **13.274** ở cặp 2. Clone sáng hơn khoảng **12.0–12.9 mức luminance** trong ROI. Ảnh đối chiếu cho thấy clone có hạt nhiễu rõ hơn ở vùng tối. Đây là quan sát của cảnh này, không suy rộng thành công thức bù sáng cho mọi cảnh hoặc điểm phần trăm “giống nhau”.

Camera dump xác nhận cả hai dùng camera ID 0; latest received frame báo AE compensation 0 và AWB AUTO; last request sent báo effect OFF. ROM chỉ cung cấp partial result 17 entries ở các snapshot ổn định: **không dùng ISO/exposure/gains trong request làm capture result đã đo**. Chưa tách được đóng góp của AE/ISP, độ phân giải, khử nhiễu và JPEG vào sai khác. Việc renderer Original bỏ qua LUT không chứng minh toàn bộ pipeline ảnh tương đương.

Preset 1536×2048 là trạng thái Timemark đang đo trên LG, không phải giới hạn cố định cho mọi thiết bị hay mọi cài đặt Timemark. Không sửa filter hoặc chất lượng ảnh để khớp một cảnh đơn lẻ trong lượt kiểm thử này.

## Bằng chứng và tái lập

Thư mục cục bộ bị ignore: `reference_app/v10.0.316/default-filter/qa-pair/`.

- `app-debug.apk`, `build.log`, `source-state.txt`, `lg-installed-package.txt`.
- `original-1.jpg`, `original-2.jpg`, `clone-1.jpg`, `clone-2.jpg`: ảnh xuất nguyên bản.
- `comparison.png`: ảnh đối chiếu có nhãn, cùng tỉ lệ hiển thị; `comparison.json`: số đo đầy đủ.
- `lg-*-camera.txt`, `lg-*-state.txt`, preview PNG và danh sách MediaStore trước/sau.
- `lg-runtime-errors.txt`: không có lỗi AndroidRuntime trong log thu sau lượt chụp; không suy ra mọi tính năng app đã được kiểm thử.

Tái tạo phép đo và ảnh đối chiếu:

```bash
python3 docs/timemark_reference/default-filter/compare_pair.py \
  /home/developer/Projects/PG-kit/reference_app/v10.0.316/default-filter/qa-pair
```

SHA-256 ảnh nguồn:

| Ảnh | SHA-256 |
| --- | --- |
| original-1 | `98347691608c4a826307bc5f81c5643e568ad5efe0a1825f99d962fb20da32c1` |
| original-2 | `6cfd7c2cb635762c8f74be3a371b8527b36791b4513a344f145a1e7c33b3a2fa` |
| clone-1 | `3f5c217a88dfd0d430951f2882d31f3aeffe915f1083b7107df251f0c7d0a6bd` |
| clone-2 | `9cd2a3fe6b000ba34888ae6f3cedbfb0f9bdaf7de86f338387d75c7e04ff81f6` |
