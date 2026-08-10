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

## Runtime capture limitation

The current UIAutomator dump after starting the reference app contained an advertisement overlay (`Chỉ 1 quảng cáo mỗi ngày`) rather than the camera hierarchy. It is not used as evidence for camera UI structure.

## Not established

- Exact margins, dimensions, text sizes, accessibility labels, animation timing, gesture handling, or landscape layout variants. **CHƯA XÁC MINH**

