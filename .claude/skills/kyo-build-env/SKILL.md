---
name: kyo-build-env
description: A Windows build környezet felmérése, JDK/Android SDK/Gradle telepítése és a hiteles build- és tesztjelentés szabályai. Használd amikor buildet vagy tesztet futtatnál, környezeti hibába futsz (java not found, SDK location not found, Gradle wrapper), vagy a TEST_REPORT.md-t frissíted.
---

# Build környezet (Windows)

## 1. Jelenlegi tényállás (2026-11-11, felmérve)

| Komponens | Állapot |
|---|---|
| Git | **van** (2.47.0.windows.2) |
| JDK | **NINCS** (`java` nem található, `JAVA_HOME` üres) |
| Android SDK | **NINCS** (`ANDROID_HOME`/`ANDROID_SDK_ROOT` üres, nincs `%LOCALAPPDATA%\Android\Sdk`) |
| Android Studio | **NINCS** |
| Gradle (rendszer) | **NINCS** (a wrapper elég lesz, de JDK nélkül az sem fut) |
| winget | **van** (v1.29.380) – telepítéshez használható |
| Szabad hely | C: ~99 GB, F: ~338 GB |

**Következmény:** Gradle build és unit teszt **jelenleg nem futtatható**. A kód, a séma, a
kvízmotor és a tesztek megírhatók, de a zöld teszt csak a JDK telepítése után igazolható.
Ezt minden jelentésben ki kell írni.

## 2. Telepítési sorrend (felhasználói jóváhagyással)

1. **JDK 17 (Temurin LTS)** – `winget install EclipseAdoptium.Temurin.17.JDK`
   Majd új shellben: `java -version` (ellenőrzés), `JAVA_HOME` beállítás.
   Miért 17: az Android Gradle Plugin jelenlegi kiadásai JDK 17-et igényelnek/támogatnak;
   a pontos minimumot a választott AGP kiadási jegyzete adja – ellenőrizd, ne tippelj.
2. **Android SDK** – két út:
   - *Android Studio* (`winget install Google.AndroidStudio`): SDK manager + emulátor,
     instrumentált teszthez ez kell.
   - *csak cmdline-tools*: kisebb, de nincs emulátor → `connectedDebugAndroidTest` nem fut.
3. **SDK komponensek** – `sdkmanager --list` kimenetéből válassz TÉNYLEGESEN létező
   verziót: `platform-tools`, `platforms;android-<API>`, `build-tools;<verzió>`.
4. **`local.properties`** (gitignore-olt): `sdk.dir=C\:\Users\<user>\AppData\Local\Android\Sdk`
5. Ellenőrzés: `./gradlew --version`, majd `./gradlew testDebugUnitTest`.

A telepítés rendszerszintű, visszafordítható-de-invazív művelet: **mindig kérdezz előtte.**
A felmérés és a dokumentálás kérdés nélkül végezhető.

## 3. Gyakori hibák és a tényleges ok

| Hibakimenet | Ok | Megoldás |
|---|---|---|
| `java: command not found` / `JAVA_HOME is not set` | nincs JDK | 2.1 lépés |
| `SDK location not found` | nincs `local.properties` vagy `ANDROID_HOME` | 2.4 lépés |
| `Unsupported class file major version` | JDK és AGP/Gradle verzió nem kompatibilis | JDK verzió igazítása a kompatibilitási mátrixhoz |
| `Could not find ...` függőség | hálózat vagy hibás catalog koordináta | catalog ellenőrzés; offline gépen a Gradle cache feltöltése kell |
| `No connected devices` | nincs emulátor/eszköz | `connectedDebugAndroidTest` kihagyása, dokumentálva |
| Compose Compiler verzióhiba | Kotlin ↔ Compose Compiler eltérés | a Kotlin verzióhoz tartozó Compose Compiler plugin használata |

## 4. Hiteles jelentés szabályai (kötelező)

- **Csak tényleges futtatást jelents.** „A tesztek sikeresek" állítás kizárólag akkor
  írható le, ha a parancs lefutott és az exit kód 0 volt.
- A `docs/TEST_REPORT.md` minden bejegyzése tartalmazza: **dátum, pontos parancs,
  exit kód, teszt/hiba szám, mit fedett le (TR-*), mi nem futott és miért.**
- Ami nem futott, azt explicit „NEM FUTOTT – ok" sorként kell rögzíteni, nem elhallgatni.
- Hálózat nélküli gépen a Gradle dependency letöltés is blokkoló lehet – ezt külön ellenőrizd,
  ne keverd a JDK-hiánnyal.
- Ne töröld a TEST_REPORT korábbi bejegyzéseit; a report append-only napló.
