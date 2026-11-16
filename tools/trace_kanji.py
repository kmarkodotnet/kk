"""A kapott hímzésfotó kontúrjainak vektorizálása, belső nyílásokkal.

Függőségek a grafikai előkészítéshez: OpenCV, NumPy, Pillow.
Nem talál ki vonásokat: a fényképen látható sötét tinta/hímzés maszkját követi.
"""
from pathlib import Path
import json
import re
import cv2
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter1d

ROOT = Path(__file__).resolve().parents[1]
image = cv2.imread(str(ROOT / "asserts/kyo.png"))
gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
gray = cv2.GaussianBlur(gray, (5, 5), 0.9)
mask = np.uint8(gray < 86) * 255
count, labels, stats, centers = cv2.connectedComponentsWithStats(mask)
mask = np.uint8(np.isin(labels, [i for i in range(1, count) if stats[i, cv2.CC_STAT_AREA] > 180])) * 255
y, x = np.nonzero(mask)
x0, y0, x1, y1 = x.min() - 16, y.min() - 16, x.max() + 17, y.max() + 17
mask = mask[y0:y1, x0:x1]
h, w = mask.shape
contours, hierarchy = cv2.findContours(mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
paths = []
curve_polygons = []
for contour in contours:
    if abs(cv2.contourArea(contour)) < 28:
        continue
    original = contour.reshape(-1, 2).astype(float)
    closed = np.vstack([original, original[0]])
    arc = np.r_[0, np.cumsum(np.linalg.norm(np.diff(closed, axis=0), axis=1))]
    samples = np.linspace(0, arc[-1], max(12, int(arc[-1] / 2)), endpoint=False)
    uniform = np.column_stack([np.interp(samples, arc, closed[:, k]) for k in (0, 1)])
    # Az egyenletes ívhosszú simítás a hímzés pixeles fogazását csökkenti.
    # A kicsi belső nyílások enyhébb szűrést kapnak, nem tűnnek el.
    smooth = gaussian_filter1d(uniform, sigma=2 if arc[-1] > 80 else .65, axis=0, mode='wrap')
    points = cv2.approxPolyDP(smooth.astype(np.float32), 1.1 if arc[-1] > 80 else .25, True).reshape(-1, 2)
    segments = []
    dense = []
    for i, p1 in enumerate(points):
        p0, p2, p3 = points[(i-1) % len(points)], points[(i+1) % len(points)], points[(i+2) % len(points)]
        c1, c2 = p1 + (p2-p0)/6, p2 - (p3-p1)/6
        segments.append(f'C {c1[0]:.2f},{c1[1]:.2f} {c2[0]:.2f},{c2[1]:.2f} {p2[0]:.2f},{p2[1]:.2f}')
        for t in np.linspace(0, 1, max(8, int(np.linalg.norm(p2-p1))), endpoint=False):
            dense.append(((1-t)**3*p1 + 3*(1-t)**2*t*c1 + 3*(1-t)*t*t*c2 + t**3*p2).tolist())
    paths.append(f'M {points[0,0]:.2f},{points[0,1]:.2f} ' + ' '.join(segments) + ' Z')
    curve_polygons.append(np.round(dense, 3).tolist())
path = " ".join(paths)
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}">
  <title>Kyokushin – a kapott hímzésfotó részletes kontúrja</title>
  <path fill="#17317A" fill-rule="evenodd" d="{path}"/>
</svg>
'''
(ROOT / "asserts/kyo_kanji_detailed.svg").write_text(svg, encoding="utf-8")
data = dict(width=w, height=h, path=path, contours=paths, polygons=curve_polygons, sourceCrop=[int(x0),int(y0),int(x1),int(y1)])
(ROOT / "asserts/kyo_kanji_detailed.json").write_text(json.dumps(data), encoding="utf-8")
xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- A kyo.png részletes, belső nyílásokat megőrző kontúrja; tools/trace_kanji.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="160dp" android:height="{160*h/w:.2f}dp"
    android:viewportWidth="{w}" android:viewportHeight="{h}">
    <path android:fillColor="#17317A" android:fillType="evenOdd" android:pathData="{path}"/>
</vector>
'''
# Android AAPT UTF-8 stringpool: egy pathData szöveg legfeljebb 32767 bájt.
# A Bézier-vezérlőpontok egész viewport-koordinátára kerekítése <0,5 px eltérés;
# a 160 dp-s logón <0,11 dp. Az SVG/animáció teljes tizedespontossága megmarad.
android_path = re.sub(r'-?\d+\.\d+', lambda m: str(round(float(m.group()))), path)
assert len(android_path.encode('utf-8')) < 32767, 'Android pathData stringpool limit'
xml = xml.replace(f'android:pathData="{path}"', f'android:pathData="{android_path}"')
(ROOT / "asserts/ic_kyo_kanji_detailed.xml").write_text(xml, encoding="utf-8")
# A drawable a core/ui modulban él (nem app-ban): a feature modulok (pl. feature/home,
# T-703) nem függhetnek az app modulra (ADR-001, L5 fordított iránya tiltott), ezért a
# logót a :core:ui modul adja mindenkinek, a splash (app) is onnan olvassa.
(ROOT / "core/ui/src/main/res/drawable/ic_kyo_kanji.xml").write_text(xml, encoding="utf-8")
# A görbék nagy felbontású mintavételezése, majd élsimított kicsinyítés.
# Ugyanezeket a görbéket használja az SVG, az Android és az animáció.
vector_mask = np.zeros((h*4,w*4),dtype=np.uint8)
cv2.fillPoly(vector_mask,[np.rint(np.array(p)*4).astype(np.int32) for p in curve_polygons],255)
alpha = Image.fromarray(vector_mask).resize((w*2,h*2),Image.Resampling.LANCZOS)
preview = Image.composite(Image.new('RGB',alpha.size,(23,49,122)),Image.new('RGB',alpha.size,'white'),alpha)
preview.save(ROOT / "asserts/kyo_detailed_preview.png")
crop = cv2.cvtColor(image[y0:y1,x0:x1],cv2.COLOR_BGR2RGB)
panel = (1200,round(1200*h/w))
comparison = Image.new('RGB',(panel[0]*2,panel[1]),'white')
comparison.paste(Image.fromarray(crop).resize(panel,Image.Resampling.LANCZOS),(0,0))
comparison.paste(preview.resize(panel,Image.Resampling.LANCZOS),(panel[0],0))
comparison.save(ROOT / "output/kyo-vector-comparison.png")
print(f"{w}x{h}, {len(paths)} closed contours, {sum(p.count('C ') for p in paths)} cubic curves; comparison {comparison.size}")
