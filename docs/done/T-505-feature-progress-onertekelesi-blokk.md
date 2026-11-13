# T-505 · feature/progress: önértékelési blokk

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-11, US-17 |
| Modul | `feature/progress` |
| Méret | S (1–3 ó) |
| Függ | [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-28 |
| Teszt | — |

## Cél

A tanulókártya-önértékelés elkülönített megjelenítése.

## Teendők

- [x] Külön, vizuálisan elhatárolt szakasz: „Tudtam: `{n}` · Gyakorolnom kell: `{m}`”.
- [x] Magyarázó sor: „Az önértékelés nem számít bele a kvíz sikerarányába.”

## Kész, ha

- [x] Az önértékelés számai sehol nem keverednek a kvízstatisztikával.

## Figyelj rá

- A magyarázó sor nem díszítés: enélkül a felhasználó joggal hinné, hogy a két szám összefügg.

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
| Commit | (lásd a következő, dokumentációs commit) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 606 teszt, 0 hiba (a G-10 csoport együttes hozzájárulása: 587 → 606, +19) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk` (`app/build/outputs/apk/debug/`) |

**Megjegyzés:** a `ProgressOverviewUiState.Content` kapott egy `studyReviewSummary:
StudyReviewSummary` mezőt, a T-301-ben már megírt
`ProgressRepository.observeStudyReviewSummary()`-ból – ez egy teljesen önálló `Flow`,
sosem keveredik a `overallScore`/`categories` kvízstatisztikájával. A magyarázó sor a
FUNCTIONAL_SPEC 8. szó szerinti szövege.
