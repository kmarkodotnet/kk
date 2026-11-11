# T-103 · core/model: felhasználói adatmodellek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-03, US-04 |
| Modul | `core/model` |
| Méret | S (1–3 ó) |
| Függ | [T-101](T-101-core-model-azonositok-es-enumok.md) |
| Követelmény | DR-04 |
| Teszt | — |

## Cél

A felhasználói előrehaladás domain típusai. Külön életciklus a tartalomtól (DR-07).

## Teendők

- [ ] `QuizAttempt(id, sessionId, questionId, techniqueId, questionType, selectedTechniqueId, correct, answeredAt: Instant)`.
- [ ] `StudyReview(id, techniqueId, selfRating, reviewedAt: Instant)`.
- [ ] `QuizSession(id, startedAt, finishedAt?, requestedCount, answeredCount, seed)`.

## Kész, ha

- [ ] A `QuizAttempt` tárolja a `questionId`-t is – enélkül az `UNIQUE(sessionId, questionId)` index (ADR-005) nem építhető meg.
- [ ] A `QuizSession` tárolja a `seed`-et (ADR-007).

## Figyelj rá

- `Instant`-ot használunk, ami `minSdk 26`-tól natív – ezért nincs szükség desugaringra (ADR-009).
- A `finishedAt` nullable: a megszakított kvíz munkamenete befejezetlen marad (FR-19).

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
