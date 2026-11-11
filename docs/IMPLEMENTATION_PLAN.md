# Implementációs terv és állapot

| | |
|---|---|
| Verzió | 1.0 |
| Utolsó frissítés | 2026-11-11 |
| Alapja | `docs/REQUIREMENTS.md`, `docs/FUNCTIONAL_SPEC.md`, `docs/ARCHITECTURE.md` |

Ez a dokumentum az **egyetlen** hely, ahol a projekt tényleges állása követhető.
A `/kyo-next` parancs innen veszi a következő lépést, és ide írja vissza az eredményt.

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
| 0.7 | **Felhasználói jóváhagyás a tervekre** | ⏸ **ITT ÁLLUNK – várakozás** |
| 0.8 | JDK 17 + Android SDK telepítése (`/kyo-setup-env`), verziómátrix rögzítése | ⬜ blokkolt: B-01, B-02, B-04 – felhasználói jóváhagyás kell |
| 0.9 | ADR-001…ADR-007 kiírása `docs/adr/` alá | ⬜ |

---

## Fázis 1 – Gradle projektváz

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 1.1 | `settings.gradle.kts`, root `build.gradle.kts`, Gradle wrapper | NFR-02 | ⬜ |
| 1.2 | `gradle/libs.versions.toml` ellenőrzött verziókkal (B-04 után!) | NFR-02 | ⬜ |
| 1.3 | Modulok létrehozása: `app`, `core/model`, `core/quiz`, `core/data`, `core/ui`, `feature/*`, `navigation`, `di` | NFR-03 | ⬜ |
| 1.4 | `AndroidManifest.xml` – **nincs** INTERNET permission, egy Activity | NFR-01, TR-08 | ⬜ |
| 1.5 | Material 3 téma világos/sötét, `strings.xml` váz | NFR-05 | ⬜ |
| 1.6 | Hilt bekötés (`@HiltAndroidApp`, `@AndroidEntryPoint`), üres Hilt modulok | NFR-03 | ⬜ |
| 1.7 | Ellenőrzés: `./gradlew assembleDebug` | — | ⬜ |

## Fázis 2 – Domain modell és kvízmotor (tiszta Kotlin, Android nélkül)

| # | Lépés | Fedett követelmény | Állapot |
|---|---|---|---|
| 2.1 | `core/model`: value class ID-k, enumok, `Technique`, `Category`, `IllustrationRef`, felhasználói adatmodellek | DR-01…DR-04 | ⬜ |
| 2.2 | `core/quiz` validátor: `KnowledgeValidator`, `ValidationReport`, V001…V010 | DR-05, FR-31 | ⬜ |
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
| 3.1 | `assets/knowledge/manifest.json` + `techniques.json` séma, `datasetKind: "demo"` | FR-29, CR-03 | ⬜ |
| 3.2 | Demó adatkészlet: szerkezetileg teljes, `verified: false`, `quizEligible: false`, képhelyőrzőkkel | CR-01, CR-03, CR-05 | ⬜ |
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
| B-01 | Nincs JDK | minden Gradle művelet: 1.7, 2.8, minden teszt és build | `winget install EclipseAdoptium.Temurin.17.JDK` (jóváhagyás kell) |
| B-02 | Nincs Android SDK | Fázis 1 Android fordítás | Android Studio vagy cmdline-tools (jóváhagyás kell) |
| B-03 | Nincs emulátor | 8.3 (TR-06 UI tesztek) | AVD vagy fizikai eszköz |
| B-04 | Verziómátrix nem ellenőrizhető | 1.2 (`libs.versions.toml`) | B-02 után `sdkmanager --list` |
| OQ-01 | Nincs hitelesített karate tartalom | az éles adatkészlet; a (C)/(D) kvíztípus aktiválása | felhasználói döntés / szakmai hitelesítő |

A blokkolók **nem** állítják le a munkát: a Fázis 0.9, 2.1–2.7, 3.1–3.3 és a dokumentáció
kódja és tartalma megírható; csak a *futtatás* igazolása vár a JDK-ra.

## Következő három lépés

1. **0.7** – A felhasználó átnézi és jóváhagyja a három tervdokumentumot (vagy javítást kér).
2. **0.8** – JDK 17 és Android SDK telepítése jóváhagyással, majd a verziómátrix rögzítése.
3. **0.9 + Fázis 1** – ADR-ek kiírása, majd a Gradle projektváz és a modulstruktúra.

## Változásnapló

- 2026-11-11 – Fázis 0.1–0.6 kész: környezetfelmérés, Claude környezet, követelmény-,
  funkcionális és architektúraspecifikáció. Build/teszt nem futott (B-01, B-02).
