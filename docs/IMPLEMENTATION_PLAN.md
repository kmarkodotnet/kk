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
| 2 – domain modell és kvízmotor | T-101…T-114 | **M1** – a mag igazoltan helyes |
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
| 2.2 | `core/quiz` validátor: `KnowledgeValidator`, `ValidationReport`, V001…V010 | DR-05, FR-31 | 🟡 **részben** – T-104 kész: a szerződés (`KnowledgeValidator`, `KnowledgeInput`, `ValidationReport`, `RejectedRecord`, `ValidationIssue`, `IssueCode` V001…V010, `Severity`, `IssueScope`), 5 zöld szerződésteszt; a szabályok implementációja a T-105 |
| 2.3 | **Validátor tesztek** (táblázatos, minden hibakódra) | **TR-01** | ⬜ |
| 2.4 | `core/quiz` generátor: `QuizRequest`/`QuizPlan`, jelöltszűrés, distraktorválasztás, seedelt keverés, `dryRun` | FR-14, FR-22, CR-07 | ⬜ |
| 2.5 | **Generátor tesztek** (invariánsok, seed-determinizmus, fallback, típusjogosultság) | **TR-02, TR-03** | ⬜ |
| 2.6 | `QuizScoring` nullable százalékkal | FR-27 | ⬜ |
| 2.7 | **Scoring tesztek** (0%, 100%, félbehagyott, dupla, nulla válasz) | **TR-04** | ⬜ |
| 2.8 | Ellenőrzés: `./gradlew testDebugUnitTest` | NFR-12 | ⬜ |

> Ez a fázis a termék helyességének magja, és **Android SDK nélkül is fordítható/tesztelhető**
> lenne, amint a JDK rendelkezésre áll.

## Fázis 3 – Tudástár: séma, demó adat, import, Room

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 3.1 | A kanonikus séma implementálása (ADR-011): manifest + `entries`, `schema_version` string | FR-29 | ⬜ |
| 3.2 | A `knowledgebase/` tartalom bemásolása `app/src/main/assets/knowledge/` alá + `editorial_status` alapú `datasetTrust` (ADR-010) | CR-01, CR-03 | ⬜ |
| 3.3 | `docs/KNOWLEDGE_BASE.md` kiadott változata (séma, hozzáadás, hitelesítés, licenc, migráció) | FR-30, CR-04 | ⬜ |
| 3.4 | Room: tartalom- és felhasználói táblák, DAO-k, `@TypeConverter`-ek | DR-04, DR-07 | ⬜ |
| 3.5 | `UNIQUE(sessionId, questionId)` index | FR-18 | ⬜ |
| 3.6 | `ContentImporter`: DTO → validátor → upsert, idempotens, `retired` jelölés | FR-31, FR-33 | ⬜ |
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

1. **T-105…T-106** – a validátor szabály-implementációja + **TR-01 tesztek**. (**A T-104
   lezárva:** a validátor szerződése és hibatípusai állnak, a `core/quiz` Android-mentes
   marad, és a modul megkapta a `testDebugUnitTest` gyűjtőtaskot.)
2. **T-107…T-112** – kvízgenerátor + **TR-02, TR-03 tesztek**: az **M1 mérföldkő** magja,
   ahol eldől, hogy a termék helyes-e.
3. **T-113…T-114** – `QuizScoring` + **TR-04 tesztek**: nullable sikerarány, félbehagyott
   kvíz, dupla válasz (a T-103 `QuizAttempt`-jére épül).

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
