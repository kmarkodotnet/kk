# T-604 · Akadálymentességi átfésülés

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-20 |
| Modul | minden feature |
| Méret | L (6–12 ó) |
| Függ | [T-413](T-413-feature-quiz-akadalymentesseg.md), [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | NFR-04 |
| Teszt | TR-06 |

## Cél

Végigmenni a négy fő képernyőn akadálymentességi szemmel.

## Teendők

- [ ] Minden illusztráció `contentDescription`-jének ellenőrzése – különös tekintettel a kvízopciókra.
- [ ] Érintési célpontok ≥ 48 dp.
- [ ] TalkBack bejárási sorrend képernyőnként.
- [ ] Kontraszt WCAG AA világos és sötét témában.
- [ ] A helyesség/állapot nem csak színnel jelenik meg.

## Kész, ha

- [ ] Egy teljes kvíz végigvihető TalkBackkel, a válasz felfedése nélkül.
- [ ] A `FUNCTIONAL_SPEC` 9. fejezetének minden sora teljesül.

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
