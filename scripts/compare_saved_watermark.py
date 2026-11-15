#!/usr/bin/env python3
"""Measure the retained 2048x1536 LG 09:23 fixture against Timemark v10.0.316.

Only the specified dark-scene watermark regions are compared. This is not a
whole-image parity metric. Inputs must use the same text and raster dimensions.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", required=True, type=Path)
    parser.add_argument("--candidate", required=True, type=Path)
    parser.add_argument("--white-candidate", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    reference, candidate, white = [np.array(Image.open(path).convert("RGB"))
                                   for path in (args.reference, args.candidate, args.white_candidate)]
    if any(image.shape != (1536, 2048, 3) for image in (reference, candidate, white)):
        raise ValueError("Expected matched 2048x1536 images; no resizing is performed")
    regions = {"clock": (20, 1150, 390, 1340), "date": (420, 1155, 850, 1240),
               "weekday": (420, 1250, 650, 1350), "address": (25, 1360, 1020, 1520)}
    report = {"alignment": "none", "foreground_threshold": 230, "regions": {}}
    for name, (left, top, right, bottom) in regions.items():
        masks = [image[top:bottom, left:right].min(axis=2) > 230
                 for image in (reference, candidate)]
        report["regions"][name] = {"foreground_iou": float(
            (masks[0] & masks[1]).sum() / max(1, (masks[0] | masks[1]).sum()))}
    dark = white[1150:1340, 20:390].min(axis=2) < 220
    report["clock_dark_pixels_on_white"] = int(dark.sum())
    report["limitations"] = ["One fixed reference state; not all templates or orientations",
                              "JPEG camera scene and encoding can affect foreground edges",
                              "A nonzero white-background outline does not prove 1:1 parity"]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report))
    if not dark.any():
        raise SystemExit("Clock has no visible outline on white background")


if __name__ == "__main__":
    main()
