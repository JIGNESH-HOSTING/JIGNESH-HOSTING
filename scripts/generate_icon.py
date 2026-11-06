#!/usr/bin/env python3
"""Generate the tiny AZYRON mod icon using only Python's standard library."""

import math
import struct
import zlib
from pathlib import Path

SIZE = 128
OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/azyron/icon.png"


def inside_polygon(x, y, points):
    inside = False
    j = len(points) - 1
    for i, (xi, yi) in enumerate(points):
        xj, yj = points[j]
        if ((yi > y) != (yj > y)) and x < (xj - xi) * (y - yi) / ((yj - yi) or 1e-9) + xi:
            inside = not inside
        j = i
    return inside


def mix(a, b, t):
    return tuple(round(a[i] * (1.0 - t) + b[i] * t) for i in range(3))


def png_chunk(kind, payload):
    body = kind + payload
    return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)


left = [(25, 101), (52, 27), (67, 27), (43, 101)]
right = [(61, 27), (76, 27), (103, 101), (84, 101)]
bar = [(41, 66), (86, 66), (92, 81), (36, 81)]
rows = []
for y in range(SIZE):
    row = bytearray([0])
    for x in range(SIZE):
        dx = min(x, SIZE - 1 - x)
        dy = min(y, SIZE - 1 - y)
        if dx < 4 and dy < 4 and math.hypot(4 - dx, 4 - dy) > 4.2:
            row.extend((0, 0, 0, 0))
            continue
        t = (x + y) / (2 * (SIZE - 1))
        color = mix((23, 20, 45), (8, 13, 27), t)
        # Quiet, low-contrast corner glow; the mark stays the visual focus.
        glow = max(0.0, 1.0 - math.hypot(x - 24, y - 18) / 110) * 0.08
        color = mix(color, (100, 72, 160), glow)
        if inside_polygon(x + 0.5, y + 0.5, left):
            color = mix((164, 120, 255), (192, 165, 255), max(0.0, 1.0 - y / 130))
        if inside_polygon(x + 0.5, y + 0.5, right):
            color = mix((88, 218, 255), (157, 145, 255), y / 140)
        if inside_polygon(x + 0.5, y + 0.5, bar):
            color = (15, 18, 34)
        # Crisp 2 px border, matching the UI's electric-blue accent.
        if 4 <= x < 7 or 4 <= y < 7 or x >= 121 or y >= 121:
            color = mix(color, (65, 54, 102), 0.48)
        row.extend((*color, 255))
    rows.append(bytes(row))

raw = b"".join(rows)
header = struct.pack(">2I5B", SIZE, SIZE, 8, 6, 0, 0, 0)
image = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header) + png_chunk(b"IDAT", zlib.compress(raw, 9)) + png_chunk(b"IEND", b"")
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(image)
print(f"Wrote {OUT} ({len(image)} bytes)")
