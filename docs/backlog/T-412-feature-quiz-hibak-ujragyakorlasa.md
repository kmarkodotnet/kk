# T-412 · feature/quiz: hibák újragyakorlása

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-16 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-411](T-411-feature-quiz-eredmenykepernyo.md) |
| Követelmény | FR-21 |
| Teszt | TR-04 |

## Cél

Célzott ismétlés a hibás kérdések technikáiból, a statisztika torzítása nélkül.

## Teendők

- [ ] **Új** `QuizSession` **új** seeddel, a hibás kérdések helyes technikáiból.
- [ ] A régi munkamenet zárva marad, eredménye változatlan.
- [ ] A gomb csak akkor jelenik meg, ha van hibás válasz.

## Kész, ha

- [ ] A statisztika nem duplázódik: az új próbálkozások önálló, jogos rekordok.

## Figyelj rá

- Az új seed szándékos: ne ugyanabban a sorrendben jöjjenek a kérdések, különben a sorrendet tanulja meg a felhasználó.

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
