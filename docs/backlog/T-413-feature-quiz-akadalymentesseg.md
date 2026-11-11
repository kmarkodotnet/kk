# T-413 · feature/quiz: akadálymentesség

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14, US-20 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-408](T-408-feature-quiz-c-es-d-kerdes-ui.md), [T-409](T-409-feature-quiz-valasz-visszajelzes.md) |
| Követelmény | NFR-04 |
| Teszt | TR-06 |

## Cél

A kvíz TalkBackkel is helyesen használható – a válasz felfedése nélkül.

## Teendők

- [ ] Opciók `selected` szemantikával; válasz után `stateDescription`.
- [ ] A (D) opciók képleírása pozíció alapú, nevet nem tartalmaz.
- [ ] Bejárási sorrend: kérdés → opciók → visszajelzés → Következő.
- [ ] Minden érintési célpont ≥ 48 dp.

## Kész, ha

- [ ] TalkBackkel végigvihető egy teljes kvíz anélkül, hogy a felolvasás elárulná a helyes választ.

## Figyelj rá

- Ez az a pont, ahol az akadálymentesség és a kvízhelyesség ütközhet – a megoldás a pozíció alapú leírás.

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
