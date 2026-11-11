# T-409 · feature/quiz: válasz-visszajelzés

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-407](T-407-feature-quiz-a-es-b-kerdes-ui.md) |
| Követelmény | FR-17, NFR-04 |
| Teszt | TR-06 |

## Cél

Azonnali, akadálymentes visszajelzés és a helyes megoldás bemutatása.

## Teendők

- [ ] A kiválasztott opció megjelölve: pipa + „Helyes” vagy × + „Helytelen” – **ikon + szöveg + szín**.
- [ ] A helyes opció mindig kiemelve.
- [ ] Magyarázó kártya: romaji + magyar név + ábra, link a részletezőre.
- [ ] „Következő” gomb (az utolsónál „Eredmény”).

## Kész, ha

- [ ] A helyesség nem **csak** színnel jelenik meg (színtévesztés).
- [ ] `stateDescription` = „helyes” / „helytelen”.

## Figyelj rá

- A helyes megoldás hármas bemutatása (név–jelentés–ábra) a tanulási cél magja (FR-17).

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
