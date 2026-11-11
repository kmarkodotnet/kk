# T-502 · core/data: előrehaladás törlése

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-19 |
| Modul | `core/data` |
| Méret | S (1–3 ó) |
| Függ | [T-401](T-401-core-data-progressrepository-kvizresz.md) |
| Követelmény | FR-35, NFR-09 |
| Teszt | — |

## Cél

A felhasználói adatok törlése – a tartalom érintetlenül hagyásával.

## Teendők

- [ ] `clearAllProgress()`: `quiz_attempt`, `quiz_session`, `study_review` ürítése egy tranzakcióban.
- [ ] A tartalomtáblák és a `content_state` **változatlan**.

## Kész, ha

- [ ] Törlés után a tudástár és a tanulás ugyanúgy működik; csak a statisztika nullázódik.

## Figyelj rá

- A tranzakció azért kell, hogy ne maradjon árva `quiz_attempt` egy már törölt munkamenethez.

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
