# T-207 · feature/library: lista ViewModel

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06 |
| Modul | `feature/library` |
| Méret | L (6–12 ó) |
| Függ | [T-202](T-202-core-data-kereses.md), [T-203](T-203-di-hilt-modulok.md) |
| Követelmény | FR-01, FR-02, FR-03 |
| Teszt | — |

## Cél

A Tudástár lista állapotkezelése.

## Teendők

- [ ] `LibraryListUiState` sealed: `Loading` / `Empty(reason)` / `Content(...)` / `Error`.
- [ ] Keresőszöveg ~200 ms debounce-szal; kategóriaszűrő halmaz.
- [ ] Keresőszöveg és szűrők `SavedStateHandle`-ben (forgatás túlélése).
- [ ] Kategóriánkénti találatszám a chipekhez.

## Kész, ha

- [ ] Forgatás után a keresés és a szűrés megmarad.
- [ ] A ViewModel nem importál Compose típust (L6).

## Figyelj rá

- Az `Empty` okot hordoz: „nincs tartalom” vs. „nincs találat” – különben a UI nem tud helyes üzenetet adni.

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
