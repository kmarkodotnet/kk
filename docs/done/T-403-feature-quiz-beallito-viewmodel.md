# T-403 · feature/quiz: beállító ViewModel

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-12 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-111](T-111-core-quiz-dryrun-keszenleti-osszegzes.md), [T-402](T-402-core-data-kvizjeloltek-lekerdezese.md) |
| Követelmény | FR-11, FR-12, FR-13, FR-22 |
| Teszt | TR-03 |

## Cél

A kvízkonfiguráció és az élő készenléti összegzés.

## Teendők

- [x] Kategóriák, kérdésszám (5/10/20), kérdéstípusok állapota.
- [x] Minden változtatásra `dryRun()` futtatása és az összegzés frissítése.
- [x] Típusonkénti letiltás + indoklás a `SkipReason` alapján.
- [x] Indítás: seed sorsolása az injektált RNG-ből, `QuizSession` létrehozása, `QuizPlan` generálása.

## Kész, ha

- [x] Ha a `dryRun` 0 kérdést ad, az indító gomb letiltva és 3 konkrét javaslat jelenik meg.
- [x] A kiírt kérdésszám megegyezik azzal, amit a kvíz ténylegesen ad.

## Figyelj rá

- Itt tilos a `dryRun`-t „optimistán” kerekíteni: a beállító képernyő ígérete kötelező érvényű.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára *(N/A – ViewModel-only task, ugyanaz a kettéválasztás, mint T-302/T-303 és T-207/T-208 között; a UI a T-404 tárgya)*
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
| Commit | *(a következő commit rögzíti)* |

**Futtatott parancsok** (Bash, `export JAVA_HOME=”/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot”`):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **16 teszt, 0 hiba** (`QuizSetupViewModelTest`) |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **505 teszt, 0 hiba, 0 kihagyott** (489 → 505) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` |

**Mi készült:** `QuizSetupViewModel` (`feature/quiz/.../setup/`) – a `TechniqueRepository`-ból
egy közös, minden kategóriára szóló `QuizCandidateSet`-et tart életben
(`candidateSetFlow`), a kategóriaszűrést a `QuizRequest.categoryFilter`-en keresztül
memóriában végzi, és **minden** kijelölésváltásra (kategória, kérdésszám, típus) valódi
`DefaultQuizGenerator.dryRun()`-t futtat – a `QuizSetupUiState.Content.expectedQuestionCount`/
`shortfall`/`canStart` közvetlenül ennek a kimenete, nincs második, „optimista” számítás.
A típusonkénti letiltás oka (FR-13) egy **próba**-kéréssel készül, amelyben minden típus
engedélyezett, hogy a letiltás tartalmi okát (`QuizCandidateFilter.skipReasons`) a
felhasználó saját kapcsolóállása ne tudja eltorzítani. `onStartQuiz()` az injektált
`Random`-ból (már a `di/QuizModule.kt` biztosítja, T-108 óta) sorsol egy seedet,
megnyitja a munkamenetet (`ProgressRepository.startSession`), legenerálja a tervet
(`QuizGenerator.generate`) ugyanazzal a seeddel, és egy `Channel`/`Flow` egyszeri
eseményen (`startEvent`) adja ki a `QuizStartResult`-ot (ARCHITECTURE 6.1 mintája) – a
tényleges navigáció és a `QuizRunState` a T-404/T-405 tárgya.

**Korábbi munka átvétele (megszakított subagent):** a `feature/quiz/build.gradle.kts`,
a `QuizSetupUiState.kt`, a `QuizSetupViewModel.kt`, a `QuizStartResult.kt`, a
`res/values/strings.xml` és a `QuizTestFixtures.kt` már teljes, a core/quiz és core/data
tényleges szerződéseivel (ellenőrizve: `QuizGenerator`/`QuizReadiness`/`QuizRequest`/
`QuizCandidateSet`/`QuizCandidateFilter`/`QuizEligibility`, `TechniqueRepository`/
`ProgressRepository`/`ContentImporter`) **szó szerint egyező** állapotban készült el –
ebből semmit nem kellett újraírni. A félbemaradt `QuizSetupViewModelTest.kt`-ben két
tényleges hibát találtam és javítottam:

1. **Egy kitalált szöveg a FUNCTIONAL_SPEC 8. fejezete ellenében.** A letiltott „Kvíz
   indítása” gomb alá a korábbi kör **három külön** `strings.xml` bejegyzést
   (`quiz_setup_suggestion_more_categories`/`_more_types`/`_fewer_questions`) és egy
   `startDisabledSuggestions: List<Int>` mezőt tett be – de a FUNCTIONAL_SPEC 8. a
   `quiz.notEnoughData` kulcs alatt **egyetlen, szó szerinti** mondatot ad, amely mindhárom
   javaslatot egy mondatban hordozza. A CONVENTIONS 4. szabálya („A pontos szövegeket a
   FUNCTIONAL_SPEC.md 8. fejezete adja – ne találj ki újakat”) szerint ez utóbbi a kötelező
   forrás. Átírtam: `QuizSetupUiState.Content.notEnoughDataMessageRes: Int?` (egyetlen,
   nullázható `@StringRes`), a strings.xml egyetlen `quiz_setup_message_not_enough_data`
   bejegyzést kapott a szó szerinti FUNCTIONAL_SPEC-szöveggel, a három kitalált bejegyzés
   törölve. (A képes típusok letiltási indoklása – `quiz.type.disabled.images` – szó
   szerint megegyezik a FUNCTIONAL_SPEC-ben adottal, az nem változott. A szöveges típusok
   analóg indoklása, `quiz_setup_type_disabled_candidates`, a spec-ben nincs megadva –
   ez dokumentált, szándékos kiegészítés, ugyanúgy, mint a korábbi taskoknál a
   dokumentált alapérték-feltételezések.)
2. **Egy bukó teszt, hibás elvárással.** A `quizSetup_toggleCategory_narrowsLiveRequest_FR11`
   azt várta, hogy az „állások” kategóriára szűkítve (3 technika) a `dryRun` 3 kérdést ad –
   de egy négyopciós kérdéshez (1 helyes + 3 distraktor) **legalább 4** egymástól különböző,
   kvízképes technika kell egy típuson belül (`QuizQuestion.OPTION_COUNT`,
   `DistractorSelector`), és a kategóriaszűrő a disztraktorok forrását is leszűkíti (nem
   csak a helyes válaszét) – 3 technikával tehát **0** a helyes, nem-optimista eredmény
   (ugyanaz a motorviselkedés, amit a T-403 „Figyelj rá” pontja előír). A tesztet a kódot
   megtartva javítottam: egy negyedik, kvízképes „állások” technikát vettem fel a
   fixtúrába, hogy a szűkítés tényleges (nullától eltérő) hatása bizonyítható legyen
   (ugyanazzal a szabállyal: „Ha teszt bukik, alapból a kódot javítsd” – itt a teszt
   elvárása volt hibás, lásd CONVENTIONS 3.). Lásd `docs/CONVENTIONS.md` 1. és 4.1 szakasz
   új sorai.

A `QuizSetupViewModel`/`QuizSetupUiState`/`QuizStartResult` a T-403 „Kész ha” mindkét
pontját méri: `quizSetup_allTypesDisabled_cannotStartWithNotEnoughDataMessage_FR22`
(0 kérdés → letiltott gomb + a FUNCTIONAL_SPEC szó szerinti üzenete) és
`quizSetup_onStartQuiz_drawsSeedFromInjectedRandom_startsSessionAndEmitsPlan_FR19`
(`result.plan.questionCount == expectedQuestionCount`, azaz a `dryRun` és a `generate`
ténylegesen egyezik ugyanazon a kéréspáron).
