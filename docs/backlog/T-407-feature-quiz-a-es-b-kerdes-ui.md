# T-407 · feature/quiz: (A) és (B) kérdés UI

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-405](T-405-feature-quiz-quizrunstate.md) |
| Követelmény | FR-16, FR-12 |
| Teszt | TR-06 |

## Cél

A két szöveges kérdéstípus megjelenítése.

## Teendők

- [ ] (A): „Mit jelent: **`{romaji}`**?” + 4 magyar név gomb.
- [ ] (B): „Melyik a japán neve: **`{magyar}`**?” + 4 romaji gomb.
- [ ] Egy oszlop, nagy érintési célpontok, hosszú szöveg tördelése.
- [ ] Felső sáv: „`{index}` / `{összes}`” + lineáris előrehaladásjelző.

## Kész, ha

- [ ] Hosszú magyar neveknél sem törik a layout, és nem vágódik le a szöveg.

## Figyelj rá

- A tartalomban a leghosszabb magyar név ~45 karakter – a gomboknak ezt kezelniük kell.

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
