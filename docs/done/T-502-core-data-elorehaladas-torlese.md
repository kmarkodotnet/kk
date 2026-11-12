# T-502 · core/data: előrehaladás törlése

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-19 |
| Modul | `core/data` |
| Méret | S (1–3 ó) |
| Függ | [T-401](T-401-core-data-progressrepository-kvizresz.md) |
| Követelmény | FR-35, NFR-09 |
| Teszt | — |

## Cél

A felhasználói adatok törlése – a tartalom érintetlenül hagyásával.

## Teendők

- [ ] `clearAllProgress()`: `quiz_attempt`, `quiz_session`, `study_review` ürítése egy tranzakcióban.
- [ ] A tartalomtáblák és a `content_state` **változatlan**.

## Kész, ha

- [ ] Törlés után a tudástár és a tanulás ugyanúgy működik; csak a statisztika nullázódik.

## Figyelj rá

- A tranzakció azért kell, hogy ne maradjon árva `quiz_attempt` egy már törölt munkamenethez.

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

- `clearAllProgress()` egy `withTransaction` blokkban üríti a `quiz_session` és a
  `study_review` táblát. Nincs harmadik, külön `DELETE FROM quiz_attempt` hívás: a
  `quiz_attempt.sessionId` `CASCADE` idegen kulcsa (a `quiz_session`-re, felhasználói →
  felhasználói irányban, ADR-004) automatikusan viszi a próbálkozásokat is, amikor a
  menet törlődik.
- A tartalomtáblák (`category`, `technique`, `technique_alias`, `synonym_group`) és a
  `content_state` nem szerepelnek a tranzakcióban – teszt bizonyítja, hogy törlés után a
  `technique`/`category` sorok száma változatlan (FR-35, NFR-09).
- Lásd a T-301 és a T-401 lezárását a `recordStudyReview`/`observeStudyReviewSummary`
  illetve a `startSession`/`recordAttempt`/`finishSession` részletekért – ugyanaz a
  fájlpár (`ProgressRepository.kt`, `RoomProgressRepository.kt`) hordozza mindegyiket.
