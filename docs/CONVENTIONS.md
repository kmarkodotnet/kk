# Konvenciók és buktatók – amit minden fejlesztőnek tudnia kell

> **Ezt a fájlt a `/dev` és a `/devall` minden feladat előtt beolvassa.**
> Itt a **felhalmozott tapasztalat** él: döntések, amiket már meghoztunk, és csapdák,
> amikbe már belefutottunk. Ha új konvenciót vagy buktatót fedezel fel, **írd ide hozzá**
> a feladatod lezárásakor – ez olcsóbb, mint hogy a következő hat ember újra felfedezze.

A normatív szabályok máshol vannak (`CLAUDE.md`, `docs/REQUIREMENTS.md`, `docs/adr/`).
Ez a lap nem helyettesíti őket, hanem a *gyakorlati* tudást gyűjti.

---

## 1. Domain és modulszerkezet

| Tudnivaló | Részlet |
|---|---|
| **Value class azonosítók** | `TechniqueId`, `CategoryId`, `SessionId`, `QuestionId` konstruktora **privát**. Példányosítás csak validáló gyárral: `TechniqueId.of("KY-0001")`. Üres/whitespace azonosító nem kerülhet a domainbe. |
| **Domain `require()`** | A `Technique`, `Category`, `QuizSession` konstruktora `require()`-rel validál és **kivételt dob** hibás bemenetre. Aki nyers adatból épít domain objektumot (importáló, mapper), annak **előbb** kell szűrnie. |
| **`quizEligible` nincs tárolva** | Számított érték (ADR-010). Az egyetlen megfogalmazása: `core/quiz/.../validation/QuizEligibility.kt`. **Ne írj mellé másodikat.** |
| **Normalizálás** | Az egyetlen definíció: `core/quiz/.../text/NameNormalizer.kt`. A validátor, a keresés és a szinonimacsoportok mind ezt használják. |
| **`core/model`, `core/quiz`** | Kotlin **JVM** modulok – `android.*` nem is fordul le bennük. Ez a rétegszabály fordítási idejű védelme (ADR-001). |
| **Hilt kötések helye** | A `KyokushinDatabase`, a DAO-k és a `KnowledgeContentReader` `internal`, ezért a Room/olvasó/repository kötések a **`core/data`-ban** élnek, nem a `di` modulban. A `di` a `Clock`, az RNG, a dispatcherek és a kvízmotor kötéseit tartja. (Eltérés az `ARCHITECTURE.md` eredeti 9. táblázatától, lásd 7.1 szakasz.) |
| **Téma helye** | `hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme` – a **`core/ui`-ban**, nem az `app`-ban. Minden Compose modul innen importálja. |

## 2. Adatbázis

| Tudnivaló | Részlet |
|---|---|
| **A séma a 2-es verzión van** | Az adatbázis-építésnél **kötelező** `addMigrations(*KyokushinMigrations.ALL)`. Destruktív tartalék (`fallbackToDestructiveMigration()`) **tilos** – migráció nélkül a frissítő felhasználónál összeomlás lenne. |
| **Nincs FK a felhasználói adaton** | A `quiz_attempt.techniqueId` és `study_review.techniqueId` oszlopán **nincs** idegenkulcs-kényszer (nem `NO_ACTION`) – az beszúrási kényszert adna, és újra összekötné a két életciklust (ADR-004). |
| **Dupla válasz** | `UNIQUE(sessionId, questionId)` + `OnConflictStrategy.IGNORE` → ütközéskor a beszúrás `-1`-et ad vissza, **nem dob kivételt**. Erre építs, ne `try/catch`-re (ADR-005). |
| **Enum tárolás** | A konstans **neve** stringként, nem `ordinal` – az átrendezés különben elrontaná a meglévő adatot. |
| **Sémaváltozás menete** | `version` emelés + explicit `Migration` + sémaexport commitolása + ADR + `MigrationTestHelper` teszt. Lásd ADR-012 mintát. |
| **Új `TechniqueEntity` fixtúra** | Kötelezően töltsd ki a `searchRomaji` / `searchHungarian` oszlopot a `NameNormalizer`-rel, különben a keresés nem talál rá. |

## 3. Tesztelés

| Tudnivaló | Részlet |
|---|---|
| **Kotlin JVM modul tesztje** | A `core/model` és `core/quiz` kapott egy `testDebugUnitTest` **gyűjtőtaskot**, ami a `test`-re mutat. Enélkül a kötelező DoD-parancs **csendben kihagyná** a tesztjeiket. Új JVM modulnál ugyanezt vedd fel. |
| **Robolectric** | `@Config(sdk = [34])` – a `compileSdk = 37`-hez nincs `android-all` csomag. A `manifest = Config.NONE` **nem működik** (Robolectric 4.17). A modulnak kell `testOptions { unitTests { isIncludeAndroidResources = true } }`. |
| **`UnconfinedTestDispatcher`** | Át kell adni a `runTest` `testScheduler`-ét, különben a `combine` + `flowOn` lánc `Detected use of different schedulers` hibával elhasal. |
| **Valós tartalom tesztben** | A `knowledgebase_transfer/` elérése a `kyo.repoRoot` rendszertulajdonságon át megy (a modul `build.gradle.kts`-ében `tasks.withType<Test>`). Új modulnál ugyanígy. |
| **Őszinte teljes futás** | `.\gradlew.bat test testDebugUnitTest --rerun-tasks` – a `UP-TO-DATE` task nem bizonyít semmit. |
| **Hilt gráf teszt** | `@HiltAndroidTest` + `HiltTestApplication` + Robolectric – a teljes `SingletonComponent` felépíthető **emulátor nélkül** (lásd `HiltGraphTest`). |
| **Teszt vs. kód** | Ha teszt bukik, alapból a **kódot** javítsd. Eddig háromszor fordult elő, hogy a teszt elvárása volt hibás – ilyenkor írd le, **miért** volt hibás. |

## 4. Compose és akadálymentesség

| Tudnivaló | Részlet |
|---|---|
| **`contentDescription` a kvízben** | A `core/ui` komponensek **szándék-típust** vesznek át, nem stringet: `IllustrationLabel.Technique(...)` / `QuizOption(position)` / `Decorative`. A `QuizOption` ágnak **nincs paramétere a technika nevére** – a válasz kiszivárgása így kifejezhetetlen, nem figyelem kérdése (NFR-04). |
| **Lektorálatlanság-szalag** | Az `UnreviewedBanner`-nek **nincs** `onDismiss` / `dismissible` / `visible` paramétere. Egyetlen bemenete a `DatasetTrust`; `REVIEWED` mellett nem rajzol semmit (ADR-010, AC-08). |
| **Üres állapot** | `EmptyStateReason` sealed típus – az **ok** hordozza az üzenetet és az akciócímkét. Ne adj át puszta szöveget. |
| **Hibaüzenet** | `ErrorState` `@StringRes`-t vesz át, nem `String`-et – hogy kivétel angol `message`-e ne jusson a felhasználóhoz. |
| **Szín** | Hardcode `Color(0xFF…)` **tilos** komponensben; minden szín `MaterialTheme` token. |
| **Érintési célpont** | ≥ 48 dp. A Material 3 alapértelmezett gombmagassága 40 dp – `heightIn(min = 48.dp)` kell. |
| **`@Preview`** | Világos **és** sötét (`UI_MODE_NIGHT_YES`), plusz a hiányos/üres adatállapot. |
| **Szöveg** | Minden magyar szöveg `strings.xml`-ben. A `core/ui` és a `navigation` modulnak saját `strings.xml`-e van. A pontos szövegeket a `FUNCTIONAL_SPEC.md` 8. fejezete adja – ne találj ki újakat. |

## 5. Navigáció

| Tudnivaló | Részlet |
|---|---|
| **Útvonalak egy helyen** | `navigation/.../KyokushinScreen.kt` enum + `NavArgs` + `KyokushinRoutes`. **Ne írj új útvonal-sztringet** a képernyőkben. |
| **Képernyő bekötése** | A `KyokushinNavHost.kt` `composable { … }` blokkjában a `PlaceholderScreen` helyét cseréld le. |
| **Feature modulok** | Nem navigálnak maguktól és nem függenek egymástól: **lambdát adnak kifelé** (L5 rétegszabály). |
| **Bottom nav elrejtése** | A `quiz/run` és `quiz/result` képernyőn nincs bottom nav – a döntés adatként él (`KyokushinScreen.showsBottomBar`), ne szórd el `if`-ekbe. |

## 6. Fordítási és eszközcsapdák

| Csapda | Mi történik | Megoldás |
|---|---|---|
| **KDoc `/*` karakterpár** | `feature/*` egy KDoc-ban beágyazott blokk-megjegyzést nyit → `Unclosed comment`. **Négyszer** buktatta el a buildet. | Írd `feature` modulok formában, vagy tedd backtickbe úgy, hogy a `/` és a `*` ne kerüljön egymás mellé. |
| **Regex string** | Sima `"…"` literálban az escape-ek elromlanak. | Nyers literál: `"""…"""`. |
| **`aapt` levágja a záró szóközt** | `", "` elválasztó üresen jön át a `strings.xml`-ből. | Zárd dupla idézőjelbe az erőforrás értékét. |
| **`sdkmanager` pontosvessző** | `platforms;android-36` → `Package not found` (a `.bat` szétvágja). | Új CLI, perjellel: `android sdk install platforms/android-36`. |
| **`local.properties` útvonal** | Escape-elt Windows útvonal → `SdkLocator` `IOException`. | Perjel: `sdk.dir=C:/Users/…/Android/Sdk`. |
| **AGP 9 beépített Kotlin** | `org.jetbrains.kotlin.android` plugin hibát okoz Android modulban. | Ne alkalmazd; a Kotlin JVM modulokban marad a `kotlin-jvm`. |

## 7. Build környezet

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

| | |
|---|---|
| JDK | Temurin 17.0.21 |
| Gradle | 9.9.1 (wrapper) |
| AGP / Kotlin / KSP | 9.5.1 / 2.4.21 / 2.3.13 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| **Nyitott blokkoló** | **B-03: nincs emulátor** → `connectedDebugAndroidTest` nem fut (T-606 oldja fel) |
| **Nincs konfigurálva** | `ktlintCheck` – a Gradle cél **nem létezik** (T-607 tárgya). Ne jelentsd futtatottnak. |

## 8. Ismert, nyitott ügyek

| Ügy | Állapot |
|---|---|
| `KY-0020` `japanese_romaji_ascii` = `"Kirotsuke"` a `"Ki o tsuke"` helyett | **Forrásadat-hiba** a `knowledgebase/` CSV-ben és JSON-ban. Az appot nem érinti (a validátor újraszámolja). Javítás a forráson. |
| Dokumentációs drift | `ADR-003` (`Asset` mezői), `ARCHITECTURE.md` 3. (`QuizAttempt.questionId`) és 4.1 (validátor-szerződés), `FUNCTIONAL_SPEC.md` 2.1 + 8. (`demoBanner` → lektorálatlanság-szalag). Összevont átvezetés javasolt. |
| `aliases` mindenhol üres a valós tartalomban | Az alias-ág tesztje szintetikus adaton mér (R-05 kockázat). |
| `IllustrationRef.Asset` nem hordozza a kép saját `review_status`-át | Az `awaitingReview`-t a hívó dönti el a technika státuszából. Ha külön jelzés kell, `Asset` mező + ADR-003 kiegészítés. |
| `QuizCandidateSet.illustrationDigests` üres | A `TechniqueRepository` még nem tölti (T-402 tárgya). |
