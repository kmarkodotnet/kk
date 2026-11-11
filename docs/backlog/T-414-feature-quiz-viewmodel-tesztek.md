# T-414 · feature/quiz: ViewModel tesztek

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-13, US-15, US-16 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-412](T-412-feature-quiz-hibak-ujragyakorlasa.md) |
| Követelmény | FR-18, FR-19, FR-21, FR-22 |
| Teszt | TR-04 |

## Cél

A kvíz UI-logikájának lefedése emulátor nélkül.

## Teendők

- [ ] Dupla válasz → egy `QuizAttempt`, változatlan eredmény.
- [ ] `SavedStateHandle` visszaállítás → azonos kérdés és opciósorrend.
- [ ] Megszakítás → `finishedAt == null`, a válaszok megmaradnak.
- [ ] Shortfall és 0 kérdés esetén a helyes UI állapot.
- [ ] Újragyakorlás → új `sessionId`, új seed, a régi eredmény változatlan.

## Kész, ha

- [ ] Fake repository, fix `Clock`, rögzített seed; a tesztek determinisztikusak.

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
