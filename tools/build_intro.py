"""Az offline HTML-előnézet és az Androidba csomagolt azonos animáció előállítása."""
from pathlib import Path
import json
import base64
from io import BytesIO
from PIL import Image

root=Path(__file__).resolve().parents[1]
data=json.loads((root/'asserts/kyo_kanji_detailed.json').read_text(encoding='utf-8'))
template=(root/'asserts/intro_scene.template.html').read_text(encoding='utf-8')
html=template.replace('__KANJI_DATA__',json.dumps(data))
html=html.replace('__CENTERLINES__',(root/'asserts/kyo_kanji_centerlines.json').read_text())
# Veszteségmentes alfa megőrzése, WebP tömörítés a csomagmérethez; nincs újrarajzolás.
for token, filename in [('__FIST_IMAGE__','intro-fist-reference.png'),('__WOOD_IMAGE__','intro-wood-board.png')]:
    packed=BytesIO()
    Image.open(root/'asserts'/filename).save(packed,format='WEBP',quality=88,method=6)
    html=html.replace(token,'data:image/webp;base64,'+base64.b64encode(packed.getvalue()).decode('ascii'))
asset=root/'app/src/main/assets/intro/intro.html'
asset.parent.mkdir(parents=True,exist_ok=True)
asset.write_text(html,encoding='utf-8')
(root/'output').mkdir(exist_ok=True)
(root/'output/Kyokushin-Intro.html').write_text(html,encoding='utf-8')
print(f'Offline intro: {len(html.encode("utf-8"))} bytes; 14 seconds')
