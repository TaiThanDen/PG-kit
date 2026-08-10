# UI/UX: extracted facts and screenshot observations

## Resource-backed screens/components

| Area | Evidence |
| --- | --- |
| camera preview | `activity_camera_preview`, `layout_view_stub_camera_surface` |
| camera scale | `layout_camera_scale_widget` |
| photo/video actions | `view_preview_bottom_camera_action_global`, `view_preview_bottom_camera_video_action_global` |
| media preview | `fragment_album_preview`, `fragment_local_pic_preview`, `fragment_photo_preview_edit`, `fragment_video_view_player` |
| watermark editor | `dialog_edit_watermark`, `dialog_edit_full_info_watermark`, `watermark_preview_layout` |
| location settings | `activity_location_link_settings`, `global_bottom_fragment_location`, `layout_current_location_item` |

**RESOURCE**

## Screenshot-only observations

The supplied camera screenshots show a white top region, camera preview, right-side zoom controls, a large shutter, gallery/template actions, a mode row, and a Time & Location watermark overlay. These observations are visual only; they do not establish resource hierarchy, click handlers or state transitions. **SCREENSHOT**

## Runtime geometry

UIAutomator trên thiết bị 1080 × 2400, 450 dpi đã xác minh các bounds sau:

| Thành phần | Bounds / kích thước px | Quy đổi gần đúng |
| --- | ---: | ---: |
| Top bar | `[0,110]–[1080,256]` | cao 52 dp |
| Preview | `[0,256]–[1080,1696]` | 384 × 512 dp |
| Shutter | `[428,1876]–[653,2101]` | 80 × 80 dp |
| Xác thực ảnh | `[372,146]–[670,221]` | 106 × 27 dp |
| Plus | `[749,149]–[911,217]` | 58 × 24 dp |

**UIAUTOMATOR**. Tọa độ Y tuyệt đối gồm system bar; không dùng `110 px` làm padding của app.

Một lần thu thập sau đó bị `NotificationShade` che hierarchy. Lần thất bại này không phủ nhận dump camera sạch đã đo ở trên. Artifact bounds mới chưa được lưu trong `evidence/latest` nên cần bổ sung khi chạy lại script.

## Not established

- Accessibility labels, animation timing và click-handler nội bộ của app mẫu. **CHƯA XÁC MINH**
- App mẫu không tạo layout camera landscape; UI vẫn portrait trong bốn hướng cầm máy. **RUNTIME**
