# Tesztjelentés

Ez a dokumentum **append-only napló**: csak tényleges, lefuttatott parancsokat és azok
valódi kimenetét tartalmazza. Korábbi bejegyzés nem törlődik és nem íródik át.

**Szabály:** „a tesztek sikeresek" állítás kizárólag akkor szerepelhet itt, ha a parancs
lefutott és az exit kódja 0 volt. Ami nem futott, az explicit „NEM FUTOTT" sorként jelenik meg.

---

## 2026-11-11 – Környezetfelmérés (nem teszt)

| Parancs | Exit | Kimenet |
|---|---|---|
| `git --version` | 0 | `git version 2.47.0.windows.2` |
| `java -version` | ≠0 | `java: command not found` |
| `gradle -v` | ≠0 | `gradle: command not found` |
| `winget --version` | 0 | `v1.29.380` |

Környezeti változók: `JAVA_HOME` üres, `ANDROID_HOME` üres, `ANDROID_SDK_ROOT` üres.
Nem létező útvonalak: `%LOCALAPPDATA%\Android\Sdk`, `C:\Program Files\Android\Android Studio`,
`C:\Program Files\Java`, `C:\Program Files\Eclipse Adoptium`, `%USERPROFILE%\.gradle`.
JDK keresés a `C:\` és `F:\` gyökér alatt (`*jdk*` könyvtárnév): nincs találat.

---

## 2026-11-11 – Build és teszt állapot

| Cél | Állapot | Ok |
|---|---|---|
| `./gradlew testDebugUnitTest` | **NEM FUTOTT** | nincs JDK telepítve (B-01); a Gradle wrapper is JVM-et igényel. Projektváz sem létezik még. |
| `./gradlew assembleDebug` | **NEM FUTOTT** | nincs JDK (B-01) és nincs Android SDK (B-02) |
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** | B-01, B-02, továbbá nincs emulátor vagy csatlakoztatott eszköz (B-03) |
| `./gradlew lint` | **NEM FUTOTT** | B-01, B-02 |

### Tesztkövetelmény-lefedettség

| TR | Állapot |
|---|---|
| TR-01 JSON-validáció | nincs implementáció, nincs teszt |
| TR-02 Kvízgenerálás | nincs implementáció, nincs teszt |
| TR-03 Kategóriaszűrés / típusjogosultság | nincs implementáció, nincs teszt |
| TR-04 Eredményszámítás | nincs implementáció, nincs teszt |
| TR-05 Tudástárfrissítés | nincs implementáció, nincs teszt |
| TR-06 UI tesztek | nincs implementáció, nincs teszt |
| TR-07 Rétegszabály | nincs kód, nincs ellenőrzés |
| TR-08 Offline / manifest | nincs manifest |

**Összegzés: eddig egyetlen automatizált teszt sem futott.** Az eddig elkészült munka
dokumentáció (követelmény-, funkcionális és architektúraspecifikáció) és a Claude
fejlesztői környezet. A blokkolók feloldása: `docs/ENVIRONMENT.md` 4. pont,
illetve a `/kyo-setup-env` parancs.

### Debug APK

Nem készült el. Útvonala a build után: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 2026-11-11 – Build környezet felállítva, első sikeres build

### Telepítés (tényleges parancsok, mind exit 0)

| Parancs | Eredmény |
|---|---|
| `winget install EclipseAdoptium.Temurin.17.JDK` | `Successfully installed` – Temurin 17.0.21+9 |
| `winget install Google.AndroidStudio` | `Successfully installed` – 2026.2.2.8 |
| cmdline-tools `commandlinetools-win-16111833_latest.zip` | kicsomagolva `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` alá |
| `android sdk install platform-tools` | 37.0.1 |
| `android sdk install platforms/android-36` | telepítve |
| `android sdk install platforms/android-37.0` | telepítve |
| `android sdk install build-tools/36.1.0` | telepítve |
| `android sdk install build-tools/37.0.0` | telepítve |
| Gradle 9.9.1 letöltés | SHA-256 **ellenőrizve**: `d8a18a7a…ba228` → `checksum OK` |
| `gradle wrapper --gradle-version 9.9.1` | `BUILD SUCCESSFUL in 31s` |

Megjegyzés: az `sdkmanager` a `platforms;android-36` pontosvesszős szintaxissal
„Package not found" hibát adott (a wrapper .bat szétvágja a pontosvesszőt); a működő forma a
új `android sdk install platforms/android-36` perjeles alak. Rögzítve `docs/ENVIRONMENT.md`-ben.

### Build és teszt (tényleges futtatás)

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew assembleDebug` | **0** | `BUILD SUCCESSFUL in 1m 11s`, 273 task (142 executed, 11 from cache, 120 up-to-date) |
| `./gradlew testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 29s`, 177 task. **Minden `testDebugUnitTest` task `NO-SOURCE`: még egyetlen teszt sem létezik.** |

**Debug APK:** `app/build/outputs/apk/debug/app-debug.apk` – **18,69 MB**, elkészült.

Két hiba merült fel és lett javítva a sikeres build előtt (részletek: ADR-008):
1. `org.jetbrains.kotlin.android` plugin – AGP 9.0-tól felesleges, hibát okoz; eltávolítva
   az Android modulokból.
2. `compileSdk = 36` – a stabil AndroidX kiadások API 37-et követelnek; 37-re emelve.
3. `local.properties` escape-elt Windows útvonal – `SdkLocator` IOException; perjeles
   útvonalra cserélve.

### Manifest ellenőrzés (TR-08 előzetes, kézi)

Az összevont manifest (`app/build/intermediates/merged_manifest/debug/.../AndroidManifest.xml`)
**nem tartalmaz `android.permission.INTERNET`-et.** Egyetlen `uses-permission` szerepel benne:
`hu.kyokushin.trainer.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` – ezt az AndroidX
runtime adja hozzá automatikusan, saját aláírásszintű, alkalmazás-privát jogosultság.

**Következmény a TR-08 tesztre:** az assertion ne „nulla permission" legyen, hanem
„nincs `android.permission.INTERNET`" – különben az automatikusan hozzáadott receiver-
jogosultság miatt hamisan bukna.

### Tesztkövetelmény-lefedettség (változatlan)

TR-01…TR-07: **nincs implementáció, nincs teszt.** TR-08: kézi ellenőrzéssel teljesül,
automatizált teszt még nincs.

**Összegzés: a build és a teszt infrastruktúra igazoltan működik, de automatizált teszt
még nem létezik.** A Fázis 2 (validátor, kvízmotor, scoring + TR-01…TR-04) következik.

---

## T-101 · `core/model` azonosítók és enumok (2026-11-11)

### Tényleges futtatás

Környezet: Windows 11, PowerShell, `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 15s`, 180 actionable task. **`:core:model:test` lefutott: 13 teszt, 0 hiba, 0 kihagyott.** Minden `:*:testDebugUnitTest` task továbbra is `NO-SOURCE` (azokban a modulokban még nincs teszt). |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL in 3s`, 274 actionable task; `app/build/outputs/apk/debug/app-debug.apk` – 19 599 736 bájt. |

Tesztosztályok és -számok a JUnit XML-ből
(`core/model/build/test-results/test/`):

| Osztály | Teszt | Hiba |
|---|---|---|
| `hu.kyokushin.trainer.core.model.IdentifiersTest` | 7 | 0 |
| `hu.kyokushin.trainer.core.model.DomainEnumsTest` | 6 | 0 |

Lefedett követelmények a tesztnevekben: DR-01, DR-03, DR-04 (azonosítók),
FR-10, FR-12, CR-01, CR-02 (enumok).

### Megjegyzés a `testDebugUnitTest` parancshoz

A `:core:model` Kotlin JVM modul, ezért nincs benne AGP-féle `testDebugUnitTest` task –
a kötelező DoD-parancs **csendben kihagyta volna** a tiszta Kotlin magmodulok tesztjeit.
Ezért a `core/model/build.gradle.kts` felvesz egy azonos nevű gyűjtőtaskot, amely a `test`
taskra mutat. A `:core:quiz` ugyanezt fogja igényelni, amint tesztet kap (T-106).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor; a T-101 nem is tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (a T-607 task tárgya). |
| TR-01…TR-08 | **NEM FUTOTT** – ezek a tesztkövetelmények a validátorhoz, a generátorhoz, a scoringhoz és a UI-hoz tartoznak; a T-101 nem fed le TR-kódot (a task „Teszt" mezője üres). |

---

## T-102 · `core/model` tartalmi domain modell (2026-11-11)

### Tényleges futtatás

Környezet: Windows 11, PowerShell, `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:model:test` | **0** | `BUILD SUCCESSFUL in 4s` – a modul tesztjei újrafordultak és lefutottak. |
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 2s`, 180 actionable task; `:core:model:testDebugUnitTest` lefedve (az előző futtatás után `UP-TO-DATE`). **Összesen 24 teszt, 0 hiba, 0 kihagyott.** A többi modul `testDebugUnitTest` taskja továbbra is `NO-SOURCE`. |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL in 1s`, 274 actionable task; `app/build/outputs/apk/debug/app-debug.apk` – 19 605 909 bájt. |

Tesztosztályok és -számok a JUnit XML-ből (`core/model/build/test-results/test/`):

| Osztály | Teszt | Hiba |
|---|---|---|
| `hu.kyokushin.trainer.core.model.ContentModelTest` | 11 | 0 |
| `hu.kyokushin.trainer.core.model.DomainEnumsTest` | 6 | 0 |
| `hu.kyokushin.trainer.core.model.IdentifiersTest` | 7 | 0 |

Lefedett követelmények a tesztnevekben: DR-01 (technikarekord mezői, immutabilitás),
DR-02 (kategória), FR-13 / CR-05 (kép-alapú kvízkapu egyetlen típusvizsgálattal),
CR-01 (konzervatív alapállapot), ADR-002, ADR-003, ADR-010.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor; a T-102 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-01…TR-08 | **NEM FUTOTT** – a T-102 „Teszt" mezője üres, TR-kód nem tartozik hozzá; ezek a validátorhoz, generátorhoz, scoringhoz és a UI-hoz kötődnek. |
