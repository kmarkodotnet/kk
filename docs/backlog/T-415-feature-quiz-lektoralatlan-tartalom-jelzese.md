# T-415 · feature/quiz: lektorálatlan tartalom jelzése

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-08 |
| Modul | `feature/quiz` |
| Méret | S (1–3 ó) |
| Függ | [T-404](T-404-feature-quiz-beallito-ui.md), [T-411](T-411-feature-quiz-eredmenykepernyo.md) |
| Követelmény | CR-01, CR-03, ADR-010 |
| Teszt | — |

## Cél

A kvíz végig közli, hogy lektorálatlan anyagon gyakorol a felhasználó.

## Teendők

- [ ] A kvíz indítása előtt: figyelmeztető sor a beállító képernyőn.
- [ ] Az eredményképernyőn: „Az eredmény lektorálatlan tananyagon alapul.”
- [ ] A jelzés a `dataset_trust` értékből jön, nem felhasználói beállításból.
- [ ] Ha `reviewed` tartalom érkezik, a jelzések **maguktól** eltűnnek.

## Kész, ha

- [ ] A jelzés nem elrejthető és nem kikapcsolható.
- [ ] `reviewed` tartalommal a képernyők jelzés nélkül jelennek meg – kódmódosítás nélkül.

## Figyelj rá

- Ez a feltétele annak, hogy lektorálatlan tartalmon egyáltalán indulhasson kvíz (ADR-010).

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
