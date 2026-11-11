---
name: kyo-android-module
description: A projekt Android/Compose kódkonvenciói - modulhatárok, rétegszabályok, Hilt DI, ViewModel és UiState minták, Navigation Compose, Room konvenciók, akadálymentesség és verziókezelés version cataloggal. Használd minden Kotlin/Compose/Room/Hilt/Gradle kód írásánál ebben a projektben.
---

# Android / Compose konvenciók

## 1. Modulhatárok és a rétegszabály

```
core/model     tiszta Kotlin domain modellek, nincs Android, nincs Room, nincs Compose
core/quiz      tiszta Kotlin kvízmotor + validátor; csak core/model-re függ
core/data      Room, assets import, repository implementációk; kifelé CSAK interfészt ad
feature/*      Compose UI + ViewModel; csak core/model + repository interfész + core/quiz
navigation     útvonalak, NavHost
di             Hilt modulok (a kötések itt élnek, nem a feature-ökben)
```

Függőségi irány: `feature/* → core/data interfész → core/model`. Visszafelé soha.
`core/quiz` nem tud `core/data`-ról.

**Tilos:**
- Compose vagy ViewModel kódban `@Entity`, `@Dao`, DTO vagy `kotlinx.serialization` típus.
- Room entitás kiszivárogtatása repository interfészen – a repository domain modellt ad.
- `android.*` import a `core/model` és `core/quiz` modulokban.

A mapper (entity ↔ domain, DTO → domain) a `core/data` belső ügye.

## 2. ViewModel és állapot

- Egy képernyő = egy `UiState` **sealed interface vagy data class** (`Loading`, `Empty`,
  `Content`, `Error` explicit állapotokkal – az üres állapot nem „üres lista a Contentben",
  ha a felhasználónak más üzenetet kell látnia).
- `StateFlow<UiState>` kifelé, `MutableStateFlow` befelé. `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`.
- Egyszeri eseményekre (navigáció, snackbar) `Channel`/`SharedFlow`, nem `StateFlow`.
- Konfigurációváltást túlélő, de folyamatot nem túlélő állapot: `SavedStateHandle`
  (kvízállapot, keresőszöveg, kártyapozíció).
- A ViewModel nem ismeri a Compose-t: nincs `@Composable`, nincs `Color`, nincs `painterResource`.
  Szövegre `@StringRes` vagy domain típus, nem előformázott string.

## 3. Compose

- Stateless composable + state hoisting. A képernyő-szintű composable kapja a `UiState`-et
  és lambda eventeket; a `ViewModel` csak a route-szintű wrapperben jelenik meg.
- `@Preview` minden nem triviális composable-hez, legalább világos+sötét témára, és az
  üres/hiányos adatállapotra is.
- Material 3, `MaterialTheme` tokeneken keresztül. Hardcode-olt `Color(0xFF...)` tilos a
  komponensekben – minden szín a téma sémából jön.
- Lista: `LazyColumn` stabil `key = { it.id }`-vel.

## 4. Akadálymentesség (nem opcionális)

- Minden illusztrációnak értelmes `contentDescription`-ja van magyarul (a technika
  megnevezése + hogy illusztráció). Dekoratív képnél `null`, de a kvízben a kép **nem**
  dekoratív – csak ott adj kép-leírást, ahol az nem fedi fel a helyes választ
  (`JP_TO_IMAGE` opcióknál a leírás nem mondhatja ki a technika nevét; ott pozíció alapú
  leírás kell, pl. „első válaszlehetőség illusztrációja").
- Érintési célpont ≥ 48dp.
- `Modifier.semantics` a kvízválasz-gombokon: `selected`, és a visszajelzés után
  `stateDescription` („helyes" / „helytelen") – a helyesség ne csak színnel jelenjen meg.
- Szövegméret skálázódjon (`sp`), a layout ne törjön nagy betűnél.
- Kontraszt: WCAG AA (4.5:1 normál szövegre) világos és sötét témában is.
- TalkBack bejárási sorrend ellenőrizve a kvíz és a tanulókártya képernyőn.

## 5. Hilt

- `@HiltAndroidApp` az Application-ön, `@AndroidEntryPoint` az egyetlen Activityn,
  `@HiltViewModel` a ViewModelekben.
- Kötések a `di` modulban: `@Binds` repository interfész → implementáció,
  `@Provides` Room database/DAO, `@Provides` seedelt RNG factory, `@Provides` clock.
- **Clock és Random injektálva**, soha nem közvetlenül `System.currentTimeMillis()` vagy
  `Random.Default` üzleti kódban – ez a tesztelhetőség feltétele.

## 6. Room

- Tartalomtáblák és felhasználói táblák **külön** logikai csoport, egy adatbázisban.
- A felhasználói tábla idegen kulcsa a tartalomra **nem** `onDelete = CASCADE`.
- Minden sémaváltás: `version` növelés + explicit `Migration` + ADR.
  `fallbackToDestructiveMigration()` **tilos** (felhasználói adatot veszítene).
- `suspend` DAO függvények és `Flow<...>` olvasásra. `allowMainThreadQueries` tilos.
- Statisztika aggregált `@Query`-vel, nem memóriában iterálva.
- `@TypeConverter` csak egyszerű típusokra (Instant→Long, enum→String).

## 7. Gradle és verziók

- Kotlin DSL (`build.gradle.kts`) és **version catalog** (`gradle/libs.versions.toml`).
  Verziószám csak a catalogban, soha a modulokban inline.
- **Verziót ne találj ki.** A konkrét verziók a telepített Android SDK-hoz és a hivatalos
  kompatibilitási mátrixhoz (AGP ↔ Gradle ↔ Kotlin ↔ Compose Compiler) illeszkedjenek.
  Ha nem tudod ellenőrizni, állj meg és dokumentálj, ne tippelj.
- `minSdk` olyan alacsony, amennyire a Compose és Room engedi, indoklás ADR-ben.
- `AndroidManifest.xml`: **nincs** `android.permission.INTERNET`. Egy Activity.
- Lint és formázás: `./gradlew lint` + ktlint; a CI-szerű ellenőrzés a `/kyo-test` része.

## 8. Tesztelés

- `core/quiz` és a validátor: sima JVM JUnit teszt, 100% szabálylefedés a cél
  (lásd `kyo-quiz-engine` invariánsok).
- Repository: fake DAO vagy in-memory Room.
- ViewModel: `kotlinx-coroutines-test` + `TestDispatcher`, injektált fake clock/RNG.
- Compose UI teszt: `createAndroidComposeRule`, csak a navigációra, tanulókártyára és egy
  teljes kvízre (ezek emulátort igényelnek – ha nincs, dokumentáld, ne hamisíts).
- Minden teszt neve utaljon a követelményazonosítóra, pl.
  `fun quizGenerator_sameSeed_producesIdenticalPlan_TR02()`.
