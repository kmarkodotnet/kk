# T-305 · feature/learn: feliratos kép elrejtése felfedésig

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-10 |
| Modul | `feature/learn` |
| Méret | S (1–3 ó) |
| Függ | [T-304](T-304-feature-learn-kartya-ui.md) |
| Követelmény | FR-07, ADR-011 |
| Teszt | — |

## Cél

A képbe égetett név ne fedje fel a választ a felfedés előtt.

## Teendők

- [ ] Ha `IllustrationRef.Asset.hideableOnStudyCard == false`, a kép **csak felfedés után** jelenik meg.
- [ ] Addig semleges helyőrző + rövid magyarázat, miért nem látszik még a kép.

## Kész, ha

- [ ] A KY-0045, KY-0050 és KY-0052 kártyáján a kép felfedés előtt nem látható.
- [ ] A többi kép a szokásos módon megjelenik.

## Figyelj rá

- Ez a mostani tartalom konkrét következménye: 3 képen rajta a név. Ha később felirat nélküli képek érkeznek, a konverter újrafuttatása után ez az ág magától kikapcsol.

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
