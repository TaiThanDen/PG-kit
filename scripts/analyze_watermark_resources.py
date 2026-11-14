#!/usr/bin/env python3
"""Resolve packaged watermark/UI geometry, keeping raw AAPT output and APK identity."""
import argparse, hashlib, json, pathlib, re, subprocess, zipfile

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=pathlib.Path)
    parser.add_argument('--aapt2', required=True, type=pathlib.Path)
    parser.add_argument('--output', required=True, type=pathlib.Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    def aapt(*values):
        return subprocess.check_output([str(args.aapt2), 'dump', *values, str(args.apk)] if values[0] == 'resources' else [str(args.aapt2), 'dump', 'xmltree', str(args.apk), '--file', values[0]], text=True)
    table = aapt('resources')
    (args.output / 'resources.txt').write_text(table)
    resources = {}
    for match in re.finditer(r'^    resource (0x[0-9a-f]+) ([^\n]+)\n(.*?)(?=^    resource |\Z)', table, re.M | re.S):
        resources[match[1]] = {'name': match[2], 'configurations': match[3].rstrip()}
    def resolve(match):
        resource = resources.get('0x' + match[1])
        if not resource:
            return match[0]
        return '@' + resource['name'] + ' {' + ' / '.join(resource['configurations'].strip().splitlines()) + '}'
    prefixes = ('watermark_cover_id', 'new_watermark_cover_id')
    ui = {'fragment_universal_watermark_list_global', 'fragment_watermark_action_bottom_sheet', 'dialog_edit_watermark', 'dialog_edit_full_info_watermark', 'layout_watermark_edit_all_item', 'layout_watermark_edit_header_global', 'layout_seek_bar_for_watermark_size', 'watermark_preview_layout', 'watermark_preview_bottom_sheet'}
    manifest = []
    with zipfile.ZipFile(args.apk) as archive:
        for name in archive.namelist():
            stem = pathlib.PurePosixPath(name).stem
            if not name.startswith('res/layout/') or not name.endswith('.xml') or not (stem.startswith(prefixes) or stem in ui):
                continue
            raw = aapt(name)
            resolved = re.sub(r'@0x([0-9a-f]{8})', resolve, raw)
            target = args.output / (stem + '.resolved.txt')
            target.write_text(resolved)
            manifest.append({'archive_path': name, 'sha256': hashlib.sha256(archive.read(name)).hexdigest(), 'resolved': target.name, 'attributes': [line.strip() for line in resolved.splitlines() if 'A:' in line]})
    result = {'apk_sha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(), 'authority': 'packaged-resource', 'limitations': ['Resource declarations do not prove the active runtime route or post-binding values.', 'New covers and production covers are distinct. Select routes using REA code evidence.', 'Fonts, line breaking and dynamic scaling require runtime pixel verification.'], 'layouts': manifest}
    (args.output / 'layout-index.json').write_text(json.dumps(result, indent=2, ensure_ascii=False))
    print(f'Resolved {len(manifest)} layouts; APK SHA-256 {result["apk_sha256"]}')

if __name__ == '__main__':
    main()
