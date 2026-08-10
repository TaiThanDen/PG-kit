# PG-kit watermark renderer specification

Đây là specification của clone `com.pgkit.timemarkcamera`, không phải bằng chứng trực tiếp từ app mẫu. Giá trị reference và mức độ xác minh nằm trong `docs/timemark_reference/`.

## Live preview

`WatermarkRenderer.drawLive()` nhận density của thiết bị và dùng:

| Thành phần | Giá trị clone hiện tại |
| --- | ---: |
| Margin ngoài | 8 dp |
| Time | 38 dp-sized px theo density |
| Date / weekday | 16 dp-sized px theo density |
| Address | 14 dp-sized px theo density |
| Address maxWidth | 260 dp, tối đa 2 dòng |
| Accent | `#FFC233` |
| Shadow | dx 0, dy 1.5 dp, radius 3.5 dp |

Time dùng asset local `BigShouldersText-Medium.ttf`; nội dung phụ dùng `RobotoCondensed-Regular.ttf`, đều ở weight normal. Clone vẫn thêm outline mảnh để dễ đọc; resource app mẫu không khai báo stroke nên đây là khác biệt triển khai có chủ ý cho tới khi DEX/runtime chứng minh công thức chính xác.

Live renderer luôn dựng watermark cơ sở ở góc dưới-trái của hệ tọa độ portrait. `WatermarkOverlayView` chỉ biến đổi canvas theo `DeviceOrientation`:

| Rotation | Góc watermark | Anchor sau biến đổi |
| --- | ---: | --- |
| `ROTATION_0` | 0° | dưới-trái |
| `ROTATION_90` | +90° | trên-trái |
| `ROTATION_180` | 180° | trên-phải |
| `ROTATION_270` | -90° | dưới-phải |

Activity và UI vẫn portrait; Preview CameraX tiếp tục dùng display rotation.

## Saved JPEG

- Renderer chọn portrait/landscape bằng kích thước bitmap đã chuẩn hóa EXIF.
- Address ngang bị giới hạn ở `760 × scale`, không dùng gần toàn chiều rộng ảnh.
- Logo saved image vẽ `Time` màu vàng và `mark` màu trắng; subtitle là `100% Chân thực`.
- Verification vẽ icon khiên bằng `Canvas Path`, sau đó là code và `Timemark Verified`.
- Capture timestamp, address và rotation được chốt riêng cho từng request trước callback nền.

## Formatting

- Time: `HH:mm`.
- Date: `dd Tháng M,yyyy`.
- Weekday: `Thứ Hai` … `Thứ Bảy`; Sunday là `Chủ nhật`.
- Address: địa chỉ reverse-geocoded; tọa độ trong lúc geocode; fallback `Đang xác định vị trí…`.
- Thời gian preview và capture lấy từ đồng hồ hệ thống thiết bị.

JPEG được ghi vào `DCIM/Camera` với MediaStore orientation 0 sau khi pixel đã được chuẩn hóa và burn watermark.
