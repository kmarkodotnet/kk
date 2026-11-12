# T-207 · feature/library: lista ViewModel

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06 |
| Modul | `feature/library` |
| Méret | L (6–12 ó) |
| Függ | [T-202](T-202-core-data-kereses.md), [T-203](T-203-di-hilt-modulok.md) |
| Követelmény | FR-01, FR-02, FR-03 |
| Teszt | — |

## Cél

A Tudástár lista állapotkezelése.

## Teendők

- [ ] `LibraryListUiState` sealed: `Loading` / `Empty(reason)` / `Content(...)` / `Error`.
- [ ] Keresőszöveg ~200 ms debounce-szal; kategóriaszűrő halmaz.
- [ ] Keresőszöveg és szűrők `SavedStateHandle`-ben (forgatás túlélése).
- [ ] Kategóriánkénti találatszám a chipekhez.

## Kész, ha

- [ ] Forgatás után a keresés és a szűrés megmarad.
- [ ] A ViewModel nem importál Compose típust (L6).

## Figyelj rá

- Az `Empty` okot hordoz: „nincs tartalom” vs. „nincs találat” – különben a UI nem tud helyes üzenetet adni.

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
| Commit | `9c00d10` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:library:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 17 új teszt zöld |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **425 teszt, 0 hiba** (408 → 425) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (20 424 842 byte) |

**Mi készült el:**

- `LibraryListUiState` sealed: `Loading` / `Empty(reason)` / `Content(items)` / `Error(messageRes)`.
  A keresőszöveg, a kategóriachipek, a kijelölés és a `datasetTrust` a sealed interfész
  **közös** tulajdonsága – az üres állapotban sem tűnhet el a keresősáv, amivel a
  felhasználó visszavonhatja a saját szűkítését (US-06 4.).
- `LibraryListViewModel` (`@HiltViewModel`): a `TechniqueRepository.observeTechniqueMatches`,
  az `observeCategories` és az `observeContentState` mellé a `ContentImporter` –
  **az importot ez a ViewModel indítja**, ezért a `Loading` valódi állapot, nem a `stateIn`
  kezdőértéke.
- Keresés 200 ms debounce-szal; az **üres** keresőszó késleltetés nélkül jut át (a „×"
  visszavonás, nem újabb keresés).
- Keresőszöveg és kategóriakijelölés `SavedStateHandle`-ben (`library.query`,
  `library.categories`), ezért forgatást túlél.
- Kategóriánkénti találatszám a chipekhez, **a kijelölést figyelmen kívül hagyva** – a nem
  kijelölt chip száma különben mindig nulla lenne, és épp arra a kérdésre nem válaszolna,
  amire a felhasználó használja.
- A `@DefaultDispatcher` első fogyasztója: az összegzés és a kétszintű rendezés
  (kategória `sortOrder` → romaji) CPU-munka, nem I/O.
- `feature/library/src/main/res/values/strings.xml`: keresősáv, „Összes" chip, chip
  találatszám és az „Egyezés: `{alias}`" sor szövege a T-208 UI-nak.

**Megjegyzés:** a `Clock` és az RNG **nem** injektált ebbe a ViewModelbe – nincs mire. Egy
nem használt függőség csak azt sugallná, hogy van időfüggő logika. Ami a lista rendezését
nézve időfüggő lehetne (legutóbb gyakorolt), az a Haladás képernyőé (T-503-tól).

A `datasetTrust` már az állapotban van, de a lektorálatlanság-szalag **kirajzolása**
a T-208 dolga (`UnreviewedBanner`). Ugyanígy az „Egyezés: `{alias}`" sor és a
`library_*` stringek felhasználása.
