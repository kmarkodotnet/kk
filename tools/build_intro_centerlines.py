"""A vektor kitöltéséből középvonalak és helyi vonásvastagságok az animációhoz."""
from pathlib import Path
import json
import re
import cv2
import numpy as np
from skimage.morphology import skeletonize

root = Path(__file__).resolve().parents[1]
art = json.loads((root / 'asserts/kyo_kanji_detailed.json').read_text())
mask = np.zeros((art['height'], art['width']), dtype=np.uint8)
if 'polygons' in art:
    polys = [np.rint(p).astype(np.int32) for p in art['polygons']]
else:
    polys = [np.array([[float(x), float(y)] for x, y in re.findall(r'([\d.]+),([\d.]+)', p)], dtype=np.int32) for p in art['contours']]
cv2.fillPoly(mask, polys, 255)
mask = cv2.resize(mask, None, fx=.5, fy=.5, interpolation=cv2.INTER_NEAREST)
skel = skeletonize(mask > 0)
distance = cv2.distanceTransform(mask, cv2.DIST_L2, 5)
pixels = set(map(tuple, np.argwhere(skel)))
neighbors = {p: [(p[0]+dy, p[1]+dx) for dy in (-1,0,1) for dx in (-1,0,1) if (dy or dx) and (p[0]+dy,p[1]+dx) in pixels] for p in pixels}
seen = set()
chains = []
for start in sorted(pixels, key=lambda p: len(neighbors[p]) == 2):
    for following in neighbors[start]:
        edge = tuple(sorted((start,following)))
        if edge in seen:
            continue
        chain = [start]
        prev, current = start, following
        seen.add(edge)
        while True:
            chain.append(current)
            candidates = [n for n in neighbors[current] if n != prev]
            if len(candidates) != 1:
                break
            nxt = candidates[0]
            edge = tuple(sorted((current,nxt)))
            if edge in seen:
                break
            seen.add(edge)
            prev,current = current,nxt
        if len(chain) < 9:
            continue
        # A forrás szerinti sugár változik: a kitöltés a középvonaltól szélesedik.
        sampled = chain[::3] + [chain[-1]]
        chains.append([[int(x)*2,int(y)*2,round(float(distance[y,x])*2,1)] for y,x in sampled])
(root/'asserts/kyo_kanji_centerlines.json').write_text(json.dumps(chains),encoding='utf-8')
print(f'{len(chains)} centerline branches, {sum(map(len,chains))} radius samples')
