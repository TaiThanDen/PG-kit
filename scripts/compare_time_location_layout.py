#!/usr/bin/env python3
"""Compare the native template-34 fixture with the retained original camera capture.

No alignment, resizing, or ink-bound cropping is performed. Regions are measured in the
1080x1440 preview coordinate system. Camera pixels and JPEG artifacts preclude an RGBA
parity claim; this metric tests solid glyph shape, wrapping, and absolute placement.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    reference = np.array(Image.open(args.reference).convert("RGB"))[256:1696]
    candidate = np.array(Image.open(args.candidate).convert("RGB"))
    if reference.shape != (1440, 1080, 3) or candidate.shape != reference.shape:
        raise ValueError("Expected original 1080x2400 capture and 1080x1440 fixture")
    regions = {
        "clock": (15, 1145, 260, 1315),
        "date": (290, 1155, 590, 1230),
        "weekday": (290, 1230, 450, 1315),
        "address": (15, 1308, 730, 1418),
        "divider": (260, 1155, 290, 1300),
    }
    report = {"alignment": "none", "threshold": 220, "regions": {}}
    for name, (x, y, right, bottom) in regions.items():
        masks = []
        for source in (reference, candidate):
            crop = source[y:bottom, x:right]
            mask = np.all(crop > 220, axis=2)
            if name == "divider":
                mask = (crop[:, :, 0] > 200) & (crop[:, :, 1] > 130) & (crop[:, :, 2] < 100)
            masks.append(mask)
        union = np.count_nonzero(masks[0] | masks[1])
        score = np.count_nonzero(masks[0] & masks[1]) / max(1, union)
        bounds = []
        for mask in masks:
            yy, xx = np.where(mask)
            bounds.append([int(xx.min()) + x, int(yy.min()) + y,
                           int(xx.max()) + x + 1, int(yy.max()) + y + 1] if len(xx) else [])
        report["regions"][name] = {"iou": score, "original_bounds": bounds[0],
                                    "candidate_bounds": bounds[1]}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    for label, array in (("original", reference), ("candidate", candidate),
                         ("difference", np.abs(reference.astype(int) - candidate).astype("uint8"))):
        Image.fromarray(array[1130:1440, :760]).save(args.output.with_name(label + "-layout.png"))
    print(json.dumps(report))
    if any(item["iou"] < .999 for item in report["regions"].values()):
        raise SystemExit("Template-34 foreground geometry regressed below 0.999 IoU")


if __name__ == "__main__":
    main()
