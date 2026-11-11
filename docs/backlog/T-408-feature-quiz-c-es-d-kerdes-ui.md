# T-408 · feature/quiz: (C) és (D) kérdés UI

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-407](T-407-feature-quiz-a-es-b-kerdes-ui.md), [T-204](T-204-core-ui-techniqueillustration.md) |
| Követelmény | FR-12, FR-13, NFR-04 |
| Teszt | TR-06 |

## Cél

A két kép-alapú kérdéstípus megjelenítése.

## Teendők

- [ ] (C): illusztráció a kérdésben + 4 romaji gomb.
- [ ] (D): „Melyik ábra: **`{romaji}`**?” + 2×2 képrács.
- [ ] A képrács elemei ≥ 48 dp érintési célpontok, kiválasztás jól látható.
- [ ] A képek leírása **pozíció alapú** („első válaszlehetőség illusztrációja”).

## Kész, ha

- [ ] A (D) opciók `contentDescription`-je nem tartalmazza a technika nevét.
- [ ] Keskeny képernyőn (≈400 dp) is használható a rács.

## Figyelj rá

- Ez a két típus jelenleg csak az `stances` kategóriában aktiválható – de a kódnak készen kell állnia, mert a tartalom bővül.

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
