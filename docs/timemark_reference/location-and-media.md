# Location and media: extracted facts

## Manifest permissions

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`. **MANIFEST**
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `ACCESS_MEDIA_LOCATION`, and older storage permissions. **MANIFEST**
- Camera and microphone permission. **MANIFEST**

## Resource identifiers

- Location: `activity_location_link_settings`, `dialog_location_link_notify`, `dialog_location_remind`, `dialog_save_location_success`, `global_item_location`, `global_item_location_with_save`, `global_item_save_location_with_delete`, `layout_current_location_item`, `layout_location_guide`. **RESOURCE**
- Media: `dialog_saved_to_camera_roll`, `dialog_video_upload`, `fragment_video_view_player`, `ps_item_grid_camera`, `ps_item_grid_video`, `ps_preview_image`, `ps_preview_video`, `video_player_view`. **RESOURCE**

## JPEG output verified at runtime

- JPEG dọc 1920 × 2560; JPEG ngang 2560 × 1920. **JPEG**
- MediaStore orientation bằng 0 sau khi lưu; watermark đã được burn trực tiếp vào pixel. **JPEG**
- Ảnh mẫu được lưu trong `DCIM/Camera`; tên file gồm thời gian và một phần địa chỉ. **RUNTIME/JPEG**
- Preview crop và JPEG camera không dùng cùng hệ tọa độ, vì vậy không suy vị trí watermark JPEG từ kích thước PreviewView. **JPEG/RUNTIME**

## Not established

- Location provider/geocoder/network endpoint, fallback, cache policy và quyền chỉnh sửa address. **CHƯA XÁC MINH**
- Watermark video, codec video, MediaStore columns đầy đủ và retry behavior. **CHƯA XÁC MINH**
