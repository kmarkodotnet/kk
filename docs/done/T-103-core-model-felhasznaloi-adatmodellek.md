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

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-11 |
| Commit | `PENDING` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew testDebugUnitTest` | 0 | 35 teszt, 0 hiba (`:core:model:test`), `BUILD SUCCESSFUL` |
| `./gradlew assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` (19 610 739 bájt) |

**Megjegyzés:**

1. **A `QuizAttempt` hordozza a `questionId`-t** – a task és az ADR-005 szerint. Az
   `docs/ARCHITECTURE.md` 3. szakaszának Kotlin vázlata ezt a mezőt **még nem
   tartalmazza** (ott csak `id, sessionId, techniqueId, questionType,
   selectedTechniqueId, correct, answeredAt` áll). A kód a taskot és az ADR-005-öt
   követi, mert enélkül az `UNIQUE(sessionId, questionId)` index nem építhető meg.
   **Javasolt új task: az `ARCHITECTURE.md` kódvázlatának frissítése** (dokumentációs
   drift, ugyanaz a jelenség, mint a T-102 → ADR-003 esetében).
2. **Az `id` típusa szándékosan `Long`, nem value class.** A `QuizAttempt` és a
   `StudyReview` elsődleges kulcsát a Room generálja (`autoGenerate = true`), a `0`
   a „még nem perzisztált" rekordot jelenti. A `QuizSession.id` ezzel szemben
   `SessionId`, mert a menetre a futásállapot és a próbálkozások már a beszúrás előtt
   hivatkoznak – az azonosítót itt a domain adja, nem az adatbázis.
3. **Modellszintű invariánsok.** A `QuizSession` konstruktora elutasítja a
   `requestedCount <= 0`, a negatív `answeredCount`, az `answeredCount > requestedCount`
   és a `finishedAt < startedAt` esetet; a `QuizAttempt`/`StudyReview` a negatív
   sor-azonosítót. A `ProgressRepository` (T-301, T-401) emiatt ne számítson arra, hogy
   egy félig kitöltött számlálópárból domain objektumot tud építeni.
4. **`QuizSession.isFinished` számított érték** (`finishedAt != null`), nem tárolt
   `Boolean` – FR-19 szerint a megszakított menet befejezetlen marad, és két
   igazságforrás elcsúszhatna egymástól.
5. **`correct` megtartva a két azonosító mellett.** Nem redundancia a DR-09
   értelmében: szinonimacsoport esetén a helyesség megítélése a kvízmotor dolga, és azt
   az ítéletet kell megőrizni – a statisztika ezt a mezőt összegzi, nem számol újra. A
   `QuizScoring` (T-113) erre épül.
6. **Nincs `Instant.now()` a modulban** (NFR-07): minden időbélyeg kívülről, injektált
   `Clock`-ból érkezik paraméterként.
