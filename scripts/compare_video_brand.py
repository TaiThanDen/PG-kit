#!/usr/bin/env python3
"""Diagnostic only: alpha-composite brand PNGs over an estimated camera backdrop.

An unknown camera backdrop and lossy reference MP4 prevent this score from
certifying RGBA/video parity. No alignment or resizing is performed here.
Inputs must be matching 720x1280 reference RGB and candidate RGBA rasters.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter


def iou(left, right):
    union = np.count_nonzero(left | right)
    return float(np.count_nonzero(left & right) / union) if union else 1.0


def masks(rgb):
    white = (rgb.min(2) > 200) & (np.ptp(rgb, axis=2) < 40)
    yellow = (rgb[:, :, 0] > 200) & (rgb[:, :, 1] > 130) & (rgb[:, :, 2] < 180)
    return white, yellow


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", type=Path, required=True)
    parser.add_argument("--before", type=Path, required=True)
    parser.add_argument("--after", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    reference = Image.open(args.reference).convert("RGB")
    candidates = {key: Image.open(path).convert("RGBA") for key, path in
                  [("before", args.before), ("after", args.after)]}
    if reference.size != (720, 1280) or any(
            candidate.size != reference.size for candidate in candidates.values()):
        parser.error("All inputs must be 720x1280; do not silently align or resize.")
    # Keep a border around the measured regions for the backdrop estimator.
    box = (575, 1206, 720, 1280)
    original = np.asarray(reference.crop(box), dtype=float)
    backdrop = np.asarray(reference.crop(box).filter(ImageFilter.MedianFilter(15)), dtype=float)
    result = {"scope": "Diagnostic, not parity: unknown backdrop estimated by median15; "
              "same canvas, no alignment or resizing; thresholds affect results.", "candidates": {}}
    for label, candidate in candidates.items():
        rgba = np.asarray(candidate.crop(box), dtype=float)
        alpha = rgba[:, :, 3:4] / 255
        composite = rgba[:, :, :3] * alpha + backdrop * (1 - alpha)
        # Global wordmark bounds: x590:715, y1218:1252.
        region = np.s_[12:46, 15:140]
        ow, oy = masks(original[region])
        cw, cy = masks(composite[region])
        # The gray subtitle is below the white/gold threshold: measure contrast
        # against the same estimated backdrop instead, excluding the wordmark.
        subtitle = np.s_[44:71, 15:140]
        actual_gray = (original[subtitle] - backdrop[subtitle]).min(2) > 35
        candidate_gray = (composite[subtitle] - backdrop[subtitle]).min(2) > 35
        result["candidates"][label] = {
            "wordmark_white_IoU": iou(ow, cw),
            "wordmark_yellow_IoU": iou(oy, cy),
            "wordmark_combined_IoU": iou(ow | oy, cw | cy),
            "subtitle_contrast_IoU": iou(actual_gray, candidate_gray),
        }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
