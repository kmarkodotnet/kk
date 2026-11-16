# Ököl, fa, kék energia – indítóanimáció

A felhasználó kifejezett kérése alapján készült, 14 másodperces, hang nélküli,
dekoratív jelenet. Ez a korábbi animációtilalom alóli, kizárólag az indítóképernyőre
vonatkozó kivétel. A stilizált ököl nem hitelesített karateoktató illusztráció.

| Idő | Jelenet |
|---|---|
| 0–3,25 mp | Fehér háttér, álló deszka a széles lapjával a néző felé. A fa kitölti a kép teljes magasságát és kiér a bal széléig. A kék ököl jobbról érkezve a lap belsejét üti meg. |
| 3,25–3,8 mp | Az ütéstől balra végigfut a hasadás a kanji összes belső alapvonala mentén. |
| 3,8–5,4 mp | A teljes vonalhálózat sötét farepedésként látszik, kék fény nélkül. |
| 5,4–9,4 mp | Kizárólag balra terjedő kék tűz, a középvonalból fokozatosan szélesedve a vonások belsejébe. |
| 8,6–12,8 mp | A deszka fehérbe tűnik; 9,4 mp-től az energia függőlegesbe fordul és részletes kanjivá tisztul. A vízszintes állás a fametszetes verzióhoz képest 180°-kal fordított, így a felállás iránya is ellentétes (+90° → 0°). |
| 12,8–14 mp | Nyugodt, sötétkék embléma fehér háttéren. A háttér az egész videó alatt fehér. |

Az ököl 3,01 mp-ig nem rajzolódik ki. Az ütés 3,01–3,25 mp (0,24 mp), rövid
megtartás után a visszahúzás 3,55–5,35 mp (1,8 mp), teljesen a képen kívülre.
Nincs előzetes beúszás vagy felvillanó kézszél.

## Fájlok és újragenerálás

- Forrásfotó: `asserts/kyo.png`.
- Részletes, átlátszó vektor: `asserts/kyo_kanji_detailed.svg`.
- Android vektor: `core/ui/src/main/res/drawable/ic_kyo_kanji.xml`; a közös
  `KyoKanjiMark` komponens használja. A korábbi, egyszerűbb SVG megmaradt.
- Kontúradatok: `asserts/kyo_kanji_detailed.json` (16 kontúr, belső kivágásokkal).
- Középvonalak és helyi félvastagságok: `asserts/kyo_kanji_centerlines.json`.
- Referencia: `asserts/fist.jpg`; végső rajz: `asserts/intro-fist-reference.png`.
- Rajzolt deszka: `asserts/intro-wood-board.png` (a korábbi fametszet megmaradt forrásként).
- Grafikai eredet és végleges promptok: `docs/INTRO_ART_PROMPTS.md`.
- Jelenet forrása: `asserts/intro_scene.template.html`, kódolt Canvas-rajz.
- Csomagolt offline változat: `app/src/main/assets/intro/intro.html`.
- Megnyitható előnézet: `output/Kyokushin-Intro.html`; újrajátszás és időcsúszka.
- Videó: `output/Kyokushin-Intro.webm` (néma, álló formátum).
- Fotó–vektor összehasonlítás: `output/kyo-vector-comparison.png`.

`python tools/trace_kanji.py` követi a fotó sötét vonalait, megtartva a 16 zárt
kontúrt és a belső nyílásokat. Az ívhossz szerinti szűrés után 933 köbös
Bézier-szakasz simítja az éleket. A PNG négyszeres mintavételezéssel és Lanczos
kicsinyítéssel készül; az összehasonlító kép 2400×2901 pixel. A bal oldali eredeti
fotó részletességét nem növeli mesterségesen a vektorsimítás.
OpenCV, NumPy, SciPy és Pillow szükséges. `python tools/build_intro_centerlines.py`
a vektor kitöltését vékonyítja középvonallá (scikit-image), és a távolságtranszformációból
helyi vonásvastagságot társít hozzá. `python tools/build_intro.py`
állítja elő az offline előnézetet és az azonos Android HTML-t. A videó nem futásidejű
függőség: az alkalmazás helyben rajzol, nem játszik le letöltött filmet.

`node tools/preview_intro.mjs --video` külön, 9337-es CDP-porton futó helyi
Chromiummal nyolc ellenőrző képkockát és WebM-videót készít. Ellenőrzi a fehér
szabad jobb képsarkokat 29 időpontban, a fa teljes magasságát és balra kifutását,
mind a 105 simított belső ág sötét repedését a tűz előtt, a kizárólag balra terjedő tüzet három időpontban, valamint
a vízszintes–függőleges elfordulást. Nem használja a felhasználó
normál böngészőprofilját. A renderelő `window.intro.renderAt(másodperc)` függvénye
determinista állóképes ellenőrzéshez is használható.

Az ellenőrzés külön vizsgálja, hogy az ököl az ütés előtt és a visszahúzás után
egyetlen képpontot se hagyjon, de az ütéskor és a lassabb visszahúzás közben látszódjon.
Az Android `pathData` ugyanazokat a görbéket egész viewport-koordinátákra kerekítve
tartalmazza az AAPT 32767 bájtos korlátja miatt (25579 bájt); az SVG és az animáció
teljes tizedespontosságú. A kerekítés a 160 dp széles logón legfeljebb 0,11 dp.

A két illusztráció beágyazott WebP-ként kerül a HTML-be (az eredeti PNG-k megmaradnak),
így továbbra sincs külső fájl- vagy hálózati kérés. A lejátszás csak mindkét kép
dekódolása után indul. A belső hasadás változó szélességű középvonalszalagokból nő,
a végső alakot az eredeti vektormaszk korlátozza. Nem a kanji szélét húzzuk ki.

## Android viselkedés

Az intro azonnal átugorható, minimum 48 dp magas natív gombbal. A befejezett intro
Activity-újralétrehozás után nem tér vissza. TalkBack érintéses felfedezés vagy
nullára állított rendszeranimáció esetén a korábbi, 1,2 másodperces statikus logó
jelenik meg. Hiányzó WebView esetén szintén statikus a visszaesés; JavaScript-hiba
esetére 17 másodperces biztonsági továbblépés van.

A WebView kizárólag csomagolt saját kódot tölt: hálózat, fájl- és tartalomszolgáltató-
hozzáférés, új ablak és külső navigáció tiltva. Nincs INTERNET jogosultság vagy új
könyvtárfüggőség. A natív híd egyetlen művelete az intro befejezésének jelzése.

A fotó körvonalainak követése megőrzi a hímzés organikus éleit; nem állítjuk, hogy
az eredmény kalligráfiai szakértő által hitelesített újrarajzolás.
