# Evidence gaps requiring decompilation or runtime testing

This is intentionally not a clone plan. Each item needs direct evidence before being documented as reference behavior.

1. Locate and disassemble `PreviewActivity` and its immediate camera controller classes, then record the method that binds preview/photo/video use cases.
2. Record every call involving `setTargetRotation`, display rotation, sensor orientation, EXIF orientation and any `Matrix`/transform logic.
3. Locate the runtime resolver for watermark styles `132` and `300`; record the exact font asset, dimensions and placement calculations.
4. Capture clean portrait and landscape reference screenshots without overlays, then record observed watermark positions separately from code facts.
5. Trace the location-resolution call chain from permission result to address string; record APIs and fallback behavior.
6. Trace photo and video output callbacks to record output URI/file behavior and whether watermarking occurs before or after encoding.

The current bundled JADX download is incomplete, so source-level class navigation is not yet available. Existing evidence was obtained via Android `dexdump`, manifest inspection and archive/resource inspection.

