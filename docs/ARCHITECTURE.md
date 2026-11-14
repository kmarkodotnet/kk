# Architektúraterv – Kyokushin Karate Trainer

| | |
|---|---|
| Dokumentum | Architektúraterv |
| Verzió | 1.0 |
| Dátum | 2026-11-11 |
| Állapot | Jóváhagyásra vár |
| Alapja | `docs/REQUIREMENTS.md` v1.0, `docs/FUNCTIONAL_SPEC.md` v1.0 |

---

## 1. Architekturális célok és kényszerek

| # | Cél | Következmény |
|---|---|---|
| G1 | A **kvízhelyesség** a termék magja | a kvízlogika tiszta Kotlin, Android nélkül, JVM teszttel teljesen lefedve |
| G2 | **Offline, zárt** rendszer | nincs hálózati réteg, nincs `INTERNET` permission, nincs analitika |
| G3 | **Tartalom és felhasználói adat szétválasztása** | két külön tábla-csoport, nincs cascade, upsert-alapú frissítés |
| G4 | **Kódmódosítás nélküli tartalombővítés** | minden tartalmi tudás adatban (manifest + techniques), nem kódban |
| G5 | **Determinizmus** | injektált `Clock` és seedelt RNG, rendezett bejárás mindenhol |
| G6 | **Akadálymentesség és téma** | Material 3 tokenek, szemantikai annotációk, nincs hardcode szín |
| G7 | **Tesztelhetőség JDK-hiányos környezetben is írható kóddal** | az üzleti logika Android-független modulokban (lásd `docs/ENVIRONMENT.md`) |

Kényszerek: egyetlen APK, egyetlen Activity, minimális jogosultság, Gradle Kotlin DSL,
Hilt DI, Room perzisztencia, Jetpack Compose UI.

---

## 2. Modultérkép

```
app/                       Application, MainActivity, manifest, assets
├── core/model             tiszta Kotlin: domain modellek, value class ID-k, enumok
├── core/quiz              tiszta Kotlin: QuizGenerator, scoring, tudástár-validátor
├── core/data              Room, assets import, repository implementációk, mapperek
├── core/ui                megosztott Compose komponensek (jelölők, üres állapotok, ábra) + a téma
├── feature/library        Tudástár lista + részlet (Compose + ViewModel)
├── feature/learn          Tanulás beállítás + kártyák
├── feature/quiz           Kvíz konfiguráció + futás + eredmény
├── feature/progress       Haladás áttekintés + technika-részletező + beállítások
├── navigation             útvonalak, NavHost, bottom nav
└── di                     Hilt modulok (kötések központilag)
```

### 2.1 Függőségi gráf

```
         ┌──────────────┐
         │     app      │  (összeköt mindent, Hilt gyökér)
         └──────┬───────┘
                │
   ┌────────────┼─────────────────────────┐
   │            │                         │
navigation   feature/*                   di
   │            │  \                      │
   │            │   \                     │
   │       core/ui   \                    │
   │            │     \                   │
   └────────────┴──── core/data (csak implementáció)
                 │         │
            core/quiz   core/model
                 └─────────┘
```

**Szabályok (betartása auditált – lásd `.claude/agents/kyo-spec-auditor.md`):**

| # | Szabály | Ellenőrzés |
|---|---|---|
| L1 | `core/model` és `core/quiz`: **nincs** `android.*`, `androidx.*`, Room, Compose import | TR-07, auditor |
| L2 | `core/quiz` nem függ `core/data`-tól | modul-függőségi deklaráció |
| L3 | `feature/*` nem lát Room entitást, DAO-t, DTO-t – csak domain modellt és repository **interfészt** | TR-07, auditor |
| L4 | A repository **interfész** a `core/data` publikus API-ja; az implementáció `internal` | fordítási idejű láthatóság |
| L5 | `feature/*` modulok nem függenek egymástól; a navigáció a `navigation` modulban kötődik | modul-függőségi deklaráció |
| L6 | ViewModel nem importál Compose típust | auditor |

A `core/data` kifelé **interfészt** publikál, és a Room entitásokat `internal` láthatósággal
zárja magába. Ez fordítási időben kényszeríti ki az L3/L4 szabályt, nem csak konvencióval.

---

## 3. Domain modell (`core/model`)

```kotlin
@JvmInline value class TechniqueId(val value: String)
@JvmInline value class CategoryId(val value: String)
@JvmInline value class SessionId(val value: String)
@JvmInline value class QuestionId(val value: String)

enum class QuestionType { JP_TO_HU, HU_TO_JP, IMAGE_TO_JP, JP_TO_IMAGE }
enum class SelfRating  { KNEW, NEEDS_PRACTICE }
enum class DatasetKind { DEMO, PRODUCTION }
enum class VerificationStatus { VERIFIED, AWAITING_REVIEW, INCOMPLETE }

data class Category(
    val id: CategoryId,
    val nameHu: String,
    val sortOrder: Int,
)

data class Technique(
    val id: TechniqueId,
    val categoryId: CategoryId,
    val romaji: String,
    val hungarian: String,
    val japaneseScript: String?,
    val aliases: List<String>,
    val illustration: IllustrationRef?,   // null = nincs ábra
    val descriptionHu: String?,
    val difficulty: Int?,
    val beltRank: String?,
    val source: String?,
    val status: VerificationStatus,
    val quizEligible: Boolean,
    val retired: Boolean,                 // már nem része az aktuális tananyagnak
    val contentVersion: String,
)

sealed interface IllustrationRef {
    data class Asset(val ref: String, val fileName: String) : IllustrationRef
    data object Placeholder : IllustrationRef
}
```

**Döntés:** a `verified: Boolean` helyett `status: VerificationStatus` háromállapotú enum,
mert a funkcionális spec három különböző UI-jelölőt ír le (hitelesített / ellenőrzésre vár /
hiányos), és a boolean ezt nem tudja kifejezni. A JSON séma `verified` mezője marad boolean;
a `INCOMPLETE` állapotot a validátor származtatja. → ADR-002.

**Döntés:** `IllustrationRef` sealed, hogy a „van kép" és a „helyőrző" eset típusszinten
elkülönüljön. A `JP_TO_IMAGE`/`IMAGE_TO_JP` jogosultság így fordítási időben is olvasható
feltétel, nem rejtett `null`-ellenőrzés. → ADR-003.

Felhasználói adat domain modellje:

```kotlin
data class QuizAttempt(
    val id: Long, val sessionId: SessionId, val techniqueId: TechniqueId,
    val questionType: QuestionType, val selectedTechniqueId: TechniqueId,
    val correct: Boolean, val answeredAt: Instant,
)
data class StudyReview(
    val id: Long, val techniqueId: TechniqueId,
    val selfRating: SelfRating, val reviewedAt: Instant,
)
data class QuizSession(
    val id: SessionId, val startedAt: Instant, val finishedAt: Instant?,
    val requestedCount: Int, val answeredCount: Int, val seed: Long,
)
```

A `seed` a munkamenetben tárolódik: így egy megszakított kvíz pontosan ugyanúgy
rekonstruálható, és a hibajelentés reprodukálható.

---

## 4. `core/quiz` – a tiszta Kotlin mag

Két egymástól független felelősség, ezért két alcsomag:

### 4.1 `core.quiz.validation` – tudástár-validátor

```kotlin
interface KnowledgeValidator {
    fun validate(manifest: ManifestInput, techniques: List<TechniqueInput>): ValidationReport
}

data class ValidationReport(
    val accepted: List<Technique>,
    val rejected: List<RejectedRecord>,
    val issues: List<ValidationIssue>,
    val synonymGroups: List<Set<TechniqueId>>,   // kölcsönös distraktor-kizárás (V006)
    val datasetKind: DatasetKind,
    val contentVersion: String,
)

data class ValidationIssue(
    val code: IssueCode,            // V001..V010
    val severity: Severity,         // BLOCKING | WARNING
    val techniqueId: TechniqueId?,
    val field: String?,
    val detail: String,
)
```

A validátor **nem dob kivételt** és nem ismeri a fájlrendszert: a képfájl létezését
`Set<String> availableIllustrationFiles` bemenetként kapja. Így tisztán JVM-en tesztelhető
(TR-01). Soha nem emel státuszt, csak leszállít (`quizEligible` → `false`, `status` →
`AWAITING_REVIEW`/`INCOMPLETE`).

A `synonymGroups` a validátor kimenete, és a kvízgenerátor bemenete – ez a csatolási pont,
ami CR-07-et (szinonima nem lehet distraktor) kikényszeríti.

### 4.2 `core.quiz.generator` – kvízgenerátor

```kotlin
interface QuizGenerator {
    fun generate(request: QuizRequest): QuizPlan
    fun dryRun(request: QuizRequest): QuizReadiness   // a setup képernyő készenléti összegzése
}
```

A bemenet/kimenet pontos szerződése: `.claude/skills/kyo-quiz-engine/SKILL.md` 2. pont.
A részletes szabályok (invariánsok, distraktorválasztás, fallback) a 3–5. pontban, és
azok a `docs/QUIZ_RULES.md` kiadott formájában lesznek véglegesítve.

**Algoritmus (vázlat):**
```
1. jelöltszűrés: quizEligible && !retired && kategóriaszűrő
2. típusonkénti jogosultság: a (C)/(D) típus csak Asset illusztrációval bíró jelöltekre
3. determinisztikus rendezés: jelöltek sort by id  (SOHA nem Set-iterációs sorrend)
4. seedelt keverés (Fisher–Yates a seedelt RNG-vel)
5. kérdésenként:
     a. válassz fel nem használt helyes jelöltet
     b. distraktor-pool = kategórián belüli, nem szinonim, típusra jogosult jelöltek
     c. ha |pool| < 3 → feltöltés más kategóriából, ugyanazon szűrőkkel
     d. ha így sincs 3 → ezt a kérdést NEM hozzuk létre (shortfall okkal)
     e. opciók seedelt keverése
6. kimenet: QuizPlan(questions, skippedTypes, shortfall)
```

### 4.3 `core.quiz.scoring` – eredményszámítás

```kotlin
object QuizScoring {
    fun score(attempts: List<QuizAttempt>): Score   // null százalék, ha nincs válasz
}
data class Score(val answered: Int, val correct: Int, val ratio: Double?)
```

Tiszta függvény, nincs állapot. A `ratio` **nullable**: 0 megválaszolt kérdés → `null`,
nem `0.0` (FR-27). Ez a típusválasztás akadályozza meg, hogy a UI véletlenül 0%-ot mutasson.

---

## 5. `core/data` – perzisztencia és import

### 5.1 Rétegek

```
assets/knowledge/*.json
   │  kotlinx.serialization (ignoreUnknownKeys = true)
   ▼
ContentDto  ──► KnowledgeValidator (core/quiz) ──► ValidationReport
   │                                                     │
   │                                              accepted: List<Technique>
   ▼                                                     ▼
ContentImporter ─────────────────── upsert ──────► Room tartalomtáblák
                                                         │
                 repository interfész ◄──── mapper ◄─────┘
```

### 5.2 Room séma

**Tartalomtáblák** (frissíthető, upsert):
| Tábla | Kulcs | Megjegyzés |
|---|---|---|
| `category` | `id` TEXT | `nameHu`, `sortOrder`; `nameTranslations` (JSON string, ADR-013) |
| `technique` | `id` TEXT | minden technikamező + `status`, `quizEligible`, `retired`, `contentVersion`; kereséshez `search_romaji`, `search_hungarian` (ADR-012); `nameTranslations`, `descriptionTranslations` (JSON string, ADR-013) |
| `technique_alias` | `(techniqueId, alias)` | normalizált kereséshez `aliasNormalized` oszloppal |
| `synonym_group` | `(groupId, techniqueId)` | a validátor `synonymGroups` kimenete, hogy a generátor DB-ből is feltölthető legyen |

**Felhasználói táblák** (soha nem törlődnek tartalomfrissítéskor):
| Tábla | Kulcs | Megjegyzés |
|---|---|---|
| `quiz_session` | `id` TEXT | `startedAt`, `finishedAt?`, `requestedCount`, `answeredCount`, `seed` |
| `quiz_attempt` | `id` INTEGER AUTOINCREMENT | `sessionId`, `techniqueId`, `questionType`, `selectedTechniqueId`, `correct`, `answeredAt`; **egyedi index** `(sessionId, questionId)` |
| `study_review` | `id` INTEGER AUTOINCREMENT | `techniqueId`, `selfRating`, `reviewedAt` |
| `content_state` | `id` = 1 | a betöltött `schemaVersion`, `contentVersion`, `datasetKind`, a validációs jelentés összegzése |

**Kritikus séma-döntések:**
- A `quiz_attempt.techniqueId` **nem** idegen kulcs `CASCADE DELETE`-tel. Vagy nincs FK
  kényszer, vagy `onDelete = NO_ACTION` – és a tartalomtörlés helyett `retired = true`
  jelölést használunk (DR-07). → ADR-004.
- A `quiz_attempt` egyedi indexe `(sessionId, questionId)`-n **adatbázis-szinten** teszi
  lehetetlenné a dupla válasz rögzítését (FR-18). Ez a második védelmi vonal a kvízállapot
  ellenőrzése mögött. → ADR-005.
- `fallbackToDestructiveMigration()` **tilos**; minden `version` emelés explicit `Migration`-t
  és ADR-t kap (DR-08). A jelenlegi sémaverzió **3**: a `technique` két
  előre normalizált keresőoszlopa (`KyokushinMigrations.MIGRATION_1_2`, ADR-012),
  majd a `technique`/`category` fordítás-oszlopai (`MIGRATION_2_3`, ADR-013).
- Időbélyeg `Instant` ↔ `Long` (epoch milli) `@TypeConverter`-rel; enum ↔ `String`
  (nem ordinal, mert az átrendezésre törékeny).

### 5.3 Repository interfészek (a `core/data` publikus API-ja)

```kotlin
interface TechniqueRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeTechniques(query: String, categories: Set<CategoryId>): Flow<List<Technique>>
    suspend fun getTechnique(id: TechniqueId): Technique?
    suspend fun getQuizCandidates(categories: Set<CategoryId>): QuizCandidateSet
    fun observeContentState(): Flow<ContentState>
}

interface ProgressRepository {
    suspend fun startSession(requestedCount: Int, seed: Long): SessionId
    suspend fun recordAttempt(attempt: NewAttempt): Boolean   // false = már válaszolt
    suspend fun finishSession(id: SessionId)
    suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating)
    fun observeStudyReviewSummary(): Flow<StudyReviewSummary>   // "Tudtam" / "Gyakorolnom kell" (FR-28, T-301)
    fun observeOverallProgress(): Flow<OverallProgress>
    fun observeCategoryProgress(): Flow<List<CategoryProgress>>
    fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress>
    fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>>
    suspend fun clearAllProgress()
}

interface ContentImporter {
    suspend fun importIfNeeded(): ImportResult
    suspend fun lastValidationReport(): ValidationReport?   // debug diagnosztika
}
```

A `recordAttempt` **`Boolean`-t ad vissza** (`false`, ha a kérdésre már volt válasz) –
így az egyszeri-válasz szabály a repository szerződésének része, nem UI-konvenció.

A statisztikák **aggregált SQL `@Query`-ből** jönnek (DR-09, NFR-10), nem memóriában
számolt listákból. Az `observeCategoryProgress` például `GROUP BY categoryId` lekérdezés.

---

## 6. Prezentációs réteg

### 6.1 Minta

Egyirányú adatfolyam, képernyőnként egy ViewModel:

```
UI esemény ──► ViewModel.onEvent() ──► repository / generator (suspend)
                      │
                      ▼
            MutableStateFlow<UiState>
                      │
                      ▼
      StateFlow<UiState> ──► Compose képernyő (stateless)
```

- Minden képernyő `UiState`-je **sealed interface**: `Loading` / `Empty(reason)` /
  `Content(...)` / `Error(cause)`. Az `Empty` okot hordoz (nincs tartalom vs. nincs találat),
  mert a funkcionális spec különböző szöveget ír elő.
- Egyszeri eseményre (navigáció, snackbar) `Channel` → `Flow`, nem `StateFlow`.
- Átmeneti, de forgatást túlélő állapot: `SavedStateHandle` (keresőszöveg, szűrők,
  kártyapozíció, **teljes kvízállapot**).
- A ViewModel nem formáz szöveget: `@StringRes` azonosítót vagy domain értéket ad át.

### 6.2 A kvíz állapotkezelése (a legkritikusabb pont)

```kotlin
// SavedStateHandle-ben szerializálva
data class QuizRunState(
    val sessionId: SessionId,
    val seed: Long,
    val questions: List<QuizQuestion>,      // csak ID-k, nem megjelenített szöveg
    val currentIndex: Int,
    val answers: Map<QuestionId, TechniqueId>,
    val shortfall: Shortfall?,
)
```

Miért ez a megoldás:
- A `seed` és a `questions` mentése miatt a forgatás után **bitre ugyanaz** a kvíz áll vissza
  (FR-19, NFR-07) – nem kell újragenerálni, és ha mégis újragenerálódik, ugyanazt adja.
- Az `answers` map a megválaszoltság egyetlen igazsága → a dupla válasz a ViewModelben
  kiszűrődik, a DB egyedi index pedig a maradék versenyhelyzetet is lezárja (FR-18).
- A `questions` csak azonosítókat tartalmaz; a megjelenítendő szöveget és képet a UI a
  tudástárból oldja fel. Így a helyes válasz összehasonlítása mindig ID-alapú (DR-03).

### 6.3 Compose és téma

- Material 3, `MaterialTheme` színsémán keresztül; dinamikus szín (Material You) opcionálisan,
  de a kontraszt mindkét témában ellenőrzött (NFR-04).
- Hardcode `Color(0xFF…)` a komponensekben **tilos**; minden szín téma-token.
- A helyesség/helytelenség jelzése **ikon + szöveg + szín** hármasban (nem csak szín).
- Megosztott komponensek a `core/ui`-ban: `TechniqueIllustration`, `VerificationBadge`,
  `EmptyState`, `DemoBanner`, `ProgressBarWithLabel`, `QuizOptionButton`.
- `@Preview` minden nem triviális komponenshez: világos + sötét + hiányos adat.
- A téma (`KyokushinTrainerTheme`, paletta) a **`core/ui`-ban** él, nem az `app`-ban:
  a megosztott komponensek `@Preview`-jai és a funkciómodulok képernyői is használják,
  a függőségi irány viszont az `app` felé mutat, visszafelé nem (ADR-001). Döntés: T-204.

---

## 7. Dependency injection (`di`)

| Hilt modul | Kötések | Scope |
|---|---|---|
| `DatabaseModule` | `AppDatabase`, DAO-k | `@Singleton` |
| `RepositoryModule` | `@Binds` repository interfész → `internal` impl | `@Singleton` |
| `QuizModule` | `QuizGenerator`, `KnowledgeValidator`, `RandomFactory` | `@Singleton` |
| `TimeModule` | `Clock` (prod: rendszeróra, teszt: fix) | `@Singleton` |
| `DispatcherModule` | `@IoDispatcher`, `@DefaultDispatcher` qualifierek | `@Singleton` |

**Kötelező:** `Clock` és RNG mindig injektált. Üzleti kódban `System.currentTimeMillis()`,
`Instant.now()`, `Random.Default` **nem fordulhat elő** (NFR-07, auditor ellenőrzi).
A dispatcher is injektált, hogy a ViewModel tesztelhető legyen `TestDispatcher`-rel.

### 7.1 A kötések tényleges helye (T-203, eltérés a fenti táblázattól)

A fenti táblázat mindegyik modult a `di`-hez rendeli; a megvalósításban a kötések **két**
Gradle modulban élnek, mert a Room típusok `internal`-ok (ADR-001, L3/L4), és egy
`internal` típust csak az a modul tud kötni, amelyik látja:

| Hilt modul | Gradle modul | Kötések |
|---|---|---|
| `DatabaseModule` | `core/data` | `KyokushinDatabase` (`addMigrations(*KyokushinMigrations.ALL)`, destruktív tartalék **nélkül**, DR-08) + mind a 8 DAO |
| `ContentModule` | `core/data` | `KnowledgeAssetSource` → `AndroidKnowledgeAssetSource`, `KnowledgeContentReader` |
| `RepositoryModule` | `core/data` | `@Binds TechniqueRepository` → `RoomTechniqueRepository`, `@Binds ContentImporter` → `RoomContentImporter` |
| `TimeModule` | `di` | `Clock` = `Clock.systemUTC()` |
| `DispatcherModule` | `di` | `@IoDispatcher` = `Dispatchers.IO`, `@DefaultDispatcher` = `Dispatchers.Default` |
| `QuizModule` | `di` | `QuizGenerator` = `DefaultQuizGenerator`, `KnowledgeValidator` = `DefaultKnowledgeValidator()`, RNG = `Random.Default` |

A qualifier **annotációk** (`@IoDispatcher`, `@DefaultDispatcher`) a `core/data`-ban élnek,
mert azoknak a moduloknak kell látniuk őket, amelyek az injektálást kérik; a kötésük a
`di`-ben dől el. A gráf a `SingletonComponent`-ben áll össze, tehát a kötés fizikai helye a
modulhatárokat nem mozdítja – az alternatíva (a DAO-k publikussá tétele a DI kedvéért)
pontosan azt a határt nyitná meg, amit az ADR-001 zárva akar tartani.

Az ARCHITECTURE `RandomFactory` sora **`kotlin.random.Random`** kötésként valósult meg: a
seedelt sorozatokat már a generátor építi a kapott `Long`-ból, a kötés egyetlen feladata a
**seed sorsolása** (ADR-007). Egy gyártó interfész a `di`-ben lakna, azt pedig a
`feature` modulok szándékosan nem látják.

A gráf teljességét a `di` modul `KyokushinGraph` `@EntryPoint`-ja **fordítási időben**
kikényszeríti (hiányzó kötés → `assembleDebug` elbukik), futásidőben pedig a
`HiltGraphTest` (app modul, Robolectric) építi fel és kérdezi le – emulátor nélkül.

---

## 8. Adatfolyam végig: egy kvízkérdés megválaszolása

```
felhasználó koppint az opcióra
 └─ QuizRunScreen → onAnswer(questionId, selectedTechniqueId)
     └─ QuizRunViewModel
         ├─ ha questionId ∈ state.answers  → NO-OP (dupla védelem 1)
         ├─ correct = (selected == question.correctTechniqueId)     [ID-alapú!]
         ├─ ProgressRepository.recordAttempt(NewAttempt(...))
         │     └─ Room insert; UNIQUE(sessionId, questionId)        (dupla védelem 2)
         │         └─ ütközés → false, nincs új rekord
         ├─ state = state.copy(answers = answers + (qId to selected))
         └─ UI: visszajelzés + a helyes technika romaji+magyar+ábra feloldása a tudástárból
```

Az `answeredAt` az injektált `Clock`-ból jön. A statisztika ebből a `quiz_attempt` sorból
számolódik később – nincs külön számláló, amit frissíteni kellene (DR-09).

---

## 9. Tesztelési architektúra

| Szint | Mit | Hogyan | Fedi |
|---|---|---|---|
| Tiszta JVM unit | `KnowledgeValidator` | táblázatos tesztek hibás bemenetekre | TR-01 |
| Tiszta JVM unit | `QuizGenerator` | invariánsok + seed-determinizmus + fallback | TR-02, TR-03 |
| Tiszta JVM unit | `QuizScoring` | 0%, 100%, félbehagyott, nulla válasz | TR-04 |
| Robolectric / in-memory Room | repository, `ContentImporter` | tartalomfrissítés + előrehaladás megőrzése | TR-05 |
| JVM unit | ViewModelek | `kotlinx-coroutines-test`, fake repository, fix `Clock` | FR-18, FR-19 |
| Statikus | rétegszabály | import-ellenőrzés (lint/teszt/auditor) | TR-07, TR-08 |
| Instrumentált (emulátor) | navigáció, tanulókártya, teljes kvíz | `createAndroidComposeRule` | TR-06 |

Minden teszt neve utal a követelményazonosítóra, pl.
`quizGenerator_sameSeed_producesIdenticalPlan_TR02`.

**Környezeti megjegyzés:** a jelenlegi gépen nincs JDK és nincs Android SDK
(`docs/ENVIRONMENT.md`, B-01/B-02), ezért **egyetlen teszt sem futott még**. A fenti
architektúra szándékosan úgy osztja el a logikát, hogy a termék helyességének nagy része
sima JVM teszttel igazolható lesz, amint a JDK rendelkezésre áll.

---

## 10. Technológiaválasztás és verziók

| Terület | Választás | Indok |
|---|---|---|
| Nyelv | Kotlin | követelmény (NFR-02) |
| UI | Jetpack Compose + Material 3 | követelmény; deklaratív UI, jó akadálymentességi API |
| Navigáció | Navigation Compose | követelmény; type-safe útvonalakkal |
| Állapot | ViewModel + StateFlow + `SavedStateHandle` | konfigurációváltás túlélése (FR-19) |
| Perzisztencia | Room | követelmény; aggregált SQL statisztikákhoz ideális |
| DI | Hilt | követelmény; `Clock`/RNG injektálás egyszerű |
| Szerializáció | kotlinx.serialization | követelmény; `ignoreUnknownKeys` sémabővíthetőséghez |
| Build | Gradle Kotlin DSL + version catalog | követelmény; verziók egy helyen |
| Teszt | JUnit + AndroidX Test + `kotlinx-coroutines-test` | követelmény |

**Verziószámok:** a konkrét AGP / Gradle / Kotlin / Compose / Room / Hilt verziók
`gradle/libs.versions.toml`-ben lesznek, és **csak a telepített Android SDK és a hivatalos
kompatibilitási mátrix ellenőrzése után** kerülnek bele. Addig nem rögzítünk számot, mert
verziót kitalálni tilos (NFR-02, blokkoló B-04 a `docs/ENVIRONMENT.md`-ben).

A `minSdk` értéke szintén nyitott (OQ-03): a Compose és Room tényleges minimumához igazítjuk,
ADR-ben indokolva.

---

## 11. Architekturális döntések (ADR index)

| ADR | Döntés | Állapot |
|---|---|---|
| [ADR-001](adr/ADR-001-multi-module.md) | Teljes modulbontás (11 Gradle modul) – a rétegszabály fordítási időben kényszerül ki | **elfogadva** |
| [ADR-002](adr/ADR-002-verification-status.md) | `VerificationStatus` háromállapotú enum a `verified: Boolean` helyett a domainben | **elfogadva** |
| [ADR-003](adr/ADR-003-illustration-ref.md) | `IllustrationRef` sealed típus a nullable képhivatkozás helyett | **elfogadva** |
| [ADR-004](adr/ADR-004-retired-not-deleted.md) | Tartalomtörlés helyett `retired = true`; nincs `CASCADE DELETE` a felhasználói adatra | **elfogadva** |
| [ADR-005](adr/ADR-005-unique-answer-index.md) | `UNIQUE(sessionId, questionId)` index a dupla válasz adatbázis-szintű kizárására | **elfogadva** |
| [ADR-006](adr/ADR-006-questions-hold-ids-only.md) | A kvízkérdés csak technikaazonosítókat tárol, megjelenített szöveget/képet nem | **elfogadva** |
| [ADR-007](adr/ADR-007-seed-persistence.md) | A `seed` a `QuizSession`-ben és a `SavedStateHandle`-ben is tárolódik a reprodukálhatóságért | **elfogadva** |
| [ADR-008](adr/ADR-008-version-matrix.md) | Build toolchain és függőségi verziómátrix (AGP 9.5.1, Kotlin 2.4.21, Gradle 9.9.1, compileSdk 37) | **elfogadva, buildből igazolva** |
| [ADR-009](adr/ADR-009-min-sdk.md) | `minSdk = 26` (Android 8.0) – natív `java.time`, nincs desugaring | **elfogadva** |

| [ADR-010](adr/ADR-010-quiz-on-unreviewed-content.md) | Kvíz engedélyezése lektorálatlan tartalmon, végig jelölve; `INCOMPLETE` rekord továbbra is kizárva | **elfogadva** |
| [ADR-011](adr/ADR-011-canonical-content-schema.md) | A kapott tudástár sémája a kanonikus formátum (nincs konverziós réteg) | **elfogadva** |
| [ADR-012](adr/ADR-012-sql-side-search-columns.md) | SQL-oldali keresés előre normalizált `search_romaji` / `search_hungarian` oszlopokon; séma 1 → 2 explicit migrációval | **elfogadva, tesztből igazolva** |
| [ADR-013](adr/ADR-013-multi-language-content.md) | Többnyelvűsítés: nyílt fordítás-map a tartalomban (JSON-oszlop, nem relációs tábla), rendszernyelv-követés, magyar visszaesés; séma 2 → 3 | **elfogadva** |

Az ADR-ek részletes szövege a [`docs/adr/`](adr/) könyvtárban található.

> **ADR-010 és ADR-011 felülír részeket ebben a dokumentumban:** az 5. fejezet séma- és
> mezőnevei, valamint a `quizEligible` szigorú kapuja. A Fázis 3 implementációja az
> ADR-eket követi; az 5. fejezet átírása a Fázis 3.3 lépés része.

---

## 12. Bővíthetőségi előkészítés (kód nélkül)

A következőket az architektúra **lehetővé teszi**, de az MVP nem implementálja:

| Jövőbeli igény | Mi készíti elő |
|---|---|
| Többképkockás mozdulatábra | `IllustrationRef` sealed → új `Sequence(frames)` változat, séma `schemaVersion` emeléssel |
| Hangos kiejtés | a `Technique` rekord új opcionális média-hivatkozással bővíthető |
| Spaced repetition | a `quiz_attempt` és `study_review` teljes előzményt tárol, amiből SM-2/FSRS utólag számolható |
| Kyu/dan tanterv | `beltRank` mező már a sémában van, kategóriaként nem hardcode-olt |
| Felhős tartalomszinkron | a `ContentImporter` interfész mögé beköthető másik forrás; a validátor változatlan |
| Többnyelvűség | minden UI szöveg `strings.xml`-ben; a tartalom `hungarian` mezője később nyelvkulcsos map-re bontható |

Egyik sem igényli a kvízmotor vagy a rétegszabályok megbontását – ez a terv fő értéke.

---

## 13. Ismert architekturális kockázatok

| # | Kockázat | Hatás | Kezelés |
|---|---|---|---|
| R1 | Nincs hitelesített tartalom → a kvíz nem indítható el hitelesen | a termék fő funkciója demó módban korlátozott | a motor és a UI teljes értékű; a fallback üzenet explicit (FR-22); az éles tartalom importtal jön (FR-34) |
| R2 | Nincs build környezet → a tervek nem igazolhatók futó kóddal | nem tudjuk bizonyítani a helyességet | `docs/ENVIRONMENT.md` + `/kyo-setup-env`; a logika Android-független modulokban, hogy a JDK megjelenésekor azonnal tesztelhető legyen |
| R3 | Többmodulos Gradle beállítás overhead | lassabb kezdés | a modulhatárok a rétegszabály kikényszerítését adják vissza; ADR-001 |
| R4 | Kevés technika kategóriánként → sok kihagyott kérdés | gyenge kvízélmény | a setup képernyő előre jelzi a készenlétet (5.1); a distraktor-feltöltés kategórián kívülről is engedett |
| R5 | A validátor szinonima-felderítése túl szigorú vagy túl laza | hibás distraktor vagy indokolatlan kihagyás | a `synonymGroups` adat, nem kód: emberi felülvizsgálatra listázva a `/kyo-validate-kb` jelentésben |
