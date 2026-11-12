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
| **`SavedStateHandle` unit tesztben** | Robolectric kell hozzá (`@Config(sdk = [34])`): a handle Android `Bundle` fölött validálja a tárolt értékeket. A modulnak `testImplementation(libs.robolectric)` + `libs.androidx.test.core` és `isIncludeAndroidResources = true` kell. |
| **`StateFlow` + `WhileSubscribed` tesztje** | Aktív feliratkozó nélkül **nem számol állapotot**, és a `.value` örökre a kezdőérték marad. Kell egy `launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }`, és a végén `job.cancel()`. A `viewModelScope`-hoz `Dispatchers.setMain(UnconfinedTestDispatcher())` a `@Before`-ban. |
| **`Loading` állapot őszinte mérése** | Ha a tesztdublőr azonnal visszatér, a `Loading` csak a `stateIn` kezdőértéke – a teszt nem azt méri, amit állít. Kapuzott dublőr kell (`Job.join()`), és a kapu kinyitása utáni állapotot is ellenőrizni kell. |
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

### 4.1 ViewModel és UiState (feature modulok)

| Tudnivaló | Részlet |
|---|---|
| **A képernyő kerete a sealed interfész közös tulajdonsága** | A `Loading`/`Empty`/`Content`/`Error` a *lista helyén* zárja ki egymást, de a keresősáv és a szűrőchipek **mind a négy** állapotban látszanak. Ha a keresőszöveg csak a `Content` ágon élne, az üres állapotban eltűnne a vezérlő, amivel a felhasználó visszavonhatja a saját szűkítését. Lásd `LibraryListUiState`. |
| **`SavedStateHandle` listaérték** | `ArrayList`-et kell tárolni, nem a `plus`/`minus` adta tetszőleges `List`-et, és a `getStateFlow` kezdőértéke se `emptyList()` legyen – a `Bundle` validálása az `java.util.Collections$EmptyList`-et nem fogadja el. |
| **Debounce-mentes törlés** | `debounce { if (query.isBlank()) 0L else 200L }`: a „×" megnyomása **visszavonás**, nem újabb keresés, ezért nem vár 200 ms-ot. A sima `debounce(200)` az első (kezdő) emissziót is késlelteti. |
| **Keresőszöveg és találat egy csomagban** | `flatMapLatest { q -> repo.observe(q).map { Snapshot(q, it) } }`, nem két `Flow` `combine`-ja: különben a „nincs találat a(z) …" üzenet egy pillanatra a *régi* találatokhoz az *új* kifejezést idézi. |
| **Az importot a képernyő ViewModelje indítja** | Nem az `Application` – csak így tud a UI valódi `Loading`-ot mutatni (FUNCTIONAL_SPEC 7.1). Az `ImportResult.Rejected` **nem** `Error`: olyankor a korábbi tartalom érvényben marad (FR-31). `Error` kizárólag kivételre jár. |
| **Szűrő találatszáma** | A chipek számát a **kijelölést figyelmen kívül hagyva** kell számolni, különben minden nem kijelölt chip nullát mutat, és épp arra a kérdésre nem válaszol, amire használják. Ebből következik, hogy egy szűrés nélküli lekérdezés elég, és a kategóriaszűrés memóriában fut. |
| **`@DefaultDispatcher`** | Rendezés, összegzés, normalizálás → `flowOn(defaultDispatcher)`. Az `@IoDispatcher` készlete blokkoló lemezműveletekhez van méretezve. |
| **Nem injektálunk, amire nincs szükség** | `Clock`/`Random` csak oda, ahol tényleg van időfüggő vagy véletlen logika. Egy nem használt függőség azt sugallná, hogy van. |
| **Modul saját `R` importja kötelező** | `android.nonTransitiveRClass=true` mellett a modul `R` osztálya a `namespace` csomagban (nem a hívó alcsomagjában) jön létre. Egy `feature/library/.../list/Xyz.kt` fájlban a modul saját erőforrásaihoz **explicit** `import hu.kyokushin.trainer.feature.library.R` kell – anélkül `Unresolved reference 'R'` fordítási hiba. |
| **`ColumnScope`/`RowScope.weight` NEM importálható** | A `Modifier.weight(...)` a `ColumnScope`/`RowScope` interfészek **tag** kiterjesztése, nincs hozzá top-level import. Egy `import androidx.compose.foundation.layout.weight` sor egy MÁS, `internal` jelölésű szimbólumra illeszkedik, és `Cannot access '...RowColumnParentData?.weight...': it is internal in file` hibával buktatja a fordítást. A `weight(...)` hívás a scope receiver (`Column`/`Row` lambdájában) miatt import nélkül is elérhető – ha a fordító nem találja, az import sort kell törölni, nem hozzáadni. |
| **„Összes” szűrőchip nem önálló ViewModel-akció** | A kategóriaszűrő „Összes” chipje (FUNCTIONAL_SPEC 3.1) a meglévő `onToggleCategory(categoryId)` hívásokra bontva kapcsolja ki a jelenleg kijelölt kategóriákat egyenként – nem kell hozzá külön `clearCategories()` ViewModel-metódus, mert a `SavedStateHandle`-be író metódus szinkron, és minden hívás a FRISS állapotból olvas. Lásd `CategoryFilterRow` a `feature/library`-ban. |
| **`TechniqueIllustration` `null` hivatkozása a listán ÜRES hely, nem helyőrző** | A komponens dokumentált szerződése szerint a `null` eset a **részletezőn** helyőrzővé alakul (a hívó konvertálja), a **listán** viszont szándékosan nem – „a hely marad üresen” (T-204 docstring). A listatételen egy fix szélességű `Box` tartja a helyet, hogy az illusztráció nélküli és az illusztrációval rendelkező sorok egyvonalban maradjanak. |
| **`hiltViewModel()` deprecation figyelmeztetés** | A `hilt-navigation-compose:1.4.0` `hiltViewModel()` függvénye deprecated-ként fordul (`Moved to package: androidx.hilt.lifecycle.viewmodel.compose`), de **fordítási hiba nélkül működik**. Nem blokkoló; a csere a T-607/eszközfrissítés köre. |
| **Útvonal-argumentum egy részletező ViewModelben `navigation` modul import nélkül** | A Navigation Compose minden `NavBackStackEntry`-hez tartozó `SavedStateHandle`-t előre feltölti a route argumentumaiból (androidx.navigation 2.4+, itt: `navigationCompose = "2.10.2"`) – ez a `hiltViewModel()` hívó modultól **függetlenül** működik. Egy `feature/*` ViewModel ezért közvetlenül kiolvashatja `savedStateHandle.get<String>("techniqueId")`-vel, **anélkül**, hogy a `navigation` modulra függne (ami L5-sértés lenne: a feature modulok nem függenek a `navigation`-re, az a fordított irány). A kulcs csak **névben** egyezik a `navigation` modul `NavArgs.TECHNIQUE_ID` értékével – saját string konstansként duplikálva, dokumentált megjegyzéssel. Lásd `LibraryDetailViewModel` (T-209). |
| **Eredmény visszaadása navigáció közben (chip → lista szűrése)** | Amikor egy mélyebb képernyő (`library/detail`) egy korábbi képernyő (`library/list`) állapotát akarja módosítani visszalépés előtt (pl. kategóriachip → vissza a listára, arra a kategóriára szűrve), a helyes Navigation Compose minta: `navController.previousBackStackEntry?.savedStateHandle?.set(KEY, value)`, majd `popBackStack()`. Ez **ugyanaz** a `SavedStateHandle`, amit a célképernyő ViewModelje kap – nincs szükség megosztott ViewModelre vagy eseménybuszra. A kulcsot (`LibraryListViewModel.KEY_CATEGORIES`) emiatt **nem** `private`: a `navigation` modul ismeri, de csak ezt a string konstanst, nem a ViewModel belsejét (L5: a `navigation` az egyetlen modul, amely minden feature modult lát). Lásd `KyokushinNavHost` LIBRARY_DETAIL ága (T-209). |

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

**Bash-ből** (ez a megbízható út; a PowerShell egyes session-ökben le van tiltva):

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
```

A `.bat` kiterjesztés Git Bash alatt is kell – a `./gradlew` (kiterjesztés nélkül) Windowson
nem a megfelelő indítót hívja. PowerShellben ugyanez `$env:JAVA_HOME=...` + `.\gradlew.bat`,
**ha** a PowerShell elérhető.

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
| Dokumentációs drift | `ADR-003` (`Asset` mezői), `ARCHITECTURE.md` 3. (`QuizAttempt.questionId`) és 4.1 (validátor-szerződés), `FUNCTIONAL_SPEC.md` 2.1 + 8. (`demoBanner` → lektorálatlanság-szalag), `FUNCTIONAL_SPEC.md` 3.2 9–10. pont (`Technique`-nek nincs övfok/nehézség mezője, és nincs technikaszintű `source` mező – az ADR-011 kanonikus séma csak az illusztrációknak ad `source`-t, a technikáknak nem; a `library/detail` (T-209) ezért ezt a két szakaszt soha nem rajzolja ki). Összevont átvezetés javasolt. |
| `aliases` mindenhol üres a valós tartalomban | Az alias-ág tesztje szintetikus adaton mér (R-05 kockázat). |
| `IllustrationRef.Asset` nem hordozza a kép saját `review_status`-át | Az `awaitingReview`-t a hívó dönti el a technika státuszából. Ha külön jelzés kell, `Asset` mező + ADR-003 kiegészítés. |
| `QuizCandidateSet.illustrationDigests` üres | A `TechniqueRepository` még nem tölti (T-402 tárgya). |
| `library/detail` (T-209) nem jeleníti meg az „eddig gyakoroltam" összegzést | A FUNCTIONAL_SPEC 3.2 11. pontja előírja, de nincs hozzá adatforrás: a `ProgressRepository` (T-301/T-401) még nincs megírva, és a `TechniqueRepository` szándékosan csak a tartalmat adja. Amikor a `ProgressRepository` elkészül, ezt a mezőt kell felvenni a `LibraryDetailUiState.Content`-be (suspend lekérdezés a `QuizAttemptDao.getByTechnique`-re épülő aggregáción, lásd `UserDaos.kt`) és egy `onOpenProgress` lambdát a `LibraryDetailRoute`-hoz, a `progress/technique/{techniqueId}` útvonalra (már létezik, T-206). |
| A „Hiányos" rekord hiányzó mezőinek levezetése ma csak az illusztrációt ismeri fel | A `DefaultKnowledgeValidator` (`core/quiz`) jelenleg kizárólag a lógó képhivatkozás miatt fokoz le `INCOMPLETE`-re (V005/ADR-002) – minden más kötelező mező hiánya a rekordot a leképezés előtt elveti. A `library/detail` (T-209) ezt egy helyi `missingFieldsFor(status)` függvénnyel tükrözi (`LibraryDetailViewModel.kt`). Ha a `progress/technique` részletező (T-506) ugyanezt a leképezést igényli, érdemes ezt a függvényt megosztott helyre (pl. `core/quiz` vagy `core/data`) emelni, hogy ne legyen két hely, ahol ez a szabály élhet. |
