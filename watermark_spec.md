# Timemark watermark specification

This is the shared rendering specification implemented in `WatermarkRenderer.java`. Values are pixel-scaled from a 945 px portrait preview reference or a 2048 px landscape saved-image reference.

| Element | Portrait preview | Landscape saved image | Typeface / treatment |
| --- | ---: | ---: | --- |
| Left margin | 25 × scale | 38 × scale | — |
| Time | 100 × scale | 142 × scale | `sans-serif-condensed`, bold, X scale 0.86, white, dark outline |
| Date + weekday | 34 × scale | 48 × scale | `sans-serif-condensed`, bold, X scale 0.86, white, dark outline |
| Address | 34 × scale | 47 × scale | `sans-serif-condensed`, bold, X scale 0.82, white, dark outline; maximum two lines |
| Divider | 4 × scale stroke, 92 × scale height | 5 × scale stroke, 130 × scale height | `#FFB82B` |
| Time-to-divider gap | 15 × scale | 20 × scale | — |
| Divider-to-details gap | 20 × scale | 28 × scale | — |
| Bottom margin | 33 × scale | 50 × scale | — |

Formatting uses device-local current time:

- Time: `HH:mm`
- Date: `dd Tháng M,yyyy`
- Weekday: `Thứ hai` … `Chủ nhật`
- Address: reverse-geocoded short address when available; coordinates while geocoding; otherwise `Đang xác định vị trí…`.

The preview updates at the next whole second. At capture, the camera stores the capture timestamp and address, then renders that exact data into the mutable JPEG bitmap before saving it to `Pictures/TimemarkCamera`.
