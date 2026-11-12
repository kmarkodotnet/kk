# T-302 · feature/learn: tanulás beállítása

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09, US-10 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-203](T-203-di-hilt-modulok.md), [T-206](T-206-navigation-navhost-es-bottom-navigation.md) |
| Követelmény | FR-06, FR-07 |
| Teszt | — |

## Cél

Kategória- és elrejtési mód választása a kártyák indítása előtt.

## Teendők

- [ ] Többszörös kategóriaválasztás, kategóriánkénti darabszámmal.
- [ ] „Japán nevet rejtse el” / „Magyar nevet rejtse el” választó.
- [ ] „Kártyák indítása” gomb – letiltva, ha nincs megjeleníthető technika, indoklással.
- [ ] Az utolsó választás megjegyzése.

## Kész, ha

- [ ] A tanuláshoz **nem** kell `VERIFIED` státusz: a lektorálatlan tartalom is böngészhető, jelöléssel.

## Figyelj rá

- A kvíztől eltérően itt nincs `quizEligible` kapu – a tanulás célja az átnézés.

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
