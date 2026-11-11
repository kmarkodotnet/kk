# T-108 · core/quiz: jelöltszűrés és jogosultsági kapuk

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-02, US-12 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-107](T-107-core-quiz-a-generator-szerzodese.md) |
| Követelmény | FR-13, FR-14, CR-05 |
| Teszt | TR-03 |

## Cél

Annak eldöntése, melyik technika melyik kérdéstípusban használható.

## Teendők

- [ ] `quizEligible` formula (ADR-010): `status != INCOMPLETE && !quizExcluded && !retired && hasFieldsFor(type) && datasetTrust engedi`.
- [ ] Típusonkénti mezőkövetelmény: (A)/(B) romaji + magyar név; (C)/(D) ezen felül `IllustrationRef.Asset` **`quizUsable = true`** értékkel.
- [ ] Kategóriaszűrő alkalmazása.
- [ ] Típusonkénti jelöltszám számítása a `SkipReason` megállapításához.

## Kész, ha

- [ ] Feliratos kép (`quizUsable = false`) sosem kerül be (C)/(D) kérdésbe – sem helyesként, sem distraktorként.
- [ ] `INCOMPLETE` rekord egyetlen típusban sem jelenik meg.

## Figyelj rá

- A `quizUsable` és a `review_status` két külön dolog: egy szakmailag hiteles, de feliratos kép is kizárt a kvízből (ADR-011).
- A jelenlegi tartalomnál ez azt jelenti: (C)/(D) csak az `stances` kategóriában, 7 képpel.

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
