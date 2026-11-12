# T-210 · feature/library: üres és hibaállapotok

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06 |
| Modul | `feature/library` |
| Méret | M (3–6 ó) |
| Függ | [T-208](T-208-feature-library-lista-ui.md) |
| Követelmény | NFR-06, FR-32 |
| Teszt | — |

## Cél

A `FUNCTIONAL_SPEC` 2.4 és 10. pontja szerinti állapotok bekötése.

## Teendők

- [ ] „A tudástár üres” + debug buildben „Diagnosztika” gomb.
- [ ] „Nincs találat a(z) …” keresésre + „Szűrők törlése” gomb.
- [ ] Betöltési hiba + „Újra”.
- [ ] `Loading` a tartalomimport alatt (nincs félig betöltött lista).

## Kész, ha

- [ ] Mind a négy állapot kézzel kiváltható és helyesen jelenik meg.

## Figyelj rá

- A félig betöltött lista rosszabb, mint a töltésjelző: a felhasználó azt hinné, ennyi a tananyag.

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
| Commit | 2a5f7ef |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:library:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 9 új teszt zöld |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 434 teszt, 0 hiba, 0 kihagyott |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A négy állapot (`Loading`/`Empty` – nincs tartalom/`Empty` – nincs
találat/`Error`) mind a `LibraryListScreen` stateless composable `when`-ágában dönt el –
„kézzel kiváltható": a hozzájuk tartozó `@Preview` függvények mind a négy ágat valódi
`LibraryListUiState` értékkel hívják meg, világos és sötét témában is. A „Diagnosztika"
gomb debug kapuja `BuildConfig.DEBUG`-on áll (`feature/library/build.gradle.kts`:
`buildFeatures.buildConfig = true` új sor); a gomb maga az `EmptyState` komponens már kész
szerződése szerint jelenik meg/tűnik el (release buildben `onDiagnostics = null`). A gomb
tényleges célja (a teljes validációs jelentés képernyője) a T-508 tárgya – ott még nem
létezik út, ezért a kattintás jelenleg no-op. Az üres-állapot akció leképezését
(„melyik okhoz melyik gomb tartozik") a `libraryEmptyStateAction` tiszta függvény végzi,
ez a Compose fától függetlenül, JVM teszttel (`LibraryEmptyStateActionTest`) ellenőrzött.
Compose UI teszt (tényleges koppintás-bejárás emulátoron) nem futott – lásd
TEST_REPORT.md, B-03.
