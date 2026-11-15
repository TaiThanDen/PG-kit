#!/usr/bin/env python3
"""Compare fixed edge ROIs at original JPEG coordinates, without aligning or resizing.

Requires Pillow and NumPy. Foreground masks are a diagnostic, not RGBA or pixel parity:
different camera backgrounds and JPEG antialiasing can change threshold membership.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image


def bounds(mask, left, top):
    ys, xs = np.where(mask)
    if not len(xs):
        return None
    return [int(xs.min() + left), int(ys.min() + top),
            int(xs.max() + left + 1), int(ys.max() + top + 1)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--same-code", action="store_true",
                        help="Only set when both images contain the identical verification string")
    parser.add_argument("--threshold", type=int, default=220)
    args = parser.parse_args()
    if not 0 <= args.threshold <= 255:
        parser.error("threshold must be between 0 and 255")
    original = Image.open(args.reference).convert("RGB")
    candidate = Image.open(args.candidate).convert("RGB")
    if original.size != candidate.size:
        parser.error("image dimensions must match; this tool never resizes inputs")
    width, height = original.size
    scale = min(width, height) / 1536
    left, right = width - round(65 * scale), width - round(4 * scale)
    regions = {"label": (-460, -165), "code": (-160, 160), "icon": (160, 195)}
    args.output.mkdir(parents=True, exist_ok=True)
    report = {"reference": str(args.reference), "candidate": str(args.candidate),
              "dimensions": [width, height], "alignment": "none", "resize": "none",
              "foreground_threshold": args.threshold, "same_code": args.same_code,
              "regions": {}, "limitations": [
                  "Threshold masks depend on background and JPEG antialiasing.",
                  "Icon whites can remain below the threshold; a missing mask is not a missing icon.",
                  "Only label and explicitly identical code masks support foreground IoU.",
                  "Foreground IoU does not establish pixel or RGBA parity."]}
    first, last = height, 0
    for name, (start, end) in regions.items():
        top, bottom = round(height / 2 + start * scale), round(height / 2 + end * scale)
        roi = (left, top, right, bottom)
        a = np.asarray(original.crop(roi)).astype(np.int16)
        b = np.asarray(candidate.crop(roi)).astype(np.int16)
        mask_a = a.min(axis=2) > args.threshold
        mask_b = b.min(axis=2) > args.threshold
        row = {"roi": list(roi), "reference_bounds": bounds(mask_a, left, top),
               "candidate_bounds": bounds(mask_b, left, top),
               "reference_foreground_pixels": int(mask_a.sum()),
               "candidate_foreground_pixels": int(mask_b.sum())}
        if name == "label" or (name == "code" and args.same_code):
            union = int((mask_a | mask_b).sum())
            row["absolute_foreground_iou"] = float((mask_a & mask_b).sum() / union) if union else None
        report["regions"][name] = row
        original.crop(roi).save(args.output / f"{name}-reference.png")
        candidate.crop(roi).save(args.output / f"{name}-candidate.png")
        if name == "label" or args.same_code:
            diff = np.zeros((*mask_a.shape, 3), dtype=np.uint8)
            diff[mask_a & mask_b] = [255, 255, 255]
            diff[mask_a & ~mask_b] = [255, 65, 65]
            diff[mask_b & ~mask_a] = [65, 190, 255]
            Image.fromarray(diff).save(args.output / f"{name}-mask-difference.png")
        first, last = min(first, top), max(last, bottom)
    strip_roi = (width - round(70 * scale), first, width, last)
    a, b = original.crop(strip_roi), candidate.crop(strip_roi)
    # Preserve orientation and every source pixel. The board only adds a gutter.
    board = Image.new("RGB", (a.width * 2 + 12, a.height), "#222222")
    board.paste(a, (0, 0))
    board.paste(b, (a.width + 12, 0))
    board.save(args.output / "strip-comparison.png")
    (args.output / "metrics.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({key: value.get("absolute_foreground_iou")
                      for key, value in report["regions"].items()}, ensure_ascii=False))


if __name__ == "__main__":
    main()
