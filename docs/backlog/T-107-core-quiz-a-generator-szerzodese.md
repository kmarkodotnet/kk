# T-107 · core/quiz: a generátor szerződése

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-02 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-102](T-102-core-model-technique-category-illustrationref.md) |
| Követelmény | FR-14, FR-15, FR-22 |
| Teszt | TR-02 |

## Cél

A kvízgenerátor be- és kimeneti típusai. ADR-006: a kérdés csak azonosítókat tárol.

## Teendők

- [ ] `QuizRequest(candidates, enabledTypes, categoryFilter, requestedCount, seed, datasetTrust)`.
- [ ] `QuizQuestion(id, type, correctTechniqueId, optionTechniqueIds)` – **se szöveg, se kép**.
- [ ] `QuizPlan(questions, skippedTypes: Map<QuestionType, SkipReason>, shortfall: Shortfall?)`.
- [ ] `enum SkipReason { NOT_ENOUGH_CANDIDATES, NOT_ENOUGH_IMAGES, TYPE_DISABLED }`.
- [ ] `QuizReadiness` a `dryRun()` kimenetének (T-111).

## Kész, ha

- [ ] A `QuizQuestion` szerializálható (a `SavedStateHandle`-be kerül, T-405).
- [ ] Nincs olyan mező, amibe megjelenítendő szöveg kerülhetne.

## Figyelj rá

- Azért csak ID: ha két technikának azonos a magyar neve, a szöveges összehasonlítás hamis találatot adna (DR-03, ADR-006).
- A `skippedTypes` és a `shortfall` nem hibajelzés, hanem a UI-nak szóló indoklás (FR-22).

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
