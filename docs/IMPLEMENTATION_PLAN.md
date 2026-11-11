# Implementációs terv és állapot

| | |
|---|---|
| Verzió | 1.0 |
| Utolsó frissítés | 2026-11-11 |
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
| 4 – Tudástár UI | T-204…T-211 | **M3** – böngészhető app |
| 5 – Tanulás | T-302…T-308 | **M4** – tanulható app |
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
| 1.6 | Hilt bekötés (`@HiltAndroidApp`, `@AndroidEntryPoint`) | NFR-03 | ✅ kész (Hilt modulok tartalma a Fázis 3-ban) |
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
| 3.7 | Repository implementációk (`internal`), mapperek, aggregált statisztika query-k | NFR-03, DR-09 | ⬜ |
| 3.8 | **Tartalomfrissítés teszt**: előrehaladás megőrzése, átnevezés, visszavonás | **TR-05** | ⬜ |

## Fázis 4 – Tudástár UI

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 4.1 | `core/ui` megosztott komponensek: `TechniqueIllustration`, `VerificationBadge`, `EmptyState`, `DemoBanner` | CR-03, NFR-04 | ⬜ |
| 4.2 | Navigation Compose + bottom nav (4 cél), a kvíz futás közbeni elrejtése | — | ⬜ |
| 4.3 | Tudástár lista: keresés (normalizált, aliasokra is), kategóriachipek, `SavedStateHandle` | FR-01…FR-03 | ⬜ |
| 4.4 | Tudástár részletező | FR-04, FR-05 | ⬜ |
| 4.5 | Üres/hiba állapotok a funkcionális spec 2.4 és 10. pontja szerint | NFR-06 | ⬜ |

## Fázis 5 – Tanulás

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 5.1 | `learn/setup`: kategória- és elrejtésválasztás | FR-06, FR-07 | ⬜ |
| 5.2 | `learn/cards`: felfedés, előző/következő, seedelt sorrend, pozíció mentése | FR-08, FR-09 | ⬜ |
| 5.3 | Önértékelés → `StudyReview`, elkülönítve a kvízstatisztikától | FR-10, FR-28 | ⬜ |

## Fázis 6 – Kvíz UI

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 6.1 | `quiz/setup`: kategóriák, kérdésszám, típusok, **élő készenléti összegzés** (`dryRun`) | FR-11…FR-13, FR-22 | ⬜ |
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

## Következő három lépés

1. **T-118** – **TR-05 tesztek**: a tartalomfrissítés előrehaladás-megőrzése a kész
   `ContentImporter`-en (aggregált statisztika változatlansága, több körös frissítés,
   `TR05` jelölésű tesztnevek). (**A T-117 kész:** az importáló, a verziókapu, a `retired`
   jelölés és a tartalom oldali mapperek megvannak, 330 zöld teszttel.)
2. **T-201** – `core/data`: `TechniqueRepository` implementáció (`retired = false` szűrés,
   keresés); az entitás → domain mapper a T-117 óta megvan, a `ContentState` domain
   modellje még nem.
3. **T-203** – `di`: Hilt modulok. A `RoomContentImporter` `@Inject` konstruktorral kész,
   de a kötései (`KyokushinDatabase`, `KnowledgeContentReader`, `Clock`,
   `@IoDispatcher`, `@Binds ContentImporter`) még nincsenek megírva – és mivel az
   adatbázis és a beolvasó `internal`, ezeknek a kötéseknek a `core/data`-ban kell élniük,
   nem a `di` modulban.

Párhuzamosan, amikor sorra kerül: a `REQUIREMENTS.md` / `FUNCTIONAL_SPEC.md` /
`ARCHITECTURE.md` érintett szakaszainak átvezetése ADR-010 és ADR-011 szerint
(mezőnevek, `quizEligible` kapu, lektorálatlan jelölés, aktív kvíztípusok).

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
