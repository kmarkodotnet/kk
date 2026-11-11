# T-402 · core/data: kvízjelöltek lekérdezése

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-12 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-201](T-201-core-data-techniquerepository.md) |
| Követelmény | FR-11, FR-13 |
| Teszt | TR-03 |

## Cél

A generátor bemenetének összeállítása az adatbázisból.

## Teendők

- [ ] `getQuizCandidates(categories): QuizCandidateSet` – technikák + szinonimacsoportok + képkapuk.
- [ ] A `retired`, `quiz_excluded` és `INCOMPLETE` rekordok kizárva.
- [ ] A `dataset_trust` érték átadása a `content_state`-ből (ADR-010).

## Kész, ha

- [ ] A generátor semmilyen adatbázis-típust nem lát: tiszta domain objektumokat kap.

## Figyelj rá

- A szinonimacsoportokat a validátor állította elő és a `synonym_group` tábla őrzi – itt kell visszaolvasni, különben a CR-07 kizárás nem érvényesül.

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
