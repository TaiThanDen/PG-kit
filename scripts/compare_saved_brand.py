#!/usr/bin/env python3
"""Compare right/bottom-anchored brand masks at a matched 1536 px image short side.

Only integer crops are taken. This does not align, resize, remove camera
backgrounds, or establish complete logo/pixel parity.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image


def brand_crop(path):
    image = Image.open(path).convert("RGB")
    if min(image.size) != 1536:
        raise ValueError("Expected a 1536 px short side; resizing is not performed")
    return np.array(image.crop((image.width - 220, image.height - 100,
                                image.width, image.height)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    reference, candidate = map(brand_crop, (args.reference, args.candidate))
    report = {"alignment": "none", "resize": "none", "regions": {}}
    for name, top, bottom in (("wordmark", 10, 52), ("subtitle", 52, 95)):
        masks = []
        for image in (reference, candidate):
            region = image[top:bottom, 10:218]
            mask = region.min(axis=2) > 150
            if name == "wordmark":
                mask |= ((region[:, :, 0] > 180) & (region[:, :, 1] > 120)
                         & (region[:, :, 2] < 130))
            masks.append(mask)
        report["regions"][name] = {"foreground_iou": float(
            (masks[0] & masks[1]).sum() / max(1, (masks[0] | masks[1]).sum()))}
    report["limitations"] = ["Mask thresholds can include camera background pixels",
                              "A foreground mask does not measure color/shadow parity",
                              "The tested variant is not every Timemark brand variant"]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report))


if __name__ == "__main__":
    main()
