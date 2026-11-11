# T-203 · di: Hilt modulok

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05 |
| Modul | `di` |
| Méret | M (3–6 ó) |
| Függ | [T-201](T-201-core-data-techniquerepository.md) |
| Követelmény | NFR-03, NFR-07 |
| Teszt | — |

## Cél

A kötések egy helyen – köztük a tesztelhetőség két feltétele: injektált `Clock` és RNG.

## Teendők

- [x] `DatabaseModule` (`AppDatabase`, DAO-k), `RepositoryModule` (`@Binds` interfész → impl).
- [x] `QuizModule` (`QuizGenerator`, `KnowledgeValidator`, `RandomFactory`).
- [x] `TimeModule`: `Clock` – prod rendszeróra, teszt fix óra.
- [x] `DispatcherModule`: `@IoDispatcher`, `@DefaultDispatcher` qualifierek.

## Kész, ha

- [x] Az app elindul, a Hilt gráf felépül.
- [x] Üzleti kódban sehol nincs közvetlen `Clock.systemUTC()` vagy `Random.Default` példányosítás.

## Figyelj rá

- A dispatcher injektálása nélkül a ViewModel tesztek nem lennének determinisztikusak.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul – *nem keletkezett új felhasználói szöveg (DI kötések)*
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – *nem alkalmazható, ez a feladat nem tartalmaz UI-t*
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
| Dátum | 2026-11-11 |
| Commit | `aefc2f0` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 365 teszt, 0 hiba, 0 kihagyott (+7) |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

### Mi készült el

| Hilt modul | Gradle modul | Kötések |
|---|---|---|
| `DatabaseModule` | **`core/data`** | `KyokushinDatabase` (`addMigrations(*KyokushinMigrations.ALL)`, destruktív tartalék nélkül) + mind a 8 DAO |
| `ContentModule` | **`core/data`** | `KnowledgeAssetSource` → `AndroidKnowledgeAssetSource`, `KnowledgeContentReader` |
| `RepositoryModule` | **`core/data`** | `@Binds TechniqueRepository` → `RoomTechniqueRepository`, `@Binds ContentImporter` → `RoomContentImporter` |
| `TimeModule` | `di` | `Clock` = `Clock.systemUTC()` (UTC, DR-05) |
| `DispatcherModule` | `di` | `@IoDispatcher` = `Dispatchers.IO`, `@DefaultDispatcher` = `Dispatchers.Default` |
| `QuizModule` | `di` | `QuizGenerator` = `DefaultQuizGenerator`, `KnowledgeValidator` = `DefaultKnowledgeValidator()`, RNG = `Random.Default` |

Továbbá: `@DefaultDispatcher` qualifier (`core/data/di/Dispatchers.kt`, az `@IoDispatcher`
mellé), `KyokushinGraph` `@EntryPoint` (`di`), 7 új teszt
(`HiltGraphTest` × 4, `DatabaseModuleTest` × 3), új katalógusbejegyzés
`hilt-android-testing` ugyanabból a `hilt` kiadásból (2.60.1).

### Eltérés az ARCHITECTURE 9./7. táblázatától – szándékos

A terv mind a hat Hilt modult a `di`-hez rendeli. A megvalósításban a Room-, olvasó- és
repository-kötések a **`core/data`-ban** élnek, mert a `KyokushinDatabase`, a DAO-k, a
`KnowledgeContentReader`, a `RoomTechniqueRepository` és a `RoomContentImporter`
`internal` láthatóságúak (ADR-001, L3/L4), és `internal` típust csak az a modul tud
`@Provides`/`@Binds`-szal kötni, amelyik látja. Az alternatíva – a DAO-k publikussá tétele
a DI kedvéért – pontosan azt a modulhatárt nyitná meg, amit az ADR-001 zárva akar tartani.
A gráf a `SingletonComponent`-ben áll össze, tehát a kötés fizikai helye a rétegeket nem
mozdítja. Ugyanez az eltérés a T-117 (`@IoDispatcher` qualifier) és a T-201 nyomvonalán;
az `ARCHITECTURE.md` új **7.1** szakasza rögzíti a kötések tényleges helyét.

A `RandomFactory` sor `kotlin.random.Random` kötésként valósult meg: a seedelt sorozatot a
generátor építi a kapott `Long`-ból, a kötés egyetlen feladata a **seed sorsolása**
(ADR-007). Egy gyártó interfész a `di`-ben lakna, azt pedig a `feature` modulok
szándékosan nem látják.

### „Az app elindul, a Hilt gráf felépül" – mivel igazolt

1. **Fordítási időben:** a `KyokushinGraph` `@EntryPoint` mind a nyolc kötést kéri, így
   hiányzó vagy kétértelmű kötés mellett az `:app:hiltJavaCompileDebug` és vele az
   `assembleDebug` elbukik. (Entry point nélkül a Dagger csak a használt kötéseket oldja
   fel – a ViewModeleket a későbbi feladatok hozzák.)
2. **Futásidőben, emulátor nélkül:** a `HiltGraphTest` (`@HiltAndroidTest`,
   `HiltTestApplication`, Robolectric `sdk = [34]`) ugyanazt a `SingletonComponent`-et
   építi fel, és mind a nyolc kötést feloldja – a Room adatbázis megnyílik, az assetolvasó
   megkapja az `AssetManager`-t, a repository és az importáló megépül.

Valódi eszközön/emulátoron az indítás **nem** futott (B-03) – ezt a TR-06 (T-606) fedi, és
a `TEST_REPORT.md` „NEM FUTOTT" szakasza rögzíti.

**Megjegyzés:** a `@DefaultDispatcher`-nek még nincs üzleti fogyasztója – az első az első
ViewModel (T-207) lesz; a kötést a `HiltGraphTest` ennek ellenére feloldja, tehát nem
halott kötés. A `feature` modulok a `di`-re **nem** függenek, és nem is kell: a kötések a
`SingletonComponent`-ben állnak össze, a ViewModelek a `@HiltViewModel` konstruktorukon
kapják a függőségeiket. Két menet közbeni fordítási hiba tanulsága a `TEST_REPORT.md`-ben:
(1) KDoc-ban `/*` karakterpár nested blokk-megjegyzést nyit, (2) regexhez nyers string
literál kell.

