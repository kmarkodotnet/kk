# T-508 · feature/progress: fejlesztői diagnosztika

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-19 |
| Modul | `feature/progress` |
| Méret | M (3–6 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md), [T-507](T-507-feature-progress-beallitasok.md) |
| Követelmény | FR-32 |
| Teszt | — |

## Cél

A teljes validációs jelentés megtekintése debug buildben.

## Teendők

- [x] Csak debug buildben látható menüpont.
- [x] A `lastValidationReport()` megjelenítése: kódonkénti bontás, érintett rekordok, súlyosság.
- [x] Szűrés súlyosságra.

## Kész, ha

- [x] Release buildben a menüpont nem jelenik meg.

## Figyelj rá

- Ez a képernyő teszi gyorsan diagnosztizálhatóvá a tartalomhibát, anélkül hogy a felhasználót zavarná.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [x] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-13 |
| Commit | `d8315be` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 626 teszt, 0 hiba (613 → 626, +13) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk` (`app/build/outputs/apk/debug/`) |

**Megjegyzés:** új `feature/progress/diagnostics` csomag:
`ProgressDiagnosticsViewModel` (a `ContentImporter.lastValidationReport()` egyszeri,
pontszerű lekérdezése, nem `Flow`) + `ProgressDiagnosticsUiState`/`Screen` +
`ProgressDiagnosticsGrouping`/`ProgressDiagnosticsIssueLabels` (kódonkénti bontás,
súlyosságszűrés, `@Composable`-mentes, sima JUnittal tesztelt döntési logika). A
`feature/progress/settings` (T-507) debug-only „Tartalomdiagnosztika" gombja
(`ProgressSettingsDiagnosticsButton`) ugyanazt a `BuildConfig.DEBUG`-mintát követi,
mint a `feature/library` „Diagnosztika" gombja (T-210): `onOpenDiagnostics` csak
debug buildben nem-`null`, ettől a gomb egyáltalán nem rajzolódik ki release
buildben (ezt a Compose-forráskód szerkezete garantálja, nem egy külön unit teszt –
ugyanaz a, T-210-nél már elfogadott, nem közvetlenül tesztelt minta, mert a
`BuildConfig.DEBUG` értéke a unit tesztfutásban mindig `true`). A
`settings → progress/diagnostics` tényleges navigációs bekötés a `navigation`
modul tárgya, szándékosan nyitva marad, ugyanaz a dokumentált minta, mint a
`quiz/setup`/`quiz/run`/`quiz/result`/`settings` útvonalaknál
(`docs/CONVENTIONS.md` 5. szakasz).
