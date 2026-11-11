---
description: A build környezet (JDK 17, Android cmdline-tools, SDK) felmérése és telepítési terv.
allowed-tools: Bash, PowerShell, Read, Write, Edit
---

Mérd fel és – ha a felhasználó engedélyezi – állítsd be az Android build környezetet.

### 1. Felmérés (csak olvasás, mindig futtasd)
- `java -version`, `$env:JAVA_HOME`, `$env:ANDROID_HOME`, `$env:ANDROID_SDK_ROOT`
- Keresd: `C:\Program Files\Java`, `C:\Program Files\Eclipse Adoptium`,
  `$env:LOCALAPPDATA\Android\Sdk`, `$env:USERPROFILE\.gradle`
- `winget --version`

### 2. Telepítési javaslat (NE futtasd kérdés nélkül – telepítés rendszerszintű változás)
Ajánlott minimum:
- **JDK 17 (LTS, Temurin)** – az Android Gradle Plugin ezt támogatja hivatalosan:
  `winget install EclipseAdoptium.Temurin.17.JDK`
- **Android SDK** vagy Android Studio:
  `winget install Google.AndroidStudio` (tartalmazza az SDK managert és emulátort), VAGY
  csak cmdline-tools + `sdkmanager "platform-tools" "platforms;android-XX" "build-tools;XX.Y.Z"`
- A konkrét API/build-tools verziót a telepített `sdkmanager --list` kimenetéből vedd,
  **ne találd ki**.

### 3. Telepítés után
- Írd meg a `local.properties`-t: `sdk.dir=...` (ez gitignore-olt).
- Ellenőrizd: `./gradlew --version`, majd `./gradlew testDebugUnitTest`.
- Frissítsd `docs/ENVIRONMENT.md`-t a TÉNYLEGES verziókkal és a futtatott parancsok kimenetével.

Mindig kérdezz a telepítés előtt. A felmérés és a dokumentálás kérdés nélkül végezhető.
