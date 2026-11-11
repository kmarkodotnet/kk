# T-307 · feature/learn: záróképernyő

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09 |
| Modul | `feature/learn` |
| Méret | S (1–3 ó) |
| Függ | [T-304](T-304-feature-learn-kartya-ui.md) |
| Követelmény | FR-06 |
| Teszt | — |

## Cél

A kártyasor végén összegzés és továbblépési lehetőségek.

## Teendők

- [ ] „Végigértél `{n}` kártyán.”
- [ ] Gombok: Újrakezdés / Vissza a beállításokhoz / Kvíz indítása ezekkel a kategóriákkal.

## Kész, ha

- [ ] A „Kvíz indítása” átadja a kiválasztott kategóriákat a kvízbeállítónak.

## Figyelj rá

- A tanulás → kvíz átvezetés a természetes tanulási út: előbb átnézem, aztán ellenőrzöm magam.

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
