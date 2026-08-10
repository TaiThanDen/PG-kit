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

## What is not established

- The font selected at runtime for style `132` or style `300`. **CHƯA XÁC MINH**
- Runtime coordinates, orientation-specific positioning, alpha, line height or shadow formula. **CHƯA XÁC MINH**
- Whether a given preview overlay is rendered by Canvas, OpenGL, bitmap or a custom View. **CHƯA XÁC MINH**

