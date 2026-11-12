# T-410 · feature/quiz: előrehaladás és megszakítás

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-15 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-405](T-405-feature-quiz-quizrunstate.md) |
| Követelmény | FR-16, FR-19 |
| Teszt | TR-06 |

## Cél

A kvíz elhagyásának kezelt útja.

## Teendők

- [x] Lineáris előrehaladásjelző és „`{index}` / `{összes}`” – a `QuizRunUiState.Content.position`/`total` adja az adatot (lásd Megjegyzés: a tényleges Compose-widget egy UI-t adó későbbi feladat tárgya).
- [x] Bezárás (×) → megerősítő párbeszéd: „Megszakítod a kvízt? Az eddigi válaszaid megmaradnak.” – `onRequestClose`/`closeConfirmationVisible`.
- [x] A rendszer vissza-gombja ugyanezt váltja ki – ugyanazt az `onRequestClose`-t hívja majd a jövőbeli `BackHandler` is (lásd Megjegyzés).
- [x] Megszakításkor `finishedAt` marad `null`, `answeredCount` a tényleges érték – `onConfirmClose` szándékosan nem hív `finishSession`-t.
- [x] A kérdések között nincs visszalépés – nincs `onPrevious`, az `onNext` csak előre mozog.

## Kész, ha

- [x] A megszakított kvíz válaszai beszámítanak a Haladásba.

## Figyelj rá

- A megerősítés nélküli kilépés a leggyakoribb adatvesztési panasz forrása – ezért explicit.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – **nem alkalmazható**: lásd T-405 Megjegyzés; a progresszsáv/`AlertDialog`/`BackHandler` tényleges Compose UI-ja ennek a csoportnak (G-07) **nem** a tárgya
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

**Mi készült:** `QuizRunViewModel.onNext` (csak előre, nincs `onPrevious`, az utolsó
kérdésen no-op – a kvíz **normál** befejezése, az eredményképernyőre lépés és a
`finishSession` hívása a G-09 csoport, T-411, tárgya, szándékosan dokumentált hiány, lásd
`docs/CONVENTIONS.md` 1. szakasz új sora), és a megszakítás három metódusa:
`onRequestClose` (megerősítő párbeszéd megnyitása – ugyanezt hívná a bezárás gomb és a
jövőbeli `BackHandler` is), `onCancelClose` (becsukja a párbeszédet), `onConfirmClose`
(szándékosan **nem** hív `finishSession`-t – a `finishedAt` a FUNCTIONAL_SPEC 5.2 szerint
megszakításkor `null` marad; az addig rögzített `QuizAttempt`-ek, amiket a T-406
`onAnswer`-je írt, automatikusan megmaradnak, nincs mit visszavonni – és egy
`closeEvent: Flow<Unit>` egyszeri eseményt küld, amiből a hívó dönt a navigációról, L5).
A `QuizRunUiState.Content` a `position`/`total`/`answeredCount`/`isLastQuestion`/
`closeConfirmationVisible` mezőkkel adja a progresszsáv és a párbeszéd **adatát** – a
tényleges Compose-widget (lineáris `ProgressIndicator`, `AlertDialog`,
`androidx.activity.compose.BackHandler`) egy UI-t adó későbbi feladat tárgya, lásd
Megjegyzés. 6 dedikált teszt a `QuizRunViewModelTest`-ben (előrehaladás, határ az utolsó
kérdésen, a három megszakítás-metódus, és hogy a megszakítás megtartja a már rögzített
válaszokat – a `FakeQuizRunProgressRepository.finishSession` kivételt dob, ha mégis
meghívnák, így egy jövőbeli regresszió azonnal bukna).

**Megjegyzés:** ez a csoport (G-07) szándékosan nem ad Compose UI-t – sem a progresszsáv/
megerősítő-párbeszég pixeleit, sem a kérdés-UI-t. A tényleges rajzolás egy később
sorra kerülő feladat (és a kérdéstípusok UI-ja, T-407/T-408/T-409) dolga, ugyanaz a
mintázat, mint a `LearnCardsViewModel`-nél T-303 (ViewModel) → T-304 (UI). A `BackHandler`
ehhez a projekthez **első** előfordulása lenne (`androidx.activity:activity-compose`, már
a version catalogban, eddig csak az `app` modul `MainActivity`-jében használt
`setContent`-hez) – ezt a jövőbeli UI-feladatnak kell majd hozzáadnia a `feature/quiz`
`build.gradle.kts`-éhez.
