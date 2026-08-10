# Evidence gaps requiring decompilation or runtime testing

This is intentionally not a clone plan. Each item needs direct evidence before being documented as reference behavior.

1. **CÒN THIẾU** — Locate and disassemble `PreviewActivity` and its immediate camera controller classes, then record the method that binds preview/photo/video use cases.
2. **MỘT PHẦN** — Runtime đã chứng minh portrait lock, bốn hướng watermark và hysteresis; vẫn cần DEX production cho `setTargetRotation`, sensor/EXIF và transform nội bộ.
3. **MỘT PHẦN** — Runtime đã có kích thước/anchor; font style `132`/`300` vẫn chưa có binding DEX.
4. **ĐÃ ĐO RUNTIME** — Đã có portrait/landscape bounds. Cần lưu dump/screenshot sạch vào evidence để tái lập độc lập.
5. **CÒN THIẾU** — Trace location-resolution call chain, fallback và cache.
6. **MỘT PHẦN** — JPEG đã chứng minh kích thước, orientation=0, burn-in và `DCIM/Camera`; video/output retry vẫn chưa xác minh.

`evidence/latest` đã được làm mới bằng Android SDK ADB hiện hành và `DEX inspected: True`. DEX snippets hiện có vẫn chỉ tìm thấy `SampleCameraActivity`; chưa đủ để đóng các khoảng trống production ở mục 1–3. Không sửa raw output bằng suy đoán.
