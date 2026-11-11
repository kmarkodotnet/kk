# T-102 · core/model: Technique, Category, IllustrationRef

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-01 |
| Modul | `core/model` |
| Méret | S (1–3 ó) |
| Függ | [T-101](T-101-core-model-azonositok-es-enumok.md) |
| Követelmény | DR-01, DR-02 |
| Teszt | — |

## Cél

A tartalom domain modellje, az ADR-002 és ADR-003 döntésekkel.

## Teendők

- [ ] `Category(id, nameHu, sortOrder)`.
- [ ] `Technique(id, categoryId, romaji, romajiAscii, hungarian, japaneseScript?, aliases, descriptionHu?, illustration?, status, quizExcluded, retired, datasetVersion)`.
- [ ] `sealed interface IllustrationRef { data class Asset(ref, fileName, quizUsable, hideableOnStudyCard); data object Placeholder }`.
- [ ] A `status` a háromállapotú `VerificationStatus` (ADR-002), **nem** boolean.

## Kész, ha

- [ ] Minden mező immutable (`val`), a `Technique` `data class`.
- [ ] A kép-alapú kvízjogosultság egyetlen típusvizsgálattal eldönthető: `illustration is IllustrationRef.Asset && illustration.quizUsable`.

## Figyelj rá

- Az `Asset` hordozza a `quizUsable` és `hideableOnStudyCard` kaput, mert a feliratos kép attól még hiteles lehet, hogy kvízben használhatatlan (ADR-011 kiegészítés).
- `quizEligible` **nincs** a modellben: az számított érték (ADR-010), nem tárolt adat.

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
