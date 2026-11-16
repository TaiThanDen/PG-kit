#!/usr/bin/env python3
"""Compare equal-size saved photos or RGBA logo layers without moving/resizing inputs.

RGBA candidates are composited over a median-filter estimate of the reference
scene. Metrics diagnose raster differences; unknown backdrop/JPEG loss prevents
them from certifying RGBA parity. Generated images belong outside Git.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


def iou(left, right):
    union = np.count_nonzero(left | right)
    return float(np.count_nonzero(left & right) / union) if union else 1.0


def bounds(mask):
    y, x = np.nonzero(mask)
    return [int(x.min()), int(y.min()), int(x.max() + 1), int(y.max() + 1)] if x.size else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    parser.add_argument('--candidate', type=Path, action='append', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    reference = Image.open(args.reference).convert('RGB')
    short = min(reference.size)
    crop_width, crop_height = round(250 * short / 1536), round(120 * short / 1536)
    box = (reference.width - crop_width, reference.height - crop_height,
           reference.width, reference.height)
    original = reference.crop(box)
    backdrop = original.filter(ImageFilter.MedianFilter(21))
    original_array = np.asarray(original, dtype=float)
    report = {'dimensions': reference.size, 'crop': box, 'alignment': 'none',
              'resize': 'none', 'candidates': {}, 'limitations': [
                  'RGBA candidates use an estimated backdrop, not recovered camera pixels',
                  'Mask thresholds may include scene pixels',
                  'Foreground IoU is not a percentage of full logo parity',
                  'Sequential camera photos have different scene and encoder noise']}
    panels = [('Timemark', original)]
    args.output.mkdir(parents=True, exist_ok=True)
    for path in args.candidate:
        candidate = Image.open(path)
        if candidate.size != reference.size:
            raise ValueError(f'Image dimensions differ: {reference.size} versus {candidate.size}')
        if 'A' in candidate.getbands():
            composite = Image.alpha_composite(backdrop.convert('RGBA'), candidate.crop(box).convert('RGBA')).convert('RGB')
        else:
            composite = candidate.convert('RGB').crop(box)
        candidate_array = np.asarray(composite, dtype=float)
        regions = {}
        for name, y0, y1 in [('wordmark', 30, 77), ('subtitle', 77, 115)]:
            region = np.s_[round(y0 * short / 1536):round(y1 * short / 1536), :]
            left, right = original_array[region], candidate_array[region]
            threshold_metrics = {}
            for threshold in (150, 190, 220):
                masks = []
                for image in (left, right):
                    white = image.min(2) > threshold
                    gold = ((image[:, :, 0] > 180) & (image[:, :, 1] > 120)
                            & (image[:, :, 2] < 150) & (image[:, :, 0] - image[:, :, 2] > 65))
                    masks.append(white | gold if name == 'wordmark' else white)
                threshold_metrics[str(threshold)] = {'foreground_iou': iou(*masks),
                    'reference_bounds_in_region': bounds(masks[0]),
                    'candidate_bounds_in_region': bounds(masks[1])}
            regions[name] = {'thresholds': threshold_metrics,
                            'rgb_mae': float(np.abs(left - right).mean())}
        key = path.stem
        if key in report['candidates']:
            raise ValueError(f'Duplicate candidate name: {key}')
        report['candidates'][key] = regions
        panels.extend([(key, composite), (key + ' overlay 45%', Image.blend(composite, original, .45))])
        Image.blend(composite, original, .45).save(args.output / (key + '-overlay.png'))
    board = Image.new('RGB', (crop_width * len(panels), crop_height + 28), '#151515')
    draw = ImageDraw.Draw(board)
    for index, (label, panel) in enumerate(panels):
        x = index * crop_width
        board.paste(panel, (x, 28))
        draw.text((x + 5, 8), label, fill='white')
    board.save(args.output / 'comparison-native.png')
    (args.output / 'metrics.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
