# T-401 · core/data: ProgressRepository – kvízrész

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-13, US-15 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-116](T-116-core-data-room-sema.md) |
| Követelmény | FR-18, FR-19, DR-04 |
| Teszt | TR-04 |

## Cél

A kvízmunkamenet és a válaszok perzisztálása, az egyszeri válasz garanciájával.

## Teendők

- [ ] `startSession(requestedCount, seed): SessionId`, `finishSession(id)`.
- [ ] **`recordAttempt(attempt): Boolean`** – `false`, ha a kérdésre már volt válasz.
- [ ] Az `UNIQUE(sessionId, questionId)` ütközés `IGNORE` stratégiával, kivétel nélkül.
- [ ] `answeredAt` az injektált `Clock`-ból.

## Kész, ha

- [ ] Kétszer hívva ugyanarra a `questionId`-ra: egy sor keletkezik, a második hívás `false`.
- [ ] A megszakított munkamenet `finishedAt` értéke `null` marad.

## Figyelj rá

- A `Boolean` visszatérés teszi az „egyszeri válasz” szabályt a repository **szerződésének** részévé, nem UI-konvencióvá (ADR-005).

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
