# T-306 · feature/learn: önértékelés

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-11 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-301](T-301-core-data-progressrepository-tanulasi-resz.md), [T-304](T-304-feature-learn-kartya-ui.md) |
| Követelmény | FR-10, FR-28 |
| Teszt | TR-06 |

## Cél

„Tudtam” / „Gyakorolnom kell” rögzítése.

## Teendők

- [ ] A két gomb **csak felfedés után** aktív.
- [ ] Megnyomásra `StudyReview` mentése és automatikus továbblépés.
- [ ] Önértékelés nélküli továbblépés is megengedett (ekkor nincs rekord).

## Kész, ha

- [ ] A letiltott állapot oka TalkBackkel is bejelentve.
- [ ] A mentés az injektált `Clock` idejével történik.

## Figyelj rá

- Az automatikus továbblépés gyorsítja a gyakorlást, de ne legyen visszavonhatatlan: az Előző gomb visszavisz.

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
