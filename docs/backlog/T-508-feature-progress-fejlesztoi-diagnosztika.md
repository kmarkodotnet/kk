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

- [ ] Csak debug buildben látható menüpont.
- [ ] A `lastValidationReport()` megjelenítése: kódonkénti bontás, érintett rekordok, súlyosság.
- [ ] Szűrés súlyosságra.

## Kész, ha

- [ ] Release buildben a menüpont nem jelenik meg.

## Figyelj rá

- Ez a képernyő teszi gyorsan diagnosztizálhatóvá a tartalomhibát, anélkül hogy a felhasználót zavarná.

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
