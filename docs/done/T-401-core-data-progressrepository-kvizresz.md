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

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-12 |
| Commit | `e7b188a` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 451 teszt, 0 hiba (10 új, a G-01 csoport egészére) |
| `./gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL, `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:**

- `startSession(requestedCount, seed): SessionId` – a `SessionId`-t a repository generálja,
  de **nem** `UUID.randomUUID()`-dal vagy `System.currentTimeMillis()`-szel (NFR-07 tiltja
  az üzleti kódban a közvetlen rendszerhívást): az injektált `Clock` időbélyegét és a
  hívó (a `quiz/setup` ViewModel, T-403) által már az injektált RNG-ből sorsolt `seed`-et
  kombinálja (`"session-${epochMilli}-${seed}"`). Lásd a `RoomProgressRepository` KDoc-ját
  a gyakorlati egyediség indoklásáért.
- `recordAttempt(attempt: NewAttempt): Boolean` – a `NewAttempt` (új típus, nem a domain
  `QuizAttempt`) **nem** hordoz `id`-t és `answeredAt`-et: azokat a perzisztálás adja, a
  hívó nem tudna kitalálni se adatbázis-kulcsot, se időbélyeget. A beszúrás és a menet
  `answeredCount`-jának növelése **egy tranzakcióban** fut; a `UNIQUE(sessionId,
  questionId)` ütközés `-1` sorazonosítót ad (ADR-005) → ilyenkor a számláló nem nő, és a
  metódus `false`-t ad vissza.
- `finishSession(id)` – csak akkor ír, ha a menet még befejezetlen (a DAO `WHERE
  finishedAt IS NULL` feltétele); ismeretlen vagy már lezárt `id`-ra is biztonságos, nem
  dob kivételt, egyszerűen nem történik írás (FR-19).
- A `ProgressRepository` interfész **nem** tartalmazza az ARCHITECTURE 5.3 vázlatában
  felsorolt `observeOverallProgress`/`observeCategoryProgress`/`observeTechniqueProgress`/
  `observeNeedsPractice` tagokat – azok a T-501 (aggregált statisztika) tárgya, és korai
  felvételük olyan tagot ígérne a felületen, amihez még nincs `@Query`.
- Lásd a T-301 és a T-502 lezárását a `recordStudyReview`/`observeStudyReviewSummary`
  illetve a `clearAllProgress` részletekért – ugyanaz a fájlpár hordozza mindegyiket.
