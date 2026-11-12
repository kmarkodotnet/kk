# T-407 · feature/quiz: (A) és (B) kérdés UI

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-405](T-405-feature-quiz-quizrunstate.md) |
| Követelmény | FR-16, FR-12 |
| Teszt | TR-06 |

## Cél

A két szöveges kérdéstípus megjelenítése.

## Teendők

- [x] (A): „Mit jelent: **`{romaji}`**?” + 4 magyar név gomb – `QuizTextQuestion`
      (`QuizRunScreen.kt`), a kérdés tárgya (romaji) kiemelt, nagyobb/félkövér szövegként.
- [x] (B): „Melyik a japán neve: **`{magyar}`**?” + 4 romaji gomb – ugyanaz a
      `QuizTextQuestion`, `question.type` dönti el, melyik mező a kérdés tárgya és melyik
      az opció felirata.
- [x] Egy oszlop, nagy érintési célpontok, hosszú szöveg tördelése – `QuizOptionRow`
      `fillMaxWidth()` + `heightIn(min = 48.dp)`, a `Text` nem korlátoz sortörést/sorszámot.
- [x] Felső sáv: „`{index}` / `{összes}`” + lineáris előrehaladásjelző – `QuizRunTopBar`
      (a bezárás (×) gomb és a `BackHandler` is itt készült el, lásd Megjegyzés: a
      `QuizRunViewModel` KDoc-ja ezt kifejezetten a kérdés-UI-t adó feladatra, erre a
      csoportra hagyta).

## Kész, ha

- [x] Hosszú magyar neveknél sem törik a layout, és nem vágódik le a szöveg – a
      `QuizOptionRow` `Text`-je alapértelmezett (korlátlan) sortöréssel/sorszámmal
      dolgozik, a `Surface` magassága csak alulról korlátozott (`heightIn(min = …)`), nem
      rögzített; `@Preview` egy ~55 karakteres, szintetikus feliratra ("Hosszú magyar
      név – utolsó kérdés" preview-k).

## Figyelj rá

- A tartalomban a leghosszabb magyar név ~45 karakter – a gomboknak ezt kezelniük kell.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
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
| Commit | `d683daf` |

**Futtatott parancsok** (Bash, `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **535 teszt, 0 hiba** (528 → 535) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Mi készült:** ez a feladat a G-08 csoport (T-407+T-409) első tagja; a kettő egy
menetben készült el, mert ugyanazt a képernyőt (`QuizRunScreen.kt`) adja. A G-07 csoport
(T-405/T-406/T-410) szándékosan csak az állapotgépet adta, a kérdés-opciók
megjelenítendő nevét/ábráját **nem** oldotta fel – ezt a `QuizRunViewModel` KDoc-ja
kifejezetten erre a csoportra hagyta. A tényleges feloldás itt, a ViewModelben készült
el (nem a Compose rétegben, hogy az L5 rétegszabály ne sérüljön): a `QuizRunViewModel`
új, harmadik függősége a `TechniqueRepository`, az `uiState` pipeline egy harmadik
forrással (`techniqueRepository.observeTechniques()`) bővült, és a
`QuizRunUiState.Content` egy új `options: List<QuizOptionDisplay>` mezőt kapott – a
`question.optionTechniqueIds` ugyanabban a sorrendben (a seedelt keverés sorrendje,
ADR-007, nem rendezve újra) feloldott romaji/magyar/ábra hármasa. Ha egy opció
technikája nem található (védelmi háló, NFR-06), az eredmény `QuizRunUiState.Empty`.
A tényleges képernyő (`QuizRunScreen.kt`): `QuizRunRoute` + stateless `QuizRunScreen`,
`QuizRunTopBar` (progresszszöveg + bezárás (×) `IconButton` + lineáris
`ProgressIndicator`), `QuizTextQuestion` ((A)/(B) kiemelt kérdéstárgy + 4 opció),
`QuizOptionRow` (a válasz-visszajelzés, lásd T-409), `QuizExplanationCard` (lásd T-409),
és egy `BackHandler` + `AlertDialog` a megszakítás megerősítésére – ez utóbbi kettő a
FUNCTIONAL_SPEC 5.2/1. és 5.2/5. pontja szerint a kérdés-UI része, és a `QuizRunViewModel`
KDoc-ja is kifejezetten ide (a kérdés-UI-t adó feladatra) utalta. Új, a döntést
emulátor nélkül tesztelhetővé tévő tiszta logika: `QuizOptionFeedback` (enum) +
`QuizOptionFeedbackResolver` (`run` csomag) – melyik opció `SELECTED_CORRECT`/
`SELECTED_INCORRECT`/`CORRECT_UNSELECTED`/`NEUTRAL`/`UNANSWERED`, a válasz és a helyes
technika azonosítója alapján.
A `feature/quiz/build.gradle.kts` két új, már a version catalogban szereplő
függőséget kapott: `libs.compose.material.icons.extended` (pipa/kereszt/nyíl ikon) és
`libs.androidx.activity.compose` (`BackHandler` – ez az első előfordulása a modulban,
amit a T-410 már előre jelzett).
7 új teszt: 2 a `QuizRunViewModelTest`-ben (az opciók feloldása a `TechniqueRepository`-
ból, és a védelmi háló egy hiányzó opció-technikára), 5 a `QuizOptionFeedbackResolverTest`-
ben (sima JUnit, Robolectric nélkül).

**Megjegyzés:** lásd a T-409 lezárását – a kettő egy közös „Mi készült"/teszt
leírás alatt él, mert egy fájlban, egy menetben készültek. A (C)/(D) képes
kérdéstípusok UI-ja (T-408) és a navigációs bekötés (`quiz/setup` → `quiz/run`,
`KyokushinNavHost`) szándékosan nincs ebben a feladatban – lásd
`docs/CONVENTIONS.md` 5. szakasz.
