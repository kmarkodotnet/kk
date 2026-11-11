# T-202 · core/data: keresés

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-06 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-201](T-201-core-data-techniquerepository.md) |
| Követelmény | FR-03 |
| Teszt | — |

## Cél

Ékezet- és kisbetű-érzéketlen keresés romajira, magyar névre és aliasokra egyszerre.

## Teendők

- [ ] A normalizáló függvény **ugyanaz**, amit a validátor használ (T-105) – egy helyen definiálva.
- [ ] A keresés az előre normalizált `search_romaji` / `search_hungarian` oszlopokon és a `technique_alias.aliasNormalized`-en fut.
- [ ] Részszó-egyezés (`LIKE '%...%'`), nem csak prefix.
- [ ] Alias-találat esetén a találat jelzi, melyik alias egyezett (a UI kiírja).

## Kész, ha

- [ ] „zart labu” megtalálja a „Zárt lábú állás”-t; „DACHI” megtalálja az összes `… dachi` technikát.
- [ ] A keresés nem blokkolja a UI szálat.

## Figyelj rá

- Az `aliases` jelenleg minden rekordnál üres, ezért az alias-ág tesztje szintetikus adattal készül (R-05 kockázat).

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [ ] `./gradlew assembleDebug` exit 0
- [ ] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [ ] Nem sért rétegszabályt (ADR-001, L1–L6)
- [ ] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [ ] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [ ] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [ ] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [ ] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-11 |
| Commit | f0d1df8 |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | 358 teszt, 0 hiba (T-201 után 352 volt: +6) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 816 715 bájt) |
| `.\gradlew.bat :core:data:testDebugUnitTest --tests "*KyokushinMigrationTest*"` | 0 | a migrációs teszt önmagában is zöld |

**Megjegyzés:**

- **Sémaváltozás történt: `version` 1 → 2** (ADR-012, DR-08). A `technique` tábla két új,
  **nem nullázható** oszlopa: `search_romaji`, `search_hungarian` – a
  `NameNormalizer.normalize(...)` kimenete, a `ContentEntityMappers.toEntity()`-ben
  kitöltve. A normalizálás **nem duplikálódott**: továbbra is a `core/quiz`
  `NameNormalizer` az egyetlen definíció. Bármilyen új `TechniqueEntity` fixtúra
  kötelezően kitölti a két oszlopot, ugyanezzel a normálóval.
- A migráció (`KyokushinMigrations.MIGRATION_1_2`) `ALTER TABLE … ADD COLUMN`-nal
  dolgozik, áthidaló `lower()` visszatöltéssel, és a `content_state.contentVersion`-t
  `NULL`-ra állítja, hogy a következő import a pontos, normalizált értéket írja be.
  Felhasználói táblához nem nyúl, `fallbackToDestructiveMigration()` nincs.
- **A T-203-nak:** az adatbázist építő Hilt kötésnek
  `addMigrations(*KyokushinMigrations.ALL)`-t kell adnia, destruktív tartalék nélkül –
  a séma már a 2-es verzión van, és a migráció hiánya a felhasználó készülékén
  összeomlást okozna.
- **A T-207/T-208-nak:** a Tudástár lista az `observeTechniqueMatches(query, categories)`
  metódust használja, és a `TechniqueMatch.matchedAlias` nem `null` értéke esetén írja ki
  az „Egyezés: `{alias}`" sort (a szöveg `strings.xml`-be kerül, a 200 ms-os debounce is
  UI-oldali felelősség). Az `observeTechniques` változatlanul elérhető ott, ahol az
  egyezés forrása nem érdekes.
- **R-05 érvényben:** az `aliases` a valós tartalomban mindenhol üres, ezért az alias-ág
  tesztje szintetikus fixtúrával dolgozik.
- **Teszt-tanulság a jövőbeli migrációkhoz:** a Room `MigrationTestHelper` Robolectric
  alatt csak a **fájl alapú** konstruktorral működik (a név alapú ág a név és az
  abszolút út összetévesztése miatt elhasal), a sémaexportot a `test` source set
  assetjeként látja, és az átmeneti adatbázisfájlt a tesztnek magának kell törölnie.
  Részletek a `docs/TEST_REPORT.md` T-202 szakaszában.
- **Javasolt új task:** a `search_*` oszlopok és a `NameNormalizer` összhangját ma a
  mapper garantálja; egy tartalomimport utáni, adatbázis szintű önellenőrző lekérdezés
  (minden aktív sorra `search_romaji == normalize(romaji)`) a T-602 rétegszabály-ellenőrzés
  mellé illene.

