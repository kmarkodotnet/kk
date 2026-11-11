# Kyokushin Karate – magyar tudástár (MVP)

**Verzió:** 0.1.0 | **Rekordok:** 161 | **Állapot:** szakmai lektorálásra váró első kiadás.

## Fájlok
- `kyokushin_knowledge_hu.json`: Android importálásra alkalmas, verziózott JSON.
- `kyokushin_knowledge_hu.csv`: Excelben is szerkeszthető UTF-8 BOM-os táblázat.

## Adatmodell
- `id`: stabil rekordazonosító, ne változtasd meg tartalommódosításkor.
- `category_id`: kategória azonosítója.
- `japanese_romaji`: japán név latin betűs, hosszú magánhangzókat jelölő átírásban.
- `japanese_romaji_ascii`: ékezetmentes keresési alak.
- `japanese_script`: jelenleg null, későbbi, hitelesített japán írásjegyeknek.
- `hungarian_name`: magyar megnevezés.
- `description_hu`: rövid magyarázat.
- `image_path`: jelenleg null, később pl. `images/KY-0001.webp`.
- `aliases`, `tags`: későbbi bővítésre.
- `review_status`: minden rekord lektorálást igényel.

## Integráció
1. A JSON-t tedd az Android projekt `assets` mappájába.
2. Első indításkor importáld Room adatbázisba az `id` mező alapján, idempotens upserttel.
3. Az alkalmazás tárolja az importált `dataset_version` értéket.
4. A tanulási statisztikát külön táblában tárold, `entry_id` hivatkozással; tudástár-frissítéskor ne töröld.
5. A kvíz válaszait az egyértelmű elnevezésekből állítsd elő, a duplikált és kétértelmű opciókat zárd ki.

## Fontos korlátok
- A Kyokushin szervezetek terminológiája és kata-tanterve eltérhet.
- Nem tartalmaz ellenőrzött japán kandzsit/kanát vagy illusztrációkat.
- A magyar elnevezések pedagógiai célú fordítások, nem hivatalos szabvány.
- A `review_status` értékét csak tényleges karate-szakértői ellenőrzés után módosítsd.
