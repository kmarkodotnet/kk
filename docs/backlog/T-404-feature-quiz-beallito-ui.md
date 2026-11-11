# T-404 · feature/quiz: beállító UI

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-12 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-403](T-403-feature-quiz-beallito-viewmodel.md) |
| Követelmény | FR-11, FR-12, FR-13, FR-22 |
| Teszt | TR-06 |

## Cél

A kvízbeállító képernyő.

## Teendők

- [ ] Kategóriaválasztó a kvízképes darabszámmal („8 kvízképes / 12 technika”).
- [ ] Kérdésszám szegmentált választó: 5 / 10 / 20.
- [ ] Négy kérdéstípus jelölőkkel; a (C)/(D) letiltva + indoklás a jelenlegi képszámmal.
- [ ] Készenléti összegzés és `shortfall` üzenet a gomb felett.

## Kész, ha

- [ ] A letiltott típus mellett konkrét szám áll („jelenleg: 7”), nem általános szöveg.
- [ ] `@Preview` a 0 kérdéses és a shortfall esetre is.

## Figyelj rá

- A mostani tartalomnál a (C)/(D) csak az `stances` kategória kiválasztásakor válik aktívvá – ezt a képernyőnek érthetővé kell tennie, nem rejtélyessé.

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
