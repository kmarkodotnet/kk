# T-113 · core/quiz: QuizScoring

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-03 |
| Modul | `core/quiz` |
| Méret | S (1–3 ó) |
| Függ | [T-103](T-103-core-model-felhasznaloi-adatmodellek.md) |
| Követelmény | FR-20, FR-27, DR-09 |
| Teszt | TR-04 |

## Cél

Eredményszámítás tiszta függvényként, nullable aránnyal.

## Teendők

- [ ] `object QuizScoring { fun score(attempts: List<QuizAttempt>): Score }`.
- [ ] `Score(answered, correct, ratio: Double?)` – `ratio` **null**, ha `answered == 0`.
- [ ] Csak a ténylegesen megválaszolt kérdések számítanak; a meg nem válaszolt nem hibás.

## Kész, ha

- [ ] 0 válasz → `ratio == null` (nem `0.0`).
- [ ] Nincs állapot, nincs mellékhatás, nincs óra-hívás.

## Figyelj rá

- A nullable típus azért fontos, mert megakadályozza, hogy a UI véletlenül 0%-ot írjon ki ott, ahol „nincs adat” a helyes (FR-27).

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
