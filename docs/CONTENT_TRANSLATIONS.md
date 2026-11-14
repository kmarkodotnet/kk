# A tudástár angol és német fordításai

2026-11-14 – a felhasználó kérésére elkészült a 161 rekord neve és leírása angolul
és németül, valamint mind a 9 kategória megnevezése. Összesen **662 fordított
szöveg**: 322 név, 322 leírás és 18 kategórianév.

## Tárolás

A fordítások a meglévő magyar mezők mellett találhatók:

- `knowledgebase/kyokushin_knowledge_hu.csv` – Excelben is megnyitható szerkesztési forrás.
- `knowledgebase/kyokushin_knowledge_hu.json` – a forrás JSON-párja.
- `knowledgebase/translations_en_de.tsv` – az angol/német fordítások szerkesztési listája, tabulátorral tagolva.

| Magyar mező | Angol mező | Német mező |
|---|---|---|
| `hungarian_name` | `english_name` | `german_name` |
| `description_hu` | `description_en` | `description_de` |
| CSV: `category_hu` | `category_en` | `category_de` |
| JSON categories: `name_hu` | `name_en` | `name_de` |

Példa: **Elülső rúgás → Front kick → Vorwärtstritt**.

## Fordítási döntések és lektorálás

Saját fordítás készült, szaknyelvi támpontként a következő anyagokkal:

- [IFK Norge – terminológia](https://www.ifknorge.org/wp-content/uploads/2024/03/syllabus_hoyopp.pdf), angol technikai megnevezések.
- [Kyokushinkai Karate München – Karate-Begriffe](https://kyokushin-muenchen.de/karate-begriffe), német szakkifejezések.
- [Kampfkunstschule Eisheuer – Fachlexikon](https://www.kyokushinkai.de/html/umfang/karate/karateLexikon.html), német szóhasználati összevetés.

A leírások önálló magyar→angol/német fordítások; nem állítjuk, hogy minden mondat
szó szerint megtalálható a hivatkozásokban. A szószedetek között is vannak hibák és
eltérések, ezért például egy anatómiai célpontot nem vettünk át automatikusan
pusztán azért, mert egy weboldalon szerepel.

Minden rekord `translation_review_status = draft_review_required` jelölést kapott.
A T-708 megjegyzései a `translation_notes_hu` mezőben is megjelennek a 49 megjelölt
rekordnál. Az ökölhát és a rangidősség angol/német megfelelője a romaji fogalomhoz
igazodik; ahol a magyar kiindulás félreérthető, ezt külön megjegyzés rögzíti.
Az iránybeli bizonytalanságokat a fordítás is megtartja, nem dönti el szakmai lektor helyett.

A magyar szövegek, romaji nevek, azonosítók, képhivatkozások és eredeti hitelesítési
státuszok változatlanok. A forrás `dataset_version` értéke nem változott: ez a kör
csak a szerkesztési forrást bővíti. A csomagolt magyar adatok, Room, importáló és
felület nem változtak. A T-711 teljes alkalmazás-nyelvváltási feladata továbbra is
külön feladat; most a tartalmi fordítási előkészítés készült el.

## Ellenőrzés

`python -X utf8 tools/add_content_translations.py` → exit 0.

`python -X utf8 tools/validate_content_translations.py` → exit 0:

- 161/161 rekordhoz mindkét nyelven név és leírás, üres fordítás nélkül;
- 9/9 kategória mindkét nyelven;
- CSV és JSON fordításmezői egyeznek;
- az eredeti magyar és egyéb régi mezők összevetve a Git HEAD-del, változatlanok;
- a meglévő konverter CSV–JSON keresztellenőrzése `(0,0,0)`;
- a régi magyar konverter memóriában számolt eredménye a bővítés előtt és után azonos,
  161 rekorddal, nulla blokkoló hibával.

A készítő script írás előtt észleli, ha a CSV/JSON közben megváltozott, és ilyenkor
leáll. Nem futtatja a konverter fájlokat másoló főprogramját, így nem írja felül
Claude párhuzamos fejlesztésének app-állományait. A külön validátor első futásában
a konverter `(rekordok, szinonimák)` visszatérését hibásan listaként számolta;
az ellenőrzés javítása után a végső futás sikeres.

Android build és eszközteszt ebben a forrásadat-előkészítési körben nem futott.
