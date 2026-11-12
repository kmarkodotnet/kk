# T-405 · feature/quiz: QuizRunState

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-15 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-403](T-403-feature-quiz-beallito-viewmodel.md) |
| Követelmény | FR-19, NFR-07, ADR-007 |
| Teszt | TR-04 |

## Cél

A futó kvíz állapota, ami túléli a konfigurációváltást.

## Teendők

- [x] `QuizRunState(sessionId, seed, questions, currentIndex, answers, shortfall)` – szerializálva a `SavedStateHandle`-be.
- [x] Az `answers` map a megválaszoltság **egyetlen igazsága**.
- [x] Visszaállításkor nincs újragenerálás; ha mégis kell, a seed azonos tervet ad.

## Kész, ha

- [x] Forgatás után ugyanaz a kérdés, **ugyanaz az opciósorrend**, megőrzött válaszokkal.

## Figyelj rá

- A `questions` csak ID-ket tartalmaz (ADR-006), ezért az állapot kicsi és tartalomfrissítés után is helyes nevet jelenít meg.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – **nem alkalmazható**: a G-07 csoport (T-405/T-406/T-410) kizárólag az állapotgépet adja, Compose UI nélkül, ugyanúgy, mint a T-303
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
| `./gradlew.bat :core:quiz:test --rerun-tasks` | 0 | `BUILD SUCCESSFUL` |
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 34 teszt a `feature/quiz`-ben |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **528 teszt, 0 hiba** (509 → 528) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Mi készült:** `core/quiz`-ben `QuizRunStateCodec` (`encodePlan`/`decodePlan`/`encodeAnswers`/
`decodeAnswers`, `QuizRunStateCodecTest` 5 teszt) – ez az első hely, ahol a modul `main`
forráskészlete tényleges `Json`-t használ, mert a `feature/quiz` nem láthat
`kotlinx.serialization`-t (ADR-001, L3). Új `feature/quiz/run` csomag: `QuizRunState`
(`sessionId, seed, questions, currentIndex, answers, shortfall` – pontosan az
ARCHITECTURE 6.2 alakja, de plain Kotlin, nem `@Serializable`), `QuizRunUiState`
(`Loading`/`Empty`/`Content`) és `QuizRunViewModel` (a `SavedStateHandle`-ből olvassa
vissza a sessiont/seedet/tervet; `onNext`/`onAnswer`/`onRequestClose`/`onCancelClose`/
`onConfirmClose` – a T-406/T-410 is ebbe a fájlba került, lásd azok lezárása).
`QuizRunViewModelTest` 14 teszttel, köztük a forgatás-túlélés (T-405 „Kész ha").

**Megjegyzés:** a `QuizStartResult` (`feature/quiz/setup`) egy `seed: Long` mezővel
bővült, hogy a `QuizRunState.seed` megkapja az eredeti, injektált RNG-ből sorsolt magot –
ezt eddig a `QuizSetupViewModel.onStartQuiz` csak lokálisan használta, nem adta tovább. A
`QuizRunViewModel` bemenete (sessionId/seed/terv) ma kézzel összerakott
`SavedStateHandle`-ből jön a tesztekben; a tényleges `navigation` bekötés (a
`QuizStartResult` eljuttatása a `quiz/run` képernyő `SavedStateHandle`-jébe, **nem**
route-argumentumként – lásd a `QuizRunViewModel` KDoc-ja) dokumentált, nyitott ügy,
`docs/CONVENTIONS.md` 5. szakasz.
