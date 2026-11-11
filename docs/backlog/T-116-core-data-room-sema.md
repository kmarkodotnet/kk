# T-116 · core/data: Room séma

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-04, US-13 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-103](T-103-core-model-felhasznaloi-adatmodellek.md) |
| Követelmény | DR-04, DR-07, DR-08, FR-18 |
| Teszt | TR-05 |

## Cél

Az adatbázis: tartalom- és felhasználói táblák, a kritikus megszorításokkal.

## Teendők

- [ ] Tartalomtáblák: `category`, `technique`, `technique_alias`, `synonym_group`, `content_state`.
- [ ] Felhasználói táblák: `quiz_session`, `quiz_attempt`, `study_review`.
- [ ] **`UNIQUE(sessionId, questionId)` index** a `quiz_attempt`-en (ADR-005).
- [ ] A felhasználói táblák idegen kulcsa a tartalomra: **nincs** `CASCADE DELETE` (ADR-004).
- [ ] `@TypeConverter`: `Instant` ↔ `Long`, enum ↔ **`String`** (nem ordinal).
- [ ] `fallbackToDestructiveMigration()` **tilos**; sémaexport bekapcsolva (`room.schemaLocation`).

## Kész, ha

- [ ] A séma JSON exportálódik a `core/data/schemas/` alá.
- [ ] Minden DAO `suspend` vagy `Flow`; `allowMainThreadQueries` nincs.

## Figyelj rá

- Az enum `String`-ként tárolása azért kell, hogy az enum-értékek átrendezése ne rontsa el a meglévő adatot.
- A cascade hiánya a TR-05 garancia alapja: tartalomtörlés nem viheti el az előrehaladást.

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
