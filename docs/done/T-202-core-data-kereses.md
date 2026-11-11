# T-202 Â· core/data: keresÃ©s

| | |
|---|---|
| Epik | **E2** â€“ TudÃ¡stÃ¡r bÃ¶ngÃ©szÃ©se Ã©s keresÃ©se |
| User story | US-06 |
| Modul | `core/data` |
| MÃ©ret | M (3â€“6 Ã³) |
| FÃ¼gg | [T-201](T-201-core-data-techniquerepository.md) |
| KÃ¶vetelmÃ©ny | FR-03 |
| Teszt | â€” |

## CÃ©l

Ã‰kezet- Ã©s kisbetÅ±-Ã©rzÃ©ketlen keresÃ©s romajira, magyar nÃ©vre Ã©s aliasokra egyszerre.

## TeendÅ‘k

- [ ] A normalizÃ¡lÃ³ fÃ¼ggvÃ©ny **ugyanaz**, amit a validÃ¡tor hasznÃ¡l (T-105) â€“ egy helyen definiÃ¡lva.
- [ ] A keresÃ©s az elÅ‘re normalizÃ¡lt `search_romaji` / `search_hungarian` oszlopokon Ã©s a `technique_alias.aliasNormalized`-en fut.
- [ ] RÃ©szszÃ³-egyezÃ©s (`LIKE '%...%'`), nem csak prefix.
- [ ] Alias-talÃ¡lat esetÃ©n a talÃ¡lat jelzi, melyik alias egyezett (a UI kiÃ­rja).

## KÃ©sz, ha

- [ ] â€žzart labuâ€ megtalÃ¡lja a â€žZÃ¡rt lÃ¡bÃº Ã¡llÃ¡sâ€-t; â€žDACHIâ€ megtalÃ¡lja az Ã¶sszes `â€¦ dachi` technikÃ¡t.
- [ ] A keresÃ©s nem blokkolja a UI szÃ¡lat.

## Figyelj rÃ¡

- Az `aliases` jelenleg minden rekordnÃ¡l Ã¼res, ezÃ©rt az alias-Ã¡g tesztje szintetikus adattal kÃ©szÃ¼l (R-05 kockÃ¡zat).

## Definition of Done

A task-specifikus pontokon felÃ¼l a **globÃ¡lis DoD** is Ã©rvÃ©nyes (`docs/BACKLOG.md` â†’ â€žGlobÃ¡lis Definition of Doneâ€):

- [ ] `./gradlew assembleDebug` exit 0
- [ ] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett kÃ¶vetelmÃ©nyre
- [ ] Nem sÃ©rt rÃ©tegszabÃ¡lyt (ADR-001, L1â€“L6)
- [ ] Nincs kÃ¶zvetlen `Clock` / `Random` hÃ­vÃ¡s Ã¼zleti kÃ³dban (NFR-07)
- [ ] Minden felhasznÃ¡lÃ³i szÃ¶veg `strings.xml`-ben, magyarul
- [ ] UI esetÃ©n: `contentDescription`, â‰¥ 48 dp cÃ©lpont, `@Preview` vilÃ¡gos + sÃ¶tÃ©t tÃ©mÃ¡ra
- [ ] `docs/IMPLEMENTATION_PLAN.md` Ã¡llÃ¡sa frissÃ­tve
- [ ] Ami nem futott le, az `docs/TEST_REPORT.md`-ben â€žNEM FUTOTT â€“ okâ€ sorkÃ©nt szerepel

## KapcsolÃ³dÃ³ dokumentumok

- `docs/REQUIREMENTS.md` â€“ a hivatkozott kÃ¶vetelmÃ©nyek
- `docs/FUNCTIONAL_SPEC.md` â€“ a kÃ©pernyÅ‘ Ã©s a viselkedÃ©s leÃ­rÃ¡sa
- `docs/ARCHITECTURE.md` + `docs/adr/` â€“ a szerkezeti dÃ¶ntÃ©sek
- `.claude/skills/` â€“ `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## ElkÃ©szÃ¼lt

| | |
|---|---|
| DÃ¡tum | 2026-11-11 |
| Commit | f0d1df8 |

**Futtatott parancsok:**

| Parancs | Exit | EredmÃ©ny |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | 358 teszt, 0 hiba (T-201 utÃ¡n 352 volt: +6) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL â€“ `app/build/outputs/apk/debug/app-debug.apk` (19 816 715 bÃ¡jt) |
| `.\gradlew.bat :core:data:testDebugUnitTest --tests "*KyokushinMigrationTest*"` | 0 | a migrÃ¡ciÃ³s teszt Ã¶nmagÃ¡ban is zÃ¶ld |

**MegjegyzÃ©s:**

- **SÃ©mavÃ¡ltozÃ¡s tÃ¶rtÃ©nt: `version` 1 â†’ 2** (ADR-012, DR-08). A `technique` tÃ¡bla kÃ©t Ãºj,
  **nem nullÃ¡zhatÃ³** oszlopa: `search_romaji`, `search_hungarian` â€“ a
  `NameNormalizer.normalize(...)` kimenete, a `ContentEntityMappers.toEntity()`-ben
  kitÃ¶ltve. A normalizÃ¡lÃ¡s **nem duplikÃ¡lÃ³dott**: tovÃ¡bbra is a `core/quiz`
  `NameNormalizer` az egyetlen definÃ­ciÃ³. BÃ¡rmilyen Ãºj `TechniqueEntity` fixtÃºra
  kÃ¶telezÅ‘en kitÃ¶lti a kÃ©t oszlopot, ugyanezzel a normÃ¡lÃ³val.
- A migrÃ¡ciÃ³ (`KyokushinMigrations.MIGRATION_1_2`) `ALTER TABLE â€¦ ADD COLUMN`-nal
  dolgozik, Ã¡thidalÃ³ `lower()` visszatÃ¶ltÃ©ssel, Ã©s a `content_state.contentVersion`-t
  `NULL`-ra Ã¡llÃ­tja, hogy a kÃ¶vetkezÅ‘ import a pontos, normalizÃ¡lt Ã©rtÃ©ket Ã­rja be.
  FelhasznÃ¡lÃ³i tÃ¡blÃ¡hoz nem nyÃºl, `fallbackToDestructiveMigration()` nincs.
- **A T-203-nak:** az adatbÃ¡zist Ã©pÃ­tÅ‘ Hilt kÃ¶tÃ©snek
  `addMigrations(*KyokushinMigrations.ALL)`-t kell adnia, destruktÃ­v tartalÃ©k nÃ©lkÃ¼l â€“
  a sÃ©ma mÃ¡r a 2-es verziÃ³n van, Ã©s a migrÃ¡ciÃ³ hiÃ¡nya a felhasznÃ¡lÃ³ kÃ©szÃ¼lÃ©kÃ©n
  Ã¶sszeomlÃ¡st okozna.
- **A T-207/T-208-nak:** a TudÃ¡stÃ¡r lista az `observeTechniqueMatches(query, categories)`
  metÃ³dust hasznÃ¡lja, Ã©s a `TechniqueMatch.matchedAlias` nem `null` Ã©rtÃ©ke esetÃ©n Ã­rja ki
  az â€žEgyezÃ©s: `{alias}`" sort (a szÃ¶veg `strings.xml`-be kerÃ¼l, a 200 ms-os debounce is
  UI-oldali felelÅ‘ssÃ©g). Az `observeTechniques` vÃ¡ltozatlanul elÃ©rhetÅ‘ ott, ahol az
  egyezÃ©s forrÃ¡sa nem Ã©rdekes.
- **R-05 Ã©rvÃ©nyben:** az `aliases` a valÃ³s tartalomban mindenhol Ã¼res, ezÃ©rt az alias-Ã¡g
  tesztje szintetikus fixtÃºrÃ¡val dolgozik.
- **Teszt-tanulsÃ¡g a jÃ¶vÅ‘beli migrÃ¡ciÃ³khoz:** a Room `MigrationTestHelper` Robolectric
  alatt csak a **fÃ¡jl alapÃº** konstruktorral mÅ±kÃ¶dik (a nÃ©v alapÃº Ã¡g a nÃ©v Ã©s az
  abszolÃºt Ãºt Ã¶sszetÃ©vesztÃ©se miatt elhasal), a sÃ©maexportot a `test` source set
  assetjekÃ©nt lÃ¡tja, Ã©s az Ã¡tmeneti adatbÃ¡zisfÃ¡jlt a tesztnek magÃ¡nak kell tÃ¶rÃ¶lnie.
  RÃ©szletek a `docs/TEST_REPORT.md` T-202 szakaszÃ¡ban.
- **Javasolt Ãºj task:** a `search_*` oszlopok Ã©s a `NameNormalizer` Ã¶sszhangjÃ¡t ma a
  mapper garantÃ¡lja; egy tartalomimport utÃ¡ni, adatbÃ¡zis szintÅ± Ã¶nellenÅ‘rzÅ‘ lekÃ©rdezÃ©s
  (minden aktÃ­v sorra `search_romaji == normalize(romaji)`) a T-602 rÃ©tegszabÃ¡ly-ellenÅ‘rzÃ©s
  mellÃ© illene.


