# Timemark APK evidence index

This folder contains only facts extracted from the installed Timemark APK or separately labelled screenshot observations. It intentionally contains no proposed clone design and no inferred hidden behavior.

## APK examined

| Field | Value | Evidence |
| --- | --- | --- |
| Package | `com.oceangalaxy.camera.new` | `adb shell pm path` / APK manifest |
| App label | Timemark | APK manifest |
| Version | `v10.0.189`, code `100018900` | `dumpsys package` |
| SDK | min 26, target 35 | `dumpsys package` / manifest |
| Main activity | `com.xhey.xcamera.ui.camera.picNew.PreviewActivity` | manifest |
| APK splits pulled | base, arm64, Vietnamese, xxhdpi | `pm path` |

## Evidence labels

- **MANIFEST**: Android manifest/badging.
- **ASSET**: path and content extracted from the APK archive.
- **DEX**: disassembled DEX instruction/class reference.
- **RESOURCE**: public resource/layout/string identifier.
- **SCREENSHOT**: supplied or captured UI image.
- **CHƯA XÁC MINH**: no claim is made.

## Evidence precedence

Khi hai nguồn mâu thuẫn, dùng thứ tự: runtime package mẫu qua ADB; APK resource/manifest/DEX; UIAutomator/JPEG thật từ app mẫu; Markdown; cuối cùng mới tới ước lượng screenshot. Không dùng ảnh do `com.pgkit.timemarkcamera` tạo làm bằng chứng cho `com.oceangalaxy.camera.new`.

## Files

- [camera-and-orientation.md](camera-and-orientation.md)
- [watermark-system.md](watermark-system.md)
- [ui-ux-spec.md](ui-ux-spec.md)
- [location-and-media.md](location-and-media.md)
- [clone-build-plan.md](clone-build-plan.md) — renamed evidence gap register; it is not a build plan.

## Reproduce every claim

Run this from the workspace root with the phone connected:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify_timemark_reference.ps1 -IncludeDex -DeviceSerial <adb-serial>
```

It writes raw command output to `docs/timemark_reference/evidence/latest/`: package/version, installed APK paths, AAPT manifest data, archive entries, the exact `Time&Location` JSON, APK SHA-256, and CameraX DEX snippets. Compare those files to the `Evidence` label in each Markdown statement. The script does not install, alter, force-stop, or delete anything on the phone.

Lần thu ngày 2026-08-10 dùng `E:\Android\Sdk\platform-tools\adb.exe`, đã ghi package/version, split APK và DEX snippets đầy đủ. Giữ nguyên raw output; khi cần làm mới phải chạy script thay vì điền dữ liệu bằng tay.
