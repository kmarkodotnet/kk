# Implementációs terv és állapot

| | |
|---|---|
| Verzió | 1.0 |
| Utolsó frissítés | 2026-11-12 |
| Alapja | `docs/REQUIREMENTS.md`, `docs/FUNCTIONAL_SPEC.md`, `docs/ARCHITECTURE.md` |

Ez a dokumentum az **egyetlen** hely, ahol a projekt tényleges állása követhető.
A `/kyo-next` parancs innen veszi a következő lépést, és ide írja vissza az eredményt.

A **munkadarabok részletei** (user story-k elfogadási kritériumokkal, dev taskok
függőségekkel, méretbecsléssel és Definition of Done-nal) a [`BACKLOG.md`](BACKLOG.md)-ben
vannak. Itt a *sorrend és az állapot*, ott a *tartalom*.

**Fázis ↔ task megfeleltetés:**

| Fázis | Taskok | Mérföldkő |
|---|---|---|
| 2 – domain modell és kvízmotor | T-101…T-114 | **M1** – a mag igazoltan helyes ✅ **elérve** (T-114, 2026-11-11) |
| 3 – tudástár, import, Room | T-115…T-118, T-201…T-203, T-301, T-401, T-402, T-501, T-502, T-601 | **M2** – a tartalom bent van |
| 4 – Tudástár UI | T-204…T-211 | **M3** – böngészhető app ✅ **elérve** (T-209, 2026-11-12) |
| 5 – Tanulás | T-302…T-308 | **M4** – tanulható app ✅ **elérve** (T-306/T-308, 2026-11-12) |
| 6 – Kvíz UI | T-403…T-415 | **M5** – kvízelhető app |
| 7 – Haladás és beállítások | T-503…T-509 | **M6** – mérhető app |
| 8 – minőség és átadás | T-602…T-610 | **M7** – átadható app |

---

## Fázis 0 – Környezet és tervek

| # | Lépés | Állapot |
|---|---|---|
| 0.1 | Repository és build környezet felmérése | ✅ kész – `docs/ENVIRONMENT.md` (JDK, Android SDK, Gradle **nincs**) |
| 0.2 | Git repo inicializálása, `.gitignore`, `.gitattributes` | ✅ kész |
| 0.3 | Claude környezet: `CLAUDE.md`, `.claude/settings.json`, 7 slash command, 5 skill, 1 auditor agent | ✅ kész |
| 0.4 | Követelményspecifikáció | ✅ kész – `docs/REQUIREMENTS.md` v1.0 |
| 0.5 | Funkcionális specifikáció | ✅ kész – `docs/FUNCTIONAL_SPEC.md` v1.0 |
| 0.6 | Architektúraterv | ✅ kész – `docs/ARCHITECTURE.md` v1.0 |
| 0.7 | **Felhasználói jóváhagyás a tervekre és a nyitott döntésekre** | ✅ kész – lásd „Meghozott döntések" |
| 0.8 | JDK 17 + Android SDK telepítése, verziómátrix rögzítése | ✅ kész – JDK 17.0.21, Android Studio, cmdline-tools, platform 36+37, build-tools 36.1/37.0, Gradle 9.9.1; ADR-008 |
| 0.9 | ADR-ek kiírása `docs/adr/` alá | ✅ kész – ADR-001…ADR-011 |

### Meghozott döntések (2026-11-11)

| Kérdés | Döntés |
|---|---|
| OQ-03 / ADR-009 – `minSdk` | **26 (Android 8.0)** – natív `java.time`, nincs desugaring |
| ADR-001 – modulbontás | **teljes bontás**: app + 4 core + 4 feature + navigation + di |
| ADR-002…ADR-007 | a javaslat szerint elfogadva (lásd `docs/adr/`) |
| OQ-01 – éles tartalom | **FELÜLÍRVA (2026-11-11).** Megérkezett a tényleges tudástár: `knowledgebase/` – **161 rekord, 9 kategória**, `dataset_version 0.1.0`, minden rekord `editorial_review_required`, illusztráció egyikhez sincs. A demó adatkészlet terve elesett. |
| Kvíz lektorálatlan tartalmon | **engedélyezve, végig jelölve** – [ADR-010](adr/ADR-010-quiz-on-unreviewed-content.md). `INCOMPLETE` rekord továbbra is kizárva. |
| Kanonikus séma | **a kapott séma**, kiegészítve – [ADR-011](adr/ADR-011-canonical-content-schema.md). Nincs konverziós réteg. |
| Aktív kvíztípusok | csak **(A)** romaji→magyar és **(B)** magyar→romaji. A **(C)** és **(D)** képes típus nem aktiválható, mert egyetlen illusztráció sincs (CR-05, FR-13). |

---

## Fázis 1 – Gradle projektváz

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 1.1 | `settings.gradle.kts`, root `build.gradle.kts`, Gradle wrapper (9.9.1) | NFR-02 | ✅ kész |
| 1.2 | `gradle/libs.versions.toml` ellenőrzött verziókkal | NFR-02 | ✅ kész – ADR-008 |
| 1.3 | 11 modul létrehozása (`core/model` és `core/quiz` Kotlin JVM modul) | NFR-03 | ✅ kész |
| 1.4 | `AndroidManifest.xml` – **nincs** INTERNET permission, egy Activity | NFR-01, TR-08 | ✅ kész – összevont manifesten ellenőrizve |
| 1.5 | Material 3 téma világos/sötét, `strings.xml` váz, vektoros ikon | NFR-05 | ✅ kész |
| 1.6 | Hilt bekötés (`@HiltAndroidApp`, `@AndroidEntryPoint`) | NFR-03 | ✅ kész; a Hilt **modulok tartalma** is kész – T-203, lásd a Fázis 3 3.9 sorát |
| 1.7 | Ellenőrzés: `./gradlew assembleDebug` | — | ✅ **exit 0**, `app-debug.apk` 18,69 MB |

## Fázis 2 – Domain modell és kvízmotor (tiszta Kotlin, Android nélkül)

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 2.1 | `core/model`: value class ID-k, enumok, `Technique`, `Category`, `IllustrationRef`, felhasználói adatmodellek | DR-01…DR-04 | ✅ **kész** – T-101, T-102, T-103 (ID-k, enumok, `Technique`/`Category`/`IllustrationRef`, `QuizAttempt`/`StudyReview`/`QuizSession`); 35 zöld teszt |
| 2.2 | `core/quiz` validátor: `KnowledgeValidator`, `ValidationReport`, V001…V010 | DR-05, FR-31 | 🟡 **részben** – T-104 (szerződés) és T-105 (implementáció) kész: `DefaultKnowledgeValidator` a V001…V009 szabályokkal, `NameNormalizer` (egy helyen definiált normalizálás, a T-202 keresés is ezt kapja), `QuizEligibility` (ADR-010 formula). A V010 a `core/data` importrétegében készült el (T-115, `KnowledgeContentReader`), így a V001…V010 kódkészlet teljes |
| 2.3 | **Validátor tesztek** (táblázatos, minden hibakódra) | **TR-01** | ✅ **kész** – T-106: `KnowledgeValidationRuleTableTest` 31 nevesített, parametrizált eset a V001…V010 kódokra + `KnowledgeValidationRuleCoverageTest` (gépi lefedettségi őr), `KnowledgeValidatorGuaranteesTest` (nem dob kivételt / nem emel státuszt), `RealKnowledgeBaseValidationTest` (a valós 161 rekord, 0 BLOKKOLÓ hiba). **105 zöld teszt**, `testDebugUnitTest` és `assembleDebug` exit 0 |
| 2.4 | `core/quiz` generátor: `QuizRequest`/`QuizPlan`, jelöltszűrés, distraktorválasztás, seedelt keverés, `dryRun` | FR-14, FR-22, CR-07, NFR-07 | ✅ **kész** – T-107 (szerződés), T-108 (jelöltszűrés), T-109 (distraktorválasztás), T-110 (seedelt determinizmus) és T-111 (`dryRun` készenléti összegzés): a `QuizCandidateFilter`, a `DistractorSelector` és a `DefaultQuizGenerator` – Fisher–Yates keverés kizárólag a seedelt RNG-ből, kérdés- és opciósorrend a seedből, ismétlésmentes helyes válaszok, tényleges `Shortfall`, és kérdésépítés nélküli `dryRun`, amely a `generate`-tel közös slotlistából számol |
| 2.5 | **Generátor tesztek** (invariánsok, seed-determinizmus, fallback, típusjogosultság) | **TR-02, TR-03** | ✅ **kész** – T-112: a nyolc kvízmotor-invariáns egy 21 kérés × 7 seed mátrixon és a valódi 161 rekordos tartalmon; kategóriaszűrés, feliratos/placeholder/hiányzó kép, shortfall-okprioritás, `dryRun` ↔ `generate` egyezés a közös `questionSlots` ponton. **236 zöld teszt** |
| 2.6 | `QuizScoring` nullable százalékkal | FR-27 | ✅ **kész** – T-113: `core.quiz.scoring.QuizScoring.score(attempts): Score` tiszta függvényként, `Score(answered, correct, ratio: Double?)`; 0 megválaszolt kérdés → `ratio == null` (nem `0.0`), a `Score` init blokkja ezt az invariánst típusszinten is kikényszeríti. Egy kérdés egyszer számít: a `(sessionId, questionId)` páron dedup, a legkorábbi `answeredAt` nyer (FR-18). A helyességet nem számolja újra, a `QuizAttempt.correct`-et összegzi (DR-09) |
| 2.7 | **Scoring tesztek** (0%, 100%, félbehagyott, dupla, nulla válasz) | **TR-04** | ✅ **kész** – T-113 alaptesztek (`QuizScoringTest`, 9 eset) + T-114 teljes TR-04 mátrix (`QuizScoringTr04Test`, 17 eset): nulla válasz → „nincs adat" (szemben a 0%-kal), 100%, 0%, félbehagyott 10-ből 4 (a meg nem válaszolt **nem** hibás – a félbehagyott és a végig elhibázott menet aránya nem eshet egybe), arány és nem százalék, dupla válasz a lista szintjén is ellenőrizve (`answered == a (sessionId, questionId) párok száma`), késői „javítás" nem írja felül az első választ, sorrendtől független dedup, azonos időbélyegnél determinisztikus döntés, vegyes kérdéstípusok egy menetben és a típusonkénti bontás összegzése. **262 zöld teszt** |
| 2.8 | Ellenőrzés: `./gradlew testDebugUnitTest` | NFR-12 | ✅ **kész** – T-114: `testDebugUnitTest` exit 0 (262 teszt, 0 hiba), `assembleDebug` exit 0 |

> Ez a fázis a termék helyességének magja, és **Android SDK nélkül is fordítható/tesztelhető**
> lenne, amint a JDK rendelkezésre áll.

## Fázis 3 – Tudástár: séma, demó adat, import, Room

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 3.1 | A kanonikus séma implementálása (ADR-011): manifest + `entries`, `schema_version` string | FR-29 | ✅ **kész** – T-115: `core/data` `internal` DTO-k (`ManifestDto`, `CategoryDto`, `IllustrationDto`, `EntriesFileDto`, `EntryDto`) `@SerialName` snake_case leképezéssel és `ignoreUnknownKeys = true`, `KnowledgeAssetSource` + `AndroidKnowledgeAssetSource` (assets-olvasás, képfájllista), `KnowledgeContentReader` (`Parsed` / `Unreadable` → **V010**), DTO → `KnowledgeInput` mapper. 16 új teszt, a valódi 161 rekordon is |
| 3.2 | A `knowledgebase/` tartalom bemásolása `app/src/main/assets/knowledge/` alá + `editorial_status` alapú `datasetTrust` (ADR-010) | CR-01, CR-03 | ⬜ |
| 3.3 | `docs/KNOWLEDGE_BASE.md` kiadott változata (séma, hozzáadás, hitelesítés, licenc, migráció) | FR-30, CR-04 | ⬜ |
| 3.4 | Room: tartalom- és felhasználói táblák, DAO-k, `@TypeConverter`-ek | DR-04, DR-07 | ✅ **kész** – T-116: `KyokushinDatabase` (`version = 1`, `exportSchema = true`), 5 tartalom- és 3 felhasználói tábla, 8 `internal` DAO (minden művelet `suspend` vagy `Flow`), `KyokushinTypeConverters` (`Instant` ↔ epoch milli, enum ↔ **név**, azonosító value classok az `of()` gyárukon át). A felhasználói táblák `techniqueId`-jén **nincs** idegen kulcs a tartalomra (ADR-004) |
| 3.5 | `UNIQUE(sessionId, questionId)` index | FR-18 | ✅ **kész** – T-116: egyedi index a `quiz_attempt` táblán (ADR-005), `OnConflictStrategy.IGNORE` beszúrással – ütközéskor `-1` sorazonosító, nem kivétel; a sémaexport és egy in-memory adatbázison futó teszt is igazolja |
| 3.6 | `ContentImporter`: DTO → validátor → upsert, idempotens, `retired` jelölés | FR-31, FR-33 | ✅ **kész** – T-117: publikus `ContentImporter` + `ImportResult`, `internal RoomContentImporter` (verziókapu a tárolt `dataset_version`-re, validáció, **upsert egyetlen tranzakcióban**, a csomagból kimaradt technika `retired = true` – fizikai törlés nincs), `content_state` összegzés, `lastValidationReport()` a diagnosztikához, injektált `Clock` és `@IoDispatcher`. Tartalom oldali entitás ↔ domain mapperek. 16 új teszt |
| 3.7 | Repository implementációk (`internal`), mapperek, aggregált statisztika query-k | NFR-03, DR-09 | 🔄 **részben** – T-201: publikus `TechniqueRepository` + `ContentState` domain modell (`NotImported` / `Imported`), `internal RoomTechniqueRepository` (`@Inject`, `@IoDispatcher`). A `retired` szűrés **SQL-ben**, a listán és a kvízjelölteknél (ADR-004); a `getTechnique` a dokumentált kivétel, mert a Haladás képernyőnek a visszavont technikát is fel kell oldania (DR-07). Aliasok egyetlen lekérdezésből (nincs N+1), szinonimacsoportok a `synonym_group`-ból a szűrés utáni készletre vágva (CR-07). 12 új teszt, köztük egy reflexiós, ami a publikus API-t Room típusokra vizsgálja (TR-07). T-202: a keresés **SQL-be** költözött – előre normalizált `technique.search_romaji` / `search_hungarian` oszlopok, `LIKE '%…%' ESCAPE` a három normalizált oszlopon (alias-ág `EXISTS` altkérdéssel), az egyező alias ugyanabból a lekérdezésből a `TechniqueMatch`-ben; séma `version` 1 → 2 explicit `KyokushinMigrations.MIGRATION_1_2`-vel, `2.json` exporttal és `MigrationTestHelper` teszttel (ADR-012, DR-08). **G-01 csoport (T-301, T-401, T-402, T-502):** publikus `ProgressRepository` + `internal RoomProgressRepository` (`@Inject`, injektált `Clock`, `@IoDispatcher`) – `startSession`/`recordAttempt`/`finishSession` (T-401, a `Boolean` visszatérés és a `UNIQUE(sessionId, questionId)` ütközés `-1`-je adja az egyszeri-válasz szerződést, ADR-005; `recordAttempt` tranzakcióban írja a próbálkozást és növeli a menet `answeredCount`-ját), `recordStudyReview`/`observeStudyReviewSummary` (T-301, FR-28 – külön a kvízstatisztikától, a `StudyReviewDao.observeCountByRating` darabszámlálásával), `clearAllProgress` (T-502, egy tranzakcióban üríti a `quiz_session`-t – a `CASCADE` viszi a `quiz_attempt`-et is – és a `study_review`-t, a tartalomtáblákhoz nem nyúl). `getQuizCandidates` (T-402) immár tölti a `QuizCandidateSet.illustrationDigests` térképet a csomagolt illusztrációfájl **nevéből** (nincs SHA-256 oszlop a sémában, és becsült ujjlenyomatot nem gyártunk – a fájlnév-egyezés korrekt és elegendő, lásd `RoomTechniqueRepository` KDoc); a `quiz_excluded`/`INCOMPLETE` szűrés szándékosan **nem** a repositoryban van, azt a `QuizEligibility` kapu végzi (ADR-010), ahogy a `QuizCandidateSet` szerződése is kimondja. A kötések a `RepositoryModule`-ban és a `KyokushinGraph`/`HiltGraphTest`-ben is bekerültek. 10 új teszt (451 teszt, 0 hiba); `assembleDebug` exit 0. Hátra: aggregált statisztika (`T-501`) |
| 3.8 | **Tartalomfrissítés teszt**: előrehaladás megőrzése, átnevezés, visszavonás | **TR-05** | ✅ **kész** – T-118: `ContentUpdateProgressTest` 10 `TR05` jelölésű teszt in-memory Roomon (v1 → előrehaladás → v2 → v3): a `quiz_attempt`/`study_review` sorok és a belőlük levezetett statisztika **bitre azonos** a frissítés előttivel; átnevezésnél az `id` marad és az előrehaladás az új névre oldódik fel (DR-06); a kimaradt és a tartalom által visszavont technika próbálkozásai feloldhatók maradnak (ADR-004); ugyanaz a verzió kétszer nem duplikál (FR-30); az elutasított frissítés sem a tartalomhoz, sem az előrehaladáshoz nem nyúl |
| 3.9 | **Hilt modulok** (`DatabaseModule`, `ContentModule`, `RepositoryModule`, `TimeModule`, `DispatcherModule`, `QuizModule`) | NFR-03, NFR-07 | ✅ **kész** – T-203: a Room/olvasó/repository kötések a `core/data`-ban (a típusok `internal`-ok), a `Clock`, a két dispatcher, a `QuizGenerator`, a `KnowledgeValidator` és az RNG a `di`-ben. Az adatbázis `addMigrations(*KyokushinMigrations.ALL)`-lal épül, destruktív tartalék nélkül (DR-08). `@DefaultDispatcher` qualifier hozzáadva. A gráfot a `KyokushinGraph` `@EntryPoint` fordítási időben, a `HiltGraphTest` (Robolectric) futásidőben ellenőrzi; 7 új teszt |

## Fázis 4 – Tudástár UI

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 4.1 | `core/ui` megosztott komponensek: `TechniqueIllustration`, `VerificationBadge`, `EmptyState`, `DemoBanner` | CR-03, NFR-04 | ✅ **kész** – T-204: `TechniqueIllustration` (mindhárom `IllustrationRef` eset, 6+4 teszt); T-205: `VerificationBadge`, `UnreviewedBanner` (a „DemoBanner" tényleges, ADR-010 szerinti alakja), `EmptyState(EmptyStateReason)`, `ErrorState` (+18 teszt) |
| 4.2 | Navigation Compose + bottom nav (4 cél), a kvíz futás közbeni elrejtése | NFR-03, TR-06 | ✅ **kész** – T-206: a FUNCTIONAL_SPEC 1. tíz útvonala, négy fül magyar felirattal és ikonnal, a bottom nav a `quiz/run` és `quiz/result` képernyőn elrejtve (+15 teszt). A képernyők még megjelölt helyőrzők (T-207-től cserélődnek) |
| 4.3 | Tudástár lista: keresés (normalizált, aliasokra is), kategóriachipek, `SavedStateHandle` | FR-01…FR-03 | ✅ **kész** – T-207: `LibraryListUiState` (`Loading`/`Empty(reason)`/`Content`/`Error`) és `LibraryListViewModel` (200 ms debounce, kategóriahalmaz, `SavedStateHandle`, kategóriánkénti találatszám, az importot a ViewModel indítja); T-211: 17 ViewModel teszt; T-208: `LibraryListScreen` Compose UI (keresősáv, kategóriachip-sor „Összes” chippel, `LazyColumn` lista, listatétel egy TalkBack bejárási egységként), bekötve a `navigation` modul `library/list` útvonalára (+9 teszt, 434 összesen) |
| 4.4 | Tudástár részletező | FR-04, FR-05 | ✅ **kész** – T-209: `LibraryDetailUiState` (`Loading`/`Content`/`Error`, közös `datasetTrust`) és `LibraryDetailViewModel` (egyszeri `getTechnique` lekérdezés, `Technique.status`-ból levezetett hiányzómező-lista); `LibraryDetailScreen` Compose UI – nagy illusztráció (a `null` itt helyőrzővé lesz, szemben a listával), romaji, magyar név, japán íráskép és aliasok csak ha vannak, kattintható kategóriachip vissza a listára szűrve (a `navigation` modul a Navigation Compose „eredmény visszaadása" mintájával: `previousBackStackEntry.savedStateHandle` + `popBackStack()`), hitelesítési blokk a `VerificationBadge`-ből + önálló kvízkizárás-szöveg `INCOMPLETE` esetén. A `library/detail` helyőrzője lecserélve. A „már gyakoroltam" összegzés (FUNCTIONAL_SPEC 3.2 11.) és a `verified` forrásmező (3.2 10.) **dokumentáltan elmarad**: nincs `ProgressRepository` (T-301/T-401 hátra) és a kanonikus séma technikaszinten nem ad `source` mezőt (`docs/CONVENTIONS.md` 8.). 7 új teszt (441 teszt, 0 hiba); `assembleDebug` exit 0 |
| 4.5 | Üres/hiba állapotok a funkcionális spec 2.4 és 10. pontja szerint | NFR-06 | ✅ **kész** – T-210: mind a négy állapot (`Loading`/`Empty` – nincs tartalom + debug „Diagnosztika” gomb/`Empty` – nincs találat + „Szűrők törlése”/`Error` + „Újra”) bekötve a `LibraryListScreen`-be, `@Preview`-vel mind a négy ágra (G-03 csoport T-208-cal) |

## Fázis 5 – Tanulás

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 5.1 | `learn/setup`: kategória- és elrejtésválasztás | FR-06, FR-07 | ✅ **kész** (T-302, 2026-11-12) |
| 5.2 | `learn/cards`: felfedés, előző/következő, seedelt sorrend, pozíció mentése | FR-06…FR-09 | ✅ **kész** (G-05 csoport: T-304, T-305, T-307, 2026-11-12) – `LearnCardsScreen` Compose UI (felső sáv, kártya, felfedés, Előző/Következő), a T-305 feliratos-kép-elrejtés és a T-307 záróképernyő. Az önértékelés (FR-10) **nem** ebben a körben: az T-306 tárgya |
| 5.3 | Önértékelés → `StudyReview`, elkülönítve a kvízstatisztikától | FR-10, FR-28 | ✅ **kész** (G-06 csoport: T-306, T-308, 2026-11-12) – a `LearnCardsViewModel` `onKnew()`/`onNeedsPractice()`-je a `ProgressRepository.recordStudyReview`-n keresztül ment, majd automatikusan továbblép; a `LearnCardsScreen` két új gombja csak felfedés után aktív, letiltva mindig-látható indoklással (TalkBack). **Fázis 5 lezárva, M4 elérve.** |

## Fázis 6 – Kvíz UI

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 6.1 | `quiz/setup`: kategóriák, kérdésszám, típusok, **élő készenléti összegzés** (`dryRun`) | FR-11…FR-13, FR-22 | ✅ **kész** (T-403, 2026-11-12) – `QuizSetupViewModel`; a UI a T-404 tárgya |
| 6.2 | `quiz/run`: négy kérdéstípus megjelenítése, visszajelzés ikon+szöveg+szín | FR-16, FR-17 | ⬜ |
| 6.3 | `QuizRunState` a `SavedStateHandle`-ben; egyszeri válasz; megszakítás | FR-18, FR-19 | ⬜ |
| 6.4 | `quiz/result`: összegzés, hibalista, „Hibák újragyakorlása" új munkamenetként | FR-20, FR-21 | ⬜ |
| 6.5 | Akadálymentesség a kvízben (szemantika, a (D) opciók nem fedik fel a választ) | NFR-04 | ⬜ |
| 6.6 | **ViewModel tesztek** (dupla válasz, forgatás, megszakítás) | TR-04 | ⬜ |

## Fázis 7 – Haladás és beállítások

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 7.1 | `progress/overview`: összesítés, kategóriabontás, gyakorlást igénylő lista | FR-23, FR-24, FR-26 | ⬜ |
| 7.2 | `progress/technique/{id}`: technikánkénti statisztika, visszavont jelölés | FR-25 | ⬜ |
| 7.3 | Önértékelési blokk elkülönítve + magyarázó szöveg | FR-28 | ⬜ |
| 7.4 | `settings`: tartalominfó, előrehaladás törlése, debug diagnosztika | FR-32, FR-35 | ⬜ |

## Fázis 8 – Minőség, tesztek, dokumentáció, átadás

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 8.1 | Rétegszabály-ellenőrzés (import audit, `/kyo-doc-sync`, `kyo-spec-auditor`) | TR-07 | ⬜ |
| 8.2 | Manifest permission teszt | TR-08 | ⬜ |
| 8.3 | Compose UI tesztek (emulátor esetén): navigáció, tanulókártya, teljes kvíz | TR-06 | ⬜ |
| 8.4 | Lint + formázás | NFR-11 | ⬜ |
| 8.5 | `docs/QUIZ_RULES.md` kiadott változata | — | ⬜ |
| 8.6 | `README.md`: előfeltételek, futtatás, build, használat, ismert korlátok | NFR-11 | ⬜ |
| 8.7 | `docs/TEST_REPORT.md`: a **tényleges** parancsok és kimenetek | NFR-12 | ⬜ |
| 8.8 | `./gradlew assembleDebug` → debug APK, útvonal jelentése | AC-07 | ⬜ |
| 8.9 | Elfogadási kritériumok (AC-01…AC-08) végigellenőrzése | — | ⬜ |

---

## Aktuális blokkolók

| # | Blokkoló | Mit blokkol | Mi oldja fel |
|---|---|---|---|
| ~~B-01~~ | ~~Nincs JDK~~ | — | ✅ **feloldva**: Temurin JDK 17.0.21 telepítve |
| B-02 | Android SDK platform/build-tools még nincs | Fázis 1 Android fordítás | 🔄 cmdline-tools letöltése fut, utána `sdkmanager` |
| B-03 | Nincs emulátor (AVD) | 8.3 (TR-06 UI tesztek) | AVD létrehozása az SDK után, vagy fizikai eszköz |
| B-04 | Verziómátrix nem ellenőrizhető | 1.2 (`libs.versions.toml`), ADR-008 | B-02 után `sdkmanager --list` |
| OQ-01 | Nincs hitelesített karate tartalom | az éles adatkészlet; a (C)/(D) kvíztípus aktiválása | ✅ döntés: demó adatkészlettel indulunk; az éles tartalom később importtal (FR-34) |

A maradék blokkolók **nem** állítják le a munkát: a Fázis 2.1–2.7 és 3.1–3.3 kódja és
tartalma megírható; csak az Android-fordítás és az UI tesztek várnak az SDK-ra.

## Következő lépés

1. **T-404** – `feature/quiz`: beállító UI (FR-11…FR-13, FR-22, a `QuizSetupViewModel`
   megjelenítése). Függősége (T-403) kész.

(**A G-01 csoport – T-301, T-401, T-402, T-502 – kész, ezzel a `ProgressRepository`
írási/törlési felülete is megvan:** `startSession`/`recordAttempt`/`finishSession`
(T-401, `Boolean` visszatérés az egyszeri válaszhoz, ADR-005), `recordStudyReview`/
`observeStudyReviewSummary` (T-301, FR-28), `clearAllProgress` (T-502, FR-35), és a
`getQuizCandidates` immár az `illustrationDigests` térképet is tölti (T-402, a csomagolt
fájl nevéből, lásd `docs/CONVENTIONS.md`). **Nyitott, immár T-506/T-501-re váró
hiányosság:** a `library/detail` „már gyakoroltam" összegzése (FUNCTIONAL_SPEC 3.2 11.)
még nincs bekötve – ehhez a T-501 aggregált, technikánkénti lekérdezése kell, amit a
`ProgressRepository` interfész még nem tartalmaz (`observeTechniqueProgress`); a
`LibraryDetailUiState`/`LibraryDetailRoute` bekötése a T-501 utáni, önálló feladat.)

(**A T-203 kész:** a Hilt gráf teljes és felépül. Egy ViewModel a `TechniqueRepository`-t,
a `ProgressRepository`-t, a `ContentImporter`-t, a `Clock`-ot, az RNG-t és a két
dispatchert kérheti konstruktorban – a `@DefaultDispatcher` a CPU-munkára, az
`@IoDispatcher` a Room/asset elérésre. A `feature` modulok a `di`-re **nem** függenek és
nem is kell: a kötések a `SingletonComponent`-ben állnak össze. A `ProgressRepository`
kötését a G-01 csoport már hozzáadta a `RepositoryModule`-hoz és a `KyokushinGraph`-hoz,
ugyanúgy, mint a `TechniqueRepository`-t.)

Párhuzamosan, amikor sorra kerül: a `REQUIREMENTS.md` / `FUNCTIONAL_SPEC.md` /
`ARCHITECTURE.md` érintett szakaszainak átvezetése ADR-010 és ADR-011 szerint
(mezőnevek, `quizEligible` kapu, lektorálatlan jelölés, aktív kvíztípusok, és a 3.2
10–11. pontban leírt, de a kanonikus sémában nem létező `source`/övfok mező és a
hiányzó haladás-összegzés dokumentált eltávolítása/pontosítása).

## Változásnapló

- 2026-11-11 – Fázis 0.1–0.6 kész: környezetfelmérés, Claude környezet, követelmény-,
  funkcionális és architektúraspecifikáció. Build/teszt nem futott (B-01, B-02).
- 2026-11-11 – Fázis 0.7 kész: tervek jóváhagyva, `minSdk = 26`, teljes modulbontás,
  demó adatkészlet 4×5 rekorddal. Fázis 0.9: ADR-001…007 és ADR-009 kiírva.
  JDK 17.0.21 és Android Studio telepítve; az SDK beszerzése folyamatban.
- 2026-11-11 – Fázis 0.8, 0.9 és **Fázis 1 kész.** Build környezet felállítva (JDK 17.0.21,
  Android SDK 36+37, Gradle 9.9.1); 11 modulos Gradle projektváz; `assembleDebug` és
  `testDebugUnitTest` egyaránt exit 0 (tesztek még `NO-SOURCE`). ADR-008 a verziómátrixszal.
- 2026-11-11 – Megérkezett a tényleges tudástár (161 rekord). ADR-010 (kvíz lektorálatlan
  tartalmon, végig jelölve) és ADR-011 (a kapott séma a kanonikus formátum) elfogadva;
  ezek felülírják a demó adatkészlet tervét és a séma-mezőneveket.
- 2026-11-11 – Tudástár konvertálva: `knowledgebase_transfer/` (161 rekord, 9 kategória,
  10 illusztráció, 0 blokkoló hiba). Három kép névfelirattal → `quiz_usable: false`.
- 2026-11-11 – **`BACKLOG.md` elkészült:** 6 epik, 21 user story elfogadási kritériumokkal,
  71 dev task függőségekkel, méretbecsléssel, 7 szállítási mérföldkővel (M1…M7).
- 2026-11-11 – **T-101 kész** (Fázis 2.1 első darabja): `core/model` azonosító value
  class-ok (`TechniqueId`, `CategoryId`, `SessionId`, `QuestionId`, privát konstruktor +
  validáló `of()` gyár) és a négy domain enum (`QuestionType`, `SelfRating`,
  `VerificationStatus`, `DatasetTrust`). **Az első valódi zöld tesztfuttatás:**
  `testDebugUnitTest` exit 0, 13 teszt, 0 hiba; `assembleDebug` exit 0.
  A Kotlin JVM magmodulok `testDebugUnitTest` alias taskot kaptak, hogy a globális DoD
  parancsa ne hagyja ki őket.
- 2026-11-11 – **T-102 kész** (Fázis 2.1): `core/model` tartalmi domain modell – `Category`,
  `Technique` (immutable `data class`, `status` háromállapotú `VerificationStatus`, tárolt
  `quizEligible` nélkül – ADR-010) és a `IllustrationRef` sealed hierarchia (`Asset` a
  `quizUsable` / `hideableOnStudyCard` kapuval, `Placeholder`). `testDebugUnitTest` exit 0,
  24 teszt, 0 hiba; `assembleDebug` exit 0. Eltérés: az `Asset` az ADR-003 szövegéhez
  képest két mezővel bővebb (ADR-011 kiegészítés, T-108/T-305 igénye) – az ADR szövegének
  átvezetése hátralévő dokumentációs feladat.
- 2026-11-11 – **T-103 kész** (Fázis 2.1 **lezárva**): `core/model` felhasználói adat
  domain modell – `QuizAttempt` (a `questionId`-vel, ami nélkül az
  `UNIQUE(sessionId, questionId)` index nem építhető meg – ADR-005), `StudyReview` és
  `QuizSession` (nullable `finishedAt` a megszakított kvízhez – FR-19, tárolt `seed` –
  ADR-007). `testDebugUnitTest` exit 0, 35 teszt, 0 hiba; `assembleDebug` exit 0.
  Eltérés: az `ARCHITECTURE.md` 3. szakaszának `QuizAttempt` vázlatából hiányzik a
  `questionId` – az átvezetés hátralévő dokumentációs feladat.
- 2026-11-11 – **T-104 kész** (Fázis 2.2 részben): `core/quiz` validátor-szerződés –
  `KnowledgeValidator` (`fun interface`), `KnowledgeInput` / `ManifestInput` /
  `CategoryInput` / `IllustrationInput` / `TechniqueInput` (nyers, `null`-tűrő bemenet),
  `ValidationReport` / `RejectedRecord`, `ValidationIssue`, `IssueCode` V001…V010,
  `Severity`, `IssueScope`. `.\gradlew.bat testDebugUnitTest` **exit 0** – 40 teszt,
  0 hiba; `.\gradlew.bat assembleDebug` **exit 0**.
  *Drift:* az `ARCHITECTURE.md` 4.1 kódvázlata még `validate(manifest, techniques)`,
  `DatasetKind` és `contentVersion` néven szerepel, és `techniqueId: TechniqueId?`-t ír a
  `ValidationIssue`-ba – a megvalósult szerződés a task-fájlt követi; a szakasz átvezetése
  a dokumentációs körben (T-608 / `/kyo-doc-sync`) esedékes.
- 2026-11-11 – **T-105 kész** (Fázis 2.2 részben; a V010 és a TR-01 táblázat kint marad):
  `DefaultKnowledgeValidator` – V001 (semver **major**, nem támogatott → teljes elutasítás),
  V002/V003/V004 (rekordelvetés minden indokkal együtt), V005 (`Asset` → `Placeholder`
  lefokozás; a *lógó* hivatkozás ráadásul `INCOMPLETE` státuszt ad, a hiányzó fájl nem),
  V006 (szinonimacsoportok a normalizált nevekből), V007/V008 (ADR-010 kapu, csak lefokozás),
  V009 (üres, önütköző és duplikált alias eldobása). Új, újrahasznosítható építőkövek:
  `NameNormalizer` (FR-03/V006 – a T-202 keresés is ezt hívja) és `QuizEligibility`
  (ADR-010 formula – a T-108 jelöltszűrés is ezt hívja).
  `.\gradlew.bat testDebugUnitTest` **exit 0** – 65 teszt, 0 hiba;
  `.\gradlew.bat assembleDebug` **exit 0**.
  *Szerződésmódosítás:* a `schemaVersion` típusa `Int?` → `String?` (ADR-011: a
  `schema_version` semver string, és a V001 a major verzión dől el); a T-104 szerződésteszt
  frissült.
  *Drift:* a `kyo-knowledge-base` skill 2–4. pontja még `Int`-es `schemaVersion`-t és szigorú
  V007-et ír – az ADR-010/ADR-011 ezt felülírta, az átvezetés a Fázis 3.3 (T-608) dolga.
  *Adathiba (javasolt új task):* a konverter `japanese_romaji_ascii` értéke a `KY-0020`
  rekordon `"Kirotsuke"` a `"Ki o tsuke"` helyett; az appot nem érinti (a validátor
  újraszámolja), de a `knowledgebase_transfer/tools/convert.py` kimenetét javítani kell.
- 2026-11-11 – **T-106 kész (Fázis 2.3 kész): a TR-01 szabálytáblázat zöld.** Négy új
  tesztosztály a `core/quiz`-ben: `KnowledgeValidationRuleTableTest` (parametrizált,
  **31 nevesített eset**, mindegyik a lefedett V-kóddal kezdődő névvel – a V010 tagadó
  esettel, mert azt a kódot a `core/data` importréteg állítja elő, nem a validátor),
  `KnowledgeValidationRuleCoverageTest` (gépi őr: új `IssueCode` eset nélkül **bukó**
  tesztet ad), `KnowledgeValidatorGuaranteesTest` (a két szerződési garancia: 8 romlási
  alak egyike sem dob kivételt; `reviewed` manifest és `verified = true` mellett sem emel
  státuszt), `RealKnowledgeBaseValidationTest` (a `knowledgebase_transfer/` **valódi 161
  rekordja állandó tesztként**: `accepted = 161`, `rejected = 0`, **0 BLOKKOLÓ hiba**,
  10 `Asset`, 7 kvízben használható kép, 0 szinonimacsoport). A közös bemenetépítők a
  `ValidationFixtures.kt`-be kerültek, így a táblázat és az egyedi esetek bitre ugyanazt
  az alapállapotot látják.
  `.\gradlew.bat testDebugUnitTest` **exit 0** – **105 teszt, 0 hiba** (a `:core:model:test`
  és a `:core:quiz:test` már nem `NO-SOURCE`); `.\gradlew.bat assembleDebug` **exit 0**
  (`app-debug.apk`, 19 655 673 bájt).
  *Eltérés a tervtől:* a `core/quiz` **teszt** forráskészlete új, csak teszt hatókörű
  függőséget kapott (`libs.kotlinx.serialization.json`, tiszta JVM), mert a valós
  tartalmon futó TR-01 teszt JSON-t olvas. A `main` forráskészlet továbbra sem lát
  szerializációt (ADR-001 L1 sértetlen), a tartalomimport helye változatlanul a T-115.
  A `knowledgebase_transfer/` útvonalát a `kyo.repoRoot` rendszertulajdonság adja át
  (`core/quiz/build.gradle.kts`), nem relatív `../..` hivatkozás.
  *Adathiba (változatlanul nyitott):* a `KY-0020` rekord `japanese_romaji_ascii` értéke a
  forrás CSV-ben és JSON-ban is `"Kirotsuke"` a `"Ki o tsuke"` helyett – forrásadat-hiba,
  nem validátor-hiba; az appot nem érinti (a validátor újraszámolja az ASCII alakot).
- 2026-11-11 – **T-107 kész** (Fázis 2.4 első darabja): a kvízgenerátor **szerződése** a
  `core/quiz` `generator` csomagjában – `QuizGenerator` interfész (`generate` + `dryRun`),
  `QuizRequest` (önhordó bemenet: jelöltkészlet, engedélyezett típusok, kategóriaszűrő,
  kérdésszám, seed, `DatasetTrust`), `QuizCandidateSet` (technikák + V006
  szinonimacsoportok + illusztráció-ujjlenyomatok), `QuizQuestion` (ADR-006: **csak**
  azonosítók, négyopciós invariáns a konstruktorban), `QuizPlan`, `SkipReason`,
  `Shortfall`, `ShortfallReason`, `QuizReadiness`. A terv `@Serializable`, mert a T-405 a
  `SavedStateHandle`-be menti (ADR-007); a value class azonosítók a `core/quiz`-ban élő,
  `of(...)` gyáron átmenő sztring-szerializátorokat kapták, így a `core/model` maradt
  szerializációmentes és a privát konstruktor invariánsa a visszafejtésre is érvényes.
  **135 zöld teszt** (core/model 35 + core/quiz 100), `testDebugUnitTest` és
  `assembleDebug` exit 0. A tényleges generálási algoritmus a T-108…T-110 tárgya, ezért a
  **TR-02 még nem zöld**.
- 2026-11-11 – **T-108 kész** (Fázis 2.4 második darabja): a **jelöltszűrés és a
  jogosultsági kapuk** – `QuizCandidateFilter` a `core/quiz` `generator` csomagjában.
  Kategóriaszűrő (üres szűrő = minden kategória, FR-11), az ADR-010 kapu típusonkénti
  alkalmazása a **meglévő** `QuizEligibility.isEligible` formulával (nincs második
  kvízképességi logika), a kérdés- és az opcióoldal szétválasztása
  (`optionRequirement`: a (C) `IMAGE_TO_JP` opciói nevek, a (D) `JP_TO_IMAGE` opciói
  képek), típusonkénti jelöltszám és a kihagyott típusok `SkipReason`-ja a
  `TYPE_DISABLED` / `NOT_ENOUGH_IMAGES` / `NOT_ENOUGH_CANDIDATES` elhatárolásával
  (FR-13, FR-14, FR-22). A feliratos kép (`quizUsable = false`) és a `Placeholder` sem
  helyesként, sem opcióként nem jut képes kérdésbe (CR-05, ADR-011). A valós tartalmon
  mért állítás tesztben rögzítve: a (C)/(D) típus **csak** az `stances` kategóriában áll
  össze, **7** kvízképes képpel. **161 zöld teszt** (core/model 35 + core/quiz 126),
  `testDebugUnitTest` és `assembleDebug` exit 0. A TR-02/TR-03 teljes készlet továbbra is
  a T-112 tárgya, mert a distraktorválasztás (T-109) és a keverés (T-110) még hiányzik.
- 2026-11-11 – **T-109 kész** (Fázis 2.4 harmadik darabja): a **distraktorválasztás** –
  `DistractorSelector` a `core/quiz` `generator` csomagjában. Prioritás: (1) azonos
  kategória, (2) feltöltés más kategóriából, (3) a kérdés **nem jön létre**; nincs
  háromopciós kérdés, nincs generált opció, nincs szinonim kisegítő opció (FR-14, FR-22).
  A V006 szinonimakizárás **páronként** érvényes (CR-07): sem a helyes mellé, sem két
  distraktor közé nem kerül két szinonim rekord. A (D) `JP_TO_IMAGE` opciói között nincs
  két vizuálisan azonos illusztráció (SHA-256 ujjlenyomat); a hiányzó ujjlenyomat nem
  bizonyít egyezést. A `QuizCandidateFilter.buildableQuestionCount` ezzel **felső korlátból
  tényleges kérdésszámmá** vált, és a `skipReason` is ebből következik. A kiválasztás
  rendezett bejárású és RNG-mentes, hogy a T-110 seedelt keverése erre épülhessen.
  **181 zöld teszt** (core/model 35 + core/quiz 146), `testDebugUnitTest` és
  `assembleDebug` exit 0. A `Shortfall` tényleges előállítása és a teljes TR-02 készlet
  a T-110/T-112 tárgya, mert a `QuizGenerator` implementációja még nem létezik.
- 2026-11-11 – **T-110 kész** (Fázis 2.4 negyedik darabja): a **seedelt determinizmus** –
  `DefaultQuizGenerator` és `SeededShuffle` a `core/quiz` `generator` csomagjában. A
  `QuizGenerator` innentől nem csak szerződés: a `generate()` a `QuestionType` deklarációs
  sorrendjében, a `DistractorSelector.buildableCorrectAnswers` azonosító szerint rendezett
  listáiból építi a (típus, helyes válasz) jelölteket, **Fisher–Yates** keveréssel adja a
  kérdéssorrendet és az opciósorrendet, és **ugyanabból** az egyetlen `Random(seed)`
  példányból sorsol – nincs `Random.Default`, nincs rendszeróra, nincs `Set`-iterációs
  sorrendre épülő döntés (NFR-07, ADR-007). A keverés csak **permutál**: az opciók halmaza
  a T-109 kiválasztása, ezért a keverés nem tud opciót elhagyni vagy becsempészni.
  Ugyanaz a helyes technika egy terven belül legfeljebb egyszer szerepel, akkor is, ha
  több típusban használható lenne; kevés jelöltnél **kevesebb** kérdés lesz, tényleges
  `Shortfall`-lal (`ALL_TYPES_SKIPPED` → `NOT_ENOUGH_IMAGES` → `NOT_ENOUGH_DISTRACTORS` →
  `NOT_ENOUGH_CANDIDATES` prioritással), nem ismétlés. A determinizmust forrásszkennelő
  teszt is őrzi: a `core/quiz` `main` forráskészletében – megjegyzéseket leszámítva –
  egyetlen `Random.Default` / `currentTimeMillis` / `Instant.now` előfordulás sincs, és
  nincs paraméter nélküli `shuffled()` hívás. **196 zöld teszt** (core/model 35 +
  core/quiz 161), `testDebugUnitTest` és `assembleDebug` exit 0. A `dryRun` egyelőre a
  `generate` tervéből származtat (helyes, de nem optimális) – a kérdésépítés nélküli
  összegzés a T-111, a teljes TR-02/TR-03 készlet a T-112 tárgya.
- 2026-11-11 – **T-111 kész** (Fázis 2.4 ötödik darabja): a `DefaultQuizGenerator.dryRun()`
  **kérdésépítés nélkül** összegez (FR-13, FR-22). A `generate()` és a `dryRun()` egyetlen
  találkozási pontja a privát `questionSlots(request, skippedTypes)`: minden meg nem hagyott
  típus minden olyan helyes jelöltje, amely mellé tényleg összeáll négy opció. A `dryRun`
  innen már csak összegez – nincs `Random`, nincs keverés, nincs `QuizQuestion` –, és a
  kérdésszám mégis bizonyíthatóan egyezik: a `generate` a helyes technika szerint
  deduplikál, ezért minden különböző helyes technika pontosan egy kérdést ad, a keverés
  sorrendjétől függetlenül, vagyis a kérdésszám `min(requestedCount, a slotok különböző
  helyes technikáinak száma)`. Az `availableByType` szemantikája tisztázva: típusonkénti
  **kapacitás**, nem a terv felosztása – csak nem kihagyott típus, minden érték ≥ 1, és az
  értékek összege nagyobb lehet az `expectedQuestionCount`-nál, mert ugyanaz a technika több
  típusban is lehet helyes válasz, de egy kvízen belül legfeljebb egyszer kerül elő. A
  `QuizReadiness` ezt invariánsként is kikényszeríti (diszjunkt kulcsok a `skippedTypes`-szal,
  nincs nulla kapacitású bejegyzés, `isEmpty() == (expectedQuestionCount == 0)`, az összeg
  felső korlát). **202 zöld teszt** (core/model 35 + core/quiz 167), `testDebugUnitTest` és
  `assembleDebug` exit 0. A teljes TR-02/TR-03 készlet továbbra is a T-112 tárgya.
- 2026-11-11 – **T-112 kész** (Fázis 2.5, az **M1 mérföldkő kvízhelyességi magja**): a
  **TR-02 és TR-03 teljes lefedése**, kódmódosítás nélkül – a kvízmotor változatlan, csak a
  bizonyíték került mellé. Három új tesztosztály: a `SeededShuffleTest` a Fisher–Yates
  keverés elemi garanciáit méri (csak permutál, a bemenetet nem módosítja, pontosan
  `size - 1` sorsolást fogyaszt, egy és nulla elemnél nullát, és a seedek felett mind a 24
  négyelemű permutáció előfordul); a `QuizPlanInvariantTest` egy **21 kérés × 7 seed**
  mátrixon állítja a nyolc kvízmotor-invariánst minden terv minden kérdésére (négy opció,
  egy helyes, három páronként különböző téves, páronkénti V006 szinonimakizárás, (D)
  vizuális azonosság kizárása, ismétlésmentesség, bitazonos determinizmus, és hogy minden
  opció **létező**, a típusra jogosult, a kategóriahatókörön belüli rekord); a
  `RealContentQuizGenerationTest` ugyanezt a valódi `knowledgebase_transfer/` tartalmon
  futtatja, és rögzíti a tartalmi tényt: az `stances` kategória terve mind a négy
  kérdéstípust adja, a másik nyolc kategória viszont egyetlen képes kérdést sem, mindenhol
  `NOT_ENOUGH_IMAGES` indoklással. A közös invariáns-ellenőrzés a
  `GeneratorContractFixtures.assertQuizPlanInvariants(...)`-ba került, hogy a szintetikus és
  a valós készlet ugyanazt a garanciát bizonyítsa. A `Shortfall` okprioritása
  (`ALL_TYPES_SKIPPED` → `NOT_ENOUGH_IMAGES` → `NOT_ENOUGH_DISTRACTORS` →
  `NOT_ENOUGH_CANDIDATES`) és a `dryRun` ↔ `generate` egyezés a közös `questionSlots`
  ponton forrásellenőrzéssel is rögzítve. **236 zöld teszt** (core/model 35 + core/quiz 201;
  34 új), `testDebugUnitTest --rerun-tasks` és `assembleDebug` exit 0. Hátra az M1-hez: a
  T-113 `QuizScoring` és a T-114 TR-04 készlet.
- 2026-11-11 – Fázis 2.6 kész: **T-113 `QuizScoring`**. A `core.quiz.scoring` csomagban
  `object QuizScoring { fun score(attempts: List<QuizAttempt>): Score }` tiszta függvényként –
  nincs állapot, nincs mellékhatás, nincs óra- és véletlenhívás (a modul forrásellenőrzéses
  NFR-07 tesztje a scoring fájlra is lefut). A `Score(answered, correct, ratio: Double?)`
  aránya **nullable**: 0 megválaszolt kérdés → `null`, nem `0.0` (FR-27), és ezt a `Score`
  init blokkja is kikényszeríti, így egy üres összegzés típusszinten sem gyártható 0%-kal.
  Egy kérdés egyszer számít (FR-18): a `(sessionId, questionId)` páron dedup, a legkorábbi
  `answeredAt` nyer, ezért sem a dupla koppintás, sem egy újrajátszott eseménysor nem
  duplázza az eredményt; a meg nem válaszolt kérdés nem hibás, csak nem szerepel. A
  helyességet nem számolja újra, a `QuizAttempt.correct` mezőt összegzi (DR-09). **245 zöld
  teszt** (core/model 35 + core/quiz 210; 9 új a `QuizScoringTest`-ben), `testDebugUnitTest`
  és `assembleDebug` exit 0. Hátra az M1-hez: a T-114 teljes TR-04 készlet.
- 2026-11-11 – Fázis 2.7–2.8 kész: **T-114 TR-04 eredményszámítás tesztek – ezzel a Fázis 2
  és az M1 mérföldkő (a termék helyességének magja) lezárva**. A T-113 kilenc alapteszte
  mellé a `QuizScoringTr04Test` 17 új esete a teljes TR-04 mátrixot fedi: 0 megválaszolt
  kérdés → „nincs adat" (`ratio == null`), és ez a 0%-tól (`0.0`) *mérhetően* különbözik;
  100% és 0% határeset 10 válaszon; félbehagyott kvíz (10-ből 4 megválaszolva, 3 helyes →
  0.75), és egy párba állított eset, amely bizonyítja, hogy a meg nem válaszolt kérdés nem
  hibás válasz – ugyanaz a 4 válasz félbehagyva jobb arányt ad, mint a maradék 6 hibásként
  végigvitt menet (0.75 > 0.3), miközben a helyes válaszok száma azonos; a `ratio` arány és
  nem százalék (1/4 → `0.25`); dupla válasz a **lista szintjén is ellenőrizve** – minden
  dedupot érintő eset állítja, hogy `score.answered` megegyezik a `(sessionId, questionId)`
  párok számával és legfeljebb a rekordok száma, külön esettel a késői „javításra" (az első,
  hibás válasz marad érvényes), a listában sorrendben megelőző duplikátumra, az azonos
  időbélyeg determinisztikus feloldására és a mind a 6 kérdésre duplázott, 12 rekordos
  menetre; vegyes kérdéstípusok egy munkamenetben (mind a négy típus egy egységet ér), a
  típusonkénti bontás visszaadja az összesítést (FR-24 előkészítés), a válasz nélküli típus
  bontása pedig „nincs adat", nem 0%. A `QuizScoring` **nem igényelt módosítást**: a T-113
  szerződése első futásra elbírta a teljes mátrixot. **262 zöld teszt** (core/model 35 +
  core/quiz 227; 17 új), `testDebugUnitTest` és `assembleDebug` exit 0. Következő: T-115,
  a `core/data` kanonikus sémabeolvasás (Fázis 3, M2).
- 2026-11-11 – **T-115 kész (Fázis 3.1): a kanonikus séma beolvasása a `core/data`-ban.**
  Új, kizárólag `internal` tartalomimport-réteg a `hu.kyokushin.trainer.core.data.content`
  csomagban (ADR-001 L3: egyetlen DTO sem szivárog ki a modulból): `ManifestDto`,
  `CategoryDto`, `IllustrationDto`, `EntriesFileDto`, `EntryDto` – mind `@SerialName`
  snake_case leképezéssel, **minden mező `null`-tűrő, alapértelmezett értékkel**, és
  `ignoreUnknownKeys = true` (`KnowledgeJson`), hogy a tartalom bővíthető legyen a már
  kiadott app elszállása nélkül. `KnowledgeAssetSource` interfész +
  `AndroidKnowledgeAssetSource` (assets-olvasás és a tényleges képfájllista összegyűjtése a
  V005 bemenetéhez), `KnowledgeContentReader` (`Parsed` / `Unreadable` ág), valamint a
  DTO → `KnowledgeInput` mapper (`dataset_version` → `contentVersion`,
  `editorial_status` → `datasetKind`, `review_status == "reviewed"` → `verified`,
  `quizEligible = null` – ADR-010).
  **A V010 itt készült el:** olvashatatlan vagy a csomagból hiányzó tudástárfájl esetén a
  beolvasó nem dob kivételt, hanem kész `ValidationReport`-ot ad egyetlen V010 (BLOKKOLÓ,
  DATASET hatókör) észrevétellel, a fájlnévvel a diagnosztikában – a V001…V010 kódkészlet
  ezzel teljes. A hiányzó *kötelező mező* szándékosan **nem** V010: azt a validátor jelenti
  rekordszinten (V002), így egyetlen hiányos rekord nem viszi el a többi 160-at.
  `.\gradlew.bat testDebugUnitTest` **exit 0** – **278 teszt, 0 hiba** (16 új a
  `:core:data`-ban); `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`,
  19 719 165 bájt).
  *Tudatos duplikáció:* a `core/quiz` teszt-oldali `RealKnowledgeBaseFixture.kt` ugyanezt a
  leképezést végzi a validátor TR-01 készletéhez. A két leképezés eltérése maga is hiba
  lenne, ezért a `RealKnowledgeContentReaderTest` **ugyanazokat** az elvárásokat állítja a
  valódi tartalomra (161 rekord, 9 kategória, 10 illusztráció, 0 BLOKKOLÓ hiba,
  10 `Asset`, a három feliratos kép – KY-0045/KY-0050/KY-0052 – kvízben nem használható).
  *Megjegyzés a következő taskhoz:* az `entries.json` megismétli a manifest
  `schema_version` / `dataset_version` értékét; a beolvasó beolvassa, de **a manifest a
  hiteles forrás**, és a két hely eltérését egyelőre semmi nem jelzi – ha szükséges, önálló
  szabálykód (és task) kell rá, nem csendes felülírás.
  *Nem futott:* az `AndroidKnowledgeAssetSource` vékony delegáló osztályára nincs unit teszt
  (a `core/data` modulnak nincs saját assets könyvtára, a tartalom másolása a T-601) –
  lásd `docs/TEST_REPORT.md`.
- 2026-11-11 – **T-116 kész (Fázis 3.4, 3.5): a Room séma a `core/data`-ban.**
  Új, kizárólag `internal` perzisztenciaréteg a `hu.kyokushin.trainer.core.data.db`
  csomagban: `KyokushinDatabase` (`version = 1`, `exportSchema = true`, a séma JSON a
  `core/data/schemas/` alatt), 5 tartalomtábla (`category`, `technique`,
  `technique_alias`, `synonym_group`, `content_state`), 3 felhasználói tábla
  (`quiz_session`, `quiz_attempt`, `study_review`), 8 DAO – **minden művelet `suspend`
  vagy `Flow`**, `allowMainThreadQueries` nincs.
  **ADR-005:** `UNIQUE(sessionId, questionId)` a `quiz_attempt`-en, és
  `OnConflictStrategy.IGNORE` beszúrás, így a második válasz nem kivétel, hanem `-1`
  sorazonosító – ebből lesz a `ProgressRepository.recordAttempt` `false` válasza.
  **ADR-004:** a `quiz_attempt` és a `study_review` `techniqueId` oszlopán **nincs**
  idegen kulcs a `technique` táblára (nem is `NO_ACTION`): a `NO_ACTION` beszúrási
  kényszert adna, és újra összekötné a tartalom és az előrehaladás életciklusát (DR-07).
  Az egyetlen `CASCADE` felhasználói adat → felhasználói adat irányban van
  (`quiz_attempt.sessionId` → `quiz_session`).
  `@TypeConverter`: `Instant` ↔ `Long` epoch milli, enum ↔ a konstans **neve** (nem
  `ordinal`, mert az átrendezésre törékeny), és az azonosító value classok a privát
  konstruktoruk helyett az `of()` gyárukon át – így üres azonosító az adatbázisból sem
  kerülhet a domainbe. Ismeretlen enum-név visszaolvasása **dob**, nem esik vissza
  engedékenyebb értékre (CR-02).
  Szándékos eltérés az ARCHITECTURE 5.2 táblázatától: a `technique` táblán **nincs**
  `quizEligible` oszlop – a kvízjogosultság származtatott érték (ADR-010).
  `fallbackToDestructiveMigration()` sehol; ezt teszt is tiltja (DR-08).
  **314 zöld teszt** (36 új: 10 konverter, 14 sémaszerződés az exportált JSON-ból, 2 DAO
  szerződés reflexióval, 10 in-memory adatbázis Robolectricen), `testDebugUnitTest` és
  `assembleDebug` exit 0. Következő: T-117, a `ContentImporter`.

- 2026-11-11 – **T-117 kész (Fázis 3.6): a `ContentImporter` a `core/data`-ban.**
  A tartalombetöltés menete: `KnowledgeContentReader` → **verziókapu** → `KnowledgeValidator`
  → **upsert** egyetlen Room tranzakcióban. A publikus szerződés a `ContentImporter`
  interfész és az `ImportResult` (`UpToDate` / `Imported` / `Rejected`); a
  `RoomContentImporter` `internal` (ADR-001 L4).
  **A FR-33 garancia szerkezeti, nem futásidejű:** az importáló egyetlen helyen sem
  hivatkozik a `quiz_session`, `quiz_attempt`, `study_review` táblákra, a technikára pedig
  csak `@Upsert` és `markRetired` megy – `DELETE` nincs (ADR-004). Ami kimaradt az
  aktuális csomagból (az **elvetett** rekordot is beleértve), az `retired = true` jelölést
  kap; a visszatérő technika jelölése az upserttel feloldódik.
  Az elutasított adathalmaz (V001 séma, V010 parse) a tartalomtáblákhoz **nem nyúl**, és a
  `content_state` sorát is csak akkor írja, ha még egy sikeres betöltés sem volt – különben
  a kijelzett verzió elcsúszna a táblák tényleges tartalmától.
  A `core/data` → `core/quiz` függés `api`-ra változott: a publikus szerződés
  `ValidationReport`-ot ad vissza (ARCHITECTURE 5.3).
  Nyitott, új taskot érdemlő apróság: a manifest **kategóriamezőinek** nincs validációs
  szabálykódja (V001…V010 egyike sem fedi), ezért a név nélküli kategória az azonosítóját
  mutatja megjelenítendő névként – kitalált magyar nevet nem gyártunk (CR-01).
  **330 zöld teszt** (16 új: 10 importáló Robolectricen + in-memory Roomon, 6 mapper tiszta
  JVM-en), `testDebugUnitTest` és `assembleDebug` exit 0. Következő: T-118, a TR-05 tesztek.

- 2026-11-11 – **T-118 kész (Fázis 3.8): a TR-05 garancia mérve.**
  `ContentUpdateProgressTest` – 10 új, `TR05` jelölésű teszt in-memory Room adatbázison,
  a **valódi** `RoomContentImporter`-en. A menet mindenhol ugyanaz: v1 kiadás betöltése,
  előrehaladás felvétele (két kvízmenet, 5 válasz vegyes helyességgel, 4 önértékelés
  ismétléssel), majd v2 betöltése – és a frissítés előtti/utáni **pillanatkép egyetlen
  egyenlőségvizsgálattal** összevetve.
  A pillanatkép a nyers `quiz_attempt` és `study_review` sorokat **és** a belőlük
  levezetett statisztikát tartalmazza (helyes válaszok, találati arány, technikánkénti
  bontás a legfrissebb önértékeléssel, menetenkénti `answeredCount`): az aggregált
  lekérdezések a T-501 tárgya, de a garancia a *tárolt tényekre* vonatkozik, amiből azok
  dolgozni fognak (DR-09).
  Lefedve: statisztika-változatlanság egyetlen és több körös frissítésen át (v1 → v2 → v3);
  átnevezés (az `id` marad, a próbálkozás az **új** rekordra oldódik fel – DR-06); a
  csomagból kimaradt technika visszavonása; a tartalom által kimondottan `retired: true`
  rekord; a visszatérő technika korábbi előrehaladásának épsége (ADR-004); idempotencia
  új importálópéldánnyal és a frissítés után is (FR-30); és az elutasított frissítés, ami
  sem a tartalomhoz, sem az előrehaladáshoz nem nyúl (FR-31 + FR-33).
  A csomagolt kiadás JSON-összeállítása a `KnowledgeSourceFixtures` közös fixture-be
  került, mert a garanciát két tesztosztály méri; a T-117 tesztjei változatlanok.
  Termelési kód **nem változott** – a T-117 szerkezeti FR-33 garanciája a viselkedés
  szintjén igazolódott.
  **340 zöld teszt** (10 új, mind Robolectricen + in-memory Roomon), `testDebugUnitTest`
  és `assembleDebug` exit 0. Következő: T-201, a `TechniqueRepository`.
- 2026-11-11 – **T-201 kész** (Fázis 3.7 első darabja): a **tudástár olvasási szerződése**
  a feature modulok felé. Új, publikus `TechniqueRepository` a
  `core.data.repository` csomagban – `observeCategories()`,
  `observeTechniques(query, categories)`, `getTechnique(id)`,
  `getQuizCandidates(categories)`, `observeContentState()` –, mellé az addig nem létező
  `ContentState` domain modell `NotImported` / `Imported` ágakkal (az első indítás nem
  hibaállapot, és nem is öt `null` egy data classban). A megvalósítás
  `internal RoomTechniqueRepository`, `@Inject` konstruktorral és injektált
  `@IoDispatcher`-rel; a kötés a T-203 dolga, és a `core/data`-ban kell élnie, mert a
  `KyokushinDatabase` is `internal`.
  A feladat lényege a `retired` szűrés helye (ADR-004, DR-07): a listán és a
  kvízjelölteknél **SQL-ben** szűr (`WHERE retired = 0`), nem Kotlin oldali `filter`-rel,
  így egy jövőbeli nyers DAO-hívás sem tudja kikerülni. A `getTechnique` a **dokumentált
  kivétel**: a Haladás képernyőnek éppen a visszavont technikát is fel kell oldania, hogy
  kiírhassa, „már nem része a tananyagnak, de a korábbi eredményeid megmaradtak"
  (FUNCTIONAL_SPEC 6.2) – a `null` ezért csak ismeretlen azonosítót jelent.
  Három további döntés: az aliasok **egyetlen** `technique_alias` beolvasásból,
  memóriában csoportosítva kerülnek a technikákra (nincs N+1, NFR-10); a
  `synonym_group` csoportjai a szűrés **utáni** készletre vágva jönnek vissza, és a
  kétnél kevesebb tagúra fogyottak kiesnek (különben a `QuizCandidateSet` konstruktora
  joggal dobna – V006, CR-07); a `ContentState.activeTechniqueCount` a `technique` tábla
  aktuális tartalma, nem a legutóbbi import `acceptedCount` mérlege, mert egy elutasított
  frissítés után a kettő eltér (FR-31).
  Új DAO-lekérdezések (séma **nem** változott, migráció nem kell): kategóriaszűrt aktív
  lista, pontszerű aktív lekérdezések a kvízjelöltekhez, `observeActiveCount()`, és az
  összes alias egy kérdésben.
  Szándékos hiányok, a következő feladatokra hagyva: a keresés `LIKE`-ra és előre
  normalizált oszlopokra költöztetése + az egyező alias megjelölése a **T-202**; a
  `QuizCandidateSet.illustrationDigests` feltöltése a **T-402**, mert a képek tartalmi
  ujjlenyomatához nincs sémaoszlop – becsült ujjlenyomatot pedig nem gyártunk.
  12 új teszt (Robolectric + in-memory Room), köztük egy **reflexiós**, amely a
  `TechniqueRepository` minden metódusának generikus visszatérési és paramétertípusát
  átvizsgálja Room entitás / DAO / DTO szivárgásra (TR-07, ADR-001 L4).
  **352 zöld teszt**, `testDebugUnitTest` és `assembleDebug` exit 0. Következő: T-202, a
  keresés SQL-be költöztetése.
- 2026-11-11 – **T-202 kész** (Fázis 3.7 második darabja): a keresés **SQL-oldalra**
  költözött, és a találat megmondja, melyik alias egyezett (FR-03, US-06).
  A `technique` tábla két előre normalizált oszlopot kapott (`search_romaji`,
  `search_hungarian`), amelyeket a `ContentEntityMappers.toEntity()` tölt ki a
  `NameNormalizer`-rel – a normalizálás így továbbra is **egyetlen** helyen él (a
  validátor V006 vizsgálata ugyanezt használja). A szűrés `LIKE '%…%' ESCAPE ''`
  feltétellel az adatbázisban fut a három normalizált oszlopon (`search_romaji`,
  `search_hungarian`, `technique_alias.aliasNormalized`); az alias-ág `EXISTS`
  altkérdés, nem `JOIN` (az több aliasra duplikálta volna a technikát). A beírt `%` és
  `_` escape-elve literális karakter, nem joker. A `search_*` oszlopokra **nincs index**:
  bal oldali jokerrel az SQLite nem használhat indexet.
  Az **egyező alias** ugyanabból a lekérdezésből jön (korrelált altkérdés, ábécé szerint
  első illeszkedő alias), és egy `nameMatched` kifejezés mondja meg, kell-e egyáltalán
  kiírni. Új publikus típus: `TechniqueMatch(technique, matchedAlias)`, új metódus:
  `observeTechniqueMatches(query, categories)`; az `observeTechniques` ennek a vetülete,
  tehát a korábbi szerződés nem tört el.
  **Sémaváltozás (DR-08, ADR-012):** `version` 1 → 2, explicit
  `KyokushinMigrations.MIGRATION_1_2`-vel (`ALTER TABLE … ADD COLUMN` + áthidaló
  visszatöltés + a `content_state.contentVersion` kinullázása, hogy a következő import a
  `NameNormalizer` kimenetét írja az oszlopokba), exportált `2.json`-nal.
  `fallbackToDestructiveMigration()` **nincs** – a migráció a `quiz_session`,
  `quiz_attempt` és `study_review` táblákhoz nem is nyúl, és ezt a
  `KyokushinMigrationTest` sorszinten ellenőrzi (DR-07, FR-33).
  6 új teszt (összesen **358 zöld teszt**), `testDebugUnitTest` és `assembleDebug`
  exit 0. A migrációs teszt Robolectric alatt, Room `MigrationTestHelper`-rel fut – a
  sémaexport a `test` source set assetjeként van bekötve; a környezetspecifikus
  akadályokat (assets-ből olvasás, név vs. abszolút út, fájlújrahasználat) a
  `TEST_REPORT.md` rögzíti.
  Következő: T-203, a Hilt kötések – az adatbázis-kötésnek
  `addMigrations(*KyokushinMigrations.ALL)`-t kell adnia.

- 2026-11-11 – **T-203 kész** (Fázis 3.9; a Fázis 1.6 „Hilt modulok tartalma" hátraléka
  ezzel lezárva): **a Hilt gráf teljes és felépül.** Hat Hilt modul, **két** Gradle
  modulban – és ez szándékos eltérés az ARCHITECTURE 7. táblázatától (lásd az új 7.1
  szakaszt): a `KyokushinDatabase`, a DAO-k, a `KnowledgeContentReader` és a két
  repository-implementáció `internal` (ADR-001, L3/L4), `internal` típust pedig csak az a
  modul tud kötni, amelyik látja. Ezért a `core/data`-ban él a `DatabaseModule`
  (adatbázis + mind a 8 DAO), a `ContentModule` (`KnowledgeAssetSource` →
  `AndroidKnowledgeAssetSource`, `KnowledgeContentReader`) és a `RepositoryModule`
  (`@Binds TechniqueRepository` → `RoomTechniqueRepository`,
  `@Binds ContentImporter` → `RoomContentImporter`); a `di` modulban a `TimeModule`
  (`Clock.systemUTC()`, UTC – a tárolt időbélyegek zónamentes `Instant`-ok, DR-05), a
  `DispatcherModule` (`@IoDispatcher` = `Dispatchers.IO`, `@DefaultDispatcher` =
  `Dispatchers.Default`) és a `QuizModule` (`DefaultQuizGenerator`,
  `DefaultKnowledgeValidator`, RNG).
  **Az adatbázis-kötés `addMigrations(*KyokushinMigrations.ALL)`-t kap, destruktív
  tartalékot nem** (DR-08): a séma a 2-es verzión áll, a `fallbackToDestructiveMigration()`
  csendben törölné a `quiz_attempt` és `study_review` sorokat. Egy forrásszkennelő teszt
  a `core/data` egész `main` forrásfáján ki is zárja ezt a hívást, és azt is ellenőrzi,
  hogy a kötés tényleg átadja a migrációkat.
  Új qualifier: `@DefaultDispatcher` (a `core/data` `di` csomagjában, az `@IoDispatcher`
  mellett – a qualifiert az injektálást kérő modulnak kell látnia, a kötését a `di` adja).
  Az ARCHITECTURE `RandomFactory` sora `kotlin.random.Random` kötésként valósult meg: a
  seedelt sorozatot a generátor építi a kapott `Long`-ból, a kötés feladata a **seed
  sorsolása** (ADR-007, NFR-07) – egy gyártó interfész a `di`-ben lakna, azt pedig a
  `feature` modulok szándékosan nem látják.
  **A DoD „az app elindul, a Hilt gráf felépül" pontja két, egymást erősítő módon
  igazolt:** (1) a `di` modul `KyokushinGraph` `@EntryPoint`-ja fordítási időben
  kikényszeríti a gráf teljességét – hiányzó vagy kétértelmű kötés mellett az
  `assembleDebug` elbukik, nem az első olyan képernyő, amelyik először kérné; (2) a
  `HiltGraphTest` (app modul, Robolectric, `HiltTestApplication`) **futásidőben** felépíti
  ugyanazt a `SingletonComponent`-et, és mind a nyolc kötést feloldja – a Room adatbázis
  megnyílik, az assetolvasó megkapja az `AssetManager`-t, a repository és az importáló
  megépül. Valódi eszközön/emulátoron az indítás továbbra **nem** futott (B-03), ezt a
  TR-06 (T-606) fedi.
  7 új teszt (összesen **365 zöld teszt**), `testDebugUnitTest` és `assembleDebug`
  exit 0. Új katalógusbejegyzés: `hilt-android-testing`, ugyanabból a `hilt` kiadásból
  (2.60.1) – nem új verzió.
  Következő: T-204, a `core/ui` `TechniqueIllustration` komponense.

- 2026-11-11 – **T-204 kész** (Fázis 4.1 első fele). A `core/ui` első Compose
  komponense: `TechniqueIllustration` az `IllustrationRef` **mindhárom** esetre –
  `Asset` → a csomagolt kép, `Placeholder` → semleges helyőrző + „Ábra készül”,
  `null` → **nincs képblokk** (ADR-003, CR-05, FUNCTIONAL_SPEC 2.3).
  **A `contentDescription` nem string paraméter, hanem az `IllustrationLabel`
  szándéktípus:** `Technique(romaji, hungarian)` a Tudástárban (a FUNCTIONAL_SPEC 9.
  itt megköveteli a nevet), `QuizOption(position)` a kvíz (D) képrácsán – ezen az
  ágon a technika nevére **nincs is paraméter**, tehát a név kiszivárgása nem
  figyelmetlenség kérdése, hanem fordításilag lehetetlen (NFR-04). A leírásokat
  `@Composable`-mentes `IllustrationContentDescription` oldja fel, ezért a NFR-04
  szabály bizonyítéka unit teszt a **tényleges** `strings.xml` fölött (Robolectric,
  `sdk = [34]`), nem emulátort igénylő UI teszt.
  A kép assetből töltődik (`knowledge/illustrations/<fileName>`, T-601 teszi oda),
  `Dispatchers.IO`-n, kétlépéses dekódolással: a jelenlegi 1122×1402-es képek
  `inSampleSize = 2`-vel jönnek be. Hiányzó vagy olvashatatlan fájl → helyőrző,
  nincs összeomlás (FUNCTIONAL_SPEC 10.). `awaitingReview = true` esetén szöveges
  jelzés a kép alatt (US-08 4., CR-01) – a komponens ezt **nem** származtatja.
  `@Preview` mindhárom esetre + a kvízopcióra, világos és sötét témában.
  **Architektúraváltozás:** a téma (`KyokushinTrainerTheme`) átkerült az `app`-ból a
  `core/ui`-ba (`hu.kyokushin.trainer.core.ui.theme`), mert a megosztott komponensek
  `@Preview`-jai és a későbbi funkciómodulok is használják, `:app`-ra viszont senki
  nem függhet visszafelé (ADR-001). Az `app` importát és az ARCHITECTURE.md
  modultérképét is frissítette a task.
  10 új teszt (összesen **375 zöld teszt**), `testDebugUnitTest` és `assembleDebug`
  exit 0. Új katalógusbejegyzés nincs; a `core/ui` megkapta a már katalogizált
  `robolectric`, `androidx-test-core` és `kotlinx-coroutines-core` függőséget.
  Következő: T-205, a `core/ui` jelölői és állapotkomponensei.

- 2026-11-11 – **T-205 kész (Fázis 4.1 lezárva): a `core/ui` jelölői és
  állapotkomponensei.** Négy megosztott komponens, minden szöveg a
  `core/ui/.../values/strings.xml`-ben (NFR-02), minden szín téma-tokenből, `@Preview`
  világos és sötét témában mindegyikre.
  - **`VerificationBadge(status, missingFields)`** (FR-05, FUNCTIONAL_SPEC 2.2):
    `AWAITING_REVIEW` → „Ellenőrzésre vár" chip, `INCOMPLETE` → „Hiányos" chip + a
    hiányzó mezők magyar felsorolása, `VERIFIED` → **semmi**, mert a specifikáció
    szerint a hitelesség a jelölő hiánya. A hiányzó mezőket a hívó **nem szövegként**
    adja át, hanem a `TechniqueField` enummal, így a mezőnevek sem szöknek ki a
    `strings.xml`-ből.
  - **`UnreviewedBanner(trust)`** (ADR-010, CR-03): a nem elutasítható
    lektorálatlanság-szalag. Nincs `onDismiss`, nincs `dismissible`, nincs `visible`
    paramétere – az elrejthetetlenség az API alakjába van zárva, nem csak
    dokumentálva. Egyetlen bemenete a `DatasetTrust`, ami a manifest szerkesztői
    állapotából jön; `REVIEWED` mellett a composable nem bocsát ki UI-t, tehát
    lektorált tartalom érkezésekor a jelzés kódmódosítás nélkül eltűnik (AC-08).
  - **`EmptyState(reason, onAction?)`** (NFR-06, FUNCTIONAL_SPEC 2.4): az `EmptyStateReason`
    sealed interface **okot** kap, nem szöveget – `LibraryNoContent` (betöltési hiba,
    javasolt akció „Diagnosztika", FR-32), `LibraryNoResult(query)` (a keresőszöveget
    idézi, akció „Szűrők törlése"), `ProgressNoData` és `QuizNotEnoughData` (akció
    nélkül, mert az üzenet maga hív cselekvésre). Az ok hordozza az üzenetet **és** a
    javasolt akció címkéjét, hogy a kettő ne szakadhasson el; a gomb csak akkor
    jelenik meg, ha a hívó műveletet is adott.
  - **`ErrorState(onRetry, messageRes)`** (NFR-06): magyar hibaüzenet + „Újra".
    A `messageRes` `@StringRes`, nem `String`: így egy kivétel angol `message`-e nem
    kerülhet a felhasználó elé. A művelet nem opcionális – ha nincs mit újrapróbálni,
    az üres állapot, nem hiba.
  - Akadálymentesség: a chip egyetlen bejárási egység (`clearAndSetSemantics`, a
    címke és a felsorolás egyszerre hangzik el), a szalag `LiveRegionMode.Polite`,
    a gombok `heightIn(min = 48.dp)` – a Material 3 alapértelmezett 40 dp nem elég
    (FUNCTIONAL_SPEC 9.).
  - **18 új teszt** (összesen **393 zöld teszt**), a tényleges `strings.xml` fölött
    Robolectrickel (`sdk = [34]`). A döntő állítások: a `VERIFIED` ág nem ad jelölőt,
    az `AWAITING_REVIEW` soha nem sorol fel hiányzó mezőt, a „nincs tartalom" és a
    „nincs találat" üzenete **és** akciója különbözik, és `REVIEWED` tartalomnál a
    szalag szövege `null`.
  - **Build-változás:** a `core/ui` `androidx-core-ktx` függősége `implementation`-ből
    `api`-ra került, mert a publikus API `@StringRes` annotált azonosítókat hordoz, és
    az `androidx.annotation` ezen keresztül jut a hívó modulokba. Új
    katalógusbejegyzés nincs, verziószám nem változott.
  - **Menet közbeni hiba:** az `aapt` levágja a sztringek záró szóközét, ezért a
    felsorolás elválasztója (`", "`) üresen jött át és a mezők szóköz nélkül
    ragadtak össze. Megoldás: dupla idézőjelbe zárt erőforrásérték.
  Következő: T-206, a `navigation` NavHost és az alsó navigáció.

- 2026-11-11 – **T-206 kész (Fázis 4.2): a `navigation` NavHost és az alsó navigáció.**
  A Fázis 1 ideiglenes kezdőképernyő („A projektváz felállt") lecserélve: a
  `KyokushinApp()` már csak a `:navigation` modul `KyokushinNavHost()`-ját hívja, és az
  `app` modul nem tud a képernyőkről.
  - **Tíz útvonal** a FUNCTIONAL_SPEC 1. szerint: `library/list`,
    `library/detail/{techniqueId}`, `learn/setup`, `learn/cards`, `quiz/setup`,
    `quiz/run/{sessionId}`, `quiz/result/{sessionId}`, `progress/overview`,
    `progress/technique/{techniqueId}`, `settings`. Az útvonalak egyetlen hiteles forrása
    a `KyokushinScreen` enum: útvonalminta + magyar cím + szakasz + „fülkezdő-e" +
    „látszik-e a bottom nav". Így nem lehet olyan képernyő, amelynek útvonala van, de a
    fülkijelölés vagy a bottom nav látszása nincs eldöntve.
  - **Négy fül** (`BottomNavDestination`): Tudástár, Tanulás, Kvíz, Haladás – magyar
    felirattal (`navigation/.../values/strings.xml`) és ikonnal. A fül a szakasz alapján
    kijelölt, ezért a Tudástár fül a részletezőn is kijelölve marad, és a `settings` a
    Haladás fülhöz tartozik (nincs önálló füle, a Haladásból nyílik).
  - **A futó kvíz nem hagyható el véletlenül** (a task lényegi pontja): a `quiz/run` és
    `quiz/result` képernyőn a bottom nav **nincs összeállítva**, és a felső sávban sincs
    vissza nyíl. A döntés egyetlen helyen, adatként áll (`showsBottomBar`), nem elszórt
    `if`-ekben. A megszakítás explicit megerősítést kérő útja a **T-410** tárgya.
  - **Állapotmegőrzés fülváltáskor:** `popUpTo(startDestination) { saveState = true }` +
    `restoreState = true` + `launchSingleTop = true`.
  - **Helyőrző képernyők:** minden cél egy nyíltan megjelölt `PlaceholderScreen`-t mutat
    („Ez a képernyő még fejlesztés alatt áll."), az argumentumot hordozó útvonalak
    kiírják a megkapott azonosítót, és gombok vezetnek tovább – így a teljes váz
    bejárható. A helyőrző azonosítók (`placeholder-technique`, `placeholder-session`)
    **nem tartalom**: nem a tudástárból jönnek és nem állítanak semmit egy technikáról.
  - **15 új teszt** (összesen **408 zöld teszt**), sima JVM JUnit – a `KyokushinScreen` és
    a `BottomNavDestination` szándékosan Android-mentes adat, ezért az útvonalak, a
    fülkijelölés és a bottom nav látszása Compose-fa kirenderelése nélkül eldönthető.
    A tényleges koppintás-bejárás és a fülállapot megmaradásának mérése emulátort kér:
    **T-606** (B-03).
  - **Szövegkulcs-átrendezés:** a `nav_library`/`nav_learn`/`nav_quiz`/`nav_progress`
    kulcsok az `app` modulból a `navigation` modulba kerültek, mert a bottom nav is ott
    van. Így az erőforrás-összefűzés nem írja felül csendben az egyiket a másikkal.
  Következő: T-207, a `feature/library` lista ViewModelje.

- 2026-11-12 – **T-207 + T-211 kész** (Fázis 4.3 állapotkezelő fele, G-02 csoport). A
  `feature/library` első kódja: `LibraryListUiState` sealed négy állapottal, és a
  keresőszöveg, a chipek, a kijelölés és a `datasetTrust` a sealed interfész **közös**
  tulajdonsága – az üres állapotban sem tűnhet el a keresősáv, amivel a felhasználó
  visszavonhatja a saját szűkítését (US-06 4.). A `LibraryListViewModel` indítja a
  tartalomimportot (nem az `Application`), ezért a `Loading` valódi állapot; a `Rejected`
  import **nem** `Error`, mert olyankor a korábbi kiadás érvényben marad (FR-31). A
  keresés 200 ms debounce-szal fut, de az **üres** keresőszó késleltetés nélkül jut át (a
  „×" visszavonás, nem újabb keresés). A chipek találatszáma a kategóriakijelölést
  figyelmen kívül hagyva számolt, így egy lekérdezés elég, és a nem kijelölt chip száma
  sem nulla. Ez a `@DefaultDispatcher` első fogyasztója: az összegzés és a kétszintű
  rendezés CPU-munka, nem I/O. 17 új teszt (425 teszt, 0 hiba);
  `assembleDebug` exit 0. A Compose lista a T-208 tárgya.

- 2026-11-12 – **T-208 + T-210 kész** (Fázis 4.3 lezárva, Fázis 4.5 kész, G-03 csoport).
  A Tudástár lista Compose felülete: `LibraryListScreen.kt` a `feature/library/list`
  csomagban. Keresősáv törlés ikonnal, vízszintesen görgethető kategóriachip-sor
  „Összes" chippel (a kijelölt kategóriákat egyenként kapcsolja ki a meglévő
  `onToggleCategory`-n keresztül – nem kellett új ViewModel-metódus), `LazyColumn` a
  listával. Egy listatétel **egy** TalkBack bejárási egység: a `clearAndSetSemantics` +
  `LibraryListItemAccessibility.describe` törli a gyermekek (illusztráció, két
  szövegsor, hitelesítési chip) külön-külön felolvasását, és a FUNCTIONAL_SPEC 9. szerinti
  „`{romaji}`, `{magyar}`, `{kategória}`, `{státusz}`" mintát adja (VERIFIED rekordnál a
  státuszrész elmarad, ugyanúgy, mint a `VerificationBadge` látható chipjénél). Mind a
  négy állapot (`Loading`/`Empty` – nincs tartalom + debug „Diagnosztika" gomb/`Empty` –
  nincs találat + „Szűrők törlése"/`Error` + „Újra") bekötve, `@Preview`-val mindegyikre
  világos és sötét témában. A „Diagnosztika" gomb kapuja `BuildConfig.DEBUG` (új
  `buildFeatures.buildConfig = true` a `feature/library`-ban) – a tényleges célképernyő a
  T-508 tárgya, itt csak a láthatóság a követelmény (FR-32). A `navigation` modulban a
  `library/list` útvonal helyőrzője lecserélve a valódi `LibraryListRoute`-ra. 9 új teszt
  – `LibraryListItemAccessibility` és `libraryEmptyStateAction` tiszta függvényként,
  Compose fa nélkül tesztelve (434 teszt, 0 hiba); `assembleDebug` exit 0.

Következő: T-209, a Tudástár részletező képernyő (a `library/detail/{techniqueId}`
helyőrzőjének lecserélése).

- 2026-11-12 – **T-209 kész** (Fázis 4.4, az E2 epik és az M3 mérföldkő lezárása).
  `feature/library/detail`: `LibraryDetailUiState` (`Loading`/`Content`/`Error`, közös
  `datasetTrust`) és `LibraryDetailViewModel` – egyszeri (nem megfigyelt) `getTechnique`
  lekérdezés, mert a részletező csak már betöltött tartalomból nyitható meg, nincs saját
  importja. A hiányzó mezők listáját egy `missingFieldsFor(status)` függvény vezeti le:
  a jelenlegi validátor (`DefaultKnowledgeValidator`) kizárólag a lógó képhivatkozás
  miatt fokoz le `INCOMPLETE`-re, tehát a felsorolás ma mindig `[ILLUSTRATION]` (ha ez
  változik, itt kell bővíteni). A `techniqueId` navigációs argumentumot a ViewModel a
  saját `SavedStateHandle`-jéből olvassa – a Navigation Compose minden
  `NavBackStackEntry`-t előre feltölt a route argumentumaival, függetlenül attól, melyik
  modul hívja a `hiltViewModel()`-t –, ezért a `feature/library` nem kényszerül a
  `navigation` modulra függeni (L5 megtartva).

  `LibraryDetailScreen` Compose UI: nagy illusztráció (itt a `null` **helyőrzővé** válik,
  szemben a listával, ahol a hely egyszerűen üres marad – ugyanaz a komponens,
  [IllustrationRef.Placeholder] szubsztitúcióval), romaji, magyar név, japán íráskép és
  aliasok csak akkor jelennek meg, ha van tartalmuk (US-07 3. kritérium), kattintható
  kategóriachip. A kategóriachip a listára navigál vissza, arra a kategóriára szűrve: a
  `navigation` modul a Navigation Compose „eredmény visszaadása" mintáját használja
  (`navController.previousBackStackEntry?.savedStateHandle?.set(...)` a
  `popBackStack()` előtt), ugyanabba a kulcsba, amit a lista ViewModelje a
  kategóriaszűréshez használ (`LibraryListViewModel.KEY_CATEGORIES`, ezért az a
  companion object mostantól nem `private`). Hitelesítési blokk: a `VerificationBadge`
  adja a „Hiányos" jelölőt és a hiányzó mezők felsorolását, egy önálló szöveg pedig a
  kvízkizárás tényét (US-08 3. kritérium) – ez a harmadik, különálló elem.

  **Két, dokumentált tartalmi hiány** (lásd `docs/CONVENTIONS.md` 8.): a „már
  gyakoroltam" összegzés (FUNCTIONAL_SPEC 3.2 11.) nincs megírva, mert nincs
  `ProgressRepository` (T-301/T-401 hátra); a hitelesítési blokk nem jelenít meg
  `source` mezőt `verified = true` esetén (3.2 10.), mert a kanonikus séma (ADR-011)
  technikaszinten nem ad ilyen mezőt – csak az illusztrációknak van saját forrásuk.
  Mindkettő a Teendők listájából deliberáltan ki lett véve, a „Kész ha" lista ezeket
  nem is követeli meg.

  7 új teszt (441 teszt, 0 hiba, `--rerun-tasks`-szal is leellenőrizve);
  `assembleDebug` exit 0 (APK: `app/build/outputs/apk/debug/app-debug.apk`).

  Következő: T-301, `core/data`: `ProgressRepository` (tanulási rész).

- 2026-11-12 – **G-01 csoport kész** (T-301, T-401, T-402, T-502 egy menetben,
  `docs/backlog/GROUPS.md`): publikus `ProgressRepository` + `internal
  RoomProgressRepository` a `core/data`-ban. `startSession`/`recordAttempt`/
  `finishSession` (T-401): a `SessionId`-t az injektált `Clock` időbélyegéből és a
  hívó által átadott, injektált RNG-ből sorsolt `seed`-ből képezi
  (`"session-${epochMilli}-${seed}"`) – nincs `UUID.randomUUID()` és nincs
  `System.currentTimeMillis()` (NFR-07); a `recordAttempt(NewAttempt): Boolean` a
  próbálkozás beszúrását és a menet `answeredCount`-jának növelését egy
  tranzakcióban végzi, és `false`-t ad, ha a `UNIQUE(sessionId, questionId)` ütközik
  (ADR-005). `recordStudyReview`/`observeStudyReviewSummary` (T-301, FR-28): új
  `StudyReviewDao.observeCountByRating` darabszámlálás, teszttel bizonyítva, hogy
  sosem ér a kvízmunkamenet-táblákhoz. `clearAllProgress` (T-502, FR-35): egy
  tranzakcióban üríti a `quiz_session`-t (a `CASCADE` viszi a `quiz_attempt`-et) és a
  `study_review`-t, a tartalomtáblákhoz nem nyúl. `getQuizCandidates`
  (T-402) immár tölti a `QuizCandidateSet.illustrationDigests` térképet – a
  csomagolt illusztrációfájl **nevéből**, nem bájt-szintű SHA-256-ból (nincs ilyen
  oszlop a sémában, becsült ujjlenyomatot nem gyártunk); a `quiz_excluded`/
  `INCOMPLETE` szűrés **szándékosan nem** itt van, azt a `QuizEligibility` kapu
  végzi (ADR-010) – ez eltérés a task szó szerinti „Teendők" szövegétől, indoklással
  a T-402 lezárásában. A kötések bekerültek a `RepositoryModule`-ba és a
  `KyokushinGraph`/`HiltGraphTest`-be, a `TechniqueRepository` mintájával. 10 új
  teszt (451 teszt, 0 hiba, `--rerun-tasks`-szal is leellenőrizve); `assembleDebug`
  exit 0.

  Következő: T-302, `feature/learn`: `learn/setup` ViewModel + UI.

- 2026-11-12 – **G-04 csoport kész** (T-302, T-303, `docs/backlog/GROUPS.md`,
  `feature/learn`, Fázis 5.1 lezárva, 5.2 részben). **T-302:** `LearnSetupViewModel`
  (`Loading`/`Content`/`Error`, az importot maga indítja, ugyanaz a mintázat, mint a
  `LibraryListViewModel`-nél, csak keresés/debounce nélkül) és a hozzá tartozó Compose UI
  (`LearnSetupScreen.kt`: `LearnSetupRoute` + stateless képernyő, kategória-jelölőnégyzet-
  lista darabszámmal, elrejtésválasztó rádiógombpár, „Kártyák indítása" gomb letiltási
  indoklással, 8 `@Preview`). A kijelölés itt **kötelező választás, nem szűrő** – szemben
  a Tudástár/kvíz kategóriaszűrőjével, az üres kijelölés itt letiltja a gombot
  (FUNCTIONAL_SPEC 4.1/4.). A kategóriánkénti darabszám **minden nem visszavont**
  technikát számol, a hitelesítési státusztól függetlenül (a tanuláshoz nem kell
  `VERIFIED`/`quizEligible`) – ez eltér a FUNCTIONAL_SPEC 4.1/2. szó szerinti („a
  kvízképes és összes rekord száma") szövegétől, ami ellentmond a közvetlenül utána álló
  mondatnak; dokumentált doksi-drift (`docs/CONVENTIONS.md` 8.). Az „utolsó választás
  megjegyzése" `SavedStateHandle`-ben él (nincs DataStore a version catalogban, és az új
  függőség bevezetése „korai bővítés" lenne) – forgatást és mentett állapotból történő
  folyamat-újraindítást túlél, teljes app-bezárást nem.
  **T-303:** `LearnCardsViewModel` (`feature/learn/cards`), Compose UI **nélkül** –
  ugyanaz a kettéválasztás, mint T-207/T-208 között; a kártya UI a T-304 tárgya. A seedet
  a konstruktor egyszer sorsolja az injektált `Random`-ból, és azonnal menti a
  `SavedStateHandle`-be (ADR-007 mintája, mint a `DefaultQuizGenerator`); a sorrend
  `techniques.sortedBy{id}.shuffled(Random(seed))`, kanonikusan rendezett bemeneten (a
  `TechniqueRepository.observeTechniques` sorrendje nem specifikált szerződés). A
  felfedettség **pozíció** szerint tárolt (nem technikaazonosító szerint – egy
  munkameneten belül a kettő egyenértékű, mert a sorrend nem változik), `ArrayList<String>`
  formában. Az „Előző"/„Következő" a nyers pozíciót írja, a `buildState` szorítja érvényes
  tartományba (védelmi háló, NFR-06); a felső határ tényleges letiltását a UI végzi majd
  `hasNext`/`hasPrevious`-ból. A bemenetet (`categoryIds`/`hideMode`) a `SavedStateHandle`
  két kulcsán várja, ugyanazzal a mintával, mint a `LibraryDetailViewModel.techniqueId` –
  a nevek csak a jövőbeli navigációs argumentummal egyeznek majd, a `navigation` modul
  importálása nélkül (L5). **Szándékos eltérés mindkét feladatnál a `navigation`
  modultól:** a `learn/setup` képernyő **nincs** bekötve a `KyokushinNavHost`-ba, és a
  `learn/cards` útvonalminta nem kapott argumentumot – ez a T-304 tárgya, amikor a
  célképernyő UI-ja is elkészül, és eldönthető, milyen alakban (route-argumentum vagy más)
  kapja meg a bemenetet. 20 új teszt (471 teszt, 0 hiba, `--rerun-tasks`-szal is
  leellenőrizve); `assembleDebug` exit 0.

- 2026-11-12 – **G-05 csoport kész** (T-304, T-305, T-307, `docs/backlog/GROUPS.md`,
  `feature/learn`, Fázis 5.2 **lezárva**). **T-304:** `LearnCardsScreen.kt` –
  `LearnCardsRoute` + stateless képernyő: felső sáv (pozíció/összes, az **aktuális kártya
  saját** kategórianeve – ehhez a `LearnCardsViewModel` kibővült
  `repository.observeCategories()`-szel és a `LearnCardUiModel` egy `categoryName` mezővel
  –, bezárás), a kártya (`VerificationBadge` + ábra + a nem elrejtett név, felfedés előtt
  „Koppints a felfedéshez", utána mind a három elem), alsó Előző/Következő sáv (a
  „Következő" az utolsó kártyán **sem** letiltva – ez a záróképernyőre vezet, lásd T-307),
  `stateDescription` „elrejtve"/„felfedve", mind a hat állapot-ághoz (felfedés előtt/után,
  feliratos kép rejtve, üres kijelölés, záróképernyő, betöltés) 2×`@Preview`.
  **Akadálymentességi felismerés:** felfedés előtt az ábra `IllustrationLabel`-je
  **`Decorative`**, nem `Technique` – különben a `TechniqueIllustration`
  `contentDescription`-je kimondaná a rejtett nevet, ugyanaz a kiszivárgás, mint a
  kvízopciónál, csak egy másik komponensen keresztül (`docs/CONVENTIONS.md` 4. szakasz új
  sora). **T-305:** a feliratos képek (`IllustrationRef.Asset.hideableOnStudyCard ==
  false` – a valós tartalomban KY-0045/KY-0050/KY-0052) felfedésig egy magyarázó
  helyőrzőt kapnak a kép helyén, nem a tényleges pixeleket. **T-307:** a
  `LearnCardsViewModel` egy új `LearnCardsUiState.Finished` ággal bővült – a `buildState`
  a `position >= techniques.size` esetet **nem** szorítja vissza az utolsó kártyára
  (szándékos, dokumentált viselkedésváltás a T-303-hoz képest, lásd `docs/CONVENTIONS.md`
  5. szakasz új sora), hanem a záróképernyőt adja („Végigértél `{n}` kártyán." +
  Újrakezdés/Vissza a beállításokhoz/Kvíz indítása ezekkel a kategóriákkal); `onRestart()`
  a pozíciót és a felfedettséget nullázza, a seedet nem sorsolja újra. **A `navigation`
  modul bekötötte** a `learn/setup`-ot és a `learn/cards`-ot (eddig helyőrzők voltak): a
  `learn/cards` route két lekérdezőparamétert kapott
  (`?categoryIds={categoryIds}&hideMode={hideMode}`), a nevek a `LearnCardsViewModel`
  már létező, nem-`private` kulcs-konstansaiból jönnek (nincs duplikált `NavArgs`,
  `docs/CONVENTIONS.md` 5. szakasz új sora). A „Kvíz indítása" kategóriái a
  `LearnCardsRoute`-ból kifelé át vannak adva (`onStartQuiz: (Set<CategoryId>) -> Unit`),
  de a `quiz/setup` (T-403/T-404) még nem létezik, ezért a `navigation` modul ma csak a
  helyőrzőre navigál, a kategóriahalmazt eldobja – dokumentált nyitott ügy
  (`docs/CONVENTIONS.md` 8. szakasz). A Compose döntési logika (kép elrejtése, ábraleírás,
  `stateDescription`) `@Composable`-mentes `object`-ekbe emelve
  (`LearnCardImagePolicy`/`LearnCardIllustrationLabelPolicy`/`LearnCardStateDescription`),
  így emulátor nélkül, sima JUnit/Robolectric teszttel bizonyítható (8 teszt). 13 új teszt
  összesen (471 → 484, 0 hiba, `--rerun-tasks`-szal leellenőrizve); `assembleDebug` exit 0.

  Következő: T-306, `feature/learn`: önértékelés.

- 2026-11-12 – **G-06 csoport kész** (T-306, T-308, `docs/backlog/GROUPS.md`,
  `feature/learn`, Fázis 5 **lezárva, M4 mérföldkő elérve**). **T-306:** a
  `LearnCardsViewModel` egy `progressRepository: ProgressRepository` paraméterrel bővült
  (a `RepositoryModule` kötése már megvolt a G-01 csoport óta), és `onSelfRate(rating)` +
  a rövidítései (`onKnew()`/`onNeedsPractice()`) mentik a `StudyReview`-t, majd a meglévő
  `onNext()`-et hívják (az utolsó kártyán ez is a záróképernyőre vezet). Az időbélyeg
  **nem** a ViewModelben, hanem a `RoomProgressRepository`-ban keletkezik az injektált
  `Clock`-ból (NFR-07) – a ViewModel ezért szándékosan nem kapott saját `Clock`-ot. A UI
  (`LearnCardSelfRatingRow` a `LearnCardsScreen.kt`-ban) a két gombot csak felfedés után
  engedi, letiltva egy mindig-látható indoklással – ugyanaz a minta, mint a `learn/setup`
  letiltott „Kártyák indítása" gombjánál –, hogy a TalkBack a letiltás okát is kimondja
  (T-306 „Kész ha"). Védelmi háló (ADR-005 mintája): a `onSelfRate` a jelenlegi
  `uiState.value` felfedettségét is ellenőrzi, egy fel nem fedett kártyán no-op.
  **T-308:** a tanulási logika négy tételéből három már a T-303/T-305 tesztkészletében
  lefedve volt (seedelt sorrend, felfedési állapot megőrzése, feliratos kép elrejtése) –
  csak az önértékelés + automatikus továbblépés igényelt új tesztet: 5 eset a
  `LearnCardsViewModelTest`-ben, egy új `FakeLearnProgressRepository` tesztdublőrrel
  (`LearnTestFixtures.kt`; a kvízmenethez tartozó, itt nem várt tagjai kivételt dobnak).
  5 új teszt összesen (484 → 489, 0 hiba, `--rerun-tasks`-szal leellenőrizve);
  `assembleDebug` exit 0.

  Következő: T-403, `feature/quiz`: beállító ViewModel (Fázis 6 kezdete).

- 2026-11-12 – **T-403 kész** (`feature/quiz`: beállító ViewModel, Fázis 6 kezdete).
  A `QuizSetupViewModel` a `TechniqueRepository`-ból egy, minden kategóriára szóló
  jelöltkészletet tart életben, és **minden** kijelölésváltásra (kategória, kérdésszám,
  típus) valódi `DefaultQuizGenerator.dryRun()`-t futtat – az `expectedQuestionCount`/
  `shortfall`/`canStart` közvetlenül ennek a kimenete. A típusonkénti letiltás oka
  (FR-13) egy minden-típus-engedélyezett **próba**-kéréssel készül, hogy a felhasználó
  saját kapcsolóállása ne torzítsa el a tartalmi okot. Az indítás (`onStartQuiz`) az
  injektált `Random`-ból (már a `di/QuizModule.kt`-ban, T-108 óta) sorsol seedet, megnyitja
  a munkamenetet, legenerálja a tervet ugyanazzal a seeddel, és egy `Channel`/`Flow`
  egyszeri eseményen adja ki (`QuizStartResult`, ARCHITECTURE 6.1). **Ez a feladat egy
  korábbi, rate limit miatt félbeszakadt kör munkáját fejezte be** (a kód és a fixtúrák
  már szó szerint egyeztek a `core/quiz`/`core/data` tényleges szerződésével); két
  tényleges hibát javított: (1) a letiltott gomb alatti szöveg a korábbi körben három
  kitalált `strings.xml` bejegyzésre épült, a FUNCTIONAL_SPEC 8. viszont egyetlen,
  szó szerinti `quiz.notEnoughData` mondatot ad – átírva egyetlen
  `notEnoughDataMessageRes`-re; (2) egy teszt elvárása hibás volt (3 technikából 3
  kérdést várt egy kategóriaszűrésnél, de egy négyopciós kérdéshez legalább 4 technika
  kell egy típuson belül – 0 a helyes, nem-optimista eredmény), a tesztet javítottam, a
  kódot nem. 16 új teszt (489 → 505, 0 hiba, `--rerun-tasks`-szal leellenőrizve);
  `assembleDebug` exit 0. Lásd `docs/CONVENTIONS.md` 1. és 4.1 szakasz új sorai.

  Következő: T-404, `feature/quiz`: beállító UI.
