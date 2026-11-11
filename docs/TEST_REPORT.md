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
