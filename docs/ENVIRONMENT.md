# Build környezet állapota

**Utolsó frissítés: 2026-11-11** (gép: Windows 11 Pro 10.0.26300, munkakönyvtár `F:\Claude\kyo`)

## 0. Aktuális állapot (telepítés után)

| Komponens | Állapot | Tényleges verzió / útvonal |
|---|---|---|
| JDK | ✅ **telepítve** | Temurin 17.0.21+9 – `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot` |
| `JAVA_HOME` | ✅ beállítva (gépszintű) | a telepítő állította be |
| Android Studio | ✅ **telepítve** | `C:\Program Files\Android\Android Studio` (2026.2.2.8) |
| Android SDK cmdline-tools | ⏳ folyamatban | `commandlinetools-win-16111833` kicsomagolása `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` alá |
| SDK platform / build-tools | ⬜ még nincs | az `sdkmanager` telepíti, a tényleges `--list` kimenet alapján |
| Emulátor (AVD) | ⬜ még nincs | a TR-06 UI tesztekhez kell |

Az alábbi 1–3. fejezet a **telepítés előtti** kiindulási felmérést őrzi meg, változatlanul.

## 1. Kiinduló felmérés eredménye (2026-11-11, telepítés előtt)

Futtatott ellenőrzések és kimenetük:

| Ellenőrzés | Parancs | Eredmény |
|---|---|---|
| Java | `java -version` | **nem található** |
| JAVA_HOME | `$env:JAVA_HOME` | **üres** |
| Android SDK | `$env:ANDROID_HOME`, `$env:ANDROID_SDK_ROOT` | **üres** |
| SDK könyvtár | `Test-Path %LOCALAPPDATA%\Android\Sdk` | **nincs** |
| Android Studio | `Test-Path "C:\Program Files\Android\Android Studio"` | **nincs** |
| Gradle (rendszer) | `gradle -v` | **nem található** |
| Gradle home | `Test-Path %USERPROFILE%\.gradle` | **nincs** |
| JDK keresés | `C:\`, `F:\` alatt `*jdk*` könyvtár | **nincs találat** |
| Git | `git --version` | **2.47.0.windows.2** |
| winget | `winget --version` | **v1.29.380** |
| Szabad hely | `Get-PSDrive` | C: 99.4 GB, D: 9.1 GB, E: 617.1 GB, F: 337.8 GB |

## 2. Blokkolók

| # | Blokkoló | Mire van hatása | Megoldás |
|---|---|---|---|
| B-01 | **Nincs JDK** | Semmilyen Gradle művelet nem fut (a wrapper is JVM-et igényel): `assembleDebug`, `testDebugUnitTest`, `lint` | JDK 17 (Temurin) telepítése – `/kyo-setup-env` |
| B-02 | **Nincs Android SDK** | `assembleDebug` és minden Android fordítás; `local.properties` sem írható értelmesen | Android Studio vagy cmdline-tools + `sdkmanager` – `/kyo-setup-env` |
| B-03 | **Nincs emulátor/eszköz** | `connectedDebugAndroidTest` (UI tesztek) | Android Studio AVD vagy fizikai eszköz USB debug módban |
| B-04 | **Verziómátrix nem ellenőrizhető** | A konkrét AGP / Gradle / Kotlin / Compose Compiler verziók kiválasztása | Az SDK telepítése után `sdkmanager --list` és a hivatalos kompatibilitási mátrix alapján kell kitölteni a `gradle/libs.versions.toml`-t. **Addig verziószámot nem rögzítünk, mert kitalálni tilos.** |

## 3. Amit a blokkolók ellenére el lehet végezni

- Teljes dokumentáció (követelmény, funkcionális, architektúra, tudástár, kvízszabályok).
- A tudástár JSON sémája és a demó adatkészlet.
- A teljes Kotlin forráskód megírása, beleértve a `core/quiz` tiszta Kotlin motort és
  a validátort, valamint a hozzájuk tartozó JUnit teszteket.
- Gradle projektváz és modulstruktúra (a verziók kitöltése B-04 után).

**Amit NEM lehet:** semmilyen build, semmilyen tesztfuttatás, APK előállítás, lint.
Ezért a `docs/TEST_REPORT.md` addig „NEM FUTOTT" bejegyzéseket tartalmaz.

## 4. Telepítési terv (jóváhagyásra)

```powershell
winget install EclipseAdoptium.Temurin.17.JDK      # B-01
winget install Google.AndroidStudio                # B-02 + B-03 (SDK manager + AVD)
# majd új shellben:
java -version
sdkmanager --list                                  # B-04: tényleges verziók kiválasztása
```

A telepítés rendszerszintű változás, ezért **csak a felhasználó explicit jóváhagyásával**
hajtható végre. A felmérés és a dokumentálás jóváhagyás nélkül is frissíthető.

## 5. Változásnapló

- 2026-11-11 – első felmérés; B-01…B-04 nyitott.
- 2026-11-11 – **B-01 feloldva:** Temurin JDK 17.0.21 telepítve
  (`winget install EclipseAdoptium.Temurin.17.JDK`, exit 0). Ellenőrizve:
  `java -version` → `openjdk version "17.0.21" 2026-10-20`.
- 2026-11-11 – Android Studio 2026.2.2.8 telepítve
  (`winget install Google.AndroidStudio`, exit 0). Az IDE **nem** hozza magával az SDK-t:
  azt a first-run varázsló töltené le, ezért headless úton a hivatalos
  `commandlinetools-win-16111833_latest.zip` csomagot szereztük be a
  `https://dl.google.com/android/repository/repository2-3.xml` manifestből kiolvasott
  URL-ről. **B-02 feloldása folyamatban.**
- 2026-11-11 – Döntések (felhasználói jóváhagyással): ADR-001 teljes modulbontás,
  ADR-009 `minSdk = 26`, demó adatkészlet 4 kategória × ~5 rekord. ADR-002…ADR-007
  a javaslat szerint elfogadva.
