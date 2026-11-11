# T-411 · feature/quiz: eredményképernyő

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-16 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-113](T-113-core-quiz-quizscoring.md), [T-410](T-410-feature-quiz-elorehaladas-es-megszakitas.md) |
| Követelmény | FR-20, FR-27 |
| Teszt | TR-04, TR-06 |

## Cél

A kvíz összegzése, őszinte számokkal.

## Teendők

- [ ] „`{helyes}` / `{megválaszolt}` helyes” + százalék.
- [ ] 0 válasz → „Nem válaszoltál egyetlen kérdésre sem.” (nincs 0%).
- [ ] Megszakított kvíz → „Megszakított kvíz – `{megválaszolt}` / `{kért}` kérdés.”
- [ ] Hibás kérdések listája: típus, helyes technika (romaji + magyar + kis ábra), a saját válasz; tétel kattintható.
- [ ] Gombok: Hibák újragyakorlása / Új kvíz / Vissza a Haladáshoz.

## Kész, ha

- [ ] A sikerarány szövegesen is kiolvasható, nem csak diagramon (NFR-04).

## Figyelj rá

- A százalék a `QuizScoring`-ból jön (T-113), nem a UI-ban újraszámolva.

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
