# knowledgebase_transfer – az app-ba illeszkedő tudástár

> **Ez a mappa GENERÁLT. Ne szerkeszd kézzel.**
> A tartalmat a [`../knowledgebase/`](../knowledgebase/) mappában kell módosítani,
> utána újra kell futtatni a konvertert.

```
python knowledgebase_transfer/tools/convert.py
```

## Mi ez

A `knowledgebase/` mappa a tartalom **szerkesztési** forrása (Excelben nyitható CSV +
JSON + nyers képek). Ez a mappa ugyanannak a tartalomnak az **app-ba illeszkedő**
alakja: a projekt domain modelljéhez (`core/model`) és a kanonikus sémához
([ADR-011](../docs/adr/ADR-011-canonical-content-schema.md)) igazítva.

```
knowledgebase_transfer/
├── manifest.json            kategóriák, illusztráció-nyilvántartás, bizalmi szint, szinonimacsoportok
├── entries.json             a 161 technikarekord
├── illustrations/           a képfájlok (jelenleg 10 db .jpg)
├── VALIDATION_REPORT.md     generált konverziós és validációs jelentés
└── tools/convert.py         a konverter (ez állítja elő a fentieket)
```

## Jelenlegi tartalom

| | |
|---|---|
| Rekord | **161** (hiányos: 0) |
| Kategória | **9** |
| Illusztráció | **10** (mind az `stances` kategóriában, KY-0044…KY-0053) |
| `dataset_version` | 0.1.0 |
| `editorial_status` | `draft_review_required` – **minden rekord szakmai lektorálásra vár** |
| BLOKKOLÓ hiba | 0 |

## Leképezés: forrás → kanonikus séma → domain modell

| Forrás (CSV/JSON) | Kanonikus (`entries.json`) | Domain (`core/model`) |
|---|---|---|
| `id` | `id` | `TechniqueId` |
| `category_id` | `category_id` | `CategoryId` |
| `japanese_romaji` | `japanese_romaji` | `Technique.romaji` |
| `japanese_romaji_ascii` | `japanese_romaji_ascii` | keresési index |
| `japanese_script` | `japanese_script` (ma `null`) | `Technique.japaneseScript` |
| `hungarian_name` | `hungarian_name` | `Technique.hungarian` |
| `description_hu` | `description_hu` | `Technique.descriptionHu` |
| `image_path` (üres volt) | **`illustration_ref`** → `manifest.illustrations[].ref` | `IllustrationRef.Asset` / `null` |
| `review_status` | `review_status` | `VerificationStatus` (lásd lent) |
| `aliases`, `tags` | `aliases`, `tags` (ma üresek) | `Technique.aliases` |
| — (új) | `quiz_excluded` | emberi kézzel megadható kvízkizárás |
| — (új) | `retired` | [ADR-004](../docs/adr/ADR-004-retired-not-deleted.md) |
| — (származtatott) | `search_romaji`, `search_hungarian` | kisbetűs, ékezetmentes keresés (FR-03) |
| — (származtatott) | `incomplete` | `VerificationStatus.INCOMPLETE` jelzése |

### Státuszleképezés

| `review_status` | `VerificationStatus` | UI |
|---|---|---|
| `reviewed` | `VERIFIED` | nincs jelölő |
| `editorial_review_required` | `AWAITING_REVIEW` | „Ellenőrzésre vár" |
| bármi + `incomplete: true` | `INCOMPLETE` | „Hiányos" + kvízből kizárva |

A konverter **soha nem emel** státuszt ([CR-02](../docs/REQUIREMENTS.md)): a `reviewed`
érték kizárólag emberi döntésből, a forrás CSV-ben keletkezhet.

### `illustration_ref` – miért nem `image_path`

Az illusztráció licence, eredete, review státusza és kvízhasználhatósága **egy helyen**,
a `manifest.illustrations[]` tömbben él. A rekord csak hivatkozik rá. Így nincs két igazság
a kép metaadatairól, és a licencnyilvántartás (CR-04) teljes.

## Az illusztrációk – amit tudni kell róluk

A képek a `knowledgebase/pics/README.txt` és `manifest.json` szerint **AI által generált,
szakmailag nem ellenőrzött** oktatási illusztrációk. Ezért minden bejegyzésük:

```json
"origin": "ai_generated",
"review_status": "human_review_required"
```

Amíg ez a státusz áll, a képek a Tudástárban és a Tanulásban megjelennek, de a UI jelzi,
hogy ellenőrzésre várnak. A testhelyzet, a végtagszögek és a mozgásirány szakmai
felülvizsgálata hátravan.

### Három kép felirata felfedi a választ

A **KY-0045 (Heisoku dachi)**, **KY-0050 (Zenkutsu dachi)** és **KY-0052 (Kiba dachi)**
képekbe bele van égetve az azonosító, a japán és a magyar név. Ez két helyen okozna hibát:

- **Kvíz:** az „ábra → japán név" (C) kérdésnél a válasz le van írva a képen; a „japán név →
  ábra" (D) kérdésnél a felirat elárulja, melyik a helyes opció.
- **Tanulókártya:** elrejtett néven a felirat felfedi a választ a felfedés előtt.

Ezért a manifest két kaput ad minden képhez:

```json
"has_embedded_label": true,
"quiz_usable": false,
"hideable_on_study_card": false
```

A kvízmotor és a tanulókártya ezeket a mezőket olvassa, nem a fájlnevet. A `convert.py`
`IMAGES_WITH_EMBEDDED_LABEL` halmaza **emberi megfigyelés** eredménye (2026-11-11, mind a
10 kép vizuálisan átnézve) – automatikus szövegfelismerés nincs benne.

> **Új kép hozzáadásakor kötelező:** nézd meg a képet, és ha név vagy azonosító látszik
> rajta, vedd fel a `convert.py` `IMAGES_WITH_EMBEDDED_LABEL` halmazába. Ennek elmulasztása
> hibás kvízkérdéseket eredményez.
>
> A jobb megoldás hosszú távon: **felirat nélküli képeket** generálni, és a nevet az app
> rajzolja a kép mellé. Akkor ez a lista kiürül és mind a 10 kép kvízképes lesz.

## Kvízkészenlét (a generált jelentésből)

Szöveges kvíz – (A) romaji→magyar és (B) magyar→romaji – **mind a 9 kategóriában
indítható** (mindegyikben ≥ 4 alkalmas jelölt van).

Képes kvíz – (C) és (D) – **csak az `stances` kategóriában**, 7 felirat nélküli képpel
(a 10-ből 3 ki van zárva a felirat miatt). A többi 8 kategóriában nincs kép, ezért ott a
kvízbeállító képernyő letiltja és megindokolja a képes típusokat (FR-13).

Részletes bontás: [`VALIDATION_REPORT.md`](VALIDATION_REPORT.md).

## Mit csinál a konverter

1. Beolvassa a CSV-t (**ez a hiteles forrás**), a JSON-t és a képmanifestet.
2. Keresztellenőrzi a CSV-t és a JSON-t (`X001`…`X003` kódok); eltérésnél a CSV nyer.
3. Kategórialistát épít a JSON sorrendje szerint, `sort_order` mezővel.
4. Illusztráció-nyilvántartást épít: méret, SHA-256, licenc, eredet, review státusz,
   feliratkapu; jelzi a hiányzó fájlt (`I001`) és a bitre azonos képeket (`I003`).
5. Rekordonként validál: kötelező mezők, egyedi és formátumhelyes ID, létező kategória,
   ékezetmentes alak pótlása.
6. Szinonima-/ütközéscsoportokat keres az azonos normalizált nevek alapján
   (`synonym_groups`) – ezek a kvízben **nem lehetnek egymás válaszlehetőségei** (CR-07).
7. Átmásolja a képeket, kiírja a két JSON-t és a validációs jelentést.

A konverter **nem dob kivételt** tartalmi hibára: jelenti, és a rekordot megjelöli.
Ugyanazzal a bemenettel mindig ugyanazt a kimenetet adja.

## Hogyan kerül az appba

A Fázis 3.2 lépésben ennek a mappának a tartalma kerül az
`app/src/main/assets/knowledge/` alá. A `knowledgebase/` marad a szerkesztési forrás,
ez a mappa a köztes, ellenőrzött átadási forma.
