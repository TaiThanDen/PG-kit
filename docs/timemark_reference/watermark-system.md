# Watermark system: extracted facts

## Data asset

`assets/watermark/default_watermarks_global.json` contains a template named `Time&Location`.

| JSON field | Extracted value |
| --- | --- |
| `base_id` / `id` | `34` |
| Time item | `id=1`, `editType=6`, `style=132`, enabled |
| Address item | `id=2`, `editType=4`, `style=300`, enabled |
| Lat/Long item | `id=3`, disabled |
| Weather item | `id=4`, disabled |
| Map item | `id=210`, disabled |
| Altitude item | `id=5`, disabled |
| Compass item | `id=6`, disabled |
| Theme text color | `#FFFFFF` |
| Theme accent color | `#FFC233` |

**ASSET**

## Packaged watermark resources

- Covers/layouts exist for IDs `1`, `2`, `3`, `10`, `20`, `21`, `34`, `43`, `60`, `80`, `90`, `100`, `1034`, `120`, `130`, `140`, `150`. **RESOURCE**
- Dedicated resources include `watermark_preview_layout`, `watermark_preview_bottom_sheet`, `dialog_edit_watermark`, `dialog_edit_full_info_watermark`, `layout_watermark_edit_all_item`, `layout_seek_bar_for_watermark_size`, and `watermark_icon_time` / `watermark_icon_loc`. **RESOURCE**

## Packaged font files

`assets/fonts/global/` contains: `BebasDaka.ttf`, `BigShouldersText-Medium.ttf`, `Handlee-Regular.ttf`, `Montserrat-Black.ttf`, `PTMono-Bold.ttf`, `PTMono-Regular.ttf`, `RobotoCondensed-Bold.ttf`, `RobotoCondensed-Medium.ttf`, `RobotoCondensed-Regular.ttf`, and `XiaoHeiNumber.ttf`. `res/font/roboto_medium_numbers.ttf` also exists. **ASSET**

## Runtime measurements

Các giá trị live watermark đã được đo trên thiết bị 450 dpi:

- Time 38 dp; date/weekday 16 dp; address 14 dp; address `maxWidth=260 dp`; margin ngoài 8 dp. **RUNTIME**; đây là số đo style được cung cấp, không suy ra từ UIAutomator bounds.
- Accent `#FFC233`. **ASSET**
- Shadow dx 0, dy 1.5, radius 3.5. **RUNTIME**; raw style dump tương ứng chưa nằm trong `evidence/latest`.
- Layout reference không khai báo bold hoặc stroke. **RESOURCE**
- Bounds dọc một dòng `[0,1436]–[751,1696]`, dọc hai dòng `[0,1385]–[751,1696]`; ngang A `[0,256]–[311,1007]`; ngang B `[769,945]–[1080,1696]`. **UIAUTOMATOR**
- Watermark đổi 0/90/180/270 trong khi Activity vẫn portrait. Geometry phù hợp `RotateLayout`, nhưng class/remeasure nội bộ là **SUY ĐOÁN** nếu chưa có DEX.

## What is not established

- `BigShouldersText-Medium` cho time và `RobotoCondensed-Regular` cho nội dung phụ có xác suất cao, nhưng binding chính xác của style `132`/`300` chưa được chứng minh bằng DEX. **ASSET + SUY ĐOÁN**
- Không khẳng định preview được render bằng Canvas, OpenGL, bitmap hay custom View nếu chưa có DEX production path. **CHƯA XÁC MINH**
