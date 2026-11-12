# T-211 · feature/library: ViewModel tesztek

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06 |
| Modul | `feature/library` |
| Méret | M (3–6 ó) |
| Függ | [T-207](T-207-feature-library-lista-viewmodel.md) |
| Követelmény | FR-02, FR-03 |
| Teszt | — |

## Cél

A lista logikájának lefedése emulátor nélkül.

## Teendők

- [ ] Keresés normalizálása (ékezet, kisbetű, részszó), alias-találat jelzése.
- [ ] Kategóriaszűrés, több kategória egyszerre.
- [ ] `SavedStateHandle` visszaállítás.
- [ ] `Empty` ok helyes megállapítása (nincs tartalom vs. nincs találat).

## Kész, ha

- [ ] `kotlinx-coroutines-test` + fake repository + fix `Clock`; a tesztek determinisztikusak.

## Figyelj rá

- Az alias-ág szintetikus adattal tesztelendő, mert a valódi tartalomban minden `aliases` üres.

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
| Dátum | 2026-11-12 |
| Commit | *(a következő commit rögzíti)* |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:library:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 17 teszt, 0 hiba, 0 kihagyott |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **425 teszt, 0 hiba** (408 → 425) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (20 424 842 byte) |

**Mi készült el:** `LibraryListViewModelTest` (17 teszt) + `LibraryListFakes.kt`
(`FakeTechniqueRepository`, `FakeContentImporter`, `GatedContentImporter`, fix `Clock`).

| Teszt | Mit bizonyít |
|---|---|
| `libraryList_whileImportRunning_showsLoading_FR01` | a `Loading` **valódi** állapot: a kapuzott import megtartja, a kapu kinyitása után `Content` lesz |
| `libraryList_ordersByCategorySortOrderThenRomaji_FR01` | kategória `sortOrder`, azon belül romaji ábécérend (US-05 1.) |
| `libraryList_categoryNamesComeFromContent_FR01` | a kategórianév **tartalmi adat**, nem `strings.xml` kulcs (US-05 4., ADR-011) |
| `libraryList_importThrows_showsErrorAndRetryRecovers_FR01` | kivétel → `Error(@StringRes)`, és az „Újra" visszahozza a tartalmat |
| `libraryList_multipleCategories_filterTogetherAndCountsIgnoreSelection_FR02` | több kategória egyszerre, a chip újbóli megnyomása kikapcsol, és a **nem kijelölt** chip találatszáma sem nulla |
| `libraryList_savedStateHandle_restoresQueryAndCategories_FR02` | ugyanabból a `SavedStateHandle`-ből újraépített ViewModel visszakapja a keresést és a szűrést (forgatás, US-05 3.) |
| `libraryList_clearFilters_restoresFullList_FR02` | a „Szűrők törlése" a keresőszót **és** a chipeket is visszaállítja |
| `libraryList_searchIgnoresAccents_FR03` | „zart labu" → „Zárt lábú állás" (US-06 1.) |
| `libraryList_searchIgnoresCaseAndMatchesSubstring_FR03` | „DACHI" → minden `… dachi`, és a ViewModel a **nyers** szöveget adja a repositorynak (US-06 2.) |
| `libraryList_aliasMatch_reportsMatchedAlias_FR03` | alias-találatnál a `matchedAlias` ki van töltve – ez az „Egyezés: `{alias}`" sor forrása (US-06 3.) |
| `libraryList_nameMatch_hasNoMatchedAlias_FR03` | saját névből jött találatnál `null`, tehát nincs zajos „Egyezés" sor |
| `libraryList_typingIsDebounced_FR03` | négy leütés 200 ms-on belül → **egy** repository-lekérdezés |
| `libraryList_clearingQueryIsNotDebounced_FR03` | az üres keresőszó késleltetés nélkül jut át |
| `libraryList_withoutContent_emptyReasonIsNoContent_FR01` | `EmptyStateReason.LibraryNoContent`, és az üzenet a **tényleges** `strings.xml`-ből a specifikált magyar szöveg |
| `libraryList_withContentButNoMatch_emptyReasonCarriesQuery_FR03` | `LibraryNoResult("tornado")`, az üzenet idézi a kifejezést, és a keresősáv + chipek megmaradnak (US-06 4.) |
| `libraryList_retiredTechniqueStaysOutOfList_FR01` | visszavont technika nem jelenik meg (DR-07) |
| `libraryList_exposesDatasetTrustForUnreviewedBanner_FR01` | a `datasetTrust` a tartalomból jut az állapotba (ADR-010, AC-08) |

**Megjegyzés:**

- A `FakeTechniqueRepository` a keresést a **`NameNormalizer`-rel** utánozza – ugyanazzal a
  függvénnyel, amelyből a tárolt `search_romaji` / `search_hungarian` / `aliasNormalized`
  oszlopok készülnek. Egy saját, „majdnem ugyanilyen" normalizálás zöld tesztet adhatna
  olyan viselkedésre, amit az éles SQL nem tud. Az SQL oldali `LIKE` helyessége marad a
  `RoomTechniqueRepositoryTest` (T-202) tárgya – ez a lap nem méri újra.
- Az alias-ág **szintetikus** adaton mér (`KY-0003`, `aliases = ["chudan zuki"]`), mert a
  valódi tartalomban minden `aliases` lista üres (R-05 kockázat, CONVENTIONS 8.).
