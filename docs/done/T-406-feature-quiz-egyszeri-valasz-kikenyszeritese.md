# T-406 · feature/quiz: egyszeri válasz kikényszerítése

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-13 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-405](T-405-feature-quiz-quizrunstate.md), [T-401](T-401-core-data-progressrepository-kvizresz.md) |
| Követelmény | FR-18 |
| Teszt | TR-04 |

## Cél

A dupla koppintás nem számíthat kétszer.

## Teendők

- [x] Ha `questionId ∈ state.answers` → a válaszkezelő **no-op**.
- [x] A `recordAttempt` `false` válaszának kezelése (nem módosítja az állapotot).
- [x] A gombletiltás csak másodlagos, vizuális védelem.

## Kész, ha

- [x] Gyors dupla koppintásra egyetlen `QuizAttempt` keletkezik.
- [x] A helyesség összehasonlítása **azonosító alapú**: `selected == question.correctTechniqueId`.

## Figyelj rá

- Három védelmi vonal: ViewModel állapot → DB egyedi index → gombletiltás. A helyesség az első kettőn múlik.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – **nem alkalmazható**: lásd T-405 Megjegyzés, a gombletiltás (vizuális, másodlagos védelem) egy UI-t adó későbbi feladat tárgya
- [x] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

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
| Commit | lásd a G-07 csoport (T-405, T-406, T-410) közös commitja |

**Futtatott parancsok** (Bash, `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **528 teszt, 0 hiba** (509 → 528) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Mi készült:** `QuizRunViewModel.onAnswer` (lásd T-405 lezárása a fájl helyéről) – első
védelmi vonal: a ViewModel saját, a `SavedStateHandle`-ben kódolt `answers` map-je alapján
no-op-ol, **mielőtt** a `ProgressRepository`-t elérné; második: a `recordAttempt` `false`
visszatérése (a valódi implementációban a DB `UNIQUE(sessionId, questionId)` ütközése) nem
módosítja az állapotot. A helyesség mindig `selectedTechniqueId ==
question.correctTechniqueId` – azonosító-összehasonlítás, soha nem szöveges egyezés
(DR-03). 3 dedikált teszt a `QuizRunViewModelTest`-ben a gyors dupla koppintásra, a
repository-elutasításra és az azonosító-alapú helyességre (helyes és helytelen válasz
egyaránt).

**Megjegyzés:** a harmadik (vizuális, gombletiltás) védelmi vonal UI-feladat, nincs ebben
a körben – lásd T-405 Megjegyzés.
