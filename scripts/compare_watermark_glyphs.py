#!/usr/bin/env python3
"""Compare thresholded glyph shapes, separately from placement, background and shadows."""
import argparse, hashlib, json, pathlib
import numpy as np
from PIL import Image

def mask(path, threshold):
    return np.asarray(Image.open(path).convert('RGB')).min(axis=2) > threshold

def trim(value):
    yy, xx = np.where(value)
    if not len(xx):
        raise ValueError('Region contains no foreground pixels')
    return value[yy.min():yy.max()+1, xx.min():xx.max()+1]

def compare(target, candidate):
    if abs(target.shape[0] - candidate.shape[0]) > 4 or abs(target.shape[1] - candidate.shape[1]) > 8:
        return 0.0
    shape = tuple(max(a,b) + 8 for a,b in zip(target.shape, candidate.shape))
    a = np.zeros(shape, bool)
    a[4:4+target.shape[0], 4:4+target.shape[1]] = target
    best = 0.0
    for dx in range(9):
        for dy in range(9):
            b = np.zeros(shape, bool)
            b[dy:dy+candidate.shape[0], dx:dx+candidate.shape[1]] = candidate
            best = max(best, float(np.sum(a & b) / np.sum(a | b)))
    return best

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', required=True, type=pathlib.Path)
    parser.add_argument('--regions', required=True, type=pathlib.Path, help='JSON list: {name,bounds:[left,top,right,bottom],pattern}')
    parser.add_argument('--candidates', required=True, type=pathlib.Path)
    parser.add_argument('--output', required=True, type=pathlib.Path)
    parser.add_argument('--threshold', type=int, default=220)
    args = parser.parse_args()
    reference = mask(args.reference, args.threshold)
    results = []
    for region in json.loads(args.regions.read_text()):
        left, top, right, bottom = region['bounds']
        target = trim(reference[top:bottom, left:right])
        scores = []
        for path in args.candidates.glob(region['pattern']):
            candidate = trim(mask(path, args.threshold))
            scores.append({'candidate': path.name, 'ink_size': list(candidate.shape), 'iou_after_alignment': compare(target, candidate)})
        scores.sort(key=lambda value: value['iou_after_alignment'], reverse=True)
        results.append({'region': region, 'reference_ink_size': list(target.shape), 'best': scores[:5]})
    report = {'reference_sha256': hashlib.sha256(args.reference.read_bytes()).hexdigest(), 'threshold': args.threshold, 'metric': 'binary foreground IoU after crop and bounded alignment', 'limitations': ['Does not measure placement, antialiasing values, shadows, backgrounds, JPEG output or whole-watermark parity.'], 'results': results}
    args.output.write_text(json.dumps(report, indent=2))
    for result in results:
        best = result['best'][0] if result['best'] else None
        print(result['region']['name'], best)

if __name__ == '__main__':
    main()
