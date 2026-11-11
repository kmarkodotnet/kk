# ADR-008 – Build toolchain és függőségi verziómátrix

- **Állapot:** elfogadva (2026-11-11), **tényleges sikeres buildből igazolva**
- **Kontextus:** NFR-02 – „válassz egymással kompatibilis, stabil verziókat… ne találj ki
  verziószámokat". Korábban blokkoló volt (B-04), mert nem volt telepített SDK.

## Módszer

Egyetlen verziószám sem becslésből származik. A kiválasztás menete:

1. **Telepített eszközök tényleges verziója** (`java -version`, `android sdk list`).
2. **Gradle:** `https://services.gradle.org/versions/current` → 9.9.1, letöltés
   SHA-256 ellenőrzéssel (`d8a18a7a…ba228`).
3. **Minden függőség:** a hivatalos Maven metadata (`maven-metadata.xml`) legfrissebb
   kiadása, amelyből az `alpha`, `beta`, `rc`, `dev`, `SNAPSHOT` jelölésűek kizárva.
   Források: `dl.google.com/dl/android/maven2` (AndroidX, AGP) és `repo1.maven.org`
   (Kotlin, KSP, Hilt, kotlinx, JUnit, Truth, Robolectric).
4. **Igazolás:** `./gradlew assembleDebug` és `./gradlew testDebugUnitTest` lefuttatása.

## A rögzített mátrix

| Komponens | Verzió | Honnan |
|---|---|---|
| JDK | Temurin **17.0.21+9** | telepített, `java -version` |
| Gradle | **9.9.1** | services.gradle.org/versions/current |
| Android Gradle Plugin | **9.5.1** | Google Maven metadata (9.6.0 még alpha) |
| Kotlin | **2.4.21** | Maven Central (2.5.0 még RC) |
| KSP | **2.3.13** | Maven Central |
| `compileSdk` | **37** | lásd „Kényszerített emelés" |
| `targetSdk` | **36** | tudatosan konzervatív |
| `minSdk` | **26** | ADR-009 |
| build-tools | **37.0.0** | telepített SDK |
| platform-tools | 37.0.1 | telepített SDK |
| Compose BOM | **2026.09.00** | Google Maven |
| navigation-compose | 2.10.2 | Google Maven |
| lifecycle (viewmodel/runtime-compose) | 2.11.0 | Google Maven (2.12.0 még alpha) |
| activity-compose | 1.13.0 | Google Maven |
| core-ktx | 1.19.1 | Google Maven |
| Room | 2.8.5 | Google Maven |
| Hilt | 2.60.1 | Maven Central |
| hilt-navigation-compose | 1.4.0 | Google Maven |
| kotlinx-serialization-json | 1.12.0 | Maven Central |
| kotlinx-coroutines | 1.11.0 | Maven Central |
| JUnit4 / Truth / Robolectric | 4.13.2 / 1.4.5 / 4.17 | Maven Central |
| androidx.test ext-junit / core / espresso | 1.3.0 / 1.7.0 / 3.7.0 | Google Maven |

Mindez a `gradle/libs.versions.toml` version catalogban él; a modulok `build.gradle.kts`
fájljai inline verziót nem tartalmaznak.

## Három kényszer, amit a tényleges build tárt fel

### 1. AGP 9 beépített Kotlin-támogatása
Az AGP 9.0-tól a `org.jetbrains.kotlin.android` plugin **feleslegessé vált**, és az
alkalmazása hibát okoz:

> The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.

Ezért az Android modulok (`app`, `core/data`, `core/ui`, `feature/*`, `navigation`, `di`)
plugin-blokkjából kivettük. A tiszta Kotlin modulok (`core/model`, `core/quiz`) továbbra is
`org.jetbrains.kotlin.jvm`-et használnak – ez szándékos, és egyben megerősíti ADR-001-et:
ezek a modulok nem Android modulok.

### 2. `compileSdk = 37` kényszerítve
A `compileSdk = 36` beállítással a build elbukott: a legfrissebb stabil AndroidX kiadások
(pl. `androidx.core:core-ktx:1.19.1`, `androidx.compose.ui:ui-tooling-android:1.12.1`)
**API 37 vagy újabb** ellen fordítást követelnek. Ezért a `compileSdk` 37 lett, és a
`platforms/android-37.0` + `build-tools/37.0.0` csomagok telepítve.

A `targetSdk` szándékosan **36** maradt: a `compileSdk` emelése csak újabb API-k
*használatát* engedi, míg a `targetSdk` új *futásidejű viselkedésekre* iratkoztatna fel,
amit egy MVP-nek nem kell vállalnia. Ez a kombináció hivatalosan támogatott.

### 3. `local.properties` útvonal elválasztó
A Windows-stílusú, escape-elt útvonal (`C\:\\Users\\…`) az AGP 9 `SdkLocator`-ában
`java.io.IOException: The filename, directory name, or volume label syntax is incorrect`
hibát adott. A megoldás **előre dőlő törtvonal**: `sdk.dir=C:/Users/…/Android/Sdk`.
Ez a projekt `docs/ENVIRONMENT.md`-jében is rögzítve van, mert új gépen megismétlődik.

## Elvetett alternatíva

**AGP 8.13.2** (az utolsó 8.x stabil) kisebb migrációs kockázatot jelentett volna a
megszokott DSL miatt, de: (a) a 9.x stabil és már patch-kiadást is kapott, (b) a beépített
Kotlin-támogatás egyszerűbb konfigurációt ad (nincs külön Kotlin plugin verziózás),
(c) a projekt zöldmezős, nincs mit migrálni. A tényleges build mindkét build-céllal
sikeresen lefutott, így a 9.5.1 kockázata igazoltan alacsony.

## Következmények

- A build igazoltan működik: `assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
  (18,69 MB), `testDebugUnitTest` zöld (még nincs teszt – `NO-SOURCE`).
- A verziók emelése előtt a Maven metadata **újraellenőrzendő**; a catalog kommentje erre
  figyelmeztet.
- Ha az AndroidX később API 38-at követel, a `compileSdk` emelése külön, dokumentált lépés
  lesz – a `minSdk` és a `targetSdk` ettől függetlenül kezelhető.
