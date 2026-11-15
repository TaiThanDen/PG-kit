"""Compare exported Original/clone JPEGs; requires Pillow and NumPy.

Usage: python3 compare_pair.py /absolute/path/to/qa-pair
Input names: original-1.jpg, original-2.jpg, clone-1.jpg, clone-2.jpg.
No registration, color correction or denoising is performed. Metrics describe
these sequential captures, not an isolated LUT error or a universal parity score.
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageOps


folder = Path(sys.argv[1])
names = ("original-1", "original-2", "clone-1", "clone-2")
size = (1536, 2048)
roi = (32, 32, 1460, 1780)  # Excludes bottom and right verification watermarks.
images = {}
native_sizes = {}
for name in names:
    with Image.open(folder / (name + ".jpg")) as source:
        native_sizes[name] = source.size
        images[name] = ImageOps.exif_transpose(source).convert("RGB").resize(
            size, Image.Resampling.LANCZOS)


def compare(first, second):
    a = np.asarray(images[first].crop(roi), dtype=np.float64)
    b = np.asarray(images[second].crop(roi), dtype=np.float64)
    delta = a - b
    weights = np.array([0.2126, 0.7152, 0.0722])
    return {
        "a": first,
        "b": second,
        "mae_rgb_0_255": float(np.abs(delta).mean()),
        "rmse_rgb_0_255": float(np.sqrt(np.mean(delta * delta))),
        "mean_y_a": float((a @ weights).mean()),
        "mean_y_b": float((b @ weights).mean()),
        "mean_rgb_a": a.mean(axis=(0, 1)).tolist(),
        "mean_rgb_b": b.mean(axis=(0, 1)).tolist(),
    }


results = {
    "working_size": size,
    "normalization": "EXIF transpose, RGB, Lanczos resize to original dimensions; "
                     "no alignment, color fit, blur or exposure correction",
    "roi_xyxy": roi,
    "native_sizes": native_sizes,
    "pairs": [compare("original-1", "clone-1"), compare("original-2", "clone-2")],
    "repeat_baseline": [compare("original-1", "original-2"), compare("clone-1", "clone-2")],
}
(folder / "comparison.json").write_text(json.dumps(results, indent=2) + "\n")

# A labeled analysis contact sheet; exported source JPEGs remain untouched.
sheet = Image.new("RGB", (1200, 860), "white")
draw = ImageDraw.Draw(sheet)
for x, name, label in (
    (0, "original-2", "TIMEMARK v10.0.316 | native 1536 x 2048"),
    (600, "clone-2", "CLONE + ORIGINAL PROFILE | native 3024 x 4032"),
):
    draw.text((x + 12, 14), label, fill="black")
    sheet.paste(images[name].resize((600, 800), Image.Resampling.LANCZOS), (x, 45))
sheet.save(folder / "comparison.png")
print(json.dumps(results, indent=2))
