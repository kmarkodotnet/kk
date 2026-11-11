# T-209 · feature/library: részletező képernyő

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-07, US-08 |
| Modul | `feature/library` |
| Méret | L (6–12 ó) |
| Függ | [T-207](T-207-feature-library-lista-viewmodel.md) |
| Követelmény | FR-04, FR-05 |
| Teszt | TR-06 |

## Cél

Egy technika teljes adatlapja.

## Teendők

- [ ] Nagy illusztráció, romaji, magyar név, japán íráskép (ha van), aliasok, kategóriachip, magyarázat.
- [ ] Hitelesítési blokk: státusz és `VERIFIED` esetén a forrás.
- [ ] Ha már gyakoroltam: kis összegzés + link a haladás-részletezőre.
- [ ] Üres opcionális mező → a szakasz nem jelenik meg.

## Kész, ha

- [ ] Hiányos rekordnál a „Hiányos rekord” blokk és a kvízkizárás jelzése megjelenik.
- [ ] A kategóriachipre koppintva a listára jutunk, arra a kategóriára szűrve.

## Figyelj rá

- A „nincs adat” feliratok halmozása zajos lenne – inkább ne jelenjen meg a szakasz (FUNCTIONAL_SPEC 3.2).

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
