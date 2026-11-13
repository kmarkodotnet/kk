# T-507 · feature/progress: beállítások

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-19 |
| Modul | `feature/progress` |
| Méret | M (3–6 ó) |
| Függ | [T-502](T-502-core-data-elorehaladas-torlese.md), [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-35, NFR-09 |
| Teszt | — |

## Cél

Tartalominformáció és adatkezelés.

## Teendők

- [x] Tartalominfó: `schema_version`, `dataset_version`, `editorial_status`, rekordszám, kategóriaszám.
- [x] „Előrehaladás törlése”: **kétlépcsős** megerősítés, pontos felsorolással, hogy mi törlődik.
- [x] Verzió- és licencinformáció.

## Kész, ha

- [x] A megerősítő szöveg kimondja, hogy a művelet nem visszavonható.
- [x] A törlés nem érinti a tudástárat.

## Figyelj rá

- Az adatvédelmi ígéret (NFR-09) része, hogy a felhasználó törölni tudja, amit gyűjtöttünk róla – még ha az sosem hagyta el az eszközt.

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
| Commit | `7c232eb` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 613 teszt, 0 hiba (a G-11 csoport együttes hozzájárulása: 606 → 613, +7) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk` (`app/build/outputs/apk/debug/`) |

**Megjegyzés:** új `feature/progress/settings` csomag: `ProgressSettingsViewModel`
(`TechniqueRepository.observeContentState()`/`.observeCategories()` a
tartalominfóhoz, `ProgressRepository.clearAllProgress()` a törléshez – a kettő
teljesen független forrás) + `ProgressSettingsUiState`/`Screen`. A kétlépcsős
megerősítés (`ClearProgressStep.NONE → FIRST_WARNING → FINAL_WARNING`) a
`SavedStateHandle`-ben él, ugyanazzal a mintával, mint a `QuizRunViewModel`
`closeConfirmationVisible` mezője. A „Verzió és forrás" szakasz a tartalomverziót és
a `knowledgebase/README.md`-ből szó szerint átvett két mondatot jeleníti meg, nem
app-verziószámot (a `feature/progress` modulhatár miatt a `BuildConfig.VERSION_NAME`
nem érhető el innen – lásd `docs/CONVENTIONS.md` 5. szakasz, új sor). A `navigation`
modul `SETTINGS` ága szándékosan továbbra is `PlaceholderScreen` (önálló,
`navigation`-feladat, ugyanaz a nyitott minta, mint a `quiz/setup`/`quiz/run`/
`quiz/result`-nál). A debug-only „Tartalomdiagnosztika” (teljes `ValidationReport`) a
T-508 tárgya, ezt a csoport szándékosan nem érintette.
