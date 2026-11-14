# T-708 · Fordítások és terminológia ellenőrzése hiteles Kyokushin-forrásokból

| | |
|---|---|
| Modul | docs, core/data |
| Függ | — |
| Követelmény | FR-42 (`docs/REQUIREMENTS.md` 3.6), CR-01…CR-05 |
| Teszt | — (tartalmi átvizsgálás; a nyomán esetleg módosuló rekordokra a meglévő TR-01 validátorteszt-készlet vonatkozik) |

A jelenlegi 161 rekord mindegyike `editorial_review_required` státuszú (lásd
`docs/KNOWLEDGE_BASE.md`, OQ-02 – a szakmai hitelesítő személye továbbra is nyitott). A
felhasználó azt kéri, hogy a romaji átírások és a magyar jelentések dokumentáltan
összevetésre kerüljenek hiteles Kyokushin-forrásokkal.

**Fontos korlát (CLAUDE.md tartalmi integritás):** ez a task **nem** állíthat át egyetlen
rekordot sem `verified = true`-ra pusztán ennek az átvizsgálásnak az alapján – a
`verified` mező jelentése és az OQ-02 (ki a szakmai hitelesítő) ettől függetlenül
változatlan. A task eredménye egy **dokumentált forrásösszevetés**, nem egy
hitelesítési döntés.

## Teendők és készültségi feltételek

- [x] A `kyokushin-content` skill szabályainak követése: **tilos** kitalált
  terminológiát vagy nem ellenőrizhető forrásból származó "javítást" beírni.
- [x] A 161 rekord romaji/magyar/kategória mezőinek átvizsgálása legalább két
  egymástól független, elérhető, hiteles forrás bevonásával (pl. a Kyokushin
  nemzetközi szervezetek hivatalos technikai szószedetei, elismert
  oktatási anyagok – a konkrét forrásokat a task végrehajtója nevezi meg és
  hivatkozza, nem ez a jegy dönti el előre).
- [x] Minden rekordhoz rögzíteni: egyezik-e a jelenlegi romaji/magyar a forrással;
  ha eltér, mi az eltérés és melyik forrás szerint melyik a helyesebb; ha a
  forrás maga sem egységes, ezt is jelölni ("forrásfüggő eltérés").
- [x] Az eredmény egy új `docs/CONTENT_VERIFICATION.md` fájlban: rekordonkénti vagy
  kategóriánkénti táblázat, oszlopok: `id`, jelenlegi romaji/magyar, forrás(ok),
  egyezés/eltérés, javasolt teendő (nincs teendő / javítás javasolt / nyitott).
- [x] Ha egy eltérés **egyértelműen** elírás (pl. nyilvánvaló gépelési hiba a jelenlegi
  magyar szövegben) és a forrás egyértelmű, a javítás a szokásos adatfrissítési
  folyamaton megy át (új `dataset_version`, validátor lefuttatva, TR-05 szerint az
  előrehaladás megmarad) – ez **nem** állítja át a `verified`/`editorial_status`
  mezőt, csak a szöveghibát javítja.
- [x] `docs/KNOWLEDGE_BASE.md`-ből hivatkozás az új dokumentumra.
- [x] Ha bármilyen tartalmi adat módosul: `./gradlew testDebugUnitTest assembleDebug`
  zöld (a validátor- és importteszteken át).
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.


## Elkészült – 2026-11-14

161 rekord dokumentált romaji/magyar/kategória-áttekintése, 49 megjelölt tétel.
Kimenet: `docs/CONTENT_VERIFICATION.md` és `.csv`; reprodukáló/ellenőrző script:
`tools/review_t708.py`. Forrás- és függetlenségi korlátok a jelentésben.
`python -X utf8 tools/review_t708.py` exit 0: 161 egyedi rekord, CSV-mezőegyezés,
minden review-státusz változatlan. Nincs tartalmi adatjavítás; az adatváltozásra
vonatkozó dataset_version/TR-05/build feltétel nem alkalmazható.
Build/unit: NEM FUTOTT – kizárólag tartalmi dokumentálás; Claude párhuzamos
fejlesztésének buildjét nem indítottuk újra és nem állítottuk le.
Commit: a T-708-at megnevező dokumentációs commit.
