# T-503 · feature/progress: áttekintő képernyő

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-17 |
| Modul | `feature/progress` |
| Méret | L (6–12 ó) |
| Függ | [T-501](T-501-core-data-aggregalt-statisztika-lekerdezesek.md), [T-206](T-206-navigation-navhost-es-bottom-navigation.md) |
| Követelmény | FR-23, FR-24, FR-27 |
| Teszt | TR-06 |

## Cél

A Haladás fő képernyője.

## Teendők

- [ ] Összesítő kártya: megválaszolt, helyes, sikerarány.
- [ ] Nulla próbálkozás → „Még nincs adat. Indíts egy kvízt!” + gomb.
- [ ] Kategóriánkénti bontás sávval és **szöveges** értékkel.
- [ ] Próbálkozás nélküli kategória → „nincs adat”, nem 0%.

## Kész, ha

- [ ] Sehol nem jelenik meg 0% ott, ahol valójában nincs adat.
- [ ] A sávok értéke felolvasható (NFR-04).

## Figyelj rá

- A 0% és a „nincs adat” összekeverése demotiváló és félrevezető – ez az FR-27 lényege.

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
