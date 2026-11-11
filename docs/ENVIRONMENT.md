# Build környezet állapota

**Utolsó felmérés: 2026-11-11** (gép: Windows 11 Pro 10.0.26300, munkakönyvtár `F:\Claude\kyo`)

## 1. Tényleges felmérés eredménye

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
