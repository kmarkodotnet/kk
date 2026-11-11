# T-406 · feature/quiz: egyszeri válasz kikényszerítése

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-13 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-405](T-405-feature-quiz-quizrunstate.md), [T-401](T-401-core-data-progressrepository-kvizresz.md) |
| Követelmény | FR-18 |
| Teszt | TR-04 |

## Cél

A dupla koppintás nem számíthat kétszer.

## Teendők

- [ ] Ha `questionId ∈ state.answers` → a válaszkezelő **no-op**.
- [ ] A `recordAttempt` `false` válaszának kezelése (nem módosítja az állapotot).
- [ ] A gombletiltás csak másodlagos, vizuális védelem.

## Kész, ha

- [ ] Gyors dupla koppintásra egyetlen `QuizAttempt` keletkezik.
- [ ] A helyesség összehasonlítása **azonosító alapú**: `selected == question.correctTechniqueId`.

## Figyelj rá

- Három védelmi vonal: ViewModel állapot → DB egyedi index → gombletiltás. A helyesség az első kettőn múlik.

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
