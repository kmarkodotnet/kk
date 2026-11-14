# Implementációs terv és állapot

| | |
|---|---|
| Verzió | 1.0 |
| Utolsó frissítés | 2026-11-13 |
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
| 3 – tudástár, import, Room | T-115…T-118, T-201…T-203, T-301, T-401, T-402, T-501, T-502, T-601 | **M2** – a tartalom bent van ✅ **elérve** (T-601, 2026-11-13) |
| 4 – Tudástár UI | T-204…T-211 | **M3** – böngészhető app ✅ **elérve** (T-209, 2026-11-12) |
| 5 – Tanulás | T-302…T-308 | **M4** – tanulható app ✅ **elérve** (T-306/T-308, 2026-11-12) |
| 6 – Kvíz UI | T-403…T-415 | **M5** – kvízelhető app |
| 7 – Haladás és beállítások | T-503…T-509 | **M6** – mérhető app ✅ **elérve** (T-508, 2026-11-13) |
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
| 3.2 | A `knowledgebase/` tartalom bemásolása `app/src/main/assets/knowledge/` alá + `editorial_status` alapú `datasetTrust` (ADR-010) | CR-01, CR-03 | ✅ **kész** – a `datasetTrust` levezetése már a T-115/T-117-tel elkészült (`DefaultKnowledgeValidator.resolveDatasetTrust`, `core/quiz`); a hiányzó darab, a tényleges másolás, **T-601**-tel zárult (2026-11-13): a `knowledgebase_transfer/manifest.json`, `entries.json`, `illustrations/` (10 kép) bitre azonos, kézi másolata az `app/src/main/assets/knowledge/` alatt. Új teszt (`app`, `KnowledgeAssetImportTest`, 2 db) a valódi `AssetManager`-en és a teljes `ContentImporter`-en át igazolja a 161 rekord betöltését (`acceptedCount = 161`, `rejectedCount = 0`) és a verziókaput (FR-30). `assembleDebug` exit 0, az APK `assets/knowledge/` alatt tartalmazza mind a 10 képet, mérete ≈ 22,0 MiB |
| 3.3 | `docs/KNOWLEDGE_BASE.md` kiadott változata (séma, hozzáadás, hitelesítés, licenc, migráció) | FR-30, CR-04 | ✅ **kész** (G-13 csoport: T-608, 2026-11-13) |
| 3.4 | Room: tartalom- és felhasználói táblák, DAO-k, `@TypeConverter`-ek | DR-04, DR-07 | ✅ **kész** – T-116: `KyokushinDatabase` (`version = 1`, `exportSchema = true`), 5 tartalom- és 3 felhasználói tábla, 8 `internal` DAO (minden művelet `suspend` vagy `Flow`), `KyokushinTypeConverters` (`Instant` ↔ epoch milli, enum ↔ **név**, azonosító value classok az `of()` gyárukon át). A felhasználói táblák `techniqueId`-jén **nincs** idegen kulcs a tartalomra (ADR-004) |
| 3.5 | `UNIQUE(sessionId, questionId)` index | FR-18 | ✅ **kész** – T-116: egyedi index a `quiz_attempt` táblán (ADR-005), `OnConflictStrategy.IGNORE` beszúrással – ütközéskor `-1` sorazonosító, nem kivétel; a sémaexport és egy in-memory adatbázison futó teszt is igazolja |
| 3.6 | `ContentImporter`: DTO → validátor → upsert, idempotens, `retired` jelölés | FR-31, FR-33 | ✅ **kész** – T-117: publikus `ContentImporter` + `ImportResult`, `internal RoomContentImporter` (verziókapu a tárolt `dataset_version`-re, validáció, **upsert egyetlen tranzakcióban**, a csomagból kimaradt technika `retired = true` – fizikai törlés nincs), `content_state` összegzés, `lastValidationReport()` a diagnosztikához, injektált `Clock` és `@IoDispatcher`. Tartalom oldali entitás ↔ domain mapperek. 16 új teszt |
| 3.7 | Repository implementációk (`internal`), mapperek, aggregált statisztika query-k | NFR-03, DR-09 | ✅ **kész** – T-201: publikus `TechniqueRepository` + `ContentState` domain modell (`NotImported` / `Imported`), `internal RoomTechniqueRepository` (`@Inject`, `@IoDispatcher`). A `retired` szűrés **SQL-ben**, a listán és a kvízjelölteknél (ADR-004); a `getTechnique` a dokumentált kivétel, mert a Haladás képernyőnek a visszavont technikát is fel kell oldania (DR-07). Aliasok egyetlen lekérdezésből (nincs N+1), szinonimacsoportok a `synonym_group`-ból a szűrés utáni készletre vágva (CR-07). 12 új teszt, köztük egy reflexiós, ami a publikus API-t Room típusokra vizsgálja (TR-07). T-202: a keresés **SQL-be** költözött – előre normalizált `technique.search_romaji` / `search_hungarian` oszlopok, `LIKE '%…%' ESCAPE` a három normalizált oszlopon (alias-ág `EXISTS` altkérdéssel), az egyező alias ugyanabból a lekérdezésből a `TechniqueMatch`-ben; séma `version` 1 → 2 explicit `KyokushinMigrations.MIGRATION_1_2`-vel, `2.json` exporttal és `MigrationTestHelper` teszttel (ADR-012, DR-08). **G-01 csoport (T-301, T-401, T-402, T-502):** publikus `ProgressRepository` + `internal RoomProgressRepository` (`@Inject`, injektált `Clock`, `@IoDispatcher`) – `startSession`/`recordAttempt`/`finishSession` (T-401, a `Boolean` visszatérés és a `UNIQUE(sessionId, questionId)` ütközés `-1`-je adja az egyszeri-válasz szerződést, ADR-005; `recordAttempt` tranzakcióban írja a próbálkozást és növeli a menet `answeredCount`-ját), `recordStudyReview`/`observeStudyReviewSummary` (T-301, FR-28 – külön a kvízstatisztikától, a `StudyReviewDao.observeCountByRating` darabszámlálásával), `clearAllProgress` (T-502, egy tranzakcióban üríti a `quiz_session`-t – a `CASCADE` viszi a `quiz_attempt`-et is – és a `study_review`-t, a tartalomtáblákhoz nem nyúl). `getQuizCandidates` (T-402) immár tölti a `QuizCandidateSet.illustrationDigests` térképet a csomagolt illusztrációfájl **nevéből** (nincs SHA-256 oszlop a sémában, és becsült ujjlenyomatot nem gyártunk – a fájlnév-egyezés korrekt és elegendő, lásd `RoomTechniqueRepository` KDoc); a `quiz_excluded`/`INCOMPLETE` szűrés szándékosan **nem** a repositoryban van, azt a `QuizEligibility` kapu végzi (ADR-010), ahogy a `QuizCandidateSet` szerződése is kimondja. A kötések a `RepositoryModule`-ban és a `KyokushinGraph`/`HiltGraphTest`-ben is bekerültek. 10 új teszt (451 teszt, 0 hiba); `assembleDebug` exit 0. **T-501:** a `ProgressRepository` négy új, **olvasó**, aggregált SQL `@Query`-re épülő taggal bővült: `observeOverallProgress` (FR-23), `observeCategoryProgress` (FR-24, `category` tábláról induló két `LEFT JOIN`, hogy a próbálkozásmentes kategória is megjelenjen „nincs adat"-tal), `observeTechniqueProgress(id)` (FR-25, kérdéstípus-bontással) és `observeNeedsPractice(limit)` (FR-26, `GROUP BY techniqueId` + `ORDER BY` sikerarány, majd régiség). Minden ráta a `core/quiz` `Score` típusán megy át (DR-09: a „nincs adat ≠ 0%" szabály, FR-27, egyetlen helyen él); a mapperek (`UserEntityMappers.kt`) és a két új Room-projekció (`TechniqueScoreRow`/`TechniqueQuestionTypeScoreRow`, kettős felhasználással az egyetlen-technikás és a „gyakorlást igénylő" lekérdezések között) a `core/data/db`-ben. Nincs új számlálótábla/oszlop. A négy `feature/quiz`/`feature/learn` tesztdublőr (`FakeLearnProgressRepository`, `FakeQuizProgressRepository`, `FakeQuizRunProgressRepository`, `FakeQuizResultProgressRepository`) a bővült interfész miatt mind kapott négy új, kivételt dobó felülírást. 11 új teszt (összesen 0 hiba minden modulban); `assembleDebug` exit 0. Hátra: ezt a felületet a `feature/progress` (T-503…T-506) és a `library/detail` (T-209 nyitott pontja) köti be |
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
| 6.1 | `quiz/setup`: kategóriák, kérdésszám, típusok, **élő készenléti összegzés** (`dryRun`) | FR-11…FR-13, FR-22 | ✅ **kész** (T-403 ViewModel + T-404 UI, 2026-11-12) – `QuizSetupViewModel` + `QuizSetupScreen` (Compose) |
| 6.2 | `quiz/run`: négy kérdéstípus megjelenítése, visszajelzés ikon+szöveg+szín | FR-16, FR-17 | ✅ **kész** (G-08 csoport: T-407, T-409, 2026-11-12; majd T-408, 2026-11-12) – mind a négy kérdéstípus megjelenítése (`QuizRunScreen.kt`): az (A)/(B) szöveges és a (C)/(D) képes, a válasz-visszajelzéssel együtt |
| 6.3 | `QuizRunState` a `SavedStateHandle`-ben; egyszeri válasz; megszakítás | FR-18, FR-19 | ✅ **kész** (G-07 csoport: T-405, T-406, T-410, 2026-11-12) – `QuizRunViewModel` |
| 6.4 | `quiz/result`: összegzés, hibalista, „Hibák újragyakorlása" új munkamenetként, a lektorálatlanság jelzése | FR-20, FR-21, CR-01, CR-03 | ✅ **kész** (G-09 csoport: T-411, T-412, T-415, 2026-11-12) – `QuizResultViewModel`/`QuizResultScreen`: összegzés (sikerarány törve + százalék, „nincs adat", „megszakított"), hibás kérdések listája (típus, helyes technika romaji+magyar+ábra, saját válasz, kattintható), „Hibák újragyakorlása" (új munkamenet, új seed, kizárólag a hibás technikákból – a `ProgressRepository.getSessionResult` új, olvasó taggal), „Új kvíz”/„Vissza a Haladáshoz”. A lektorálatlanság-szalag a beállítón (T-404-ből már megvolt) és az eredményen is megjelenik, `REVIEWED` mellett automatikusan eltűnik (ADR-010). A normál befejezés (`QuizRunViewModel.onNext` az utolsó kérdésen → `finishSession` + `finishEvent`) is e csoportban készült el. |
| 6.5 | Akadálymentesség a kvízben (szemantika, a (D) opciók nem fedik fel a választ) | NFR-04 | ⬜ |
| 6.6 | **ViewModel tesztek** (dupla válasz, forgatás, megszakítás, normál befejezés, eredmény, újragyakorlás) | TR-04 | ✅ **kész** (T-414, 2026-11-12) – a G-07/G-09 csoportok tesztjei felett a hiányzó négy eset pótolva: `QuizRunViewModelTest` (19 eset: +2, nulla kérdéses/shortfall terv UI állapota, FR-22), `QuizResultViewModelTest` (10 eset: +1 félbehagyott kvíz részleges válaszokkal, +2 erősebb állítás az újragyakorlás tesztjében – új `sessionId`, a régi eredmény változatlan), `QuizResultMessagesTest` (5 eset) |

## Fázis 7 – Haladás és beállítások

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 7.1 | `progress/overview`: összesítés, kategóriabontás, gyakorlást igénylő lista | FR-23, FR-24, FR-26 | ✅ **kész** (T-503, 2026-11-13; G-10 csoport: T-504, 2026-11-13) |
| 7.2 | `progress/technique/{id}`: technikánkénti statisztika, visszavont jelölés | FR-25 | ✅ **kész** (G-10 csoport: T-506, 2026-11-13) |
| 7.3 | Önértékelési blokk elkülönítve + magyarázó szöveg | FR-28 | ✅ **kész** (G-10 csoport: T-505, 2026-11-13) |
| 7.4 | `settings`: tartalominfó, előrehaladás törlése, debug diagnosztika | FR-32, FR-35 | ✅ **kész** (G-11 csoport: T-507, 2026-11-13; önálló: T-508, 2026-11-13) – a tartalominfó, a kétlépcsős törlés és a debug-only „Tartalomdiagnosztika" (teljes `ValidationReport`) is megvan |

## Fázis 8 – Minőség, tesztek, dokumentáció, átadás

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 8.1 | Rétegszabály-ellenőrzés (import audit, `/kyo-doc-sync`, `kyo-spec-auditor`) | TR-07 | ✅ **kész** (T-602, 2026-11-13) |
| 8.2 | Manifest permission teszt | TR-08 | ✅ **kész** (T-603, 2026-11-13) |
| 8.3 | Compose UI tesztek (emulátor esetén): navigáció, tanulókártya, teljes kvíz | TR-06 | ✅ **kész** (2026-11-13) |
| 8.4 | Lint + formázás | NFR-11 | ✅ **kész** (T-607, 2026-11-13) |
| 8.5 | `docs/QUIZ_RULES.md` kiadott változata | — | ✅ **kész** (G-13 csoport: T-609, 2026-11-13) |
| 8.6 | `README.md`: előfeltételek, futtatás, build, használat, ismert korlátok | NFR-11 | ✅ **kész** (2026-11-13) |
| 8.7 | `docs/TEST_REPORT.md`: a **tényleges** parancsok és kimenetek | NFR-12 | ✅ **kész** (2026-11-13) |
| 8.8 | `./gradlew assembleDebug` → debug APK, útvonal jelentése | AC-07 | ✅ **kész** (2026-11-13) |
| 8.9 | Elfogadási kritériumok (AC-01…AC-08) végigellenőrzése | — | ✅ **kész** (2026-11-13) |
| 8.10 | Akadálymentességi átfésülés – mind a négy feature modul (`library`, `learn`, `quiz`, `progress`) | NFR-04, TR-06 | ✅ **kész** (T-604, 2026-11-13) |

**Ezzel az eredeti M1…M7 szállítási kör (T-101…T-611) lezárult.** A lenti Fázis 9 egy
**új, 2026-11-13-i igénysor** – nem az eredeti mérföldkövek folytatása.

---

## Fázis 9 – Bővítési backlog: vizuális arculat, navigáció, tartalom (2026-11-13)

> Lásd `docs/BACKLOG.md` „Bővítési backlog" szakasza (E7, E8, E9) és
> `docs/REQUIREMENTS.md` 3.6/10. szakasza (FR-36…FR-42, OQ-05…OQ-07). A taskok
> `docs/backlog/T-701`-től `T-711`-ig vannak felvéve.

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 9.1 | Indítóképernyő – kék kandzsi logó fehér háttéren (`asserts/kyo.png` vektorizálva: `asserts/kyo_kanji.svg`, `asserts/ic_kyo_kanji.xml`) | FR-36 | ✅ **kész** (T-701, 2026-11-13) |
| 9.2 | Vizuális arculat terv ("ne hasson webes űrlapnak") | FR-36 | ✅ **kész** (T-702, 2026-11-13) – `docs/DESIGN_NOTES.md` |
| 9.2b | `core/ui`: a terv (9.2) kódba ültetése – `Typography`/`Shapes` | FR-36, CR-08 | ✅ **kész** (T-712, 2026-11-14) |
| 9.3 | Kezdőoldal (Főoldal) a logóval + navigáció | FR-37 | ✅ **kész** (T-703, 2026-11-14) – új `feature/home` modul |
| 9.4 | Alsó navigáció sorrendje – Tudástár legjobboldalra | FR-38 | ✅ **kész** (T-704, 2026-11-14) |
| 9.5 | Tanulás – kategória-ikonok, technikalista drill-down | FR-39 | ✅ **kész** (T-705, 2026-11-14) |
| 9.6 | Tanulás – felső tab "Japán nevek"/"Magyar nevek" | FR-40 | ✅ **kész** (T-706, 2026-11-14) |
| 9.7 | Illusztráció-befogadási munkafolyamat | FR-41 | ✅ **kész** (T-707, 2026-11-14) – `docs/KNOWLEDGE_BASE.md` 6a. pontja |
| 9.8 | Fordítások ellenőrzése hiteles forrásokból | FR-42 | 🔴 **nyitott** – T-708 |
| 9.9 | Japán kiejtés lejátszása | — | ⛔ **jövőbeli, nem indítható** – T-709 (OQ-05) |
| 9.10 | Kiejtés-alapú kvízkérdés-típus | — | ⛔ **jövőbeli, nem indítható** – T-710 (OQ-05, OQ-06) |
| 9.11 | Többnyelvűsítés (EN/DE) | — | ⛔ **jövőbeli, nem indítható** – T-711 (OQ-07) |

---

## Aktuális blokkolók

| # | Blokkoló | Mit blokkol | Mi oldja fel |
|---|---|---|---|
| ~~B-01~~ | ~~Nincs JDK~~ | — | ✅ **feloldva**: Temurin JDK 17.0.21 telepítve |
| ~~B-02~~ | Android SDK | — | ✅ platform 37 / Build Tools 37.0.0, működő build |
| ~~B-03~~ | Emulátor | — | ✅ T-606: API 37 AVD, 6 sikeres eszközteszt |
| ~~B-04~~ | Verziómátrix | — | ✅ ADR-008, működő build és tesztek |
| OQ-01 | Nincs hitelesített karate tartalom | az éles adatkészlet; a (C)/(D) kvíztípus aktiválása | ✅ döntés: demó adatkészlettel indulunk; az éles tartalom később importtal (FR-34) |

A maradék blokkolók **nem** állítják le a munkát: a Fázis 2.1–2.7 és 3.1–3.3 kódja és
tartalma megírható; csak az Android-fordítás és az UI tesztek várnak az SDK-ra.

## Következő lépés

**Az eredeti M1…M7 szállítás (T-101…T-611) lezárult.** A Fázis 9 (fent) egy új
igénysor: a T-701…T-708 és a T-712 mind elkészült, ezzel az E7 és E8 epik is
lezárult. A `docs/backlog/`-ban **nincs** `/dev`-del indítható feladat – a
T-709…T-711 szándékosan **nem** indítható (lásd a Fázis 9 táblázatát és
`docs/backlog/README.md`).

<details>
<summary>Korábbi (M1…M7-höz tartozó) "Következő lépés" bejegyzés – archív</summary>

1. **T-507** – `feature/progress`: beállítások (G-11 csoport első tagja, T-509-cel).
   Függőségei (T-502, T-503) készek.
   (**G-10 csoport – T-504, T-505, T-506 – kész, 2026-11-13**, lásd lejjebb.)

(**A T-501 kész, 2026-11-13:** a `ProgressRepository` négy aggregált, olvasó taggal
bővült (`observeOverallProgress`/`observeCategoryProgress`/`observeTechniqueProgress`/
`observeNeedsPractice`, FR-23…FR-27, DR-09) – lásd a 3.7 sor részletezését. Ezzel a
`library/detail` (T-209) „már gyakoroltam" összegzése és a `feature/progress` teljes
epikja (T-503…T-506) immár az adatrétegre épülhet; mindkettő önálló, még hátralévő
feladat.)

(**A G-09 csoport – T-411, T-412, T-415 – kész, 2026-11-12:** a `quiz/result`
eredményképernyő, a „Hibák újragyakorlása" és a lektorálatlanság jelzése. A
`ProgressRepository` egy új, **olvasó** taggal bővült: `getSessionResult(sessionId):
QuizSessionResult?` – egyetlen, már ismert menet visszaolvasása (FR-20, FR-21), **nem**
a T-501 aggregált, több-munkamenetes statisztikája (az változatlanul hátravan). A
`QuizRunViewModel.onNext()` az utolsó kérdésen immár lezárja a menetet
(`finishSession`) és kiadja a `finishEvent`-et – a `quiz/run`/`quiz/result` közötti
tényleges navigáció (a `navigation` modul bekötése) továbbra sincs megoldva, lásd a
`QuizResultViewModel`/`QuizRunViewModel` KDoc-ja és `docs/CONVENTIONS.md` 5. szakasza:
a `quiz/setup`/`quiz/run`/`quiz/result` teljes navigációs gráfja egyetlen, önálló,
`navigation`-t érintő feladatra van hagyva.)

(**A G-01 csoport – T-301, T-401, T-402, T-502 – kész, ezzel a `ProgressRepository`
írási/törlési felülete is megvan:** `startSession`/`recordAttempt`/`finishSession`
(T-401, `Boolean` visszatérés az egyszeri válaszhoz, ADR-005), `recordStudyReview`/
`observeStudyReviewSummary` (T-301, FR-28), `clearAllProgress` (T-502, FR-35), és a
`getQuizCandidates` immár az `illustrationDigests` térképet is tölti (T-402, a csomagolt
fájl nevéből, lásd `docs/CONVENTIONS.md`). **Nyitott, immár a T-501 utánra felszabadult
hiányosság:** a `library/detail` „már gyakoroltam" összegzése (FUNCTIONAL_SPEC 3.2 11.)
még nincs bekötve – a T-501 (2026-11-13, lásd lejjebb) már tartalmazza a szükséges
`observeTechniqueProgress`-t, de a `LibraryDetailUiState`/`LibraryDetailRoute` tényleges
bekötése önálló, még hátralévő feladat.)

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

</details>

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

- 2026-11-12 – **T-404 kész** (`feature/quiz`: beállító UI, Fázis 6.1 lezárása).
  `QuizSetupScreen.kt` – stateless `QuizSetupScreen` + a `QuizSetupViewModel`-t látó
  `QuizSetupRoute` (ugyanaz a minta, mint a `LearnSetupScreen`/`LearnSetupRoute`): a
  kategórialista jelölőnégyzettel és „`{kvízképes}` kvízképes / `{összes}` technika"
  darabszámmal (FUNCTIONAL_SPEC 5.1/2.), `SingleChoiceSegmentedButtonRow` az 5/10/20
  kérdésszámhoz (FUNCTIONAL_SPEC 5.1/3., FR-11 – ez az első szegmentált választó a
  projektben, Material3 1.4.0 stabil API), négy kérdéstípus jelölőnégyzete, letiltott
  sor alatt mindig látható, konkrét darabszámos indoklással (FR-13, ugyanaz a minta, mint
  a `LearnCardSelfRatingRow`-nál: nem `stateDescription`, hanem normál bejárási sorrendű
  szöveg), és az élő készenléti összegzés + a 0 kérdéses eset javaslata a gomb felett
  (FR-22). A „Kvíz indítása" gomb eredménye (`QuizSetupViewModel.startEvent`) a
  `QuizSetupRoute`-ból **kifelé** adott lambda (`onStartQuiz: (QuizStartResult) -> Unit`),
  nem közvetlen navigáció (L5) – a tényleges `navigation` modul bekötés (navigálás a
  `quiz/run/{sessionId}`-re és a `QuizPlan` átadása) **szándékosan nincs** ebben a
  feladatban, mert a `quiz/run` képernyő még `PlaceholderScreen` (nem fogadna `QuizPlan`-t)
  és a `QUIZ_SETUP` route-nak a `learn/cards`-ból érkező kategóriák átvétele is hátravan –
  lásd `docs/CONVENTIONS.md` 5. szakasz új sora a pontos nyitott teendőről. Két
  `@Composable`-mentes, Robolectric-kel (a tényleges `strings.xml` fölött) tesztelt
  segédfüggvény (`QuizSetupMessages.kt`): `questionTypeLabelRes` (a négy kérdéstípus
  felirata, szó szerint a FUNCTIONAL_SPEC 8. `quiz.type.*` kulcsaiból) és
  `quizReadinessSummary` (a „ennyi kérdés áll össze" / hiány-szöveg döntése, szó szerint a
  FUNCTIONAL_SPEC 5.1/5. és a `quiz.shortfall` kulcs mondatából). 4 új teszt
  (505 → 509, 0 hiba, `--rerun-tasks`-szal leellenőrizve); `assembleDebug` exit 0.
  `@Preview` világos + sötét, három `Content` variánsra (indítható, shortfall, 0 kérdés
  letiltva) + `Loading`/`Error`. Lásd `docs/CONVENTIONS.md` 4.1 és 5. szakasz új sorai.

  Következő: T-405, `feature/quiz`: `QuizRunState` (`SavedStateHandle`, FR-18, FR-19).

- 2026-11-12 – **G-07 csoport kész** (T-405, T-406, T-410, `docs/backlog/GROUPS.md`,
  `feature/quiz`, Fázis 6.3/6.6 – a futó kvíz állapotgépe). **T-405:** új
  `QuizRunStateCodec` a `core/quiz`-ben – ez az **első** hely, ahol a modul `main`
  forráskészlete tényleges `Json`-t használ (a `QuizPlan`/`QuizQuestion` már a T-107 óta
  `@Serializable`, de kódolatlanul), mert a `feature/quiz` `build.gradle.kts`-e kifejezetten
  tiltja a `kotlinx.serialization` típusokat (ADR-001, L3) – a ViewModel ezért csak
  `encodePlan`/`decodePlan`/`encodeAnswers`/`decodeAnswers` néven kap/ad sima `String`-et.
  Az új `feature/quiz/run` csomagban `QuizRunState` (sessionId, seed, questions,
  currentIndex, answers, shortfall – pontosan az ARCHITECTURE 6.2 alakja, de **nem**
  `@Serializable`, lásd fent), `QuizRunUiState` (`Loading`/`Empty`/`Content`) és a
  `QuizRunViewModel`. **T-406:** `onAnswer` két védelmi vonala – a ViewModel saját
  `answers`-map-ellenőrzése (első, no-op, mielőtt a `ProgressRepository`-t elérné) és a
  `recordAttempt` `false` válasza (második, DB-szintű ütközés – nem módosítja az
  állapotot); a helyesség mindig `selected == question.correctTechniqueId`
  azonosító-összehasonlítás (DR-03). **T-410:** `onNext` (előre, nincs `onPrevious`, az
  utolsó kérdésen no-op – a kvíz **normál** befejezése a G-09 csoport (T-411) tárgya,
  szándékosan dokumentált hiány) és a megszakítás három metódusa (`onRequestClose`/
  `onCancelClose`/`onConfirmClose`): a bezárás (×) és a rendszer vissza-gombja
  **ugyanazt** a `onRequestClose`-t hívja majd (a tényleges `BackHandler`/ikongomb egy
  UI-t adó feladat tárgya), az `onConfirmClose` szándékosan **nem** hív
  `finishSession`-t, mert a megszakított menet `finishedAt`-ja `null` marad
  (FUNCTIONAL_SPEC 5.2) – az addig rögzített válaszok automatikusan megmaradnak, nincs
  mit visszavonni.
  **Ez a csoport szándékosan nem ad Compose UI-t** (sem a kérdés-UI-t, sem a
  progresszsáv/megerősítő-párbeszéd pixeleit) – a G-07 cím szerint is „a futó kvíz
  állapotgépe", a tényleges rajzolás a T-407/T-408 (kérdéstípusok), T-409 (visszajelzés)
  és egy még hátralévő `BackHandler`/`AlertDialog` feladat dolga, ugyanaz a mintázat, mint
  a T-303 (ViewModel) → T-304 (UI) a `feature/learn`-ben. A `QuizRunViewModel` bemenete
  (sessionId/seed/terv) ezért ma még kézzel összerakott `SavedStateHandle`-ből jön a
  tesztekben – a tényleges `navigation` bekötés (hogyan jut el a `QuizStartResult` a
  `quiz/run` képernyő `SavedStateHandle`-jébe) is dokumentált, nyitott ügy, lásd a
  `QuizRunViewModel` KDoc-ja és `docs/CONVENTIONS.md`.
  A `QuizStartResult` (`feature/quiz/setup`) egy `seed: Long` mezővel bővült, hogy a
  `QuizRunState.seed` is megkapja az eredeti, injektált RNG-ből sorsolt magot (a
  `QuizSetupViewModel.onStartQuiz` már sorsolta, csak eddig nem adta tovább) – a
  `QuizSetupViewModelTest` egy sora frissült, kódmódosítás nélkül mást nem érintett.
  19 új teszt (509 → 528, 0 hiba, `--rerun-tasks`-szal leellenőrizve): 5 a
  `QuizRunStateCodecTest`-ben (`core/quiz`) és 14 a `QuizRunViewModelTest`-ben
  (`feature/quiz`); `assembleDebug` exit 0. Lásd `docs/CONVENTIONS.md` 1. szakasz új sora
  (a `SavedStateHandle`-be mentett komplex állapot kódolásának mintája).

  Következő: T-407, `feature/quiz`: az (A)/(B) kérdéstípus UI-ja (G-08 csoport).

- 2026-11-12 – **G-08 csoport kész** (T-407, T-409, `docs/backlog/GROUPS.md`,
  `feature/quiz`, Fázis 6.4 – az (A)/(B) kérdés UI és a válasz-visszajelzés). A két
  feladat egy menetben készült, mert ugyanazt a képernyőt adja. A G-07 állapotgépe
  (T-405/T-406/T-410) fölött a `QuizRunViewModel` egy harmadik függőséget kapott
  (`TechniqueRepository`): az `uiState`-et építő `combine` egy harmadik forrással
  bővült (`observeTechniques()`), és a `QuizRunUiState.Content` egy új
  `options: List<QuizOptionDisplay>` mezőt kapott – a kérdés opcióinak romaji/magyar/ábra
  adatai, **ugyanabban a sorrendben**, mint a `question.optionTechniqueIds` (ADR-007). Ha
  egy opció technikája nem oldható fel, `QuizRunUiState.Empty` (NFR-06), nem kivétel.
  Új `QuizRunScreen.kt`: `QuizRunRoute`/`QuizRunScreen`, `QuizRunTopBar`
  (progresszszöveg + bezárás (×) + lineáris jelző), `QuizTextQuestion` ((A)/(B), a (C)/(D)
  ág szándékosan üres – T-408 tárgya), `QuizOptionRow` (a válasz-visszajelzés: ikon+szöveg+
  szín+`stateDescription`, a mindig kiemelt helyes opció **ugyanazt** a jelzést kapja,
  mint a kiválasztott helyes opció), `QuizExplanationCard` (romaji+magyar+ábra, kattintható,
  `onOpenTechnique` kifelé adott lambda, US-14/3.), és egy `BackHandler`+`AlertDialog` a
  megszakítás megerősítésére (a `QuizRunViewModel` KDoc-ja ezt kifejezetten ide, a
  kérdés-UI-t adó feladatra hagyta). Új, emulátor nélkül tesztelhető tiszta logika:
  `QuizOptionFeedback` enum + `QuizOptionFeedbackResolver`. Két új, a version catalogban
  már szereplő függőség a `feature/quiz`-ben: `androidx.activity:activity-compose`
  (`BackHandler`) és a `material-icons-extended` (ikonok). 7 új teszt (528 → 535, 0 hiba,
  `--rerun-tasks`-szal leellenőrizve); `assembleDebug` exit 0. Lásd
  `docs/CONVENTIONS.md` 4.1 szakasz öt új sora.

  Következő: T-408, `feature/quiz`: a (C)/(D) kérdéstípus UI-ja.

- 2026-11-12 – **T-408 kész** (`docs/backlog/GROUPS.md` szerint szándékosan **önálló**,
  nem csoporttag – „a képes kérdéstípusok akadálymentessége önálló gondolkodást kíván").
  A `QuizRunScreen.kt`-ban eddig üres `Unit` ág (`IMAGE_TO_JP`/`JP_TO_IMAGE`, G-08
  lezárása) megkapta a tényleges Compose UI-t: `QuizImageToJpQuestion` ((C): „Melyik
  technika ez?" + a helyes opció illusztrációja + 4 `QuizOptionRow` romaji felirattal –
  a meglévő (A)/(B) opciógomb újrahasznosítva) és `QuizJpToImageQuestion` ((D): „Melyik
  ábra: **`{romaji}`**?" + 2×2 képrács `QuizImageOptionCard`-dal, a meglévő
  `QuizOptionFeedbackResolver`/ikon+szöveg+szín visszajelzés-logika újrahasznosítva kép
  alatti felirattal).
  **Akadálymentesség (NFR-04, a feladat valódi súlypontja):** a (C) kérdésábra a HELYES
  opció illusztrációja – ha a leírása a technika nevét mondaná ki, a TalkBack idő előtt
  elárulná a választ. Erre a `core/ui` `IllustrationLabel` sealed típusa (T-204) még nem
  adott ágat (csak `Technique`, `QuizOption`, `Decorative` – ebből `Technique` elárulná a
  nevet, `Decorative` pedig a FUNCTIONAL_SPEC szerint kvízben nem használható, mert a kép
  itt információt hordoz). Új ág: `IllustrationLabel.QuizPrompt` – pozíció nélküli,
  harmadik, semleges leírás („A kvízkérdés ábrája", `illustration_a11y_quiz_prompt`),
  amit az `IllustrationContentDescription.forAsset`/`forPlaceholder` old fel; a (D) rács
  változatlanul a már T-204-ben elkészült `IllustrationLabel.QuizOption(position)`-t
  használja (pozíció alapú, a technika nevét nem tartalmazza).
  **Új, `@Composable`-mentes, sima JUnit-tal tesztelt logika:** `QuizImageOptionGrid`
  (`feature/quiz/run`) – a 2×2 rács pozíció-hozzárendelése (1-alapú, az `options` lista
  EREDETI sorrendjéből, nem rendezi újra), ugyanaz a „Compose döntési logika tesztelése
  emulátor nélkül" minta, mint a `QuizOptionFeedbackResolver`.
  4 új teszt a `feature/quiz`-ben (`QuizImageOptionGridTest`) + 3 új teszt a `core/ui`-ban
  (`IllustrationContentDescriptionTest`, a `QuizPrompt` ág), 535 → **542**, 0 hiba
  (`--rerun-tasks`-szal leellenőrizve, teljes projekt); `assembleDebug` exit 0. Négy új
  `@Preview` pár (világos/sötét, (C) és (D), válasz előtt/után) + egy `widthDp = 400`
  pár a (D) rácshoz (a „Kész ha": „Keskeny képernyőn (≈400 dp) is használható a rács").
  Lásd `docs/CONVENTIONS.md` 4. és 4.1 szakasz új sorai.

  Következő: T-411, `feature/quiz`: a `quiz/result` eredményképernyő (G-09 csoport).
- 2026-11-12 – **G-09 csoport kész (T-411/T-412/T-415, Fázis 6.4):** a `quiz/result`
  eredményképernyő, a hibák újragyakorlása és a lektorálatlanság jelzése.
  `QuizResultViewModel` + `QuizResultScreen` (Compose): nagy összegzés (`{helyes}/
  {megválaszolt} helyes` + százalék, két külön `Text`-ben – NFR-04 „nem csak diagramon"),
  a „0 válasz”/„megszakított kvíz” variáns (`@Composable`-mentes `quizResultSummary`,
  `QuizResultMessages.kt`, ugyanaz a döntés-kiemelési minta, mint a `QuizSetupMessages`-nél),
  hibás kérdések listája (típus, helyes technika romaji+magyar+ábra, saját válasz,
  kattintható → technika-részletező), „Hibák újragyakorlása”/„Új kvíz”/„Vissza a
  Haladáshoz” gombok.
  **Új `ProgressRepository` tag:** `getSessionResult(sessionId): QuizSessionResult?` –
  **egyetlen, már ismert menet** visszaolvasása (FR-20), **nem** a T-501 aggregált
  statisztikája; `RoomProgressRepository`-ban `QuizSessionDao.getById` +
  `QuizAttemptDao.getBySession` + egy új entitás→domain mapper (`UserEntityMappers.kt`,
  a `ContentEntityMappers.kt` mintájára – eddig nem volt rá szükség, mert a repository
  csak írt/törölt, sosem olvasott vissza domain objektumot).
  **„Hibák újragyakorlása” (T-412, FR-21):** a kvízmotor `QuizRequest`-je csak
  kategória szerint szűr – nincs technikaszintű allow-list. A `QuizResultViewModel`
  ezért egy **kizárólag** a hibás technikákat tartalmazó `QuizCandidateSet`-et épít
  (társ-technika hozzáadása nélkül), így a correct-answer pool ÉS a distraktorpool is
  garantáltan csak a hibás technikákból áll össze (US-16 AC-1: „ugyanazokra a
  technikákra”). Ennek következménye a motor már dokumentált, nem-optimista szabálya:
  4-nél kevesebb egyedi hibás technikával 0 kérdés állna össze – ilyenkor egy előzetes
  `dryRun()` (ugyanaz a minta, mint a `quiz/setup`-nál) megakadályozza egy üres
  munkamenet megnyitását.
  A `quiz/run` normál befejezése (eddig a G-07-ben szándékosan no-op, lásd
  `QuizRunViewModel.onNext` korábbi KDoc-ja) is e csoportban készült el:
  `finishSession` + egy új `finishEvent` (`QuizRunViewModel`/`QuizRunRoute`).
  **A lektorálatlanság jelzése (T-415):** a `quiz/setup` figyelmeztető sora a T-404-ben
  **már elkészült** (`UnreviewedBanner(trust = state.datasetTrust)`) – ez a csoport csak
  az eredményképernyőn adta hozzá, ugyanazzal a komponenssel (ADR-010, nem elrejthető,
  `REVIEWED` mellett automatikusan eltűnik).
  Új tesztek: `RoomProgressRepositoryTest` (+3, `getSessionResult`), `QuizRunViewModelTest`
  (+2, a normál befejezés), `QuizResultViewModelTest` (9), `QuizResultMessagesTest` (5).
  `.\gradlew.bat testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, 0 hiba);
  `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`).
  *Nyitva marad:* a `quiz/setup`/`quiz/run`/`quiz/result` navigációs gráfja (a
  `navigation` modul bekötése) – lásd a „Következő lépés” szakasz.

  Következő: T-413, `feature/quiz`: akadálymentesség.

**2026-11-12 – T-413 kész:** `feature/quiz`: akadálymentesség. A Teendők nagy része
(opciók `selected`/`stateDescription`, (D) pozíció alapú leírás, bejárási sorrend,
48 dp célpont) **már megvolt** a T-407/T-408/T-409/T-411/T-412 saját UI-munkájából –
ez a feladat kódolvasással, egyenként igazolta mindegyiket (lásd
`docs/done/T-413-feature-quiz-akadalymentesseg.md` „Elkészült” szakasza). Egy valódi
hiányt talált és javított: a `QuizCloseConfirmationDialog` (`QuizRunScreen.kt`)
`AlertDialog`-jának két `TextButton`-ja a Material 3 alapértelmezett (~40 dp)
magasságot kapta, nem a projekt 48 dp-es szabályát – mindkettő explicit
`Modifier.heightIn(min = MIN_TOUCH_TARGET)`-et kapott. Új, `internal`-ra nyitott
leképező függvények (`quizOptionIcon`/`quizOptionFeedbackTextRes`/
`quizOptionStateDescriptionRes`, korábban `private`) + egy új
`QuizOptionAccessibilityMappingTest` (6 teszt) bizonyítja emulátor nélkül, hogy válasz
előtt és egy nem kiválasztott helytelen opciónál nincs `stateDescription`/ikon/szöveg.
`.\gradlew.bat testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, 566 teszt,
0 hiba, 560 → 566); `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`). TR-06
(TalkBack emulátoron) a B-03 blokkoló miatt nem futott.

Következő: T-414, `feature/quiz`: ViewModel tesztek (a `docs/backlog/GROUPS.md`
szerint szándékosan nem csoportosítva a T-413-mal; a legkisebb azonosítójú, kész
függőségű feladat T-414 és T-501 közül).

**2026-11-13 – T-501 kész (Fázis 3.7): `core/data` aggregált statisztika `@Query`-k.**
A `docs/backlog/GROUPS.md` szerint szándékosan **önálló** (`L` méretű, aggregált SQL).
A `ProgressRepository` négy új, **olvasó** taggal bővült, mindegyik aggregált SQL
`@Query`-ből, nem memóriabeli listából (DR-09, NFR-10):
- `observeOverallProgress()` (FR-23) – `COUNT(*)`/`COALESCE(SUM(correct), 0)` a teljes
  `quiz_attempt` táblán.
- `observeCategoryProgress()` (FR-24) – a `FROM category` indul, két `LEFT JOIN`-nal
  (`category → technique → quiz_attempt`), hogy **minden** kategória megjelenjen, a
  próbálkozásmentes is „nincs adat"-tal (nem `0%`-kal, FUNCTIONAL_SPEC 6.1/3.).
- `observeTechniqueProgress(id)` (FR-25) – egyetlen technika összesítése +
  kérdéstípus-bontás (két `@Query` `combine`-olva).
- `observeNeedsPractice(limit)` (FR-26) – `GROUP BY techniqueId` (ettől magától csak a
  ≥ 1 próbálkozású technika kerül bele), `ORDER BY` sikerarány növekvő, majd
  `MAX(answeredAt)` növekvő (a legrégebben gyakorolt elöl); a kérdéstípus-bontást egy
  második, `IN (:techniqueIds)` lekérdezés adja a top-N azonosítóra (nem N+1).

Minden ráta a `core/quiz` már tesztelt `Score` típusán megy át (DR-09: a „nincs adat ≠
0%" szabály, FR-27, egyetlen helyen él, nem egy második SQL-beli megfogalmazásban) – két
új, kettős felhasználású Room-projekció (`TechniqueScoreRow`/
`TechniqueQuestionTypeScoreRow`, `UserEntities.kt`) szolgálja ki egyszerre az
egyetlen-technikás és a „gyakorlást igénylő" lekérdezést, a mapperek (`toScore`/
`toTechniqueProgress`, `UserEntityMappers.kt`) pedig mindkét hívási útra ugyanazt az
összeállítást futtatják. Nincs új számlálótábla/oszlop, nincs séma-/migrációváltozás.

A bővült interfész miatt mind a négy `ProgressRepository` tesztdublőr
(`FakeLearnProgressRepository`, `FakeQuizProgressRepository`,
`FakeQuizRunProgressRepository`, `FakeQuizResultProgressRepository`) négy új, kivételt
dobó felülírást kapott (ugyanaz a „nem várt hívás azonnal kiderül" minta, mint a
meglévő tagjaiknál).

Új tesztek: `RoomProgressRepositoryTest` (+11, in-memory Roomon – „nincs adat" mindkét
granularitáson, kategóriák közötti csoportosítás, kérdéstípus-bontás, a „gyakorlást
igénylő" lista rendezése és limitje). `.\gradlew.bat testDebugUnitTest --rerun-tasks`
**exit 0** (teljes projekt, minden modul 0 hiba); `.\gradlew.bat assembleDebug`
**exit 0** (`app-debug.apk`).

*Nyitva marad:* ezt a felületet még nem köti be UI – a `feature/progress` epikja
(T-503…T-506) és a `library/detail` (T-209) „már gyakoroltam" összegzése mindkettő
önálló, hátralévő feladat.

**2026-11-13 – T-503 kész (Fázis 7 kezdete, E5 epik): `feature/progress` áttekintő
képernyő.** A `docs/backlog/GROUPS.md` szerint szándékosan **önálló**: ez a képernyő a
G-10 csoport (T-504/T-505/T-506) kerete – a „Gyakorlást igénylő" lista és az
önértékelési blokk **nincs** benne, azokat a két soron következő feladat adja hozzá.

- `ProgressOverviewViewModel` a `ProgressRepository.observeOverallProgress()` +
  `observeCategoryProgress()` és a `TechniqueRepository.observeCategories()` +
  `observeContentState()` négyes `combine`-ja (FR-23, FR-24): a kategórianevet és a
  sorrendet a `TechniqueRepository` adja, a pontszámot a `ProgressRepository` –
  `CategoryId` szerint párosítva, nem index szerint.
- `progressScoreDisplay(score)` (`ProgressMessages.kt`) a „0 megválaszolt kérdés → nincs
  adat, nem 0%" (FR-27) elágazás egyetlen, `@Composable`-mentes megfogalmazása – ezt
  hívja mind az összesítő kártya, mind egy kategóriasor (ugyanaz a minta, mint a
  `feature/quiz` `quizResultSummary`-je, CONVENTIONS 4.1).
- `ProgressOverviewScreen`: lektorálatlanság-szalag (ADR-010 – a táblázat „Haladás”
  sora eddig nem volt bekötve, ez zárta le), összesítő kártya (nulla próbálkozásnál a
  kártya **helyén** „Még nincs adat. Indíts egy kvízt!” + gomb, `progress.noData` szó
  szerint a FUNCTIONAL_SPEC 8.-ból), kategóriánkénti bontás sávval és szöveges értékkel,
  „Beállítások” link. **Próbálkozás nélküli kategóriánál szándékosan nincs sáv** – egy
  `0f` értékű `LinearProgressIndicator` a Material 3 beépített szemantikájával „0
  százalék"-ot jelentene be a TalkBacknek, pontosan azt a félreértést keltve, amit az
  FR-27 kizár (lásd a komponens KDoc-ja és az új CONVENTIONS-sor).
- Navigációs bekötés: a `KyokushinNavHost` `progress/overview` ága mostantól
  `ProgressOverviewRoute`-ot hív (nem helyőrzőt) – „Kvíz indítása” → `quiz/setup`,
  „Beállítások” → `settings` (mindkettő már létező útvonal/helyőrző). A már nem
  hivatkozott `PLACEHOLDER_TECHNIQUE_ID` konstans eltávolítva.

Új tesztek: `ProgressMessagesTest` (4) + `ProgressOverviewViewModelTest` (5) – nincs
`SavedStateHandle` (a route nem vesz át argumentumot), ezért Robolectric sem kellett.
`.\gradlew.bat testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, 587 teszt,
0 hiba, 578 → 587); `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`).

Következő: T-504, `feature/progress`: gyakorlást igénylő lista (G-10 csoport, T-503
függősége teljesült).

- 2026-11-13 – **G-10 csoport kész (T-504, T-505, T-506):** a Haladás áttekintő két
  hiányzó alszakasza és a technika-részletező.
  - **T-504 – „Gyakorlást igénylő" lista (FR-26).** `ProgressOverviewUiState.Content`
    új `needsPractice: List<ProgressNeedsPracticeItem>` mezőt kapott; a
    `ProgressOverviewViewModel` a `ProgressRepository.observeNeedsPractice(10)`-et a
    `TechniqueRepository.observeTechniques()`-ből feloldott romaji/magyar/ábra
    hármassal párosítja `techniqueId` szerint. Egy visszavont, de korábban gyakorolt
    technika **kiesik** ebből a listából (az `observeTechniques()` csak az aktívakat
    adja) – a teljes historikus adat a T-506 részletezőn marad elérhető (DR-07). Üres
    lista → „Még nincs elég adat a javaslathoz." Tétel kattintható →
    `progress/technique/{techniqueId}`.
  - **T-505 – önértékelési blokk (FR-28).** `ProgressOverviewUiState.Content` új
    `studyReviewSummary: StudyReviewSummary` mezőt kapott (a T-301-ben már megírt
    `ProgressRepository.observeStudyReviewSummary()`-ból) – külön, elhatárolt
    szakasz, a FUNCTIONAL_SPEC 8. szó szerinti magyarázó sorával.
  - **T-506 – technika-részletező (FR-25, DR-07).** Új `feature/progress/technique`
    csomag: `ProgressTechniqueViewModel` (a `feature/library`
    `LibraryDetailViewModel` mintáját követi: `SavedStateHandle`-ből olvasott
    `techniqueId`, `TechniqueRepository.getTechnique()` – ami szándékosan a
    visszavont technikát is visszaadja) + `ProgressTechniqueScreen`. Azonosító adatok
    + „Megnyitás a Tudástárban" link, kvízstatisztika kérdéstípus-bontással,
    önértékelési előzmény külön blokkban, visszavont technika explicit jelzése (a
    statisztika ekkor is látható marad). Bekötve a `KyokushinNavHost`
    `progress/technique/{techniqueId}` ágába (a `PlaceholderScreen` helyén).
  - **`core/data` bővítés:** a `ProgressRepository` új tagja,
    `observeStudyReviewHistory(techniqueId): Flow<List<StudyReview>>` (a T-506
    technikánkénti önértékelési előzményéhez) – a `StudyReviewDao.observeForTechnique`
    (új, `Flow`-visszatérésű testvérmetódus a meglévő `suspend getForTechnique`
    mellett) + egy új `StudyReviewEntity.toStudyReview()` mapper adja. Mind az öt
    `ProgressRepository` tesztdublőr kapott egy új felülírást (CONVENTIONS 1. szakasz
    „Egy `ProgressRepository`-bővítés minden tesztdublőrjét érinti").

Új tesztek: `ProgressFormattingTest` (2), `ProgressNeedsPracticeAccessibilityTest` (2),
`ProgressOverviewViewModelTest` (+4 → 9), `ProgressTechniqueLabelsTest` (2),
`ProgressTechniqueViewModelTest` (7), `RoomProgressRepositoryTest` (+2 → 23).
`.\gradlew.bat testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, 606 teszt,
0 hiba, 587 → 606); `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`).

Következő: T-507, `feature/progress`: beállítások (G-11 csoport első tagja, T-509-cel;
a T-502/T-503 függőségek teljesültek).

- 2026-11-13 – **G-11 csoport kész (T-507, T-509):** a Beállítások (`settings`)
  képernyő és a hozzá tartozó ViewModel-tesztek.
  - **T-507 – Beállítások (FR-32, FR-35, NFR-09).** Új `feature/progress/settings`
    csomag: `ProgressSettingsViewModel` + `ProgressSettingsUiState` +
    `ProgressSettingsScreen`. A tartalominfó
    (`schemaVersion`/`contentVersion`/szerkesztői állapot/rekord- és kategóriaszám) a
    `TechniqueRepository.observeContentState()` + `.observeCategories()`-ből jön –
    **teljesen független** forrás a törléstől. Az „Előrehaladás törlése" kétlépcsős
    megerősítést kér (`ClearProgressStep.NONE → FIRST_WARNING → FINAL_WARNING`, a
    `SavedStateHandle`-ben tárolva, ugyanazzal a mintával, mint a `QuizRunViewModel`
    `closeConfirmationVisible` mezője): az első párbeszéd felsorolja, mi törlődik
    (kvízpróbálkozások, munkamenetek, önértékelések) és mi nem (a tudástár), a második
    a végső megerősítés – mindkettő kimondja, hogy a művelet nem visszavonható. Csak a
    végső megerősítés hívja a `ProgressRepository.clearAllProgress()`-t (T-502-ből).
    **Nyitva hagyva, dokumentáltan** (lásd `docs/CONVENTIONS.md` 5. szakasz): a
    `navigation` modul `settings` ága még `PlaceholderScreen` – a bekötés külön,
    `navigation`-feladat; és az app saját `BuildConfig.VERSION_NAME`-je
    (`app/build.gradle.kts`) a modulhatár miatt nem érhető el a `feature/progress`-ből
    (feature modulok nem függhetnek az `app`-ra) – a „Verzió és forrás" szakasz ezért a
    tartalomverziót és a `knowledgebase/README.md`-ből szó szerint átvett korlátozó
    mondatokat jeleníti meg, nem egy kitalált app-verziószámot.
  - **T-509 – ViewModel tesztek (FR-26, FR-27, FR-35).** `ProgressSettingsViewModelTest`
    (6 eset): importált/nem importált tartalominfó, a kétlépcsős megerősítés (törlés
    csak a második lépés után hívódik), „Mégsem" bármelyik lépésben, és hogy a törlés
    nem változtatja meg a tartalominfót. Egy dedikált teszt (ugyanazon
    `ProgressRepository`-példányt megosztva egy `ProgressOverviewViewModel` és egy
    `ProgressSettingsViewModel` között) bizonyítja, hogy a Beállításokon megerősített
    törlés az áttekintő statisztikáját is nullázza, a tartalominfót nem. A „gyakorlást
    igénylő" rendezés **megőrzését** (a tényleges rendezés a T-501 SQL-lekérdezése) egy
    új `ProgressOverviewViewModelTest` eset fedi (`+1 → 10`); a „nincs adat" esetek és
    az önértékelés-függetlenség a G-10 csoport tesztjei már lefedték.

Új tesztek: `ProgressSettingsViewModelTest` (6), `ProgressOverviewViewModelTest` (+1 → 10).
`.\gradlew.bat testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, 613 teszt,
0 hiba, 606 → 613); `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`).

Következő: T-508, `feature/progress`: debug diagnosztika (a teljes `ValidationReport`
megjelenítése) – T-117 és T-507 függőségei teljesültek. Ezt nem ez a csoport kapta
feladatul (lásd a megbízás hatóköre), a `docs/backlog/` mappában marad.

**2026-11-13 – T-508 kész (a `docs/backlog/GROUPS.md` szerint szándékosan önálló, nem
a G-11 csoport tagja; Fázis 7 lezárva, M6 mérföldkő elérve, az E5 epik lezárva):**
`feature/progress`: fejlesztői diagnosztika.

Új `feature/progress/diagnostics` csomag: `ProgressDiagnosticsViewModel` +
`ProgressDiagnosticsUiState` + `ProgressDiagnosticsScreen`, a már meglévő
`ContentImporter.lastValidationReport()` (`core/data`, T-203-ban megkötve) **ebben a
processzben** lefutott jelentését jeleníti meg (FR-32) – a részletes észrevételek
dokumentáltan nem perzisztálódnak, ezért a VM egy pontszerű `suspend` hívást zár egy
`Loading`/`Loaded` belső állapotba, nem `Flow`-t figyel. Három ág: `Loading` (a hívás
fut), `NoReport` (ebben a munkamenetben nem futott validáció, mert az import
`ImportResult.UpToDate` miatt kimaradt – **nem** hibaállapot), `Content` (kódonkénti
bontás, `IssueCode` szerint rendezve, V001 < … < V010; érintett rekordok száma;
súlyosságszűrő). A csoportosítás és szűrés `@Composable`-mentes `object`-ben
(`ProgressDiagnosticsGrouping`), a rekordleírás-/felirat-leképezés egy másik, pure
`object`-ben (`ProgressDiagnosticsIssueLabels`) él – ugyanaz a minta, mint a
`QuizOptionFeedbackResolver`/`ProgressNeedsPracticeAccessibility`. A súlyosságszűrő a
`SavedStateHandle`-ben él, forgatás alatt is megmarad.

A `feature/progress/settings` (T-507) debug-only menüpontja (`ProgressSettingsScreen`,
`ProgressSettingsDiagnosticsButton`, `OutlinedButton`) ugyanazt a
`BuildConfig.DEBUG`-mintát követi, mint a `feature/library` „Diagnosztika" gombja
(T-210): `onOpenDiagnostics` csak debug buildben nem-`null`, release buildben a gomb
egyáltalán nem rajzolódik ki. A `feature/progress/build.gradle.kts` kapott egy
`buildFeatures.buildConfig = true` sort ehhez. A tényleges navigációs bekötés
(`settings` → `progress/diagnostics`) a `navigation` modul tárgya, szándékosan
továbbra is nyitva marad, ugyanaz a dokumentált, nyitott minta, mint a
`quiz/setup`/`quiz/run`/`quiz/result`/`settings` útvonalaknál
(`docs/CONVENTIONS.md` 5. szakasz) – a debug build mindkét oldalon (gomb láthatósága,
célképernyő) kész, csak a köztük lévő navigáció nincs.

Új tesztek: `ProgressDiagnosticsGroupingTest` (4), `ProgressDiagnosticsIssueLabelsTest`
(5), `ProgressDiagnosticsViewModelTest` (4).
`.\gradlew.bat testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, 626 teszt,
0 hiba, 613 → 626); `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`).

Következő: T-601, „A tudástár bemásolása az assets alá" (`app` modul, Fázis 3/E6) – a
T-117 függősége teljesült; a `docs/backlog/GROUPS.md` szerinti legkisebb azonosítójú,
kész függőségű feladat.

- 2026-11-13 – **T-601 kész (Fázis 3.2 utolsó darabja, E6): a tudástár bemásolása az
  assets alá.** A `knowledgebase_transfer/manifest.json`, `entries.json` és
  `illustrations/` (10 kép) bitre azonos, **kézi** másolata bekerült az
  `app/src/main/assets/knowledge/` alá (`diff`/`cmp` ellenőrizve, eltérés nélkül) – a
  `knowledgebase/` marad a szerkesztési forrás. A másolás módja (kézi, nem Gradle-task –
  a task mindkettőt megengedte, a kézi egyszerűbb és auditálhatóbb, ugyanaz a minta, mint
  a `knowledgebase_transfer/` saját, `convert.py`-jal előállított és commitolt
  mivolta) a `knowledgebase_transfer/README.md` „Hogyan kerül az appba" szakaszában és egy
  új `app/src/main/assets/knowledge/README.md`-ben dokumentálva.

  A „Kész, ha: `assembleDebug` után az app elindul és betölti a 161 rekordot" kritériumot
  (eszköz/emulátor nélkül, B-03) egy új `app` teszt bizonyítja: `KnowledgeAssetImportTest`
  (Robolectric, teljes Hilt-gráf – ugyanaz a minta, mint a `HiltGraphTest`, T-203), amely
  a **valódi**, csomagolt `AssetManager`-en át meghívja a `ContentImporter.importIfNeeded()`-t:
  `ImportResult.Imported(contentVersion = "0.1.0", acceptedCount = 161, rejectedCount = 0,
  retiredCount = 0)`, majd a második hívás `ImportResult.UpToDate("0.1.0")` (FR-30
  verziókapu). Ezzel az a korábban (T-115) nyitva hagyott láncszem is lezárult, hogy a
  `core/data` `RealContentAssetSource` tesztjei addig csak a `knowledgebase_transfer/`
  fájlrendszeri elérését, nem a tényleges csomagolt assetet ellenőrizték – a
  `TestKnowledgeAssetSources.kt` KDoc-ja frissítve, hogy ezt tükrözze.

  Új tesztek: `KnowledgeAssetImportTest` (2). `.\gradlew.bat testDebugUnitTest --rerun-tasks`
  **exit 0** (teljes projekt, **628 teszt**, 0 hiba, 626 → 628); `.\gradlew.bat assembleDebug`
  **exit 0** (`app-debug.apk`, ≈ 22,0 MiB – a 10 képpel +~2,2 MB, elfogadható; `unzip -l`
  igazolja, hogy `assets/knowledge/manifest.json`, `entries.json` és mind a 10
  `illustrations/*.jpg` a csomagolt APK-ban van).

  Ezzel a **Fázis 3 (M2 – „a tartalom bent van") lezárult**: a T-115…T-118, T-201…T-203,
  T-301, T-401, T-402, T-501, T-502 mind korábban, T-601 most elkészült.

  Következő: T-602, „TR-07 rétegszabály-ellenőrzés automatizálva" (Fázis 8/E6) – a
  T-110 függősége teljesült; a `docs/backlog/` legkisebb azonosítójú, kész függőségű
  feladata.

- 2026-11-13 – **T-602 kész (Fázis 8.1, E6): TR-07 rétegszabály-ellenőrzés
  automatizálva.** A `core/model`/`core/quiz` Kotlin JVM modul volta (ADR-001 L1) és a
  `core/data` Room-típusainak `internal` láthatósága (ADR-001 L5) már fordítási időben
  kikényszeríti a rétegszabályt – ez a feladat a **forráskód-szintű**, olvasható
  bizonyítékot adja hozzá, három új teszttel a `core/quiz` modulban (a task modulmezője
  szerint):

  1. `coreModelAndCoreQuiz_haveNoForbiddenImport_TR07_NFR03` – a `core/model`/`core/quiz`
     `src/main/kotlin` fáján nincs `android.`/`androidx.`/`room`/`compose` importsor;
  2. `coreModelAndCoreQuiz_haveNoDirectClockOrRandomCall_TR07_NFR07` – ugyanott nincs
     közvetlen `Random.Default`/`System.currentTimeMillis`/`Instant.now`/`Math.random`
     hívás (NFR-07) – a modulok KDoc-jai **dokumentáltan**, szó szerint idézik ezeket a
     hívásokat a hiányukat magyarázva (pl. `QuizGenerator.kt`, `SeededShuffle.kt`), ezért
     a kereső előbb kommentet (blokk- és sorvégi) távolít el, különben a modul a saját
     dokumentációján bukna el;
  3. `featureModules_doNotImportRoomEntityDaoOrDtoType_TR07_NFR03` – a négy `feature/*`
     modul `src/main/kotlin` fáján nincs `androidx.room` import, és nincs `Entity`/`Dao`/
     `Dto` végű típusnévre mutató import.

  A szabálylogika (`LayeringRuleDetector`, `core/quiz` teszt forráskészlet) sima
  `String`-eken dolgozik, elválasztva a fájlrendszer-bejárástól – ez adja a task „Kész,
  ha" pontjának (**„a teszt elbukik, ha valaki véletlenül behúz egy tiltott importot"**)
  tényleges bizonyítását: a `LayeringRuleDetectorTest` 10 szintetikus esettel (pozitív:
  tényleg elkapja a tiltott importot/hívást; negatív: a megengedett domain-importot és a
  csak KDoc-ban/sorvégi megjegyzésben szereplő mentést nem téveszti össze találattal)
  mutatja meg, hogy a detektor valódi hibát is jelezne, nem csak azt, hogy a mai forrás
  éppen tiszta. A valódi forrásfák ellenőrzése (`LayeringRuleTest`, 3 teszt) a
  `kyo.repoRoot` rendszertulajdonságon (már a `core/quiz/build.gradle.kts`-ben beállítva
  a T-01-es `RealKnowledgeBaseFixture`-höz) ugyanazt a detektort hívja.

  Új tesztek: `LayeringRuleDetectorTest` (10), `LayeringRuleTest` (3). `.\gradlew.bat
  testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, **641 teszt**, 0 hiba,
  628 → 641); `.\gradlew.bat assembleDebug` **exit 0** (`app-debug.apk`).

  Következő: T-603, „TR-08 manifest-teszt – nincs `android.permission.INTERNET`"
  (Fázis 8.2/E6) – nincs függősége; a `docs/backlog/` legkisebb azonosítójú, kész
  függőségű feladata.

- 2026-11-13 – **T-603 kész (Fázis 8.2, E6): TR-08 manifest permission teszt.** Új
  `app` teszt (`ManifestPermissionTest.mergedManifest_doesNotRequestInternetPermission_TR08`)
  Robolectric alatt (`@Config(application = Application::class)`, szándékosan **nem** a
  `HiltTestApplication`-t használja – a Hilt gráf felépítése itt nem kell, csak az
  összevont manifest) lekéri a `PackageManager.getPackageInfo(GET_PERMISSIONS)` eredményét,
  és kifejezetten az `android.permission.INTERNET` **hiányát** ellenőrzi
  (`doesNotContain`), nem a permissionök számát – a task „Figyelj rá" pontja szerint az
  AndroidX saját, aláírásszintű permissiont ad hozzá az összevont manifesthez, amit egy
  „nulla permission" assertion hamisan buktatna. `.\gradlew.bat
  :app:testDebugUnitTest --tests "...ManifestPermissionTest" --rerun-tasks` **exit 0**.

  Következő: T-607, „Lint és kódformázás" (Fázis 8.4/E6, gyökér modul) – ugyanaz a
  csoport (G-12), nincs függősége.

- 2026-11-13 – **T-607 kész (Fázis 8.4, E6): lint és kódformázás bekötve.** Android
  Lint már alapból fut (AGP), egyetlen valódi hibát talált: a gépspecifikus, gitignore-olt
  `local.properties` `PropertyEscape` panaszát (a perjeles `sdk.dir` szándékos, lásd
  `docs/CONVENTIONS.md` 3.5. szakasz) – ez `app/build.gradle.kts` `lint { disable +=
  "PropertyEscape" }`-jével elnyomva, indoklással; emellé `checkDependencies = true`
  került (a könyvtármodulok lint-találatai is bekerülnek az `app` összevont jelentésébe –
  ez a reális "teljes app" nézet egy 9 modulos projektben). Egy triviális, valódi találat
  (`RedundantLabel` a `MainActivity`-n) javítva.

  A ktlint-gradle plugin (14.2.0, a plugins.gradle.org legfrissebb stabil kiadása;
  a bundle-özött ktlint motor 1.5.0, a plugin saját, tesztelt default verziója – nem
  felülírva) minden modulra bekötve a gyökér `build.gradle.kts` `subprojects {}`
  blokkjából. A szabályokat a gyökér `.editorconfig` adja (ktlint 1.x óta ez a kanonikus
  hely). Három felfedezés, mind dokumentálva a `docs/CONVENTIONS.md` 6. szakaszában:
  (1) a `ktlint_code_style = android` érték **érvénytelen** ktlint 1.5.0-ban (csendben
  `ktlint_official`-ra esik vissza, figyelmeztetéssel) – törölve; (2) a ktlint 1.x újabb
  sortörés-szabályai (`function-signature`, `multiline-expression-wrapping`,
  `argument-list-wrapping`, `parameter-list-wrapping`, `class-signature`,
  `property-wrapping`, `chain-method-continuation`) a projekt meglévő kódján nem
  konvergálnak stabilan – kikapcsolva, indoklással; (3) a `function-naming` a Compose
  PascalCase `@Composable` függvényeket hibásan jelölné – kikapcsolva. A `filename`
  szabály négy, lezárt feladatok dokumentációja által szó szerint idézett fájlnevet
  (`ProgressMessages.kt` stb.) jelölt volna – kikapcsolva, hogy a historikus
  `docs/done/`/`TEST_REPORT.md`/`IMPLEMENTATION_PLAN.md` bejegyzések ne essenek szét egy
  átnevezés miatt.

  A tényleges formázási eltéréseket (sorhossz, üres sor osztálytest elején, import-sorrend
  stb.) a `ktlintFormat` automatikusan kijavította a teljes forrásfán; a maradék,
  automatikusan nem javítható eseteket (Markdown-táblázat KDoc-ban, hosszú referenciák)
  kézzel tördeltem, két helyen (`DefaultKnowledgeValidator.resolveIllustration`,
  `DefaultQuizGenerator`, `KyokushinDatabase`) `@Suppress("ktlint:standard:max-line-length")`
  elszigetelt elnyomással, mert a táblázatcellák tördelése eltörné a Markdown-renderelést.

  `.\gradlew.bat lint --rerun-tasks` **exit 0** (teljes projekt, 9 Android modul).
  `.\gradlew.bat ktlintCheck --rerun-tasks` **exit 0** (teljes projekt, 77 task,
  mindegyik futott, nincs `UP-TO-DATE`-re bízott bizonyíték). `.\gradlew.bat
  testDebugUnitTest --rerun-tasks` **exit 0** (teljes projekt, **642 teszt**, 0 hiba,
  641 → 642, az új `ManifestPermissionTest`-tel). `.\gradlew.bat assembleDebug
  --rerun-tasks` **exit 0** (`app-debug.apk`).

  Következő: a `docs/backlog/` legkisebb azonosítójú, kész függőségű feladata (T-608,
  G-13).

- 2026-11-13 – **G-13 csoport kész (Fázis 3.3 + 8.5, E6): a tudástár és a kvízszabályok
  kiadott dokumentációja.** `docs/KNOWLEDGE_BASE.md` (**T-608**) és `docs/QUIZ_RULES.md`
  (**T-609**) a korábbi, tervezett (VÁZLAT) szöveg helyett a **tényleges** kódhoz mérve.

  **T-608** a csomagolt `app/src/main/assets/knowledge/manifest.json`/`entries.json`
  mezőit írja le (ADR-011 kanonikus séma, snake_case), a `KnowledgeInputMapper.kt`
  leképezését a validátor camelCase bemenetére (`core/quiz` `KnowledgeInput`), a tényleges
  V001…V010 szabálytáblát (`DefaultKnowledgeValidator.kt`, `ValidationIssue.kt`), az új
  technika/kép hozzáadásának lépéseit (`knowledgebase/` → `convert.py` →
  `knowledgebase_transfer/` → kézi másolás az `app` csomagba → `dataset_version` léptetés –
  mind kódmódosítás nélkül, AC-08), a kétszintű hitelesítési folyamatot (rekordszintű
  `review_status` a Tudástár-badge-hez, manifest-szintű `editorial_status` a
  `DatasetTrust`/szalaghoz – **egyik sem** azonos a kvízjogosultsággal, lásd lent), a
  licenctáblázatot (mind a 10 kép `ai_generated`/`project-internal`, 3 feliratos →
  `quiz_usable: false`) és a migráció négy esetét (rendes tartalomkiadás, átnevezés,
  visszavonás – mind kódmódosítás nélkül; `schema_version` major-emelés – **ez igen**
  kódot igényel, elhatárolva a Room séma 2-es verziójától). Rögzített nyitott kérdés:
  OQ-02 (a szakmai hitelesítő személye) változatlanul a felhasználóra várva.

  **T-609** a tényleges `DefaultQuizGenerator`/`DistractorSelector`/`QuizCandidateFilter`/
  `QuizEligibility`/`QuizScoring` kódot írja le, minden állítást egy létező teszthez kötve
  (`DefaultQuizGeneratorTest`, `DistractorSelectorTest`, `RealContentQuizGenerationTest`,
  `QuizPlanInvariantTest`, `QuizScoringTest`/`QuizScoringTr04Test`, T-112). Rögzített,
  kódból levezetett, nem nyilvánvaló tény: a `DatasetTrust` két értéke (`REVIEWED`,
  `PRACTICE_ON_UNREVIEWED`) **mindkettő** átengedi a `QuizEligibility` kaput – a bizalmi
  szint ma **nem** zár ki technikát a kvízből, csak a UI szalagját vezérli; a tényleges
  szűrés a `status != INCOMPLETE`, a `quizExcluded`/`retired` és a mezőteljesség.

  Mindkét dokumentum a már meglévő, teljes tesztkészletre hivatkozik; a task fejléce
  szerint (`Teszt | —`) új teszt nem tartozik hozzájuk. `.\gradlew.bat testDebugUnitTest
  --rerun-tasks` **exit 0** (teljes projekt, változatlanul **642 teszt**, 0 hiba – a
  változás tisztán dokumentáció). `.\gradlew.bat assembleDebug` **exit 0**.
  `connectedDebugAndroidTest` és `lint`/`ktlintCheck` ebben a körben **nem futott** (nem
  kódváltozás; a legutóbbi, T-607/T-602-beli futásuk változatlanul érvényes).

  Következő: a `docs/backlog/` legkisebb azonosítójú, kész függőségű feladata (**T-604**,
  „Akadálymentességi átfésülés" – függőségei, T-413 és T-503, már a `docs/done/`-ban).

- 2026-11-13 – **T-604, T-605, T-606, T-610 és T-611 kész.**
  A korábbi, félbehagyott T-604 kódellenőrzést valódi emulátoros vizsgálat egészítette ki.
  Bekötve a kvíz teljes folyamata, a beállítások és a debug diagnosztika; a navigációs
  bemenet explicit átkerül a ViewModel mentett állapotába. Tanulásból a kategóriák
  átadása, kvízből a hibák újragyakorlása, megszakítás, forgatás és visszaállítás tesztelve.
  Nagy betűméretnél a tartalom görgethető; a tudástár státuszjelzése nem szorítja össze
  a neveket. Az alapértelmezett téma a kontrasztteszttel ellenőrzött paletta.

  Teljes ellenőrzés: `.\gradlew.bat testDebugUnitTest assembleDebug lint ktlintCheck
  connectedDebugAndroidTest --console=plain` **exit 0**, 647 unit teszt és 6 eszközteszt,
  0 hiba. A kártya tényleges forgatásával kiegészített végső
  `.\gradlew.bat :app:connectedDebugAndroidTest ktlintCheck --console=plain` **exit 0**,
  6 eszközteszt, 0 hiba. B-03 lezárva: API 37 AVD működik. Külön offline/folyamat-
  újraindítási próba: AC-01 és AC-06 PASS. Részletek: TEST_REPORT.md és ACCEPTANCE.md.

  README: Android Studio indítás, APK-telepítés, használat és ismert korlátok.
  APK: `app/build/outputs/apk/debug/app-debug.apk`, 22 858 574 byte.
  A kijelölt öt backlogfeladat átkerült a `docs/done/` mappába.

  **Következő:** nincs további számozott backlogfeladat. Az éles tartalomhoz oktatói
  lektorálás, hiányzó ábrák, majd a jóváhagyott csomag elfogadási próbája kell.
  AC-08 részben igazolt; a külön tudástár-adatlapi gyakorlási összegzés hiánya a
  CONVENTIONS ismert ügyei között marad, nem szerepel megoldottként.
- 2026-11-13 – **Bővítési backlog felvéve (Fázis 9, E7/E8/E9).** A felhasználó
  design- és funkcióvisszajelzése alapján 11 új task (`T-701…T-711`) került a
  `docs/backlog/`-ba: indítóképernyő a saját `asserts/kyo.png` gi-fotóból
  vektorizált kandzsi logóval (`asserts/kyo_kanji.svg`, `asserts/ic_kyo_kanji.xml` –
  OpenCV kontúrkövetés + Chaikin-simítás, nincs harmadik féltől származó kép),
  Kezdőoldal + Főoldal gomb, a Tudástár jobbra tolása, kategória-ikonos Tanulás
  checkbox helyett, felső tab Japán/Magyar névhez, illusztráció-befogadási
  munkafolyamat, fordítás-ellenőrzési jegy. A T-709…T-711 (hangos kiejtés,
  kiejtés-alapú kvíztípus, többnyelvűsítés) a mesterprompt 9/10. pontja miatt
  **szándékosan nem indítható** – a „Függ" mezőjük egy nyitott kérdésre (OQ-05…OQ-07,
  `REQUIREMENTS.md` 10. szakasz) mutat, amit a `/dev` sosem talál teljesítve.
  Build/teszt ehhez a felvételhez nem futott (csak dokumentáció + a logó-vektorizáló
  szkript futott, Python/OpenCV-vel, a repón kívül, scratchpadben).
- 2026-11-13 – **T-701 kész (Fázis 9.1, E7): indítóképernyő.** `SplashScreen.kt` (app
  modul): fix fehér háttér + a vektorizált kandzsi logó (`ic_kyo_kanji.xml`), 1200 ms
  után automatikus átadás a `KyokushinNavHost`-nak (`KyokushinApp.kt`), a célképernyőtől
  (ma Tudástár, T-703 után Kezdőoldal) függetlenül. `contentDescription` +
  `semantics { heading() }`, `@Preview` világos/sötét. A `core/ui` `wcagContrastRatio`
  `internal` → publikus nyitásával bizonyított WCAG AA kontraszt (`SplashContrastTest`,
  ≈ 11,95:1). A `values-night/themes.xml` cold-start háttere is fehérre állítva. Az
  Android 12+ natív `SplashScreen` API **szándékosan nincs** bekötve – a
  `core-splashscreen` nincs a verziókatalógusban, ez egy új, jóváhagyást igénylő
  task marad, a kézi Compose-fallback addig (és utána is, a régebbi `minSdk`-khoz)
  teljes értékű.

  `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain` **exit 0**, 648 teszt
  (647 → +1), 0 hiba, `app-debug.apk` ≈ 22,75 MB. `.\gradlew.bat ktlintCheck lint
  --console=plain` **exit 0**, nincs új találat. Soron következő: **T-702**.
- 2026-11-13 – **T-702 kész (Fázis 9.2, E7): vizuális arculat terv.** Új
  `docs/DESIGN_NOTES.md`: diagnózis (nincs egyedi `Typography`/`Shapes` a
  `Theme.kt`-ban, nyolc fájl duplikálja a `RoundedCornerShape(12.dp)`-t,
  `OutlinedTextField` adja a "webes űrlap" hatás nagy részét), CR-08 keretezés
  (visszafogott márka, nem harcművészeti klisé), szín-/tipográfia-/forma-javaslatok
  a T-701 logóból levezetve, explicit "mit NEM szabad" lista. Tisztán dokumentációs
  task, nincs Kotlin/Compose-kód változás, build/teszt nem futott (nem is volt
  tárgya). A kódba ültetést egy új task viszi: **T-712** (`core/ui: Typography`/
  `Shapes` bevezetése, Függ: T-702). Soron következő: **T-703**.

### 2026-11-13 – Külön felhasználói kérés: filmes indítóanimáció

Elkészült a 14 mp-es, offline, átugorható intro és a fotót részletesebben követő
kanji-vektor. A felhasználó pontosítása szerint az ököl jobbról balra üt, a repedés
és a kék energia vízszintes, majd az energia függőlegesbe fordul és kitisztul.
Részletek és generálás: `docs/CINEMATIC_INTRO.md`. Kimenetek:
`output/Kyokushin-Intro.html`, `output/Kyokushin-Intro.webm`,
`output/Kyokushin-Trainer.apk`. A meglévő T-703 és további arculati backlog
állapota ettől nem változik; a közben megjelent kezdőoldali módosítások megőrizve.

Ellenőrzés: unit/build/app lint/ktlint exit 0, 648 összesített unit teszteredmény,
0 hiba; `IntroFlowTest` emulátoron 2/2 sikeres, exit 0. A részletes futásnapló a
`docs/TEST_REPORT.md` végén található.

**2026-11-14 – az intro felhasználói pontosítása:** végig fehér háttér; az
`asserts/fist.jpg` alapján generált, részletes kék ökölrajz és évgyűrűs fametszet.
A repedés és a tűz kizárólag a jobb oldali ütés helyétől balra terjed. A kanji
kitöltéséből számolt középvonalak helyi vastagság szerint szélesednek; a kontúrok
körberajzolása megszűnt. A vízszintes–függőleges fordulás és az eredeti részletes
végső kanji megmaradt. Grafikai források/promptok: `docs/INTRO_ART_PROMPTS.md`.

**2026-11-14 – további kis pontosítás:** a fametszetet rajzolt deszka váltotta.
A vízszintes kanji 180°-kal megfordult; a felállás +90° → 0°, az előzővel
ellentétes irányú. Oldalirányú repedés, fehér háttér és végső álló kanji megmaradt.
Videó, HTML és APK frissítve; grafikai ellenőrzés és Gradle build/test/lint exit 0.

**2026-11-14 – álló deszka, ütés a lapra:** a deszka széles lapja a néző felé áll,
az ütés x=500 pontja a látható x≈185…625 felületen belül van. A hasadás és a tűz
balra, a lap szélességén belül fut; a kisebb vízszintes kanji a korábbi végső
méretre növekszik a fordulás közben. A +90° → 0° forgásirány megmaradt.

**2026-11-14 – teljes magasságú deszka és repedési szünet:** a deszka túlér a
felső, alsó és bal képszélen. A kanji nagyobb kezdőmérettel jelenik meg; az összes
alapvonala 3,8 mp-re sötét repedés, a kék energia 5,4 mp-től indul. A grafikai
ellenőrzés mind a 168 ágat igazolja. Videó/HTML/APK frissítve, build exit 0.

**2026-11-14 – sima vektorszélek és határozott ütés:** a fotó kontúrjai immár
933 Bézier-szakaszt alkotnak, 16 zárt kontúrral; a PNG-összehasonlítás 2400×2901
pixel, élsimított. A belső törésvonalak újragenerálva (105 ág). Az ököl az ütésig
teljesen rejtett, 0,24 mp alatt csapódik be, majd 1,8 mp alatt húzódik vissza.
A képkockás ellenőrzés a korai kézszél hiányát is igazolja; friss videó és APK.
- 2026-11-14 – **T-703 kész (Fázis 9.3, E7): Kezdőoldal + Főoldal gomb.** Új
  `feature/home` modul (nincs ViewModel/Hilt – nincs megfigyelendő állapot):
  `HomeRoute`/`HomeScreen` csak a márka-kandzsit mutatja, körülötte semmi mást.
  `KyokushinScreen.HOME` (`"home"`) lett az induló útvonal a Tudástár listája
  helyett; `BottomNavDestination.HOME` a sor bal szélén (Főoldal, Tudástár,
  Tanulás, Kvíz, Haladás – a Tudástár jobbra tolása T-704 tárgya marad).
  `NavigationGraphTest` bővítve az új induló útvonalra és az öt fülre.

  **Menet közben szükségessé vált refaktor:** a T-701 splash-logója az `app` modul
  drawable-je volt, de egy `feature/*` modul nem függhet `app`-ra (ADR-001, L5) – a
  drawable és a leírás átkerült a `core/ui`-ba egy új, megosztott `KyoKanjiMark`
  komponensbe, amit a splash (`app`) és a Kezdőoldal (`feature/home`) is hív. A
  `tools/trace_kanji.py` kimeneti útvonala is frissült erre.

  **Egyeztetés a párhuzamos intró-munkával:** a fenti, felhasználói kérésre készült
  filmes intro (2026-11-13/14) ezt a taskot nyitva, változatlanul hagyta – nincs
  ütközés: az intro a `KyokushinApp`-ban a splash elé került, a Kezdőoldal útvonala,
  a bottom nav sorrendje és a `KyoKanjiMark` a T-703 commitjával együtt áll össze.

  `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain` **exit 0**, 648
  teszt, 0 hiba (`:feature:home:assembleDebug` is lefutott). `.\gradlew.bat
  ktlintCheck lint --console=plain` **exit 0**, `feature/home` és `navigation`
  lint-jelentése üres. Soron következő: **T-704**.
- 2026-11-14 – **T-704 kész (Fázis 9.4, E7): a Tudástár a bottom nav jobb szélén.**
  `BottomNavDestination` enum átrendezve: `HOME`, `LEARN`, `QUIZ`, `PROGRESS`,
  `LIBRARY` – a deklarációs sorrend maga a UI sorrend, nem kellett külön mező.
  `NavSection`/`selectedFor` változatlan (pozíció-független, szekció szerint dönt),
  ezért a Tudástár fül a részletezőn is kijelölve marad, csak más helyen.
  `NavigationGraphTest` frissítve az új sorrendre.

  Az első futtatás `java.io.EOFException`-nel bukott egy korábbi, megszakított
  buildből maradt sérült Kotlin-inkrementális/teszt-cache miatt (nem kódhiba) –
  `gradlew --stop` + egy célzott `--rerun-tasks` helyreállította, lásd
  `docs/CONVENTIONS.md` 7. szakasz új sora. `.\gradlew.bat testDebugUnitTest
  assembleDebug ktlintCheck lint --console=plain` utána **exit 0**, 648 teszt,
  0 hiba, `navigation` lint-jelentése üres. Soron következő: **T-705**.
- 2026-11-14 – **T-705 kész (Fázis 9.5, E7): kategória-ikonok és technikalista
  drill-down a Tanulásban.** Új `core/ui` `categoryIcon(categoryId)` – a 9 valós
  kategória mindegyike egy semleges, geometrikus Material-ikont kap (nem egy
  mozdulatot ábrázoló piktogram, CR-08 + `docs/DESIGN_NOTES.md`), ismeretlen
  kategória a `Icons.Filled.Category` helyőrzőt kapja. A `learn/setup` checkbox-
  listája kategóriakártya-rácsra cserélve; egy kártyára koppintva megnyílik az adott
  kategória technikalistája (`LearnSetupUiState.Content.drillDown`), ahonnan egy
  gombbal ki-/bekapcsolható a teljes kategória.

  **Tervezési döntés (dokumentálva a kódban):** a kijelölés **kategória-szinten**
  maradt, nem vált technika-szintűvé – a task fejlécének modulhatára
  (`feature/learn, core/ui`) és a `LearnCardsViewModel`/`navigation` szerződésének
  érintetlenül hagyása indokolta; az US-09 "több kategória egyszerre" folytonossága
  így is megmaradt (`selectedCategories` változatlan). A kategóriakártya ikonja
  szándékosan dekoratív (`contentDescription = null`) a `docs/CONVENTIONS.md`
  "ne duplikáld a TalkBack-bejelentést" szabálya szerint, nem a task szó szerinti
  "contentDescription minden ikonra" megfogalmazása szerint – a kijelölt állapotot
  `stateDescription` jelzi.

  `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain` **exit 0**, 656
  teszt (648 → +8), 0 hiba. `.\gradlew.bat ktlintCheck lint --console=plain`
  **exit 0**, `core/ui` és `feature/learn` lint-jelentése üres. Emulátoros vizuális
  ellenőrzés nem futott (nem volt tárgya a tasknak). Soron következő: **T-706**.
- 2026-11-14 – **T-706 kész (Fázis 9.6, E7): felső fülváltó "Japán nevek"/"Magyar
  nevek" a tanulókártyán.** Az elrejtési irány (FR-07) a `learn/setup`-ból a
  `learn/cards` felső `SecondaryTabRow`-jába költözött – élőben váltható
  (`LearnCardsViewModel.onSelectHideMode`), nem csak a munkamenet elején egyszer
  eldöntött beállítás. Fülváltáskor csak az ÉPPEN LÁTOTT kártya felfedettsége
  törlődik, a pozíció és a többi kártya felfedettsége érintetlen. A korábbi,
  önálló "Mit rejtsünk el?" rádiógombos választó megszűnt a `learn/setup`-ból (nem
  maradt két ellentmondó vezérlő); ennek a `onStartCards` szerződésváltásán
  keresztül a `navigation` modul route-ja (`KyokushinScreen.LEARN_CARDS`,
  `KyokushinRoutes.learnCards`) is mechanikusan módosult: nincs több `hideMode`
  route-argumentum.

  **Tervezési döntés:** a fülnév a KEZDŐ, látható oldalt nevezi meg, nem a
  rejtettet – "Japán nevek" = `HideMode.HIDE_HUNGARIAN`, "Magyar nevek" =
  `HideMode.HIDE_JAPANESE` (fordított leképezés a `HideMode` enumnevekhez képest,
  dokumentálva a kódban). Az alapértelmezés is "Japán nevek"-re váltott (korábban
  `HIDE_JAPANESE` volt az alapértelmezés) – szándékos, a felhasználó kérésének
  szórendje ("Kiindulás: Japán nevek, Magyar nevek") alapján.

  `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain` **exit 0**, 659
  teszt (656 → +3), 0 hiba. `.\gradlew.bat ktlintCheck lint --console=plain`
  **exit 0**, `feature/learn`, `navigation` és `core/ui` lint-jelentése üres.
  Ismert, nem ebben a körben javított drift: `FUNCTIONAL_SPEC.md` 4.1/3. és 4.2
  még a régi "Mit rejtsünk el?" választót írja le (`docs/CONVENTIONS.md` 8.
  szakasz új sora). Soron következő: **T-707** (T-708 és T-712 is függetlenül
  indítható).
- 2026-11-14 – **T-712 kész (Fázis 9.2b, E7): `core/ui` `Typography`/`Shapes`
  bevezetése – a T-702 terve kódba ültetve, az E7 epik ezzel lezárult.**
  `LightScheme.primary` → `#17317A` (a T-701 splash-logó színe, a logó az
  igazságforrás), `primaryContainer` → `#D9E2F7`; a `ThemeContrastTest` mindkét
  új párra lefutott (`onPrimary`/`primary` ≈ 11,95:1, `onPrimaryContainer`/
  `primaryContainer` ≈ 12,91:1, mindkettő jóval 4,5:1 fölött). Új, megnevezett
  `Shapes` (`small/medium/large` = 4/12/16 dp) – a ténylegesen **10** (nem a
  becsült 8) kézzel kiírt `RoundedCornerShape(12.dp)` hívási hely
  (`QuizResultScreen`, `QuizRunScreen` ×3, `ProgressTechniqueScreen`,
  `ProgressDiagnosticsScreen`, `ProgressOverviewScreen` ×2,
  `ProgressSettingsScreen` ×2) és a `LearnCardsScreen.CARD_CORNER_RADIUS` mind
  `MaterialTheme.shapes.medium`/`.large`-ra cserélve. Új, megnevezett
  `Typography`: `titleLarge`/`headlineSmall` félkövér a Material 3
  alapértelmezett `Medium` helyett, nincs új betűtípus-fájl. A `@Preview`
  Teendő nem igényelt kódszerkesztést – a megosztott `KyokushinTrainerTheme`-en
  át minden meglévő előnézetben automatikusan érvényesül.

  Az első `ktlintCheck lint` futás `feature/learn:ktlintMainSourceSetCheck`
  hibával bukott (`Needless blank line(s)` a `CARD_CORNER_RADIUS` törlése után
  maradt dupla üres sor miatt) – a sor törlése után `BUILD SUCCESSFUL`.
  `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain` **exit 0**,
  659 teszt, 0 hiba. `.\gradlew.bat ktlintCheck lint --console=plain` **exit 0**,
  minden érintett modul (`core/ui`, `feature/learn`, `feature/quiz`,
  `feature/progress`) lint-jelentése üres. Soron következő: **T-707** (a T-708
  is függetlenül indítható).


## 2026-11-14 – T-708: terminológiai forrásösszevetés

A teljes 161 rekord romaji/magyar/kategória-mezőinek dokumentált áttekintése
elkészült: `docs/CONTENT_VERIFICATION.md` és `.csv`. 112 megfeleltethető,
5 javításjavaslat, 14 pontosítás, 5 forrásfüggő, 7 nyitott és 18 kategóriajelzés.
IFK szervezeti anyag, magyar dojo-szótár és külön Kyokushin-kan címhasználat
összevetve; az átvett források függősége külön dokumentált.
Tananyag, dataset_version és review-státusz nem változott. A CSV és a csomagolt
adat vizsgált mezői egyeznek. `python -X utf8 tools/review_t708.py` exit 0.
Build/unit nem futott (dokumentációs task); párhuzamos Claude-munka megőrizve.

## 2026-11-14 – T-707: illusztráció-befogadási munkafolyamat

**Fázis 9.7 kész (FR-41).** `docs/KNOWLEDGE_BASE.md`-be felvéve a **6a. pont**
("Illusztráció utólagos hozzáadása egy meglévő technikához") a 6. és 7. pont közé
(lettered sub-section, hogy a meglévő "lásd 6./7./8. pont" kereszthivatkozások ne
boruljanak fel) – fájlrendszeri hely, elnevezési konvenció, `illustration_ref`
bekötés egy meglévő rekordra, `dataset_version` léptetés szabálya, és explicit
megerősítés, hogy a `review_status`/`editorial_status` a kép megléte miatt
**soha** nem emelkedik automatikusan.

**Kódból feltárt, új ismert ügy (dokumentálva, nem javítva ebben a körben):**
`knowledgebase_transfer/tools/convert.py` `ILLUSTRATION_PROVENANCE` konstansa
**globális**, minden illusztrációra ugyanazt az `"ai_generated"`/
`"project-internal"` eredetet írja – egy felhasználó-adta (nem AI-generált) kép
esetén ez téves eredetet eredményezne, amíg kézzel nem korrigálják a generált
`manifest.json`-t átmásolás előtt. Lásd `docs/CONVENTIONS.md` 8. szakasz új sora és
`docs/KNOWLEDGE_BASE.md` 6a. pont – javasolt, fel nem vett követő task a
`convert.py` per-kép eredet-beolvasására.

A Teendők harmadik pontját ("kell-e egy pre-import formátum/méret ellenőrző
szkript") megvizsgálva: a meglévő `DefaultKnowledgeValidator` V004/V005 szabálya
már gracefully kezeli a hiányzó/lógó illusztráció-hivatkozást (lásd `core/quiz`
`ValidationIssue.kt`), formátum/méret-ellenőrzés viszont valóban nincs sehol –
ezt dokumentáltuk, de **nem** készült hozzá új szkript/Gradle-feladat (a Teendő
feltételes megfogalmazása és a task modulhatára – `docs`, `core/data` – szerint ez
egy önálló eszközfejlesztési döntés lenne). A `.claude/skills/kyo-knowledge-base/
SKILL.md` új 8. szakasza hivatkozza a `KNOWLEDGE_BASE.md` 6a. pontját.

Kódváltozás nem történt (tisztán dokumentációs task), ezért `./gradlew` futtatás
nem volt a DoD része és nem futott – a task fejlécének `Teszt` mezője is csak
feltételesen ("ha készül ellenőrző szkript") írta elő. Soron következő: a
`docs/backlog/`-ban nincs több `/dev`-del indítható feladat (T-709…T-711 szándékosan
blokkolt).
