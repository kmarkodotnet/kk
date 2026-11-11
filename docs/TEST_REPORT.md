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

---

## T-103 · `core/model` felhasználói adatmodellek (2026-11-11)

### Tényleges futtatás

Környezet: Windows 11, PowerShell, `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 3s`, 180 actionable task; `:core:model:test` + `:core:model:testDebugUnitTest` lefutott. **Összesen 35 teszt, 0 hiba, 0 kihagyott.** A többi modul `testDebugUnitTest` taskja továbbra is `NO-SOURCE`. |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL`; `app/build/outputs/apk/debug/app-debug.apk` – 19 610 739 bájt. |

Tesztosztályok és -számok a JUnit XML-ből (`core/model/build/test-results/test/`):

| Osztály | Teszt | Hiba |
|---|---|---|
| `hu.kyokushin.trainer.core.model.ContentModelTest` | 11 | 0 |
| `hu.kyokushin.trainer.core.model.UserDataModelTest` | 11 | 0 |
| `hu.kyokushin.trainer.core.model.IdentifiersTest` | 7 | 0 |
| `hu.kyokushin.trainer.core.model.DomainEnumsTest` | 6 | 0 |

Lefedett követelmények a tesztnevekben: DR-04 (a felhasználói rekordok mezői),
DR-07 (a tartalomtól külön életciklus – csak azonosító-hivatkozás, reflexiós teszttel),
FR-18 / ADR-005 (a `questionId` jelenléte a `UNIQUE(sessionId, questionId)` indexhez),
FR-19 (a megszakított menet `finishedAt = null`), FR-20 (számlálóinvariánsok),
ADR-007 (seed a munkamenetben), ADR-009 (`Instant` desugaring nélkül).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor; a T-103 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-01…TR-08 | **NEM FUTOTT** – a T-103 „Teszt" mezője üres, TR-kód nem tartozik hozzá. A felhasználói adat tényleges perzisztenciáját a TR-05 (T-118) és az eredményszámítást a TR-04 (T-114) fogja mérni. |

---

## T-104 · `core/quiz` a validátor szerződése (2026-11-11)

### Tényleges futtatás

Környezet: Windows 11, PowerShell, `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:quiz:test` | **0** | `BUILD SUCCESSFUL in 6s`; `:core:quiz:compileKotlin`, `:core:quiz:compileTestKotlin`, `:core:quiz:test` lefutott. |
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 3s`, 183 actionable task. **Összesen 40 teszt, 0 hiba, 0 kihagyott** (`core/model` 35 + `core/quiz` 5). Az Android-modulok `testDebugUnitTest` taskja továbbra is `NO-SOURCE`. |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL`; `app/build/outputs/apk/debug/app-debug.apk` – 19 627 013 bájt. |

Tesztosztályok és -számok a JUnit XML-ből:

| Osztály | Teszt | Hiba |
|---|---|---|
| `hu.kyokushin.trainer.core.quiz.validation.KnowledgeValidatorContractTest` | 5 | 0 |
| `hu.kyokushin.trainer.core.model.ContentModelTest` | 11 | 0 |
| `hu.kyokushin.trainer.core.model.UserDataModelTest` | 11 | 0 |
| `hu.kyokushin.trainer.core.model.IdentifiersTest` | 7 | 0 |
| `hu.kyokushin.trainer.core.model.DomainEnumsTest` | 6 | 0 |

Lefedett követelmények a tesztnevekben: TR-01 (a hibás tartalom reprezentálható és
jelentésbe kerül, a kódkészlet V001…V010 teljes), FR-31 (a szerződés nem dob kivételt, a
`Technique` viszont igen – ezért a rekordot a leképezés előtt kell elvetni), CR-02 (a
jelentés alapértelmezett bizalmi szintje a szigorúbb `PRACTICE_ON_UNREVIEWED`).

Rétegszabály-ellenőrzés: a `core/quiz` Kotlin JVM modul, `grep` szerint nincs benne
`android`/`androidx`/`room` import, sem `Instant.now()`, `System.currentTimeMillis()`
vagy `Random.Default` hívás (ADR-001, NFR-07).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor; a T-104 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-01 teljes szabálytáblázat | **RÉSZBEN FUTOTT** – a T-104 csak a szerződést fedi (kivételmentesség, kódkészlet, jelentésmezők). A szabályonkénti (V001…V010) táblázatos tesztkészlet a T-106 tárgya, implementáció (T-105) nélkül nem futtatható. |
| TR-02…TR-08 | **NEM FUTOTT** – ezek a generátorhoz (TR-02, TR-03), a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |


---

## 2026-11-11 – T-105: a tudástár-validátor implementációja (`core/quiz`)

Környezet: `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`,
Windows PowerShell, Gradle wrapper (`.\gradlew.bat`).

| Parancs | Exit | Kimenet |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL`; 65 teszt, 0 hiba, 0 kihagyva |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL`; `app/build/outputs/apk/debug/app-debug.apk` – 19 655 673 bájt |

Tesztosztályok és -számok a JUnit XML-ből:

| Osztály | Teszt | Hiba |
|---|---|---|
| `hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidatorTest` | 25 | 0 |
| `hu.kyokushin.trainer.core.quiz.validation.KnowledgeValidatorContractTest` | 5 | 0 |
| `hu.kyokushin.trainer.core.model.ContentModelTest` | 11 | 0 |
| `hu.kyokushin.trainer.core.model.UserDataModelTest` | 11 | 0 |
| `hu.kyokushin.trainer.core.model.IdentifiersTest` | 7 | 0 |
| `hu.kyokushin.trainer.core.model.DomainEnumsTest` | 6 | 0 |

Lefedett követelmények a tesztnevekben: **TR-01** (V001 nem támogatott major / hiányzó
sémaverzió → teljes elutasítás; V002 blank kötelező mező → rekordelvetés; V003 duplikált
`id` → a *későbbi* rekord esik ki; V004 ismeretlen `categoryId` → rekordelvetés; V005
hiányzó fájl, lógó `ref`, licenc nélküli és szándékos helyőrző kép; V009 üres/önütköző/
duplikált alias), **CR-07** (V006 szinonimacsoportok normalizált névből, aliasra is),
**CR-02** (a `verified = false`/hiányzó soha nem lesz `VERIFIED`; az ismeretlen
`datasetKind` a szigorúbb `PRACTICE_ON_UNREVIEWED`-ot adja; a lógó képhivatkozás `VERIFIED`
tartalmat is `INCOMPLETE`-re fokoz le), **ADR-010** (V007 hiányos rekord → nulla kvízképes
típus; V008 nem kvízben használható kép → csak a szöveges típusok maradnak), **FR-31**
(a legdurvább, mindenhol `null`/blank bemenet sem dob kivételt), **NFR-07** (ugyanaz a
bemenet kétszer bitre azonos jelentést ad), **FR-03** (a `romajiAscii` származtatása).

### Kézi próba a valós tudástáron (nem része a commitolt tesztkészletnek)

A `knowledgebase_transfer/manifest.json` + `entries.json` (161 rekord, 9 kategória,
10 illusztráció) tartalmából generált, **ideiglenes** teszttel lefuttatva, utána törölve:

```
accepted=161 rejected=0 trust=PRACTICE_ON_UNREVIEWED groups=0
issues: (nincs)
assets=10 quizUsableAssets=7
asciiMismatch=1 [KY-0020:Ki o tsuke != Kirotsuke]   <- a konverter kimenete hibás, nem a validátor
searchMismatch=0
```

A 7 kvízben használható kép megfelel a 10 illusztrációnak mínusz a 3 feliratos
(`quiz_usable: false`) képnek.

Rétegszabály-ellenőrzés: a `core/quiz` Kotlin JVM modul; `grep` szerint a főkódban nincs
`android`/`androidx`/`room` import, sem `Instant.now()`, `System.currentTimeMillis()` vagy
`Random.Default` hívás (ADR-001 L1, NFR-07). A `NameNormalizer` csak JDK API-t használ
(`java.text.Normalizer`, `java.util.Locale.ROOT`), ami Androidon is elérhető.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-105 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| `V010` szabály | **NEM TESZTELHETŐ ITT** – a kódot nem a validátor állítja elő, hanem a `core/data` importréteg a deszerializálás meghiúsulásakor (T-115). |
| TR-01 teljes szabálytáblázat | **RÉSZBEN FUTOTT** – minden szabály (V001…V009) legalább egy, a hatását bizonyító esettel szerepel, de a szabályonkénti, táblázatos (parametrizált) készlet a T-106 tárgya. |
| TR-02…TR-08 | **NEM FUTOTT** – a generátorhoz (TR-02, TR-03), a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |

---

## 2026-11-11 – T-106: TR-01 validátor tesztek (táblázatos készlet + valós tartalom)

**Környezet:** `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`,
Gradle wrapper, Windows PowerShell.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL` – **105 teszt, 0 hiba, 0 kihagyva** |
| `.\gradlew.bat cleanTest testDebugUnitTest` | **0** | `BUILD SUCCESSFUL` – ugyanaz a 105 teszt újrafuttatva |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk`, 19 655 673 bájt |

A `:core:model:test` és a `:core:quiz:test` task **már nem `NO-SOURCE`**; a többi modul
teszttaskja továbbra is `NO-SOURCE` (nincs bennük még teszt).

Tesztosztályok és -számok a JUnit XML-ből:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…core.quiz.validation.KnowledgeValidationRuleTableTest` | **31** | 0 |
| `…core.quiz.validation.DefaultKnowledgeValidatorTest` | 25 | 0 |
| `…core.quiz.validation.KnowledgeValidatorContractTest` | 5 | 0 |
| `…core.quiz.validation.KnowledgeValidatorGuaranteesTest` | **4** | 0 |
| `…core.quiz.validation.RealKnowledgeBaseValidationTest` | **4** | 0 |
| `…core.quiz.validation.KnowledgeValidationRuleCoverageTest` | **1** | 0 |
| `…core.model.ContentModelTest` | 11 | 0 |
| `…core.model.UserDataModelTest` | 11 | 0 |
| `…core.model.IdentifiersTest` | 7 | 0 |
| `…core.model.DomainEnumsTest` | 6 | 0 |
| **Összesen** | **105** | **0** |

(A T-105 utáni 65-ről +40: 31 táblázatos eset, 1 lefedettségi őr, 4 garanciateszt,
4 valós tartalom teszt.)

### A TR-01 szabálytáblázat (parametrizált, esetnevekkel)

`KnowledgeValidationRuleTableTest.validate_ruleTableCase_matchesTheRuleTable_TR01[<eset>]` –
minden eset neve a lefedett V-kóddal kezdődik, és a
`KnowledgeValidationRuleCoverageTest.ruleCoverage_everyIssueCodeHasANamedCase_TR01` teszt
gépileg kikényszeríti, hogy egyetlen `IssueCode` se maradhasson eset nélkül:

| Kód | Esetek |
|---|---|
| `V001` | `missingSchemaVersion`, `unsupportedMajorVersion`, `nonNumericMajorVersion`, `supportedMajorWithHigherMinor_isAccepted` |
| `V002` | `blankId`, `missingCategoryId`, `blankRomaji`, `blankHungarian`, `missingContentVersion_rejectsEveryRecord`, `everyMandatoryFieldMissing_reportsAllOfThemAtOnce` |
| `V003` | `duplicateId_rejectsTheLaterRecordOnly`, `duplicateIdDifferingOnlyInWhitespace` |
| `V004` | `unknownCategoryId`, `manifestWithoutUsableCategories`, `knownCategoryId_producesNoIssue` |
| `V005` | `danglingIllustrationRef` (BLOKKOLÓ + `INCOMPLETE`), `illustrationFileMissingFromBundle` (FIGYELM.), `registryEntryWithoutLicense`, `intentionalPlaceholderWithNullFile` (nincs észrevétel), `licensedFilePresentInBundle_becomesAsset` |
| `V006` | `namesEqualAfterNormalization`, `aliasCollidingWithAnotherRecordsName`, `distinctNames_produceNoGroup` |
| `V007` | `quizEligibleRequestedOnIncompleteRecord_isDowngraded`, `quizEligibleNotRequested_producesNoDowngradeIssue` |
| `V008` | `quizEligibleRequestedWithLabelledImage_downgradesImageTypes` |
| `V009` | `blankAndNullAliases`, `aliasEqualToOwnName`, `duplicateAliases_keepOnlyTheFirstOccurrence`, `distinctNonCollidingAlias_isKeptWithoutIssue` |
| `V010` | `isNeverProducedByTheValidator` – **tagadó eset**: rögzíti, hogy a kódot a `core/data` importréteg állítja elő (T-115), nem ez a réteg |

### A két szerződési garancia (külön osztály)

| Teszt | Mit bizonyít |
|---|---|
| `validate_everyMalformedInputShape_reportsInsteadOfThrowing_TR01` | 8 romlási alak (üres manifest, csupa `null`, csak whitespace, `v1.0.0` olvashatatlan sémaverzió, hiányzó tartalomverzió, duplikált id + ismeretlen kategória, lógó képhivatkozás, csak kombináló jelekből álló romaji) – egyik sem dob kivételt, és minden rekordról számot ad a jelentés |
| `validate_contentWithoutVerifiedFlag_neverBecomesVerified_TR01_CR02` | `reviewed` manifest + hibátlan, kvízjogosultságot kérő rekord mellett is `AWAITING_REVIEW` marad |
| `validate_incompleteRecord_isNeverRaisedBackToVerified_TR01_CR02` | `verified = true` + lógó képhivatkozás → `INCOMPLETE`, `Placeholder`, nulla kvízképes típus |
| `validate_unknownDatasetKind_neverRaisesTrust_TR01_CR02` | 6 ismeretlen/hiányzó `editorial_status` → mindig `PRACTICE_ON_UNREVIEWED` |

### A valós tartalom – állandó tesztként (nem kézi próba)

`RealKnowledgeBaseValidationTest` a `knowledgebase_transfer/manifest.json` +
`entries.json` tényleges tartalmát olvassa be (az útvonal a `kyo.repoRoot` rendszer-
tulajdonságból jön, amit a `core/quiz/build.gradle.kts` állít be), és a validátort azon
futtatja. A T-105-ben ideiglenes, utána törölt próba helyére ez a négy teszt lépett:

| Teszt | Elvárás | Tény |
|---|---|---|
| `validate_realKnowledgeBase_hasNoBlockingIssue_TR01` | 0 BLOKKOLÓ hiba, `datasetRejected = false`, `rejected` üres | **teljesül** |
| `validate_realKnowledgeBase_acceptsEveryRecord_TR01` | `accepted = 161`, `schemaVersion = 1.0.0`, `datasetVersion = 0.1.0`, `datasetTrust = PRACTICE_ON_UNREVIEWED`, minden rekord `AWAITING_REVIEW` | **teljesül** |
| `validate_realKnowledgeBase_resolvesEveryIllustrationToAnAsset_TR01` | 10 `Asset`, 0 `Placeholder`, és pontosan a `KY-0045`, `KY-0050`, `KY-0052` nem kvízben használható | **teljesül** |
| `validate_realKnowledgeBase_findsNoSynonymCollision_TR01_CR07` | 0 V006 észrevétel, 0 szinonimacsoport (a manifest `synonym_groups` listája is üres) | **teljesül** |

### Rétegszabály és eltérés a tervtől

- A `core/quiz` **főkódja** változatlanul csak a `core:model`-re függ; nincs benne
  `android`/`androidx`/`room` import, sem `Instant.now()`, `System.currentTimeMillis()`
  vagy `Random.Default` (ADR-001 L1, NFR-07).
- **Eltérés:** a `core/quiz` **teszt** forráskészlete új, csak teszt hatókörű függőséget
  kapott: `libs.kotlinx.serialization.json` (tiszta JVM, verzió a version catalogból). A
  valós tartalmon futó TR-01 teszt nélküle nem tudná beolvasni a JSON-t. A `main`
  forráskészlet továbbra sem lát szerializációt – a validátor bemenete `KnowledgeInput`,
  nem fájl. A tartalomimport igazi helye változatlanul a `core/data` (T-115).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-106 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| `V010` szabály működése | **NEM TESZTELHETŐ ITT** – a kódot a `core/data` importréteg állítja elő a deszerializálás meghiúsulásakor (T-115). A T-106 csak azt rögzíti tesztben, hogy a validátor **soha nem** állítja elő. |
| TR-02…TR-08 | **NEM FUTOTT** – a generátorhoz (TR-02, TR-03), a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |

**TR-01: zöld.** Ez a projekt első olyan tesztfuttatása, amelyben a validátor teljes
szabálytáblázata és a valódi, 161 rekordos tudástár is szerepel.

---

## 2026-11-11 – T-107: a kvízgenerátor szerződése (core/quiz)

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:quiz:testDebugUnitTest` | 1 | 100 teszt, **1 bukott** – `quizQuestion_declaresIdentifierFieldsOnly_ADR006` (a reflexiós mezőlista a `companion`, a `$childSerializers` és az `OPTION_COUNT` **statikus** mezőket is látta) |
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL`, **135 teszt, 0 hiba, 0 kihagyott** (core/model 35 + core/quiz 100) |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` |

A bukott futás **a teszt elvárása volt hibás**, nem a kód: a példányállapot vizsgálatához
a statikus mezőket ki kell szűrni. A szűrés után a teszt ugyanazt az állítást fogalmazza
meg, csak helyesen. A kód nem változott miatta.

### Amit ez a kör lefedett

A T-107 **szerződés**, nem algoritmus: a generátor be- és kimeneti típusai, invariánsokkal.
A tényleges generálás a T-108…T-110 tárgya, ezért a TR-02 **még nem zöld** – a szerkezeti
garanciák azonban már tesztelt formában rögzítettek.

| Teszt | Mit bizonyít |
|---|---|
| `quizQuestion_rejectsFewerThanFourOptions_FR14` | 3 opciós kérdés nem jöhet létre |
| `quizQuestion_rejectsMoreThanFourOptions_FR14` | 5 opciós kérdés sem |
| `quizQuestion_rejectsRepeatedOption_FR14` | ismétlődő opció tilos |
| `quizQuestion_rejectsCorrectAnswerMissingFromOptions_FR14` | a helyes válasz nem hagyható ki az opciókból |
| `quizQuestion_hasExactlyOneCorrectOptionAndThreeDistractors_FR14` | 4 opció, 1 helyes, 3 distraktor |
| `quizQuestion_declaresIdentifierFieldsOnly_ADR006` | a kérdés példánymezői pontosan `id`, `type`, `correctTechniqueId`, `optionTechniqueIds` – szöveg- vagy képmező felvétele bukást okoz |
| `serializedQuestion_leaksNoDisplayTextOrImageName_ADR006` | a szerializált kérdésben nincs romaji, magyar név, képfájlnév vagy képhivatkozás |
| `quizQuestion_survivesSerializationRoundTripWithOptionOrder_FR19` | a `SavedStateHandle`-körbefordulás az opciósorrendet is megőrzi |
| `corruptSavedState_cannotBypassQuestionInvariants_FR14` | sérült mentett állapotból nem áll elő 2 opciós kérdés és nem jut be üres azonosító |
| `answerEvaluation_comparesIdentifiersNotText_DR03` | két azonos magyar jelentésű technika esetén az azonosító-összehasonlítás nem ad hamis találatot |
| `quizQuestion_identityIsItsIdNotItsContent_ADR005` | a kérdést az azonosítója azonosítja |
| `emptyPlan_isValidAndCannotStart_FR22` | nulla kérdés érvényes, megindokolt kimenet; nem kivétel |
| `plan_rejectsDuplicateQuestionIds_ADR005` | egy terven belül a kérdésazonosítók egyediek |
| `plan_withAllRequestedQuestions_hasNoShortfall_FR22` | teljes terv esetén `shortfall = null` |
| `shortfall_existsOnlyWhenFewerQuestionsThanRequested_FR22` | a `Shortfall` nem fogalmazható meg hiány nélkül |
| `plan_survivesSerializationRoundTrip_FR19` | a teljes terv (kérdések + kihagyási okok + shortfall) körbefordul |
| `readiness_shortfallAgreesWithExpectedCount_FR22` | a készenléti összegzés nem hallgathatja el és nem mondhatja másnak a hiányt |
| `readiness_withoutAnyQuestion_cannotStart_FR22` | nulla várható kérdés → nem indítható |
| `request_emptyCategoryFilterMeansAllCategories_FR11` | üres kategóriaszűrő = minden kategória |
| `request_rejectsNonPositiveQuestionCount_FR13` | nulla kért kérdésszám értelmetlen bemenet |
| `request_acceptsNoEnabledType_FR22` | üres típushalmaz érvényes bemenet, nem hiba |
| `candidateSet_exposesMutualSynonymExclusion_CR07` | a szinonimakapcsolat kétirányú és lekérdezhető |
| `candidateSet_rejectsSynonymGroupOutsideTheSet_CR07` | a csoport nem hivatkozhat készleten kívüli technikára |
| `candidateSet_rejectsSingleMemberSynonymGroup_CR07` | egytagú szinonimacsoport értelmetlen |
| `candidateSet_rejectsDuplicateTechnique` | egy technika egyszer szerepel a jelöltkészletben |
| `candidateSet_exposesVisualIdentityOfIllustrations_TR02` | a vizuálisan azonos kép felismerhető (ujjlenyomat-egyezés) |
| `generator_returnsExplainedPlanInsteadOfThrowing_FR22` | az interfész kivétel nélkül teljesíthető elégtelen adatra |
| `dryRunPromise_matchesGeneratedQuestionCount_FR13` | a `dryRun` ígérete és a `generate` kérdésszáma egyezik |
| `dryRunAndGenerate_reportTheSameSkipReasons_FR22` | a két ág ugyanazt az indoklást adja |
| `sameRequestTwice_producesEqualPlans_NFR07` | a kérés önhordó, ezért a kétszeri hívás összehasonlítható |

### Rétegszabály és eltérés a tervtől

- A `core/quiz` főkódja továbbra is csak a `core:model`-re és (újonnan) a
  `kotlinx-serialization-json`-ra függ; nincs `android`/`androidx`/`room` import, és a
  modulban egyetlen tényleges `Instant.now()`, `System.currentTimeMillis()` vagy
  `Random.Default` **hívás** sincs – ezek csak dokumentációs kommentekben szerepelnek
  (ADR-001 L1, NFR-07).
- **Eltérés:** a `core/quiz` **főkódja** megkapta a `kotlin-serialization` fordítói
  plugint és a `kotlinx-serialization-json` függőséget (verzió a version catalogból). Erre
  a T-107 „Kész, ha" pontja kényszerít: a `QuizQuestion` szerializálható kell legyen, mert
  a T-405 a `SavedStateHandle`-be menti. A T-106 jegyzete szerint addig csak a teszt
  forráskészlet látott szerializációt – ez most tudatosan változott.
- A `core/model` **érintetlen**: a `TechniqueId`/`QuestionId` value classok privát
  konstruktora megmaradt, és a szerializálásuk a `core/quiz`-ban élő
  `TechniqueIdSerializer`/`QuestionIdSerializer` sztring-szerializátorokon keresztül megy,
  amelyek a `of(...)` gyárat hívják. Így a visszafejtés sem tudja megkerülni a domain
  invariánst – erre külön teszt van.
- Felhasználói szöveg nem keletkezett: a `SkipReason` és a `ShortfallReason` enum, a magyar
  üzenet és a javaslat a UI-ban, `strings.xml`-ből készül (FR-22, T-404).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-107 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-02 teljes készlet | **NEM FUTOTT** – a generálási invariánsok (pontosan egy helyes, három különböző téves, nincs szinonima a tévesek között, seed-determinizmus, graceful fallback) tényleges algoritmust igényelnek: T-108…T-110, a tesztek T-112. |
| TR-03…TR-08 | **NEM FUTOTT** – a jelöltszűréshez (TR-03), a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |
