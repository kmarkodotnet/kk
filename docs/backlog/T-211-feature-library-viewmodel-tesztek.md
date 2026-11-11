# T-211 · feature/library: ViewModel tesztek

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06 |
| Modul | `feature/library` |
| Méret | M (3–6 ó) |
| Függ | [T-207](T-207-feature-library-lista-viewmodel.md) |
| Követelmény | FR-02, FR-03 |
| Teszt | — |

## Cél

A lista logikájának lefedése emulátor nélkül.

## Teendők

- [ ] Keresés normalizálása (ékezet, kisbetű, részszó), alias-találat jelzése.
- [ ] Kategóriaszűrés, több kategória egyszerre.
- [ ] `SavedStateHandle` visszaállítás.
- [ ] `Empty` ok helyes megállapítása (nincs tartalom vs. nincs találat).

## Kész, ha

- [ ] `kotlinx-coroutines-test` + fake repository + fix `Clock`; a tesztek determinisztikusak.

## Figyelj rá

- Az alias-ág szintetikus adattal tesztelendő, mert a valódi tartalomban minden `aliases` üres.

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
