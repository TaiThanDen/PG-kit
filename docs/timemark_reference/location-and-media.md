# Location and media: extracted facts

## Manifest permissions

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`. **MANIFEST**
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `ACCESS_MEDIA_LOCATION`, and older storage permissions. **MANIFEST**
- Camera and microphone permission. **MANIFEST**

## Resource identifiers

- Location: `activity_location_link_settings`, `dialog_location_link_notify`, `dialog_location_remind`, `dialog_save_location_success`, `global_item_location`, `global_item_location_with_save`, `global_item_save_location_with_delete`, `layout_current_location_item`, `layout_location_guide`. **RESOURCE**
- Media: `dialog_saved_to_camera_roll`, `dialog_video_upload`, `fragment_video_view_player`, `ps_item_grid_camera`, `ps_item_grid_video`, `ps_preview_image`, `ps_preview_video`, `video_player_view`. **RESOURCE**

## Not established

- Which location provider/geocoder/network endpoint is used, the fallback format, cache policy, or whether address text is editable. **CHƯA XÁC MINH**
- How images/videos are encoded, whether watermark pixels are burned into video frames, output folder, MediaStore columns, or export retry behavior. **CHƯA XÁC MINH**

