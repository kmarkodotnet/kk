# T-210 · feature/library: üres és hibaállapotok

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06 |
| Modul | `feature/library` |
| Méret | M (3–6 ó) |
| Függ | [T-208](T-208-feature-library-lista-ui.md) |
| Követelmény | NFR-06, FR-32 |
| Teszt | — |

## Cél

A `FUNCTIONAL_SPEC` 2.4 és 10. pontja szerinti állapotok bekötése.

## Teendők

- [ ] „A tudástár üres” + debug buildben „Diagnosztika” gomb.
- [ ] „Nincs találat a(z) …” keresésre + „Szűrők törlése” gomb.
- [ ] Betöltési hiba + „Újra”.
- [ ] `Loading` a tartalomimport alatt (nincs félig betöltött lista).

## Kész, ha

- [ ] Mind a négy állapot kézzel kiváltható és helyesen jelenik meg.

## Figyelj rá

- A félig betöltött lista rosszabb, mint a töltésjelző: a felhasználó azt hinné, ennyi a tananyag.

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
