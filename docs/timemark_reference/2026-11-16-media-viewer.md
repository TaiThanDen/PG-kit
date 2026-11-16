# Thư viện và trình xem Timemark

## Phạm vi

Đối chiếu UI thư viện/xem ảnh theo yêu cầu người dùng. PG-kit dùng trình xem nội bộ, nền trắng, nút đóng, chia sẻ hàng loạt và thanh icon kèm nhãn dưới ảnh. Không thay đổi camera, renderer hay màu watermark trong nhóm thay đổi này.

## Bằng chứng mẫu

APK tham chiếu: `com.oceangalaxy.camera.new`, SHA-256 `b59c50b6c84ccfa180f8648bc6864a83040b61ff6fcd70eb7e092321042a1a98`.

REA kiểm tra `com.xhey.xcamera.base.activitymanage.TransparentGeneralActivity`:

- Class Evidence: `ev_50ea3950842fb23a8b46a904b37368b942e65e70dba1450fa132789b609dd8b6`.
- onCreate Evidence: `ev_9b80eabb2a4ef3685e194e52262e6a6027db4730d3958976f5a05d9ec5b0f993`.
- Static code nạp fragment bằng `fragmentSrc.bundle` và `fragmentSrc.code`; navigation bar trắng mặc định. Bằng chứng này không tự chứng minh bố cục runtime.

Quan sát trên LG V50S: Activity trên chứa GlideExitViewPager và PhotoView. Trình xem có nền trắng, nút X bên trái, “Chia sẻ hàng loạt” bên phải và thanh icon/nhãn bên dưới. Chia sẻ hàng loạt mở lưới 3 cột theo ngày, vòng tròn chọn từng ảnh và chọn cả ngày. Các thao tác xuất Excel/PDF và chỉnh sửa ảnh gốc không nằm trong phạm vi dựng lại này.

Bundle REA lưu tạm tại `/tmp/pg-timemark-gallery-rea-evidence.json`. Screenshot/hierarchy, APK và dữ liệu thiết bị không đưa vào Git.

## Triển khai PG-kit

- MediaViewerActivity: shell trắng, đóng, vuốt ảnh, lưới theo ngày, chọn nhiều, chia sẻ, thông tin, xác nhận xóa và phát video nội bộ.
- ZoomImageView: fit ảnh nguyên tỷ lệ, pinch/pan, double tap, ẩn hiện thanh công cụ; chuyển ảnh bằng vuốt khi chưa zoom.
- MediaLibrary: đọc ảnh/video do app tạo, decode trên worker, downsample và cache thumbnail có giới hạn. Android 10 trở lên không xin quyền đọc toàn thư viện.
- Chia sẻ dùng ACTION_SEND/ACTION_SEND_MULTIPLE với ClipData và quyền đọc URI tạm thời. Chỉ khởi chạy bảng chia sẻ khi người dùng bấm nút.
- Xóa dùng ContentResolver.delete sau khi xác nhận trong AppSheet. Lỗi quyền được hiển thị bằng sheet, không crash.
- AppSheet, CameraUi, UiMotion và vector drawable dùng chung; tôn trọng cấu hình tắt animation của hệ thống. Activity tiếp tục khóa dọc và không export.

## Kiểm tra

- Clean assembleDebug và 26 unit test của working tree: thành công; 4 test MediaLibrary kiểm tra downsampling.
- LG thật: mở ảnh trong MediaViewerActivity của PG-kit, không có chooser chọn ứng dụng xem ảnh. Vuốt chuyển ảnh, pinch hai con trỏ, double tap trả zoom về gốc đã kiểm tra.
- Chọn một ảnh và một video: hiển thị “Đã chọn 2 mục”; bảng chia sẻ nhận 2 tệp. Không gửi tệp trong kiểm tra.
- Sheet thông tin và xác nhận xóa hiển thị đúng; hủy xóa giữ nguyên dữ liệu. Chưa kiểm thử thao tác xóa thực tế hoặc khôi phục quyền xóa.
- Video phát hết đoạn 31 giây trong app và hiển thị “Phát lại”; có play/pause/seek.
- Chưa kiểm thử Android 8/9, thiết bị khác LG hoặc đo frame time bằng profiler.
