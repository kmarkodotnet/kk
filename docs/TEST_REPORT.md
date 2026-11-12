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

---

## 2026-11-11 – T-108: jelöltszűrés és jogosultsági kapuk (core/quiz)

**Környezet:** Windows 11 Pro, JDK 17.0.21+9 (Eclipse Adoptium), Gradle wrapper 8.13,
`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Tényleges futtatások

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:quiz:test` | 0 | BUILD SUCCESSFUL – 126 teszt, 0 hiba |
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **161 teszt, 0 hiba** (core/model 35 + core/quiz 126) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL |

A tesztszámok a `core/*/build/test-results/test/*.xml` JUnit XML-ek `tests` és `failures`
attribútumainak összegei, nem becslés.

### Ami elkészült

`QuizCandidateFilter` (`core/quiz/.../generator/QuizCandidateFilter.kt`): a generálás első
fele – kategóriaszűrő (FR-11), az ADR-010 jogosultsági kapu alkalmazása típusonként,
típusonkénti jelöltszám és a kihagyott típusok `SkipReason`-ja (FR-13, FR-14, FR-22, CR-05).

- **Egyetlen jogosultsági formula:** minden döntés a meglévő `QuizEligibility.isEligible`
  kapun megy át – nincs második kvízképességi logika. Külön teszt állítja, hogy a
  jelöltkészlet pontosan a formula kimenete (`correctAnswerPool_usesTheSingleEligibilityFormula_ADR010`).
- **Kérdés- és opcióoldal szétválasztva:** a (C) `IMAGE_TO_JP` opciói romaji nevek (a kép a
  kérdés oldalán van), a (D) `JP_TO_IMAGE` opciói illusztrációk. Ezt az
  `optionRequirement(type)` mezőkövetelmény-leképezés hordozza, nem új szabály.
- **Determinizmus:** minden visszaadott lista technikaazonosító szerint rendezett; a szűrés
  nem sorsol, `Set`-iterációs sorrendre nem épül (NFR-07).

### Új tesztek (TR-03)

| Teszt | Mit bizonyít |
|---|---|
| `inCategoryScope_emptyFilter_meansEveryCategory_FR11` | üres kategóriaszűrő = minden kategória |
| `inCategoryScope_filteredCategory_excludesEveryOtherTechnique_TR03` | a szűrt kategórián kívüli rekord egyetlen típus jelöltkészletében sem szerepel |
| `inCategoryScope_isOrderedById_forDeterminism_NFR07` | a kimenet rendezett, bemeneti sorrendtől független |
| `correctAnswerPool_usesTheSingleEligibilityFormula_ADR010` | nincs duplikált kvízképességi logika |
| `correctAnswerPool_incompleteRecord_appearsInNoQuestionType_CR02` | `INCOMPLETE` rekord egyetlen típusban sem jelenik meg |
| `correctAnswerPool_quizExcludedOrRetired_appearsInNoQuestionType_ADR010` | szerkesztői kizárás és visszavonás kiesik |
| `optionPool_textTypes_needRomajiAndHungarianOnly_FR14` | (A)/(B) mezőkövetelmény |
| `correctAnswerPool_captionedImage_isNeverAnImageCandidate_CR05_ADR011` | a feliratos kép (`quizUsable = false`) sem helyesként, sem opcióként nem kerül (C)/(D) kérdésbe |
| `correctAnswerPool_placeholderIllustration_isNotAValidatedImage_CR05` | a `Placeholder` nem validált kép |
| `optionPool_imageToJp_optionsAreNamesNotImages_FR13` | (C)-nél csak a helyesnek kell kép |
| `optionPool_jpToImage_visuallyIdenticalImagesCountAsOneOption_FR13` | azonos ujjlenyomatú két kép nem négy valódi opció |
| `buildableQuestionCount_jpToImage_fourDistinctImages_isBuildable_FR13` | négy különböző képpel a (D) összeáll |
| `buildableQuestionCount_fewerThanFourOptions_yieldsNoQuestion_FR14` | nincs három opciós kérdés |
| `skipReasons_disabledType_isReportedAsDisabledNotAsMissingData_FR12` | a letiltott típus oka a letiltás, nem adathiány |
| `skipReasons_noValidatedImage_skipsImageTypesWithDocumentedReason_TR03` | (C)/(D) kihagyása dokumentált okkal |
| `skipReasons_imageToJp_imagePresentButTooFewNames_isCandidateShortage_FR14` | a két ok elhatárolása |
| `skipReason_everyDatasetTrustLevel_keepsTheGateDownwardOnly_ADR010` | a bizalmi szint nem írja felül a képhiányt |
| `realContent_imageTypes_haveExactlySevenQuizUsableCandidates_TR03_CR05` | a valódi tartalomban 7 kvízképes kép (a 3 feliratos kiesik) |
| `realContent_imageCandidates_areAllInTheStancesCategory_TR03` | mind az `stances` kategóriában |
| `realContent_imageTypesOutsideStances_areSkippedWithDocumentedReason_TR03_FR13` | `punches` szűrővel a (C)/(D) kimarad, a szöveges típusok állnak |
| `realContent_stancesCategory_buildsEveryQuestionType_TR03` | `stances` szűrővel mind a négy típus összeáll |
| `realContent_categoryFilter_neverLeaksTechniqueFromAnotherCategory_FR11` | a szűrő nem szivárog |
| `realContent_everyTextTypeCandidate_isEligibleAcrossAllCategories_FR14` | 161 rekord mind jogosult a szöveges típusokban |

### Rétegszabály és eltérés a tervtől

- A `core/quiz` főkódja továbbra is csak a `core:model`-re és a
  `kotlinx-serialization-json`-ra függ; nincs `android`/`androidx`/`room` import, és
  tényleges `Random.Default` / rendszeróra-hívás sincs (ADR-001 L1, NFR-07).
- **Eltérés a task „Teendők" listájától:** a `quizEligible` formulát nem írtuk meg újra –
  az már a T-105-ben elkészült `QuizEligibility`-ben él, egyetlen helyen (ADR-010). A T-108
  ezt **alkalmazza**. Ez tudatos: két igazságforrás a kvízképességre regresszió lenne.
- **Teszt-refaktor:** a valódi tudástár JSON → `KnowledgeInput` leképezése a
  `RealKnowledgeBaseValidationTest`-ből a közös `RealKnowledgeBase` teszt-fixture-be
  került, mert a TR-03 készlet is ugyanazt a tartalmat olvassa. Két példányban az egyik
  csendben elavulna.
- Felhasználói szöveg nem keletkezett: a `SkipReason` enum, a magyar üzenet a UI-ban,
  `strings.xml`-ből (FR-22, T-404).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-108 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-02 teljes készlet | **NEM FUTOTT** – a distraktorválasztás (T-109) és a seedelt keverés (T-110) még nincs meg; a tesztek a T-112-ben. |
| TR-03 teljes készlet | **RÉSZBEN** – a kategóriaszűrés és a kérdéstípus-jogosultság lefedve (fenti 23 teszt); a generált kérdéseken át vezetett végponti TR-03 ellenőrzés a T-112-ben. |
| TR-04…TR-08 | **NEM FUTOTT** – a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |

---

## 2026-11-11 – T-109: distraktorválasztás (core/quiz)

**Környezet:** Windows 11 Pro, JDK 17.0.21+9 (Eclipse Adoptium), Gradle wrapper 8.13,
`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Tényleges futtatások

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **181 teszt, 0 hiba** (core/model 35 + core/quiz 146) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 683 417 bájt) |

A tesztszámok a `core/*/build/test-results/**/TEST-*.xml` JUnit XML-ek `tests`, `failures`
és `errors` attribútumainak összegei, nem becslés; a `DistractorSelectorTest`
önmagában 20 teszt, 0 hiba.

### Ami elkészült

`DistractorSelector` (`core/quiz/.../generator/DistractorSelector.kt`): a három téves válasz
kiválasztása (FR-14, FR-22, CR-07).

- **Prioritás:** (1) azonos kategória, (2) feltöltés más kategóriából, (3) a kérdés **nem
  jön létre** – `selectDistractors` ilyenkor `null`. Nincs háromopciós kérdés, nincs
  generált opció, nincs szinonim kisegítő opció.
- **Kizárási szűrő:** a típus opciókészlete (ADR-010 kapu) ÉS nem a helyes ÉS nem a helyes
  szinonimája (V006) ÉS nem egy már kiválasztott distraktor szinonimája ÉS (D)
  `JP_TO_IMAGE` esetén nem vizuálisan azonos kép (SHA-256 ujjlenyomat).
- **Kölcsönös szinonimakizárás:** a CR-07 betű szerinti olvasata („szinonim rekordok nem
  lehetnek egymás distraktorai") itt **páronként** érvényes: két szinonim rekord egy
  kérdésbe sem kerül, nem csak a helyes mellé nem. Ez szigorúbb, mint a kvízmotor-skill
  4. invariánsa, és a szigorúbb irány a biztonságos (FR-22).
- **A `buildableQuestionCount` nem becslés többé:** a T-108-ban felső korlát volt (a
  szinonimakizárás nem szerepelt benne); most a
  `DistractorSelector.buildableCorrectAnswers` tényleges kimenetét számolja, ezért a
  `skipReason` is a tényleges összeállíthatóságból következik.
- **Determinizmus rendezett bejárással:** a jelöltek a rendezett `optionPool`-ból jönnek,
  azonos kategória elöl; a kiválasztás az első három elfogadható jelölt. RNG-t ez a lépés
  nem használ – a seedelt keverés a T-110 tárgya, és az `acceptableDistractors` rendezett
  listájára épülhet a kizárási szabályok újrafogalmazása nélkül.

### Új tesztek (TR-02 részlegesen, FR-14, CR-07)

| Teszt | Mit bizonyít |
|---|---|
| `selectDistractors_enoughCandidates_yieldsThreeDistinctWrongAnswers_FR14` | pontosan három, páronként különböző téves válasz |
| `selectOptionIds_buildsExactlyFourOptionsWithOneCorrect_FR14` | négy opció, pontosan egy helyes; a `QuizQuestion` konstruktora elfogadja |
| `selectDistractors_everyOptionIsAnExistingTechniqueRecord_FR14` | a motor nem gyárt opciót |
| `selectDistractors_fewerThanThreeAcceptable_buildsNoQuestion_FR14_TR02` | nincs háromopciós kérdés: inkább semmi |
| `selectDistractors_prefersSameCategoryWhenThreeAreAvailable_FR14` | a kategórián belüli distraktor az elsődleges |
| `selectDistractors_fewerThanThreeInCategory_fillsFromOtherCategories_FR14` | feltöltés más kategóriából, a saját kategória elöl |
| `selectDistractors_synonymOfCorrectAnswer_isNeverADistractor_CR07` | a helyes szinonimája soha nem distraktor |
| `selectDistractors_twoSynonymousCandidates_neverShareOneQuestion_CR07` | két szinonim jelölt nem kerül egy kérdésbe |
| `selectDistractors_synonymShortageIsNotRescuedBySynonym_FR22_CR07` | szinonimával nem „segít ki" a motor |
| `buildableQuestionCount_synonymExclusionNarrowsTheUpperBound_CR07` | a T-108 felső korlátja a V006 kizárás után ténylegessé válik |
| `buildableQuestionCount_enoughRecordsBesideASynonymPair_staysExact_CR07` | elég rekordnál a szinonimapár nem csökkenti a kérdésszámot |
| `skipReason_everyCorrectAnswerBlockedBySynonyms_isCandidateShortage_FR22` | a kihagyás dokumentált okot kap |
| `selectDistractors_jpToImage_neverRepeatsAVisuallyIdenticalIllustration_TR02` | (D)-nél nincs két azonos ujjlenyomatú kép az opciók között |
| `selectDistractors_jpToImage_imageIdenticalToTheCorrectOne_isExcluded_TR02` | a helyessel vizuálisan azonos kép kiesik |
| `selectDistractors_jpToImage_tooFewDistinctImages_buildsNoQuestion_FR14` | kevés különböző kép → nincs kérdés, `NOT_ENOUGH_IMAGES` |
| `selectDistractors_jpToImage_missingDigestDoesNotProveIdentity_FR14` | hiányzó ujjlenyomat nem bizonyít egyezést |
| `selectDistractors_imageToJp_distractorsAreNamesSoNoImageIsRequired_FR14` | (C)-nél a distraktornak nem kell kép |
| `selectDistractors_isDeterministicAndIndependentOfInputOrder_NFR07` | kétszer hívva ugyanaz, bemeneti sorrendtől független |
| `acceptableDistractors_areOrderedSameCategoryFirstThenById_NFR07` | rendezett bejárás: azonos kategória elöl, azon belül azonosító szerint |
| `buildableCorrectAnswers_areOrderedById_forStableQuestionSelection_NFR07` | a helyes jelöltek rendezett listája a nem ismétlés alapja |

### Rétegszabály és eltérés a tervtől

- A `core/quiz` főkódja továbbra is csak a `core:model`-re és a
  `kotlinx-serialization-json`-ra függ; `android`/`androidx`/`room` import nincs, tényleges
  `Random.Default`, `System.currentTimeMillis()` vagy `Instant.now()` hívás sincs
  (ADR-001 L1, NFR-07) – a találatok kizárólag KDoc-szövegek.
- **Eltérés:** a task „Helyes technika nem ismétlődik, amíg van fel nem használt jelölt;
  különben `shortfall`" pontja ebben a körben a `buildableCorrectAnswers` rendezett,
  ismétlésmentes listájaként készült el. A `Shortfall` tényleges előállítása a `QuizPlan`
  összeállításához tartozik, az pedig a T-110 (a `QuizGenerator` implementációja még nem
  létezik, csak az interfész).
- **Konzervatív kiválasztás:** a bejárás az első elfogadható jelöltet veszi, nem keres
  maximális párosítást. Egymást keresztező (nem tranzitív) szinonimacsoportoknál így
  elvileg kimaradhat egy elvben összeállítható kérdés. Ez tudatos: FR-22 szerint a
  kevesebb, de helyes kérdés a jó irány, és mivel a `canBuildQuestion` ugyanazt a
  függvényt kérdezi, a számolt és a tényleges kérdésszám nem tud elválni (T-111).
- Felhasználói szöveg nem keletkezett: a motor szövegfüggetlen, a magyar üzenetek a UI
  `strings.xml`-jében élnek (FR-22, T-404).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-109 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-02 teljes készlet | **RÉSZBEN** – a distraktorválasztás invariánsai lefedve (fenti 20 teszt); a seed-determinizmus a teljes terven (T-110) és a végponti TR-02 készlet (T-112) még hátravan. |
| TR-03 teljes készlet | **RÉSZBEN** – a T-108 óta változatlan; a generált kérdéseken át vezetett ellenőrzés a T-112-ben. |
| TR-04…TR-08 | **NEM FUTOTT** – a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |

---

## 2026-11-11 – T-110: seedelt determinizmus (core/quiz)

**Környezet:** Windows 11 Pro, JDK 17.0.21+9 (Eclipse Adoptium), Gradle wrapper 8.13,
`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Tényleges futtatások

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **196 teszt, 0 hiba** (core/model 35 + core/quiz 161) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 686 975 bájt) |

A tesztszámok a `core/*/build/test-results/**/TEST-*.xml` JUnit XML-ek `tests`, `failures`
és `errors` attribútumainak összegei, nem becslés; a `DefaultQuizGeneratorTest` önmagában
**15 teszt, 0 hiba**.

### Ami elkészült

`SeededShuffle` és `DefaultQuizGenerator` (`core/quiz/.../generator/`): a `QuizGenerator`
első és egyetlen implementációja (NFR-07, ADR-007, FR-14, FR-22).

- **Egyetlen véletlenforrás.** A `generate()` egy darab `Random(request.seed)` példányt hoz
  létre, és minden sorsolás abból megy: először a (típus, helyes válasz) jelöltek
  **Fisher–Yates** keverése adja a kérdéssorrendet, majd kérdésenként a négy opció
  keverése. A `SeededShuffle.shuffled(items, random)` az egyetlen keverő belépési pont, és
  az RNG-t **paraméterként** kéri – seed nélküli keverés így fordítási hiba, nem
  elfelejtett paraméter.
- **A keverés csak permutál.** Az opciók halmazát a T-109 `selectOptionIds` adja; a keverés
  nem ad hozzá, nem hagy el és nem duplikál opciót. Ezért a keverés nem tud kérdést
  elrontani (FR-14).
- **Rendezett bejárás.** A típusok a `QuestionType` deklarációs sorrendjében, a helyes
  jelöltek a `DistractorSelector.buildableCorrectAnswers` azonosító szerint rendezett
  listáiból. `Set`-iterációs sorrendre épülő döntés nincs; a hívó jelöltlistájának sorrendje
  nem változtatja meg a tervet (külön teszt).
- **Nincs ismétlés.** Ugyanaz a helyes technika egy terven belül legfeljebb egyszer
  szerepel, akkor is, ha több kérdéstípusban is használható lenne. Ha a kért kérdésszám
  több, mint ahány különböző helyes jelölt van, **kevesebb** kérdés lesz.
- **Tényleges `Shortfall`.** A terv most állítja elő, prioritási sorrendben:
  `ALL_TYPES_SKIPPED` (nulla kérdés) → `NOT_ENOUGH_IMAGES` (egy engedélyezett képes típus
  egésze kiesett) → `NOT_ENOUGH_DISTRACTORS` (a kapun átjutott helyes jelölt mellé nem
  állt össze három distraktor) → `NOT_ENOUGH_CANDIDATES`.
- **Kérdésazonosító:** `q-01`, `q-02`, … a terven belüli pozícióból, `padStart`-tal, nem
  `String.format`-tal – az utóbbi területi beállítástól függően más számjegyeket írhatna,
  a mentett állapotnak viszont minden eszközön egyeznie kell (ADR-005, ADR-007).

### A T-110 tesztjei (`DefaultQuizGeneratorTest`, 15 teszt)

| Teszt | Mit bizonyít |
|---|---|
| `generate_sameRequestAndSeedTwice_producesIdenticalPlan_NFR07` | kétszer hívva bitre ugyanaz a terv, az opciósorrenddel és a helyes válasz indexeivel együtt |
| `generate_equalRequestsBuiltSeparately_produceIdenticalPlan_NFR07` | a determinizmus a kérés **értékén** áll, nem objektumidentitáson |
| `generate_candidateInputOrder_doesNotChangeThePlan_NFR07` | a hívó listájának sorrendje nem számít (rendezett bejárás) |
| `generate_differentSeeds_produceDifferentQuestionOrder_NFR07` | a seed tényleg hat – a keverés nem no-op |
| `quizModuleMainSources_containNoUnseededRandomOrClock_NFR07` | a `main` forráskészletben – megjegyzéseket leszámítva – nincs `Random.Default` / `currentTimeMillis` / `nanoTime` / `Instant.now` / `*.now()` és nincs paraméter nélküli `shuffled()` |
| `generate_optionShuffle_onlyReordersTheSelectedOptions_FR14` | az opciók halmaza a T-109 kiválasztása, a keverés csak permutál |
| `generate_correctAnswerPosition_isShuffledNotAlwaysFirst_TR02` | a helyes válasz nem mindig az első opció |
| `generate_neverRepeatsTheSameCorrectTechnique_TR02` | 20 kérdés, 20 **különböző** helyes technika |
| `generate_fewerCandidatesThanRequested_yieldsShortfallInsteadOfRepetition_FR22` | kevés jelölt → kevesebb kérdés + `Shortfall`, nem ismétlés |
| `generate_exactlyEnoughCandidates_hasNoShortfall_FR13` | pontosan kijövő kérdésszámnál nincs `Shortfall` |
| `generate_noUsableCandidate_returnsExplainedEmptyPlan_FR22` | üres, de megindokolt terv – nincs kivétel |
| `generate_imageTypesWithoutIllustrations_reportSkipReasonAndShortfall_TR03` | kép nélkül a (C)/(D) kimarad `NOT_ENOUGH_IMAGES` okkal, a szöveges típusok futnak |
| `generate_questionIds_areUniqueAndFollowPlayOrder_ADR005` | a kérdésazonosítók egyediek és a lejátszási sorrendet adják |
| `generate_mixesEnabledQuestionTypesWithinOnePlan_FR12` | a terv nem típusonként tömbösített |
| `dryRun_promisesExactlyWhatGenerateProduces_FR13` | `dryRun(r).expectedQuestionCount == generate(r).questionCount` négy különböző kérésre |

### Rétegszabály és eltérés a tervtől

- A `core/quiz` főkódja továbbra is csak a `core:model`-re, a `kotlinx-serialization-json`-ra
  és a `kotlin.random`-ra függ; `android`/`androidx`/`room` import nincs (ADR-001 L1).
- A `Random(seed)` **nem** tiltott RNG-hívás: a seed a `QuizRequest` része, a hívó
  injektált RNG-ből sorsolja (ADR-007). A tiltás a seed nélküli véletlenre és a
  rendszerórára vonatkozik – ezt a forrásszkennelő teszt kényszeríti ki.
- **Eltérés:** a `dryRun()` ebben a körben a `generate()` tervéből származtat. Ez helyes
  (definíció szerint nem tud eltérni), de nem optimális: a beállító képernyő élő
  összegzéséhez kérdéseket épít. A kérdésépítés nélküli összegzés és az `availableByType`
  pontos szemantikája a T-111 tárgya.
- A `Shortfall` okainak prioritási sorrendje a motor **döntése**, a specifikáció csak az
  enumot írja elő. A sorrend a KDoc-ban indokolt; ha a TR-02 készlet (T-112) más sorrendet
  kíván, ott kell egyeztetni.
- Felhasználói szöveg nem keletkezett: a motor szövegfüggetlen, a magyar üzenetek a UI
  `strings.xml`-jében élnek (FR-22, T-404).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-110 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-02 teljes készlet | **RÉSZBEN** – a seed-determinizmus a teljes terven és a nem ismétlés lefedve (fenti 15 teszt); a végponti TR-02 készlet a T-112. |
| TR-03 teljes készlet | **RÉSZBEN** – a kategóriaszűrés és a (C)/(D) kihagyás a generált terven át is ellenőrizve; a teljes készlet a T-112. |
| TR-04…TR-08 | **NEM FUTOTT** – a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |

---

## 2026-11-11 – T-111: `dryRun()` készenléti összegzés (core/quiz)

**Commit:** `9703870`

**Környezet:** Windows 11 Pro, JDK 17.0.21+9 (Eclipse Adoptium), Gradle wrapper 8.13,
`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Tényleges futtatások

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **202 teszt, 0 hiba** (core/model 35 + core/quiz 167) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 688 230 bájt) |

A tesztszámok a `core/*/build/test-results/**/TEST-*.xml` JUnit XML-ek `tests`, `failures`
és `errors` attribútumainak összegei, nem becslés; a `DefaultQuizGeneratorTest` önmagában
**20 teszt, 0 hiba** (a T-110 15 tesztje + a T-111 öt új tesztje).

### Ami elkészült

A `DefaultQuizGenerator.dryRun()` mostantól **nem épít kérdést** (FR-13, FR-22, T-111).

- **Közös kód, nem közös eredmény-utánszámolás.** A `generate()` és a `dryRun()` egyetlen
  találkozási pontja a privát `questionSlots(request, skippedTypes)`: minden meg nem hagyott
  típus minden olyan helyes jelöltje, amely mellé a `DistractorSelector` szerint tényleg
  összeáll négy opció. Ez a lista a `generate()` keverés előtti bemenete **és** a `dryRun()`
  teljes nyersanyaga – a kettő így nem tud elválni egymástól.
- **Miért egyezik a kérdésszám?** A `generate()` a keverés után a *helyes technika* szerint
  deduplikál, és a kért kérdésszámnál megáll; a `buildableCorrectAnswers` garanciája szerint
  pedig minden slotból létrejön kérdés. Ezért minden különböző helyes technika pontosan egy
  kérdést ad, **a keverés sorrendjétől függetlenül**: a kérdésszám
  `min(requestedCount, a slotok különböző helyes technikáinak száma)`. Ezt számolja a
  `dryRun`, kérdésépítés, keverés és RNG nélkül.
- **`availableByType` szemantikája tisztázva.** Típusonkénti **kapacitás** (ennyi kérdés
  állítható össze abból a típusból), nem a terv felosztása. Csak nem kihagyott típus
  szerepel benne, így minden értéke ≥ 1, és a kulcsai nem fedik a `skippedTypes` kulcsait.
  Mivel ugyanaz a technika több típusban is lehet helyes válasz, de egy kvízen belül
  legfeljebb egyszer kerül elő, az **értékek összege nagyobb lehet** az
  `expectedQuestionCount`-nál – a UI tehát típusonkénti elérhetőséget mutat, nem kiosztást.
  A térkép a slotlistából (`groupingBy { it.type }.eachCount()`) áll össze, nem egy második
  számolásból.
- **Új invariánsok a `QuizReadiness`-ben:** nulla kapacitású típus nem szerepelhet az
  `availableByType`-ban; az `availableByType` és a `skippedTypes` kulcsai diszjunktak;
  `availableByType.isEmpty() == (expectedQuestionCount == 0)`; és
  `expectedQuestionCount <= availableByType.values.sum()`.
- **`Shortfall`:** ugyanaz a privát `shortfallOrNull(...)` állítja elő, mint a tervnél,
  ugyanazzal az okprioritással – a beállító képernyő tehát ugyanazt az indoklást kapja,
  amit a kvíz is adni fog.

### A T-111 tesztjei

| Teszt | Mit bizonyít |
|---|---|
| `dryRun_matchesGenerateOnEveryBranch_FR13` | 11 különböző kérésre (bőséges/szűk/négy alatti készlet, letiltott típusok, üres `enabledTypes`, kategóriaszűrő, képes típusok, azonos képujjlenyomatok, szinonimacsoport, határeset) a `dryRun` kérdésszáma, `skippedTypes`-a, `shortfall`-ja és `canStart`-ja megegyezik a `generate` tervével |
| `dryRun_isSeedIndependentWhileGenerateCountStays_FR13` | öt seedre (köztük negatív és `Long.MAX_VALUE`) ugyanaz a `QuizReadiness`, és a `generate` kérdésszáma mindegyiknél ugyanennyi |
| `dryRun_availableByTypeIsPerTypeCapacity_FR13` | 6 technika + két szöveges típus → `availableByType = {JP_TO_HU: 6, HU_TO_JP: 6}`, `expectedQuestionCount = 6`: az összeg **nagyobb** a kérdésszámnál, a kulcsok diszjunktak a kihagyott típusokkal |
| `dryRun_availableByTypeAgreesWithCandidateFilter_FR22` | típusonként a kapacitás a `QuizCandidateFilter.buildableQuestionCount` pontos számával egyezik, a kihagyott típusok kapacitása 0 és kimaradnak a térképből |
| `dryRun_bodyBuildsNoQuestionAndDrawsNoRandom_FR13` | forrásellenőrzés: a `dryRun` törzsében nincs `generate(` hívás, nincs `QuizQuestion`, nincs `Random`, nincs `SeededShuffle` |
| `readiness_availableByTypeIsDisjointFromSkippedTypes_FR22` (`QuizPlanContractTest`) | a `QuizReadiness` elutasítja a nulla kapacitású típust, az egyszerre kihagyott és kapacitásos típust, valamint a „van kapacitás, mégsincs kérdés" állapotot |

### Rétegszabály és eltérés a tervtől

- A `core/quiz` főkódja változatlanul csak a `core:model`-re, a `kotlinx-serialization-json`-ra
  és a `kotlin.random`-ra függ; `android`/`androidx`/`room` import nincs (ADR-001 L1).
- A `dryRun` ágban nincs RNG és nincs rendszeróra-hívás (NFR-07) – a T-110 forrásszkennelő
  tesztje továbbra is zöld.
- **Helyreigazítás:** a `QuizPlanContractTest.readiness_shortfallAgreesWithExpectedCount_FR22`
  korábban azt állította, hogy `availableByType.values.sum() == expectedQuestionCount`. Ez a
  tisztázott szemantika szerint téves elvárás (a kapacitások összege felső korlát), ezért az
  az egy `assert` sor a kapacitás-szemantikát ellenőrzi: diszjunkt kulcsok, minden érték ≥ 1,
  és az összeg ≥ a várható kérdésszám. Maga a kézzel épített fixture nem változott.
- A `QuizCandidateFilter.buildableQuestionCounts(request)` (minden típus, a letiltottakkal
  és nullákkal együtt) a `dryRun`-nak már nem bemenete; diagnosztikai/tartalomellenőrzési
  célra megmarad, a KDoc-ja ezt mondja ki. Jelenleg csak teszt hívja.
- Felhasználói szöveg nem keletkezett: a motor szövegfüggetlen, a magyar üzenetek a UI
  `strings.xml`-jében élnek (FR-22, T-404).

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-111 nem tartalmaz instrumentált tesztet. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-03 teljes készlet | **RÉSZBEN** – a `dryRun`/`generate` egyezés és a kihagyási okok lefedve; a teljes TR-02/TR-03 készlet a T-112 tárgya. |
| TR-02 teljes készlet | **RÉSZBEN** – változatlanul a T-110 lefedettsége; a végponti készlet a T-112. |
| TR-04…TR-08 | **NEM FUTOTT** – a scoringhoz (TR-04), a tartalomfrissítéshez (TR-05) és a UI-hoz (TR-06…TR-08) tartoznak. |

---

## 2026-11-11 – T-112: TR-02 + TR-03 generátor tesztek (core/quiz) – **M1 mag**

### Tényleges futtatások

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:quiz:test` | 0 | BUILD SUCCESSFUL – a kvízmodul teljes készlete zöld |
| `.\gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | BUILD SUCCESSFUL – `:core:model:test` + `:core:quiz:test` újrafuttatva, **236 teszt, 0 hiba** (core/model 35 + core/quiz 201) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

A `JAVA_HOME` a futtatásokhoz: `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.
Tesztszám a `core/*/build/test-results/test/*.xml` JUnit XML-ekből összesítve
(202 → 236, azaz **34 új teszt**; meglévő teszt nem íródott át).

### Mit fed le a T-112 készlete

Három új tesztosztály, mindegyik a `core/quiz` JVM unit tesztje (emulátor nélkül):

| Osztály | Darab | Tárgy |
|---|---|---|
| `SeededShuffleTest` | 7 | a Fisher–Yates keverés elemi garanciái (T-110 alapköve) |
| `QuizPlanInvariantTest` | 20 | a nyolc kvízmotor-invariáns egy **végigsöpört kérésmátrixon**, a kategóriaszűrés, a típusjogosultság, a shortfall-prioritás és a `dryRun` ↔ `generate` egyezés |
| `RealContentQuizGenerationTest` | 7 | ugyanez a **valódi** `knowledgebase_transfer/` tartalmon (161 rekord) |

A közös invariáns-ellenőrzés a `GeneratorContractFixtures.kt`-ba került
(`assertQuizPlanInvariants(label, request, plan)`), hogy a szintetikus mátrix és a valódi
tartalom **ugyanazt** a garanciát bizonyítsa, ne két, egymástól elcsúszó példányt.

### A nyolc invariáns lefedése (TR-02)

| # | Invariáns | Hol dől el |
|---|---|---|
| 1 | pontosan 4 opció | `assertQuizPlanInvariants` – minden terv minden kérdésére |
| 2 | pontosan 1 helyes opció | ugyanott (`options.count { it == correct } == 1`) |
| 3 | a 3 distraktor egymástól és a helyestől is különböző | ugyanott (`options.toSet().size == 4`) |
| 4 | egyetlen opciópár sem szinonima (V006, CR-07) | ugyanott, **páronként** – nem csak a helyeshez mérve |
| 5 | (D) `JP_TO_IMAGE`: nincs két vizuálisan azonos illusztráció | ugyanott + `generate_jpToImage_everyOptionHasADistinctQuizUsableImage_TR03`, `generate_jpToImage_visuallyIdenticalPairLeavesTooFewOptions_TR02` |
| 6 | nincs ismétlés; kevesebb kérdés `Shortfall`-lal | ugyanott + `generate_shortfallPlan_stillSatisfiesEveryQuestionInvariant_TR02` |
| 7 | determinizmus (kérdés- és opciósorrend is) | `generate_sameRequestOnEveryBranch_producesBitIdenticalPlan_TR02`, `generate_candidateInputOrderOnEveryBranch_doesNotChangeThePlan_TR02`, `realContent_sameSeed_producesIdenticalPlan_TR02` |
| 8 | a motor nem gyárt opciót | ugyanott: minden opció létező rekord, benne van a típus `optionPool`-jában és a kategóriahatókörben |

A mátrix 21 kérést × 7 seedet jár végig (`1, 2, 7, 42, -13, Long.MAX_VALUE, Long.MIN_VALUE`):
bőséges és szűk készlet, négy alatti és **üres** jelöltkészlet, letiltott típusok és üres
`enabledTypes`, kategóriaszűrő mindkét irányban, kép nélküli / feliratos / placeholderes
illusztrációk, azonos és részben azonos képujjlenyomatok, szinonimacsoport és **átfedő**
szinonimacsoportok, `INCOMPLETE` / `quizExcluded` / `retired` rekordok, mindkét bizalmi szint.

### TR-03: kategóriaszűrés és típusjogosultság

| Teszt | Mit bizonyít |
|---|---|
| `generate_filteredCategory_neverLeaksAnOutsideTechniqueIntoAnyOption_TR03` | a szűrt kategórián kívüli technika **sem helyesként, sem distraktorként** nem jelenik meg – a feltöltés nem lép ki a szűrőből |
| `generate_filteredCategoryTooSmall_buildsNoQuestionInsteadOfLeaving_TR03` | négy alatti kategóriában inkább nincs kérdés, mint kategórián kívüli opció |
| `generate_captionedIllustrations_skipImageTypesWithReason_TR03` | feliratos kép (`quizUsable = false`) → (C)/(D) `NOT_ENOUGH_IMAGES`, a szöveges típusok állnak |
| `generate_placeholderIllustrations_skipImageTypesWithReason_TR03` | `Placeholder` nem validált kép |
| `generate_missingIllustration_keepsTextTypesAndSkipsImageTypes_TR03` | hiányzó kép ugyanígy |
| `generate_imageToJp_correctAnswerAlwaysHasQuizUsableImage_TR03` | (C): a kép a **helyes** válaszé, a distraktorok nevek – kép nélküli rekord is lehet opció |
| `generate_gateBlockedRecords_buildNoQuestionAtAll_TR03` | `INCOMPLETE` / `quizExcluded` / `retired` → egyetlen kérdés sem; a motor a tartalmi állapotot **nem emeli** (CR-02) |
| `generate_mixedVerificationStatus_followsTheTrustGateOnly_TR03` | lektorálatlan rekord is kvízképes, ha a bizalmi szint engedi (ADR-010) |

### Fallback és a `Shortfall` okprioritása

A `generate_shortfallReason_followsTheDocumentedPriority_TR03` rögzíti a motordöntést:
`ALL_TYPES_SKIPPED` → `NOT_ENOUGH_IMAGES` → `NOT_ENOUGH_DISTRACTORS` →
`NOT_ENOUGH_CANDIDATES`. A `NOT_ENOUGH_DISTRACTORS` ághoz **átfedő** szinonimacsoportok
kellenek (`{KY-0001, KY-0002}` és `{KY-0001, KY-0003}` öt rekord mellett): ekkor öt helyes
jelölt közül csak kettő mellé áll össze három elfogadható distraktor – egyetlen
szinonimacsoport önmagában szimmetrikusan zárna, és a típus egésze esne ki.
A `generate_zeroCandidates_returnsEmptyExplainedPlan_TR02` a nulla jelöltet fedi: üres, de
megindokolt terv, kivétel nélkül.

### `dryRun()` ↔ `generate()`

| Teszt | Mit bizonyít |
|---|---|
| `dryRun_matchesGenerateOnTheWholeMatrix_TR03` | a 21 kérés × 7 seed teljes mátrixán egyezik a kérdésszám, a `skippedTypes`, a `shortfall` és a `canStart`; a terv minden felhasznált típusának kapacitása ≥ az abból adott kérdésszám |
| `dryRunAndGenerate_shareTheSameQuestionSlotsFunction_TR03` | forrásellenőrzés: **mindkét** törzs a `questionSlots(request, skippedTypes)` közös pontot és a `QuizCandidateFilter.skipReasons(request)`-et hívja, és a `dryRun` nem számol másodszor `buildableCorrectAnswers`-ből – a két ág ezért nem tud elválni |
| `dryRun_availableByTypeIsCapacityNotAllocation_TR03` | az `availableByType` típusonkénti **kapacitás**: 8 képes rekordnál mind a négy típus 8, az összeg 32, a várható kérdésszám 8 |
| `realContent_dryRunMatchesGenerateForEveryCategory_TR03` | ugyanez a valódi tartalmon, mind a 9 kategóriára, szöveges és képes típusbeállítással |

### Teszt a valódi tartalomra (TR-03)

| Teszt | Mit bizonyít |
|---|---|
| `realContent_stancesCategory_yieldsImageQuestionsInThePlan_TR03` | az `stances` kategória terve mind a négy kérdéstípust tartalmazza – a (C)/(D) kérdések helyes válaszai a hét kvízképes rekord közül jönnek, `skippedTypes` üres |
| `realContent_everyOtherCategory_yieldsNoImageQuestion_TR03` | a másik **8** kategória egyikében sem keletkezik képes kérdés, és a kihagyás oka mindenhol `NOT_ENOUGH_IMAGES`; a szöveges kvíz viszont minden kategóriában elindítható |
| `realContent_imageOnlyQuizOutsideStances_cannotStart_TR03` | csak képes típusokkal, `kata` kategóriában a terv üres, az ok `ALL_TYPES_SKIPPED` |
| `realContent_everyGeneratedQuestionSatisfiesAllInvariants_TR02` | 3 kérdésszám × 5 seed a teljes 161 rekordon, minden kérdésre a nyolc invariáns |
| `realContent_differentSeeds_produceDifferentPlans_TR02` | más seed más kvíz ugyanabból a tartalomból |

### Megfigyelés a valódi tartalomról

A validátor a mostani tartalomra **üres** V006 szinonimacsoport-listát ad (ezt a
`RealKnowledgeBaseValidationTest` is rögzíti). A valós tartalmi teszt ezért ezt a tényt
mondja ki explicit `assert`-tel: ha a tartalom egyszer szinonimacsoportot hoz, az ne
csendben kerüljön a motorba. A kölcsönös kizárás **viselkedését** a szintetikus mátrix fedi.

### Rétegszabály

A `core/quiz` főkódja **nem változott** ebben a taskban – a T-112 csak tesztet adott. A
modul továbbra is csak a `core:model`-re, a `kotlinx-serialization-json`-ra és a
`kotlin.random`-ra függ; `android`/`androidx`/`room` import nincs (ADR-001 L1, TR-07).
Felhasználói szöveg nem keletkezett.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-112 tisztán JVM unit teszt. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-04…TR-08 | **NEM FUTOTT** – az eredményszámítás (TR-04) a T-113/T-114, a tartalomfrissítés (TR-05) a T-115…T-118, a UI (TR-06…TR-08) a 2. mérföldkő tárgya. |

**TR-02 és TR-03 ezzel lefedett: az M1 mérföldkő kvízhelyességi magja kész.**

---

## 2026-11-11 – T-113: QuizScoring (core/quiz)

**Környezet:** Windows 11 Pro, JDK 17.0.21+9 (Eclipse Adoptium), Gradle wrapper,
`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Tényleges futtatások

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **245 teszt, 0 hiba, 0 kihagyott** (core/model 35 + core/quiz 210) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 688 289 bájt) |

A tesztszámok a `core/*/build/test-results/test/TEST-*.xml` JUnit XML-ek `tests`,
`failures`, `errors` és `skipped` attribútumainak összegei, nem becslés. Az új
`QuizScoringTest` önmagában **9 teszt, 0 hiba**.

### Ami elkészült

`core/quiz/src/main/kotlin/.../scoring/QuizScoring.kt`:

- `data class Score(answered: Int, correct: Int, ratio: Double?)` – a `ratio` **nullable**:
  0 megválaszolt kérdés → `null`, nem `0.0` (FR-27). Az `init` blokk ezt az invariánst
  kétirányúan kikényszeríti (`answered == 0` → `ratio == null`; `answered > 0` → `ratio`
  nem null és `0.0..1.0`), továbbá `correct in 0..answered`.
- `object QuizScoring { fun score(attempts: List<QuizAttempt>): Score }` – tiszta függvény:
  nincs állapot, nincs mellékhatás, nincs óra- és nincs véletlenhívás. A modul már meglévő
  forrásellenőrzéses NFR-07 tesztje (`quizModuleMainSources_containNoUnseededRandomOrClock_NFR07`)
  az új fájlt is átvizsgálja, és zöld.
- Egy kérdés egyszer számít (FR-18): dedup a `(sessionId, questionId)` páron, a legkorábbi
  `answeredAt` nyer, azonos időbélyegnél a lista sorrendje dönt. A meg nem válaszolt kérdés
  nem szerepel az összegzésben, és nem hibás.
- A helyességet nem számolja újra: a `QuizAttempt.correct` mezőt összegzi (DR-09).

### A `QuizScoringTest` esetei (9)

| Teszt | Mit bizonyít |
|---|---|
| `score_noAttempts_hasNoRatioInsteadOfZeroPercent_FR27` | 0 válasz → `ratio == null`, nem `0.0` |
| `score_allWrong_isZeroRatioNotMissingData_FR27` | a 0% valós adat, nem esik `null`-ra |
| `score_allCorrect_isFullRatio_FR20` | 100% határeset |
| `score_abandonedQuiz_countsOnlyAnsweredQuestions_FR20` | 10-ből 4 megválaszolt, 3 helyes → 75% |
| `score_repeatedAnswerForSameQuestion_countsFirstOnly_FR18` | dupla válasz nem duplázza az eredményt, az első számít |
| `score_sameQuestionIdInDifferentSessions_bothCount_FR21` | a kulcs a `(sessionId, questionId)` pár, nem a kérdés |
| `score_doesNotRecomputeCorrectnessFromIds_TR04` | szinonim választás esetén a mentett ítélet nyer (DR-09) |
| `score_isPureAndOrderIndependent_NFR07` | sorrendfüggetlen és ismételhető eredmény |
| `score_rejectsInconsistentManuallyBuiltScore_FR27` | a `Score` invariánsai kézi példányosításnál is tartanak |

### Rétegszabály

A `core/quiz` továbbra is csak a `core:model`-re és a `kotlinx-serialization-json`-ra függ;
a scoring csomag `android`/`androidx`/`room`/`java.io` importot nem tartalmaz (ADR-001 L1,
TR-07). Felhasználói szöveg nem keletkezett – a „nincs adat" szöveg és a százalékformázás a
UI dolga (T-411), a `null` arány csak a jelzés.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-113 tisztán JVM unit teszt. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-04 teljes készlet | **RÉSZBEN** – a fenti 9 alapteszt a szerződést fedi; a teljes TR-04 készlet (bővebb határesetek, valódi tartalom, aggregált statisztika) a T-114 tárgya. |
| TR-05…TR-08 | **NEM FUTOTT** – a tartalomfrissítés (TR-05) a T-115…T-118, a UI (TR-06…TR-08) a 2. mérföldkő tárgya. |

---

## 2026-11-11 – T-114: TR-04 eredményszámítás tesztek (core/quiz) – **M1 lezárva**

**Környezet:** Windows 11 Pro, JDK 17.0.21+9 (Eclipse Adoptium), Gradle wrapper,
`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Tényleges futtatások

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **262 teszt, 0 hiba, 0 kihagyott** (core/model 35 + core/quiz 227) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 688 289 bájt) |

A tesztszámok a `core/*/build/test-results/test/TEST-*.xml` JUnit XML-ek `tests`,
`failures`, `errors` és `skipped` attribútumainak összegei, nem becslés (22 XML fájl).
Az új `QuizScoringTr04Test` önmagában **17 teszt, 0 hiba**; a T-113 `QuizScoringTest`-je
változatlanul 9 teszt.

### Ami elkészült

Egyetlen új fájl, kizárólag teszt – a `QuizScoring` **nem igényelt módosítást**, minden
TR-04 eset első futásra zöld volt:
`core/quiz/src/test/kotlin/hu/kyokushin/trainer/core/quiz/scoring/QuizScoringTr04Test.kt`.

### A TR-04 mátrix esetei (17)

| Teszt | Mit bizonyít |
|---|---|
| `score_zeroAnsweredQuestions_isMissingDataNotZeroPercent_TR04` | 0 válasz → `ratio == null`, és ez *mérhetően* más, mint a 0% (`0.0`) |
| `score_quizStartedButNoQuestionAnsweredYet_isMissingData_TR04` | elindított, de meg nem válaszolt kvíz → `Score(0, 0, null)` |
| `score_allTenAnswersCorrect_isFullRatio_TR04` | 100% határeset 10 válaszon |
| `score_allTenAnswersWrong_isZeroRatio_TR04` | 0% határeset – valós adat, nem `null` |
| `score_abandonedQuizOfTen_countsOnlyTheFourAnswered_TR04` | 10-ből 4 megválaszolva, 3 helyes → `0.75` (US-03/2) |
| `score_unansweredQuestionsAreNotCountedAsWrong_TR04` | a félbehagyott menet (`0.75`) nem eshet egybe a maradék 6-ot hibásként végigvivő menettel (`0.3`), miközben a helyes válaszok száma azonos |
| `score_abandonedQuizWithOnlyCorrectAnswers_isStillFullRatio_TR04` | a félbehagyás nem von le: 3/3 → `1.0` |
| `score_ratioIsFractionNotPercentage_TR04` | 1/4 → `0.25`, nem `25.0` – a formázás a UI dolga |
| `score_doubleTapOnSameQuestion_doesNotDoubleTheResult_TR04` | 2 rekord, 1 kérdés → számláló és nevező is 1 (FR-18) |
| `score_laterCorrectionDoesNotOverrideFirstAnswer_TR04` | a késői „javítás" nem javítja fel az arányt: az első, hibás válasz marad érvényes |
| `score_duplicatesInArbitraryOrder_keepTheEarliestAnswer_TR04` | a duplikátum a párja előtt is állhat a listában – az `answeredAt` dönt |
| `score_duplicatesWithIdenticalTimestamp_areDeterministic_TR04` | azonos időbélyegnél determinisztikus feloldás, egy kérdés akkor is egyszer számít |
| `score_everyQuestionDuplicatedOnceAcrossWholeSession_TR04` | mind a 6 kérdés duplázva: 12 rekord → 6 megválaszolt kérdés |
| `score_mixedQuestionTypesInOneSession_everyTypeCountsOnce_TR04` | mind a négy kérdéstípus egy egységet ér, a típus nem súlyoz |
| `score_perQuestionTypeBreakdownSumsBackToTheWholeSession_TR04` | a típusonkénti bontás összege visszaadja az összesítést (FR-24 előkészítés) |
| `score_typeWithNoAnsweredQuestion_isMissingDataNotZeroPercent_TR04` | a válasz nélküli típus bontása „nincs adat", nem 0% |
| `score_mixedTypesWithDuplicatesAcrossSessions_aggregatesForProgressScreen_TR04` | több menet aggregálása vegyes típusokkal, a dedup kulcsa a `(sessionId, questionId)` pár (FR-21) |

**A „dupla válasz a lista szintjén is ellenőrizve" pont** egy közös segítővel valósult meg
(`assertAnsweredMatchesDistinctQuestions`), amelyet minden dedupot érintő eset meghív:
`score.answered == attempts.map { sessionId to questionId }.toSet().size`,
`score.answered <= attempts.size`, és `score.correct <= score.answered`.

### Rétegszabály és NFR-07

Tiszta JVM teszt a `core/quiz` modulban: nincs `android`/`androidx`/`room` import (ADR-001
L1, TR-07). Az időbélyegek fix `Instant.parse(...)`-ból származnak – a teszt sem hívja az
órát, és nincs véletlenhívás (NFR-07). Felhasználói szöveg nem keletkezett: a „nincs adat"
magyar szöveg és a százalékformázás a T-411 (eredményképernyő) tárgya.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-114 tisztán JVM unit teszt. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-05…TR-08 | **NEM FUTOTT** – a tartalomfrissítés (TR-05) a T-115…T-118, a rétegszabály- és manifest-ellenőrzés (TR-07, TR-08) a T-602/T-603, a UI tesztek (TR-06) a T-606 tárgya. |

**Mérföldkő:** a TR-01 (validátor), TR-02/TR-03 (generátor) és TR-04 (eredményszámítás)
készlet együtt zöld → a Fázis 2 és az **M1 mérföldkő** (a termék helyességének magja) lezárva.

---

## 2026-11-11 – T-115: a kanonikus séma beolvasása (core/data)

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | BUILD SUCCESSFUL – **16 teszt, 0 hiba, 0 kihagyott** (`KnowledgeContentReaderTest` 10, `RealKnowledgeContentReaderTest` 6) |
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **278 teszt, 0 hiba, 0 kihagyott** (core/model 35 + core/quiz 227 + core/data 16) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 719 165 bájt) |

A tesztszámok a `core/*/build/test-results/**/TEST-*.xml` JUnit XML-ek `tests`,
`failures`, `errors` és `skipped` attribútumainak összegei, nem becslés (24 XML fájl).
Ez az első `:core:data` modulban futó tesztkészlet: korábban a modul tesztje `NO-SOURCE` volt.

### Ami elkészült

Négy új `main` fájl a `core/data/src/main/kotlin/hu/kyokushin/trainer/core/data/content/`
alatt – **mind `internal`** (ADR-001 L3: egyetlen DTO sem szivárog ki a modulból):

| Fájl | Tartalom |
|---|---|
| `KnowledgeDto.kt` | `KnowledgeJson` (`ignoreUnknownKeys = true`), `ManifestDto`, `CategoryDto`, `IllustrationDto`, `EntriesFileDto`, `EntryDto` – `@SerialName` snake_case leképezés, minden mező `null`-tűrő |
| `KnowledgeAssetSource.kt` | `KnowledgeAssetSource` interfész, `KnowledgeAssetLayout` (fájlnevek), `AndroidKnowledgeAssetSource` (assets-olvasás + képfájllista) |
| `KnowledgeInputMapper.kt` | DTO → `KnowledgeInput` / `ManifestInput` / `CategoryInput` / `IllustrationInput` / `TechniqueInput` |
| `KnowledgeContentReader.kt` | a beolvasás egyetlen belépési pontja, `Parsed` / `Unreadable` kimenet, a **V010** jelentés előállítása |

Két új teszt- és egy segédfájl a `core/data/src/test/` alatt:
`KnowledgeContentReaderTest.kt`, `RealKnowledgeContentReaderTest.kt`,
`TestKnowledgeAssetSources.kt`.

### A lefedett esetek (16)

| Teszt | Mit bizonyít |
|---|---|
| `read_unknownExtraFields_areIgnoredAndDoNotFailTheLoad_FR29` | jövőbeli extra mező manifest-, kategória-, illusztráció- és rekordszinten sem hiba |
| `read_schemaVersion_isSemverString_notInteger_FR29` | a `schema_version` semver **string**, nem Int (ADR-011) |
| `read_mapsEveryCanonicalFieldToTheValidatorInput_FR29` | a teljes mezőleképezés, a `quiz_eligible` hiánya (ADR-010) és a képfájllista |
| `read_reviewStatus_onlyReviewedMeansVerified_CR01` | csak a `reviewed` jelent hitelesítést; ismeretlen státusz nem emel szintet |
| `read_missingMandatoryField_isReportedByTheValidatorAsV002_notAsParseFailure_DR05` | hiányzó `hungarian_name` → **nem** V010, hanem rekordszintű V002; a másik rekord elfogadva |
| `read_malformedJson_producesV010AndRejectsTheDataset_DR05` | csonka JSON → egyetlen V010 (BLOKKOLÓ, DATASET), a fájlnév a diagnosztikában, `datasetRejected == true` |
| `read_structurallyWrongJson_producesV010_DR05` | érvényes JSON, de `entries` objektum tömb helyett → V010 |
| `read_missingContentFile_producesV010_DR05` | hiányzó `manifest.json` → V010, nem kivétel (FR-31) |
| `read_missingIllustrationFile_isNotALoadFailureButAV005Warning_DR05` | üres képkönyvtár → V005 FIGYELMEZTETÉS, a rekord megmarad |
| `read_emptyEntriesList_isValidContentNotAParseFailure_DR05` | üres `entries` tömb érvényes tartalom |
| `read_realKnowledgeBase_parsesTheCanonicalSchema_FR29` | a valódi tartalom: 161 rekord, 9 kategória (1…9 `sort_order`), 10 illusztráció |
| `read_realKnowledgeBase_collectsThePackagedIllustrationFiles_FR29` | a csomagolt 10 képfájl csupasz neve pontosan a manifest `file` értékeinek utolsó szegmense |
| `read_realKnowledgeBase_neverClaimsVerifiedContent_CR01` | mind a 161 rekord `verified = false`, `quizEligible = null` |
| `read_realKnowledgeBase_passesTheValidatorWithoutBlockingIssue_DR05` | a saját leképezéssel is **0 BLOKKOLÓ hiba**, `accepted = 161`, `datasetTrust = PRACTICE_ON_UNREVIEWED` |
| `read_realKnowledgeBase_resolvesEveryIllustrationToAnAsset_DR05` | 10 `Asset`, 0 `Placeholder`, a három feliratos kép (KY-0045/KY-0050/KY-0052) kvízben nem használható |
| `read_realKnowledgeBase_findsNoSynonymCollision_DR05` | 0 V006 észrevétel, 0 szinonimacsoport – a konverter megállapításával egyezően |

### A V010 helye

A `V010` kódot **az importréteg** állítja elő, nem a validátor: a `KnowledgeContentReader`
elfogja a `SerializationException` / `IOException` / `IllegalArgumentException` hibákat, és
kész `ValidationReport`-ot ad egyetlen V010 észrevétellel (BLOKKOLÓ, DATASET hatókör), a
fájlnévvel a `field` mezőben. Ezzel a V001…V010 kódkészlet teljes: a T-106
`V010_isNeverProducedByTheValidator` tagadó tesztje és ez a készlet együtt írja le a
felelősségmegosztást.

### Rétegszabály, NFR-07, felhasználói szöveg

- Minden új típus `internal` (ellenőrizve: a `content` csomagban nincs `public` deklaráció),
  tehát a DTO-k nem szivárognak ki a `core/data`-ból (ADR-001 L3/L4, TR-07).
- Nincs `Clock`, `Instant.now()` vagy `Random` hívás (NFR-07): a réteg állapotmentes.
- Felhasználói szöveg nem keletkezett. A V010 `detail` szövege **diagnosztikai** üzenet
  (fejlesztői diagnosztika, FR-45), nem UI-felirat – a `ValidationIssue.detail` szerződése
  szerint ezek nem `strings.xml`-be tartoznak.
- A valódi tartalom útvonalát a `kyo.repoRoot` rendszertulajdonság adja át
  (`core/data/build.gradle.kts`), nem relatív `../..` hivatkozás – ugyanaz a konvenció,
  mint a `core/quiz`-ben.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `AndroidKnowledgeAssetSource` unit teszt | **NEM FUTOTT** – a `core/data` modulnak nincs saját `assets/` könyvtára (a tudástár másolása az `app` assetsbe a T-601), és a Robolectric `AssetManager` csak a modul tényleges assetjeit látja. A beolvasó logikája a `KnowledgeAssetSource` absztrakción keresztül teljesen lefedett; a delegáló osztály (útvonal-összefűzés + UTF-8 olvasás) első valódi ellenőrzése a T-601 utáni indításkor és a T-606 UI tesztekben történik. |
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03); a T-115 tisztán JVM unit teszt. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-05 | **NEM FUTOTT** – a tartalomfrissítés előrehaladás-megőrzése Room sémát és importálót igényel: T-116, T-117, majd a tesztek T-118. |

---

## 2026-11-11 – T-116: Room séma (core/data)

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:kspDebugKotlin` | 0 | BUILD SUCCESSFUL – a séma JSON létrejött: `core/data/schemas/hu.kyokushin.trainer.core.data.db.KyokushinDatabase/1.json` |
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | BUILD SUCCESSFUL – **52 teszt, 0 hiba, 0 kihagyott** (`KyokushinTypeConvertersTest` 10, `RoomSchemaContractTest` 14, `DaoContractTest` 2, `KyokushinDatabaseTest` 10, a T-115 16 tesztje változatlanul zöld) |
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **314 teszt, 0 hiba, 0 kihagyott** (core/model 35 + core/quiz 227 + core/data 52); 36 új teszt |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 772 902 bájt) |

A tesztszámok a `*/*/build/test-results/**/TEST-*.xml` JUnit XML-ek `tests`, `failures`,
`errors` és `skipped` attribútumainak összegei, nem becslés.

### Amit a tesztek mérnek

| Teszt | Mit rögzít |
|---|---|
| `converter_instant_roundTripsThroughEpochMilli_DR08` | `Instant` ↔ `Long` epoch milli, oda-vissza |
| `converter_instant_nullStaysNullForUnfinishedSession_FR19` | a befejezetlen menet `finishedAt`-ja NULL marad |
| `converter_instant_epochAndNegativeValuesSurvive_DR08` | `Instant.EPOCH` és epoch előtti időpont is sértetlen |
| `converter_instant_subMillisecondPrecisionIsIntentionallyTruncated_DR08` | a nanoszekundumos rész tudatos vesztése (csonkolás, nem kerekítés) |
| `converter_enums_arePersistedByNameNotOrdinal_DR08` | a tárolt alak a konstans **neve**, nem szám |
| `converter_enums_everyConstantRoundTrips_DR08` | mind az 5 enum minden konstansa oda-vissza |
| `converter_enums_unknownStoredValueThrowsInsteadOfSilentDefault_CR02` | ismeretlen tárolt érték **dob**, nem esik vissza engedékenyebb értékre |
| `converter_illustrationKind_nullMeansNoIllustrationReference_ADR003` | a „nincs illusztráció" állapot a NULL oszlopérték |
| `converter_identifiers_roundTripThroughTheirFactory_DR03` | a value class azonosítók a privát konstruktor helyett az `of()` gyáron át |
| `converter_identifiers_blankStoredValueIsRejectedByTheFactory_DR01` | üres azonosító az adatbázisból sem jut a domainbe |
| `schema_isExportedForTheCurrentVersion_DR08` | a sémaexport megvan, `version = 1`, `identityHash` kitöltött |
| `schema_containsEveryContentAndUserTable_DR04` | pontosan a 5 tartalom- és 3 felhasználói tábla |
| `quizAttempt_hasUniqueIndexOnSessionAndQuestion_FR18` | **egyetlen** egyedi index, és az a `(sessionId, questionId)` páron (ADR-005) |
| `quizAttempt_createSqlDeclaresTheUniqueIndex_FR18` | a tényleges DDL `CREATE UNIQUE INDEX` – a megszorítás az adatbázisban él |
| `userTables_haveNoForeignKeyToContentTables_DR07` | a felhasználói tábláknak **nincs** idegen kulcsa tartalomtáblára |
| `userTables_techniqueReferenceIsPlainIndexedColumn_TR05` | a `techniqueId` sima, indexelt TEXT oszlop FK nélkül |
| `contentTables_neverCascadeIntoUserTables_DR07` | a megfordított irány is tilos |
| `technique_referencesCategoryWithoutCascade_ADR004` | a tartalmon belüli kategória-hivatkozás `NO ACTION` |
| `quizAttempt_cascadesOnlyFromItsOwnSession_DR07` | az egyetlen `CASCADE` felhasználói → felhasználói irányban van |
| `schema_storesInstantsAsIntegerEpochMilli_DR08` | mind az 5 időbélyeg-oszlop INTEGER |
| `schema_storesEnumsAsTextNotOrdinal_DR08` | mind az 5 enum-oszlop TEXT |
| `schema_identifiersAreTextAndGeneratedKeysAreInteger_DR04` | `quiz_session.id` TEXT (domain adja), `quiz_attempt.id`/`study_review.id` INTEGER `autoGenerate` |
| `technique_hasNoQuizEligibleColumn_ADR010` | nincs `quizEligible` oszlop – származtatott érték |
| `dataModule_neverFallsBackToDestructiveMigration_DR08` | a modul forrásában nincs `fallbackToDestructiveMigration(` és `allowMainThreadQueries(` hívás (a KDoc-említések kiszűrve) |
| `dao_everyFunctionIsSuspendOrReturnsFlow_NFR01` | mind a 8 DAO minden függvénye `suspend` vagy `Flow` – reflexióval a lefordított interfészen |
| `dao_databaseExposesEveryDao_DR04` | nincs tábla DAO nélkül |
| `recordAttempt_secondAnswerToTheSameQuestionIsIgnoredNotDuplicated_FR18` | a második válasz `-1`-et ad, 1 sor marad, és az **első** ítélet marad meg |
| `recordAttempt_differentQuestionsInTheSameSessionAreBothRecorded_FR18` | az index nem fog túl sokat |
| `recordAttempt_sameTechniqueTwiceInOneSessionIsAllowed_ADR005` | ugyanaz a technika két külön kérdésben rögzülhet |
| `recordAttempt_theSameQuestionInANewSessionIsRecordedAgain_FR21` | a hibák újragyakorlása új `sessionId`-vel nem ütközik |
| `userData_survivesRetiringAndDeletingTheTechnique_TR05` | `retired` jelölés **és** a technikasor fizikai törlése után is megvan a próbálkozás és az önértékelés |
| `contentUpsert_isIdempotentAndKeepsUserRows_FR32` | ugyanaz a rekord újra betöltve upsert, a felhasználói sor érintetlen |
| `deletingASession_cascadesToItsOwnAttemptsOnly_DR07` | a menet törlése a saját válaszait viszi, az önértékelést és a tartalmat nem |
| `storedColumns_holdEnumNamesAndEpochMillis_DR08` | nyers SQL olvasás: `"IMAGE_TO_JP"`, epoch milli, TEXT azonosító |
| `unfinishedSession_keepsNullFinishedAtUntilClosed_FR19` | a lezárás egyszer ír, másodszor nem |
| `contentState_staysASingleRowAcrossImports_DR04` | a `content_state` egysoros marad |

### Az exportált séma tényleges tartalma

A `1.json` ellenőrzött lényege (a tesztek ezt állítják, nem leírás):

- `quiz_attempt`: `index_quiz_attempt_sessionId_questionId` **unique**, és egy sima index a
  `techniqueId`-n; egyetlen idegen kulcs: `quiz_session(sessionId)` `CASCADE`.
- `study_review`: **nulla** idegen kulcs, sima index a `techniqueId`-n.
- `technique`: egyetlen idegen kulcs a `category`-ra, `NO ACTION`; nincs `quizEligible` oszlop.
- Időbélyegek `INTEGER`, enumok és azonosítók `TEXT`.

### Rétegszabály, NFR-07, felhasználói szöveg

- A `db` csomagban **nincs public deklaráció**: minden entitás, DAO, konverter és az
  adatbázis is `internal` (ADR-001 L3/L4, TR-07).
- Nincs `Clock`, `Instant.now()` vagy `Random` hívás (NFR-07): a konverter kapott értéket
  alakít, a tesztek fix időbélyegeket használnak.
- Felhasználói szöveg nem keletkezett. A konverterek kivételszövegei **fejlesztői
  diagnosztika** (olvashatatlan adatbázisérték), nem UI-felirat – nem `strings.xml`-be tartoznak.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy futó emulátor (B-03). A séma viselkedését emulátor helyett Robolectric + in-memory Room méri. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| Room `MigrationTestHelper` | **NEM FUTOTT** – `version = 1`, migráció még nem létezik; az első migrációs teszt a `2.json` megjelenésekor esedékes (DR-08). |
| TR-05 teljes lefedése | **RÉSZBEN** – a `userData_survivesRetiringAndDeletingTheTechnique_TR05` és a `contentUpsert_isIdempotentAndKeepsUserRows_FR32` a séma szintjén már bizonyítja az előrehaladás megőrzését, de a teljes TR-05 (átnevezés, valódi `contentVersion` váltás a `ContentImporter`-en keresztül, statisztika-változatlanság) a T-117 után, a T-118 tárgya. |

---

## 2026-11-11 – T-117: ContentImporter (core/data)

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | BUILD SUCCESSFUL – **68 teszt, 0 hiba, 0 kihagyott** (`ContentImporterTest` 10, `ContentEntityMappersTest` 6, a T-115/T-116 52 tesztje változatlanul zöld) |
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **330 teszt, 0 hiba, 0 kihagyott** (core/model 35 + core/quiz 227 + core/data 68); 16 új teszt |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` (19 789 772 bájt) |

A tesztszámok a `*/*/build/test-results/**/TEST-*.xml` JUnit XML-ek `tests`, `failures` és
`errors` attribútumainak összegei, nem becslés.

### Amit a tesztek mérnek

| Teszt | Mit rögzít |
|---|---|
| `importIfNeeded_loadsPackagedContentIntoTheContentTables_FR29` | kategória, technika, alias (normalizált alakkal) és `Asset` illusztráció bekerül; a hitelesítési státusz a tartalomból jön |
| `importIfNeeded_fillsContentStateWithTheValidationSummary_FR32` | a `content_state` egy sora: séma- és tartalomverzió, `datasetTrust`, fix `importedAt`, elfogadott/elvetett darabszám |
| `lastValidationReport_exposesTheReportOfTheLastImport_FR32` | import előtt `null`, utána a tényleges jelentés – a fejlesztői diagnosztika forrása |
| `importIfNeeded_sameVersionTwice_skipsTheImportAndDoesNotDuplicate_FR30` | **új importáló példánnyal**: a verziókapu a tárolt állapotból dönt, nincs duplikáció, validáció sem fut |
| `importIfNeeded_newVersionRetiresMissingTechniqueAndKeepsUserRows_FR33` | a kimaradt technika `retired = true` (sora megvan), a `quiz_attempt` és `study_review` sorszám **változatlan**, a hivatkozás feloldható, az átnevezett technika `id`-ja ugyanaz |
| `importIfNeeded_removedAliasDoesNotSurviveTheUpdate_FR29` | az aliaslista cserélődik, nem gyűlik |
| `importIfNeeded_retiredTechniqueReturningInANewVersionBecomesActiveAgain_ADR004` | a visszatérő technika `retired` jelölése feloldódik (az upsert a teljes sort írja) |
| `importIfNeeded_unreadableContentIsRejectedAndKeepsTheLoadedContent_FR31` | V010: a tartalomtáblák és a kijelzett `contentVersion` az előző, sikeres kiadásé marad |
| `importIfNeeded_unsupportedSchemaVersionIsRejectedWithoutWritingContent_FR31` | V001: egyetlen tartalomsor sem keletkezik; első indulásnál az elutasítás a `content_state`-be kerül |
| `importIfNeeded_rejectedRecordIsNotImportedButDoesNotStopTheRest_FR31` | V002-s rekord kimarad, a többi betöltődik |
| `technique_roundTripsThroughTheEntityWithoutLoss_DR01` | entitás ↔ domain körbejárás veszteségmentes |
| `technique_illustrationRefKeepsItsThreeStates_ADR003` | nincs kép / `Placeholder` / `Asset` a négy nullázható oszlopon át is megmarad, a kvízkapukkal együtt |
| `techniqueAlias_normalizedFormIsDerivedOnImport_FR03` | az `aliasNormalized` a `NameNormalizer` kimenete (kisbetű, ékezet nélkül, összehúzott whitespace) |
| `category_roundTripsAndKeepsTheContentDrivenOrder_DR02` | kategória körbejárás a tartalom diktálta `sortOrder`-rel |
| `categoryInput_missingSortOrderFallsBackToTheArrayOrder_DR02` | hiányzó `sort_order` → tömbindex; azonosító nélküli bejegyzés kimarad; név nélküli kategória az azonosítóját mutatja (kitalált magyar nevet nem gyártunk) |
| `synonymGroups_areStoredDeterministically_CR07` | a `synonym_group` sorai `TechniqueId` szerint rendezve, determinisztikusan |

### A FR-33 garancia helye a kódban

A `RoomContentImporter` **nem hivatkozik** a `quiz_session`, `quiz_attempt` és
`study_review` táblákra – a felhasználói adat megőrzése nem futásidejű óvatosság, hanem
szerkezeti tény. A tartalomoldalon `DELETE ALL` nincs: a technikára csak `@Upsert` és
`markRetired` megy (ADR-004), aliasra technikánkénti csere, és az egyetlen teljes ürítés a
`synonym_group`, amelynek `groupId`-ja nem stabil azonosító, és felhasználói adat nem
hivatkozik rá.

### Rétegszabály, NFR-07, felhasználói szöveg

- A `ContentImporter` interfész és az `ImportResult` publikus, a `RoomContentImporter`,
  a DAO-k, az entitások és a mapperek `internal` (ADR-001 L4).
- A `core/data` → `core/quiz` függés `implementation`-ből `api`-ra változott, mert a
  publikus szerződés `core/quiz` típust ad vissza (`ValidationReport`). A `core/quiz`
  tiszta Kotlin, a továbbadása nem sért rétegszabályt (L1/L2 a fordított irányt tiltja).
- NFR-07: az `importedAt` injektált `Clock`-ból, a futás injektált `@IoDispatcher`-en;
  `Instant.now()`, `System.currentTimeMillis()` és `Random` nincs. A tesztek fix órát és
  `UnconfinedTestDispatcher`-t adnak.
- Felhasználói szöveg nem keletkezett: a jelentés `detail` szövegei fejlesztői
  diagnosztika, nem UI-felirat.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). Az import viselkedését Robolectric + in-memory Room méri. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| TR-05 teljes lefedése | **RÉSZBEN** – a `importIfNeeded_newVersionRetiresMissingTechniqueAndKeepsUserRows_FR33` már a valódi importálón mér (sorszám-változatlanság, visszavonás, átnevezés), de a teljes TR-05 készlet (aggregált statisztika változatlansága, több körös frissítés, `TR05` jelölésű tesztnevek) a T-118 tárgya. |
| Az import az **éles** csomagolt assetsen | **NEM FUTOTT** – az `app/src/main/assets/knowledge/` feltöltése a T-601 feladata; addig az importáló szintetikus tartalmon fut, a beolvasó pedig a valódi 161 rekordon (T-115). |

---

## 2026-11-11 – T-118: TR-05 tartalomfrissítés tesztek (core/data)

### Futtatott parancsok

`JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`, PowerShell, `F:\Claude\kyo`.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | BUILD SUCCESSFUL – **78 teszt, 0 hiba, 0 kihagyott** (`ContentUpdateProgressTest` 10 új teszt, a T-115/T-116/T-117 68 tesztje változatlanul zöld) |
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – összesen **340 teszt, 0 hiba, 0 kihagyott** (core/model 35 + core/quiz 227 + core/data 78); 10 új teszt |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL (`:app:assembleDebug` UP-TO-DATE – a feladat csak tesztkódot adott, az APK tartalma nem változott) |

A tesztszámok a `core/*/build/test-results/**/TEST-*.xml` JUnit XML-ek `tests`, `failures`
és `skipped` attribútumainak összegei, nem becslés.

Közbenső, **elbukott** futás (a jelentés része): az első
`.\gradlew.bat :core:data:testDebugUnitTest` exit 1, 2 hibás teszt – mindkettő **hibás
teszt-elvárás** volt, nem terméktermékhiba: (1) a frissítés utáni technikasorok száma 4 és
nem 3, mert a visszavont `KY-0003` sora megmarad (éppen ez a garancia), (2) a kimaradó
technikára nem volt felvett önértékelés. Mindkettő a tesztben javítva, a termelési kód
nem változott.

### Amit a 10 új teszt mér (mind `TR05` jelöléssel)

| Teszt | Mit rögzít |
|---|---|
| `contentUpdate_keepsEveryAttemptAndTheComputedStatisticsUnchanged_TR05` | v1 → előrehaladás → v2 (átnevezés + visszavonás + új technika **egyszerre**): a `quiz_attempt` és `study_review` sorok **bitre azonosak**, és a levezetett statisztika is (5 válasz, 3 helyes, 60 %, technikánkénti bontás, menetenkénti `answeredCount`) |
| `contentUpdate_progressSurvivesSeveralConsecutiveVersions_TR05` | v1 → v2 → v3: több körös frissítés után is változatlan a teljes előrehaladás-kép |
| `contentUpdate_keepsStudyReviewsAndTheLatestSelfRating_TR05` | az önértékelések megmaradnak, és a „legfrissebb ítélet" levezetés (DR-09) ugyanazt adja |
| `contentUpdate_renamedTechniqueKeepsItsIdAndProgressFollowsTheNewName_TR05` | DR-06: az `id` marad, a `romaji`/magyar név/alias új, a próbálkozások ugyanazok, és az **új** rekordra oldódnak fel |
| `contentUpdate_techniqueMissingFromTheNewVersionIsRetiredButStaysResolvable_TR05` | a csomagból kimaradt technika `retired = true`, kiesik az aktív listából, a sora és a rá hivatkozó előrehaladás viszont megvan és feloldható |
| `contentUpdate_techniqueDeclaredRetiredByTheContentKeepsItsProgress_TR05` | a tartalom által kimondottan `retired: true` rekord: elfogadott marad (nem a `retireMissing` ágon jelölődik), az előrehaladása sértetlen |
| `contentUpdate_retiredTechniqueComingBackKeepsItsEarlierProgress_TR05` | ADR-004: a visszatérő technika újra aktív, és a **teljes** korábbi előrehaladása alatta van |
| `contentUpdate_sameVersionImportedTwice_duplicatesNothing_TR05` | FR-30 idempotencia új importálópéldánnyal: `UpToDate`, nincs tartalmi és nincs felhasználói duplikáció, validáció sem fut |
| `contentUpdate_repeatedImportOfTheUpdatedVersionDuplicatesNothing_TR05` | a **frissítés után** harmadszor-negyedszer betöltött v2 sem duplikál; a 4 technikasor közül 3 aktív, 1 visszavont |
| `contentUpdate_rejectedDatasetKeepsBothContentAndProgress_TR05` | V001-es elutasítás: a tartalom az előző kiadásé marad (FR-31) **és** az előrehaladás is sértetlen |

### Hogyan mér „statisztikát", ha az aggregált lekérdezések még nincsenek meg

A `ProgressRepository` aggregált lekérdezései a T-501 feladat tárgya. A teszt ezért a
tárolt sorokból **maga** számolja a statisztikát (`ProgressSnapshot`: a nyers sorok, az
összes helyes válasz, a találati arány százalékban, a technikánkénti bontás a legfrissebb
önértékeléssel, és a menetek `answeredCount`-ja), majd a frissítés előtti és utáni
pillanatképet **egyetlen egyenlőségvizsgálattal** veti össze. Ez ugyanaz az adat, amiből a
későbbi lekérdezések is dolgoznak (DR-09: a statisztika levezetett, nem duplikált
számláló), így a garancia a *tárolt tényekre* vonatkozik, és nem romlik el attól, ha a
T-501 lekérdezései máshogy alakulnak. A nyers sorok és a levezetett számok együtt vannak
a pillanatképben: az összegek egy sorcsere esetén még egyezhetnének.

### Szerkezeti változás a tesztekben

A csomagolt kiadás JSON-összeállítása a `TestKnowledgeAssetSources.kt`-ba került
(`KnowledgeSourceFixtures`: `source()`, `manifest()`, `entry()`), mert a TR-05 garanciát
két tesztosztály méri, és ugyanazt a tartalmat kell két kiadásban előállítani. A T-117
`ContentImporterTest` **tesztjei változatlanok**, csak a privát segédfüggvényei
delegálnak a közös fixture-re. A tartalom hiteles Kyokushin terminológia
(`Zenkutsu dachi`, `Kokutsu dachi`, `Kiba dachi`, `Sanchin dachi`), minden rekord
`review_status = editorial_review_required`, a hitelesítési szintet a teszt sem emeli
(CR-02).

### Rétegszabály, NFR-07, felhasználói szöveg

- A feladat **csak tesztkódot** adott a `core/data` `test` forráskészletéhez; termelési
  kód, séma és publikus szerződés nem változott.
- NFR-07: fix `Clock.fixed(...)` és `UnconfinedTestDispatcher`; `Instant.now()`,
  `System.currentTimeMillis()` és `Random` a tesztekben sincs.
- Felhasználói szöveg nem keletkezett, UI nem érintett.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A TR-05 garanciát Robolectric + in-memory Room méri, `@Config(sdk = [34])` (a compileSdk = 37-hez nincs `android-all` artifact). |
| `./gradlew ktlintCheck` | **NEM FUTOTT** – `Task 'ktlintCheck' not found in root project 'KyokushinKarateTrainer'`, exit 1: a ktlint konfigurációja még nincs beállítva (T-607). |
| `./gradlew lint` | **NEM FUTOTT** – nem része a feladat DoD-jának; a T-607 tárgya. |
| A TR-05 az **éles** csomagolt assetsen | **NEM FUTOTT** – az `app/src/main/assets/knowledge/` feltöltése a T-601 feladata; a tesztek szintetikus, de hiteles terminológiájú kiadásokon futnak. |

---

## T-201 – `core/data`: `TechniqueRepository` (2026-11-11)

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | a modul tesztjei zöldek (90 teszt) |
| `.\gradlew.bat testDebugUnitTest` | 0 | **352 teszt, 0 hiba, 0 kihagyott** (12 új) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL, `app/build/outputs/apk/debug/app-debug.apk` (18,9 MB) |

JAVA_HOME: `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Amit a 12 új teszt mér (`RoomTechniqueRepositoryTest`, Robolectric + in-memory Room)

| Teszt | Mit rögzít |
|---|---|
| `observeCategories_followsContentSortOrderNotAlphabet_DR02` | a kategóriák a `sortOrder` szerint jönnek, nem ábécében – a fixtúra szándékosan fordított sorrendben szúrja be őket |
| `observeTechniques_excludesRetiredTechniques_DR07` | a visszavont technika **nincs** a listában |
| `getQuizCandidates_excludesRetiredTechniques_DR07` | a visszavont technika **nincs** a kvízjelöltek között |
| `getTechnique_resolvesRetiredTechniqueSoProgressKeepsReference_DR07` | a visszavont technika **feloldható** (a Haladás képernyőnek kell), az ismeretlen azonosító viszont `null` |
| `observeTechniques_matchesRomajiHungarianAndAliasCaseAndAccentInsensitively_FR03` | „DACHI" → `Heisoku Dachi`; „zart labu" → „Zárt lábú állás"; „ZUKI" → alias-találat a tárolt normalizált alakon; csak whitespace keresés = nincs szűrés |
| `observeTechniques_emptyCategoryFilterMeansEveryCategory_FR02` | üres kategóriahalmaz = minden kategória, nem „semmi" |
| `observeTechniques_attachesAliasesWithoutPerRecordQuery_FR03` | az aliasok egyetlen lekérdezésből kerülnek a technikára (NFR-10) |
| `getQuizCandidates_dropsSynonymMembersOutsideTheCandidateSet_CR07` | a szűrés után megcsonkult szinonimacsoport tagjai kiesnek, az egyeleműre fogyott csoport is – különben a `QuizCandidateSet` konstruktora dobna (V006) |
| `getQuizCandidates_honoursCategoryFilterAndKeepsIllustrationGates_FR11` | kategóriaszűrés, és a `quizUsable = false` kapu változatlanul megy át (CR-02) |
| `observeContentState_reportsNotImportedBeforeTheFirstImport_FR32` | import előtt `ContentState.NotImported`, `hasContent = false`, bizalmi szint a szigorúbb olvasat |
| `observeContentState_reportsCurrentTableContentNotTheLastImportBalance_FR31` | elutasított frissítés után `datasetRejected = true` és `acceptedCount = 0` **mellett** `activeTechniqueCount = 1` és `hasContent = true` – a tartalomtáblák a korábbi kiadásé |
| `techniqueRepository_publicApiExposesNoRoomOrDatabaseTypes_NFR03` | reflexióval: a `TechniqueRepository` egyetlen metódusának sem a visszatérési, sem a paramétertípusa (generikus típusargumentumokkal együtt) nem Room entitás / DAO / DTO (TR-07, ADR-001 L4) |

### Egy bukott futás és a javítása

Az első futás 6 `Flow`-s tesztje elhasalt: `IllegalStateException: Detected use of
different schedulers`. Ok: a `UnconfinedTestDispatcher()` paraméter nélkül **saját**
`TestCoroutineScheduler`-t hoz létre, és a `combine` + `flowOn` láncon a Room megfigyelt
lekérdezése két különböző ütemezővel találkozott. Javítás a **tesztben**, a termelési kód
változtatása nélkül: a dispatcher a `runTest` `testScheduler`-ét kapja meg, közös
`repositoryTest { … }` gyárfüggvényből. Pontszerű, `withContext`-es metódusnál ez a hiba
nem derül ki – ezért érdemes a `Flow`-s ágakat mindig külön teszttel is lefedni.

### Rétegszabály, NFR-07, felhasználói szöveg

- A publikus API (`TechniqueRepository`, `ContentState`) kizárólag `core/model` és
  `core/quiz` típusokat használ; a megvalósítás és minden DAO `internal`. A reflexiós
  teszt ezt gépileg is állítja (TR-07).
- NFR-07: nincs `Clock`, `Instant.now()`, `System.currentTimeMillis()` és `Random` hívás
  a repositoryban; a dispatcher injektált, a tesztben teszt-dispatcher.
- Felhasználói szöveg nem keletkezett (a kategórianév tartalmi adat, nem `strings.xml` –
  ADR-011), UI nem érintett.
- A Room séma **nem változott**: csak új `@Query` metódusok kerültek a DAO-kba, migráció
  nem kellett, a `core/data/schemas/1.json` érintetlen.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A repository viselkedését Robolectric + in-memory Room méri, `@Config(sdk = [34])`. |
| `./gradlew ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| `./gradlew lint` | **NEM FUTOTT** – nem része a feladat DoD-jának; a T-607 tárgya. |
| A keresés `LIKE`-os, SQL-oldali ága | **NINCS MÉG** – a T-202 tárgya; a mért viselkedés a beolvasott soron futó, de szerződés szerinti szűrés. |
| A `QuizCandidateSet.illustrationDigests` feltöltése | **NINCS MÉG** – nincs ujjlenyomat-oszlop a sémában, a T-402 tárgya. Üres térképre nincs teszt, mert nincs mit mérni. |

---

## 2026-11-11 – T-202: SQL-oldali keresés + séma 1 → 2 migráció (`core/data`)

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest --tests "*KyokushinMigrationTest*"` | 0 | BUILD SUCCESSFUL – a migrációs teszt önmagában |
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – **358 teszt, 0 hiba, 0 error** (T-201 után 352 volt: +6) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk`, 19 816 715 bájt |

`JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.

### Új tesztek

| Teszt | Mit bizonyít |
|---|---|
| `RoomTechniqueRepositoryTest.observeTechniques_findsEveryDachiTechniqueForAnUppercaseQuery_FR03` | „DACHI" → **mindkét** `… dachi` technika (US-06 2. kritérium); a `LIKE '%…%'` részszó, nem prefix |
| `RoomTechniqueRepositoryTest.observeTechniques_filtersOnTheStoredSearchColumnsInSql_FR03` | a szűrés a tárolt `search_*` oszlopokon fut: az üres keresőoszlopú sor **nem** találat, pedig a neve illeszkedne – ha a szűrés Kotlin oldalon futna, az lenne |
| `RoomTechniqueRepositoryTest.observeTechniqueMatches_tellsWhichAliasMatched_FR03` | alias-találatnál a `matchedAlias` az egyező alias eredeti alakja („Chūdan Zuki"); saját névre, magyar névre és üres keresésre `null` (US-06 3. kritérium) |
| `RoomTechniqueRepositoryTest.observeTechniques_treatsLikeWildcardsAsLiteralCharacters_FR03` | a beírt `%` és `_` literális karakter, nem joker – egyetlen `%` nem adja vissza a teljes tudástárat |
| `RoomSchemaContractTest.techniqueTable_hasThePreNormalizedSearchColumns_FR03` | a `search_romaji` / `search_hungarian` az exportált sémában is ott van |
| `KyokushinMigrationTest.migration1To2_addsSearchColumnsAndKeepsUserProgress_DR08` | a Room `MigrationTestHelper` a `2.json` exporthoz **validálja** a migrált sémát; a `search_romaji` áthidaló visszatöltése `heisoku dachi`; a `content_state.contentVersion` `NULL` (verziókapu nyitva); a `quiz_session` / `quiz_attempt` / `study_review` sorok megmaradnak (DR-07, FR-33) |

### A `MigrationTestHelper` ebben a környezetben – ami működött és ami nem

A migrációs teszt **lefutott** (emulátor nélkül, Robolectric alatt), de két
környezetspecifikus akadályt kellett megoldani, és mindkettő a jövőbeli migrációkra is
érvényes:

1. **A séma JSON-t assetsből olvassa.** Az első kísérlet
   `FileNotFoundException: Cannot find the schema file in the assets folder … Missing file:
   hu.kyokushin.trainer.core.data.db.KyokushinDatabase/1.json` hibával elhasalt. Megoldás:
   a `core/data/schemas` könyvtár a **`test` source set** assetjeként van bekötve
   (`core/data/build.gradle.kts`) – a leszállított APK-ba így nem kerül bele.
2. **A név alapú ág nem használható Robolectric alatt.** A
   `MigrationTestHelper(instrumentation, databaseClass)` + `createDatabase(name, version)`
   út `IllegalArgumentException: This driver is configured to open a database named
   'migration-test.db' but 'C:\…\databases\migration-test.db' was requested` hibát ad
   (a driver a nevet, a Room az abszolút utat kéri). Megoldás: a **fájl alapú**
   konstruktor (`MigrationTestHelper(instrumentation, File, AndroidSQLiteDriver(),
   KyokushinDatabase::class)`) és a `createDatabase(1)` / `runMigrationsAndValidate(2, …)`
   `SQLiteConnection`-os API.
3. **A segítő nem törli a fájlt.** Egy második futtatás
   `IllegalStateException: A migration from 2 to 1 was required but not found` hibával
   bukott, mert az előző futás fájlja már a 2-es verzión állt. Megoldás: a teszt `@Before`
   és `@After` törli az átmeneti fájlt a `-wal` / `-shm` / `-journal` kísérőkkel együtt.

### Rétegszabály, NFR-07, felhasználói szöveg

- A `TechniqueMatch` a `core/data` publikus szerződésének része, és kizárólag
  `core/model` típust hordoz (`Technique`); a `TechniqueSearchRow` Room-projekció
  `internal` (TR-07, ADR-001 L4). A `techniqueRepository_publicApiExposesNoRoomOrDatabaseTypes_NFR03`
  reflexiós teszt az új metódust is átvizsgálja.
- NFR-07: nincs `Clock`, `Instant.now()`, `System.currentTimeMillis()` vagy `Random` hívás
  az új kódban; a dispatcher injektált.
- Felhasználói szöveg nem keletkezett: az „Egyezés: `{alias}`" sor megjelenítése a UI
  feladata (T-207/T-208), a szöveg akkor kerül `strings.xml`-be.
- A `NameNormalizer` **nem duplikálódott**: a normalizálás továbbra is egyetlen helyen él,
  az SQL csak az általa előállított oszlopokon illeszt.

### NEM FUTOTT

| Parancs / teszt | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A keresést és a migrációt Robolectric + Room méri, `@Config(sdk = [34])`. |
| `./gradlew ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| `./gradlew lint` | **NEM FUTOTT** – nem része a feladat DoD-jának; a T-607 tárgya. |
| Az alias-ág mérése **valódi** tartalmon | **NEM MÉRHETŐ** – az `aliases` mező a kapott tudástárban mindenhol üres (R-05), ezért az alias-tesztek fixtúrája szintetikus. A tárolt `aliasNormalized` ugyanaz a normalizáló kimenete, mint éles úton. |
| A `search_hungarian` oszlop **pontos** értéke migráció után, import előtt | **NEM ÁLL ELŐ** – a migráció SQL-oldali `lower()`-e az ékezeteket nem bontja le; a pontos értéket a verziókapu nyitása után a következő import írja be (ADR-012). A teszt ezért a `contentVersion = NULL` kaput méri, nem egy ékezetmentes magyar alakot. |

---

## 2026-11-11 – T-203 · `di`: Hilt modulok (Hilt gráf)

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 30s` – **365 teszt, 0 hiba, 0 kihagyott** (a T-202 utáni 358-hoz képest +7) |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL in 13s` – `app/build/outputs/apk/debug/app-debug.apk` |

Környezet: `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`,
Windows PowerShell, Gradle wrapper 9.9.1.

### Új tesztek (7)

| Teszt | Mit igazol |
|---|---|
| `HiltGraphTest.hiltGraph_providesRepositoryAndImporterBehindInterfaces_NFR03` | a `@Binds` a Room-implementációkra mutat, a hívó mégis csak az interfészt látja |
| `HiltGraphTest.hiltGraph_providesQuizEngineBindings_NFR03` | `QuizGenerator` = `DefaultQuizGenerator`, `KnowledgeValidator` = `DefaultKnowledgeValidator` |
| `HiltGraphTest.hiltGraph_providesInjectedClockAndRandom_NFR07` | a `Clock` injektált és UTC zónájú; az RNG-ből valóban húzható seed |
| `HiltGraphTest.hiltGraph_bindsDistinctIoAndDefaultDispatchers_NFR07` | a két qualifier két **különböző** készletre mutat |
| `DatabaseModuleTest.provideDatabase_opensWithCurrentSchemaVersionAndIsUsable_NFR03` | a kötés adta példány a 2-es sémával megnyílik és lekérdezhető |
| `DatabaseModuleTest.provideDaos_bindEveryDaoFromTheSingleDatabase_NFR03` | mind a 8 DAO-nak van kötése, egyetlen adatbázisból (DR-04) |
| `DatabaseModuleTest.coreDataMainSources_containNoDestructiveMigrationFallback_DR08` | a `core/data` `main` forrásfájában nincs `fallbackToDestructiveMigration`, és a kötés átadja a migrációkat |

### A Hilt gráf felépülése – mivel igazolt

1. **Fordítási időben.** A `di` modul `KyokushinGraph` `@EntryPoint`-ja mind a nyolc
   alkalmazásszintű kötést kéri, így azok a Dagger *pruned* gráfjába is bekerülnek: egy
   hiányzó vagy kétértelmű kötés az `:app:hiltJavaCompileDebug` taskot – és ezzel az
   `assembleDebug`-ot – elbuktatja. Entry point nélkül a nem használt kötések hiányzó
   függőségei csendben elnézve maradnának, mert a ViewModeleket csak a későbbi feladatok
   hozzák.
2. **Futásidőben, emulátor nélkül.** A `HiltGraphTest` `@HiltAndroidTest` +
   `HiltTestApplication` + Robolectric (`@Config(sdk = [34])`): ugyanaz a
   `SingletonComponent` épül fel, mint a `@HiltAndroidApp` alatt, minden classpathon lévő
   `@InstallIn(SingletonComponent::class)` modullal. Az `@Inject` mezők feloldása tehát a
   **példányosítást** is igazolja – a Room adatbázis megnyílik, az assetolvasó megkapja az
   `AssetManager`-t, a repository és az importáló megépül.

### Menet közbeni hibák és megoldásuk (hitelesség kedvéért rögzítve)

1. `:di:compileDebugKotlin FAILED` – `Syntax error: Missing '}'` + `Unclosed comment`.
   Ok: a KDoc szövegében szereplő `` `feature/*` `` **nested blokk-megjegyzést** nyitott
   (a Kotlin blokk-megjegyzései egymásba ágyazhatók), így a záró `*/` csak a belsőt zárta.
   Megoldás: a KDoc nem ír `/*` karakterpárt.
2. `:core:data:compileDebugUnitTestKotlin FAILED` – `Unsupported escape sequence`.
   Ok: a forrásszkennelő teszt regexe sima string literálban kapott `\*`-ot. Megoldás:
   nyers (`"""…"""`) string literál.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A gráf felépülését Robolectric méri; a valódi indítást a TR-06 (T-606) fedi. |
| Az app indítása **valódi eszközön/emulátoron** | **NEM FUTOTT** – B-03. A DoD „az app elindul" pontja a fenti kettős (fordítási idejű + Robolectric futásidejű) gráfellenőrzéssel igazolt, eszközindítással nem. |
| `./gradlew ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |
| `./gradlew lint` | **NEM FUTOTT** – nem része a feladat DoD-jának; a T-607 tárgya. |
| `@DefaultDispatcher` **tényleges használata** üzleti kódban | **NINCS MÉG HASZNÁLÓJA** – a qualifier és a kötése kész, az első fogyasztója az első ViewModel (T-207) lesz. A kötést a `HiltGraphTest` ennek ellenére feloldja, tehát nem „halott" kötés. |

---

## 2026-11-11 – T-204 · `core/ui`: `TechniqueIllustration` (az első Compose komponens)

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 8s` – a `core/ui` modul **10 új tesztje** zöld (`:core:ui:testDebugUnitTest` ténylegesen lefutott, nem NO-SOURCE) |
| `.\gradlew.bat test testDebugUnitTest --rerun-tasks` | **0** | `BUILD SUCCESSFUL in 32s` – a teljes készlet újrafúratva: **375 teszt, 0 hiba, 0 kihagyott** (a T-203 utáni 365-höz képest +10) |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL in 2s` – `app/build/outputs/apk/debug/app-debug.apk` (20 288 364 byte) |

Commit: `85caebe`. Környezet: `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`,
Windows PowerShell, Gradle wrapper 9.9.1.

Modulonkénti bontás a `build/test-results` XML-ekből: `core/quiz` 227, `core/data` 99,
`core/model` 35, **`core/ui` 10**, `app` 4 – összesen 375, hiba és hibás befejezés 0.

### Új tesztek (10)

| Teszt | Mit igazol |
|---|---|
| `IllustrationContentDescriptionTest.assetDescription_inLibrary_namesTheTechnique_NFR04` | a Tudástárban a leírás **tartalmazza** a romajit és a magyar nevet (FUNCTIONAL_SPEC 9. ezt megköveteli) |
| `IllustrationContentDescriptionTest.assetDescription_forQuizOption_omitsTechniqueName_NFR04` | **a T-204 lényegi pontja:** a kvízopció leírása sem a romajit, sem a magyar nevet nem mondja ki, csak a pozíciót |
| `IllustrationContentDescriptionTest.placeholderDescription_forQuizOption_omitsTechniqueName_NFR04` | a helyőrző sem szivárogtat névet a kvízrácson |
| `IllustrationContentDescriptionTest.placeholderDescription_inLibrary_statesTheMissingIllustration_NFR04` | a helyőrző leírása pontosan „Ehhez a technikához még nincs ábra” (FUNCTIONAL_SPEC 2.3) |
| `IllustrationContentDescriptionTest.decorativeLabel_hasNoDescription_NFR04` | dekoratív képnél `contentDescription = null`, tehát a TalkBack átugorja |
| `IllustrationContentDescriptionTest.quizOptionLabel_rejectsNonPositivePosition_NFR04` | a nem pozitív sorszám `require`-rel elbukik, nincs „0. válaszlehetőség” leírás |
| `IllustrationSampleSizeTest.sampleSize_forPackagedIllustrations_downscalesLargeImages_NFR05` | a tényleges 1122×1402-es készlet `inSampleSize = 2`-vel jön be |
| `IllustrationSampleSizeTest.sampleSize_imageWithinLimit_isNotDownscaled_NFR05` | a korláton belüli képet nem ritkítjuk |
| `IllustrationSampleSizeTest.sampleSize_isSmallestPowerOfTwoKeepingLongestEdgeWithinLimit_NFR05` | hat méretre: a lépték a legkisebb kettőhatvány, amely belül tartja a hosszú élt – és nem ritkít túl |
| `IllustrationSampleSizeTest.sampleSize_unknownBounds_fallsBackToFullResolution_NFR05` | ismeretlen méret (−1) vagy értelmetlen korlát esetén nincs ritkítás |

### Miért unit teszt a NFR-04 bizonyítéka, és nem UI teszt?

A leírásokat `@Composable`-mentes `IllustrationContentDescription` oldja fel egy
`Resources` paraméterből, nem `stringResource` a komponens belsejében. Ezért a szabály
**emulátor nélkül**, Robolectric alatt (`@Config(sdk = [34])`, `isIncludeAndroidResources
= true`) a **tényleges** `core/ui/src/main/res/values/strings.xml` fölött mérhető: ha
valaki a magyar szöveget átírja úgy, hogy a kvízopció leírásába technikanév-helyet tesz,
a teszt elbukik. UI teszttel ez a B-03 blokkoló (nincs emulátor) mellett nem lenne
mérhető.

Második, fordítási idejű védelem: a komponens nem `contentDescription: String`-et kap,
hanem az `IllustrationLabel` szándéktípust, és a `QuizOption` ágon a technika nevére
**nincs paraméter** – a név kiszivárgása így nem programozói figyelmetlenség kérdése.

### Menet közbeni hibák és megoldásuk (hitelesség kedvéért rögzítve)

1. `:core:ui:compileDebugKotlin FAILED` – `Unclosed comment` a `Theme.kt`-ben,
   és nyomában `Unresolved reference 'KyokushinTrainerTheme'`. Ok: **ugyanaz a
   csapda, mint a T-203-ban** – a KDoc szövegében szereplő `` `feature/*` ``
   beágyazott blokk-megjegyzést nyitott. Megoldás: a KDoc nem ír `/*` karakterpárt.
2. `IllustrationContentDescriptionTest > classMethod FAILED` –
   `UnsupportedOperationException at DefaultSdkProvider.java:174`. Ok: `@Config(manifest =
   Config.NONE)` mellett a Robolectric 4.17 a `compileSdk = 37`-hez próbált SDK-képet
   keresni. Megoldás: a projekt meglévő konvenciója, `@Config(sdk = [34])` – ez
   egyszerre adja vissza az erőforrásokat is, amire a teszt épít.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt (`createAndroidComposeRule`) a komponensre | **NEM FUTOTT** – B-03. A `contentDescription` szabályt a fenti unit tesztek fedik; a tényleges TalkBack bejárás a T-604 és T-606 tárgya. |
| `@Preview` **tényleges kirenderelése** (világos/sötét) | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel, fejnélküli CLI-ből nem fut. Ami igazolt: a négy előnézet-függvény (nyolc `@Preview` bejegyzés) **lefordul** (`:core:ui:compileDebugKotlin` exit 0), és a párosítás világos + `UI_MODE_NIGHT_YES` kódban ellenőrizhető. |
| A kép **tényleges** megjelenítése assetből | **MÉG NINCS ASSET** – a `knowledge/illustrations/` tartalmát a T-601 teszi az `app/src/main/assets` alá. Addig az `Asset` ág a „hiányzó fájl” → helyőrző tartalékon fut, ami a FUNCTIONAL_SPEC 10. szerinti elvárt viselkedés (nincs összeomlás). |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

---

## 2026-11-11 – T-205 · `core/ui`: jelölők és állapotkomponensek

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:ui:testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 5s` – a `core/ui` 28 tesztje zöld (18 új) |
| `.\gradlew.bat testDebugUnitTest` | **0** | `BUILD SUCCESSFUL in 9s` – az Android modulok (`core/data`, `core/ui`, `app`) tesztjei zöldek |
| `.\gradlew.bat test testDebugUnitTest --rerun-tasks` | **0** | `BUILD SUCCESSFUL in 30s` – a teljes készlet újrafúratva: **393 teszt, 0 hiba, 0 kihagyott** (a T-204 utáni 375-höz képest +18) |
| `.\gradlew.bat assembleDebug` | **0** | `BUILD SUCCESSFUL in 2s` – `app/build/outputs/apk/debug/app-debug.apk` (20 296 639 byte) |

Környezet: `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`,
Windows PowerShell, Gradle wrapper 9.9.1. Commit: `e73ac8e`.

Modulonkénti bontás a `build/test-results` XML-ekből: `core/quiz` 227, `core/data` 99,
`core/model` 35, **`core/ui` 28**, `app` 4 – összesen 393, hiba és hibás befejezés 0.

### Új tesztek (18)

| Teszt | Mit igazol |
|---|---|
| `VerificationBadgeTextTest.verifiedRecord_hasNoBadgeAtAll_FR05` | a hitelesített rekord **nem** kap jelölőt – a hitelesség a jelölő hiánya (FUNCTIONAL_SPEC 2.2) |
| `VerificationBadgeTextTest.awaitingReview_isLabelledAwaitingReview_FR05` | a címke pontosan „Ellenőrzésre vár" |
| `VerificationBadgeTextTest.incomplete_isLabelledIncomplete_FR05` | a címke pontosan „Hiányos" |
| `VerificationBadgeTextTest.incomplete_listsTheMissingFieldsByName_FR05` | a hiányzó mezők magyar megnevezéssel, az erőforrásból jönnek |
| `VerificationBadgeTextTest.incomplete_withoutMissingFieldList_showsLabelWithoutDetail_FR05` | üres felsorolás mellett a chip részletezés nélkül áll, nem tűnik el |
| `VerificationBadgeTextTest.awaitingReview_neverListsMissingFields_FR05` | ép, csak nem lektorált rekordnál nincs hiányzó mező, a hívó tévedése nem jut a képernyőre |
| `VerificationBadgeTextTest.missingFields_areDeduplicated_FR05` | a kétszer átadott mező egyszer szerepel |
| `VerificationBadgeTextTest.everyTechniqueField_hasAHungarianLabel_NFR02` | mind a hat `TechniqueField` értéknek van nem üres erőforrás-szövege |
| `UnreviewedNoticeTest.unreviewedDataset_showsTheNonDismissibleBanner_CR03` | a szalag szövege szó szerint az ADR-010 szerinti |
| `UnreviewedNoticeTest.reviewedDataset_showsNoBannerWithoutCodeChange_ADR010` | **a T-205 lényegi pontja:** `REVIEWED` tartalomnál a jelzés magától eltűnik (AC-08) |
| `UnreviewedNoticeTest.bannerText_dependsOnlyOnDatasetTrust_ADR010` | a szalag szövege kizárólag a bizalmi szinttől függ, nincs rejtett állapot |
| `EmptyStateReasonTest.noContent_andNoResult_differInMessageAndAction_NFR06` | **a másik lényegi pont:** a „nincs tartalom" és a „nincs találat" üzenete ÉS akciója különbözik |
| `EmptyStateReasonTest.libraryNoContent_reportsLoadFailureAndOffersDiagnostics_FR32` | a betöltési hiba üzenete + a „Diagnosztika" akció |
| `EmptyStateReasonTest.libraryNoResult_quotesTheQueryAndOffersFilterReset_NFR06` | a keresőszöveg látszik az üzenetben, az akció „Szűrők törlése" |
| `EmptyStateReasonTest.progressNoData_callsToActionInTheMessageAndHasNoButton_NFR06` | akció nélküli ok: „Még nincs adat. Indíts egy kvízt!" |
| `EmptyStateReasonTest.quizNotEnoughData_explainsWhatToChange_NFR06` | a kvíz üres állapota megmondja, mit kell változtatni |
| `EmptyStateReasonTest.everyReason_producesANonEmptyHungarianMessage_NFR06` | mind a négy ok ad nem üres magyar üzenetet |
| `EmptyStateReasonTest.errorState_offersRetryInHungarian_NFR06` | az „Újra" akció és a hibaüzenet erőforrásból jön |

### Miért unit teszt az elrejthetetlenség bizonyítéka?

Két egymást erősítő bizonyíték van, és egyik sem UI teszt (B-03: nincs emulátor):

1. **Fordítási idejű:** az `UnreviewedBanner`-nek nincs `onDismiss`, `dismissible` vagy
   `visible` paramétere, és a szöveget feloldó `UnreviewedNotice.bannerText` egyetlen
   bemenete a `DatasetTrust`. Nincs olyan hívás, amivel a szalagot ki lehetne kapcsolni –
   nem azért, mert senki nem teszi meg, hanem mert nincs rá API.
2. **Futásidejű:** a fenti három Robolectric teszt a **tényleges** `strings.xml` fölött
   fut, és a `REVIEWED` → `null`, `PRACTICE_ON_UNREVIEWED` → pontos szöveg párosítást
   méri. Ha valaki a szalagot felhasználói beállításhoz kötné, a bemenet típusa miatt
   ezek elbuknak.

### Menet közbeni hiba és megoldása (hitelesség kedvéért rögzítve)

`VerificationBadgeTextTest.incomplete_listsTheMissingFieldsByName_FR05 FAILED` –
a felsorolás „illusztráció,magyar jelentés" lett a várt „illusztráció, magyar jelentés"
helyett. Ok: az `aapt` levágja a sztringerőforrás záró szóközét, így a `", "`
elválasztóból `","` maradt. Megoldás: az erőforrásérték dupla idézőjelbe zárva
(`<string name="...">", "</string>`), ami megőrzi a szóközt. Ez nem a teszt hibás
elvárása volt, ezért a kód – pontosabban az erőforrás – javult, nem a teszt.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt (`createAndroidComposeRule`) a négy komponensre | **NEM FUTOTT** – B-03. A szövegszabályokat a fenti unit tesztek fedik; a tényleges TalkBack bejárás, a `LiveRegionMode.Polite` szalag felolvasása és a 48 dp-s érintési célpont mérése a T-604 és T-606 tárgya. |
| `@Preview` **tényleges kirenderelése** (világos/sötét) | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel, fejnélküli CLI-ből nem fut. Ami igazolt: a kilenc előnézet-függvény (18 `@Preview` bejegyzés) **lefordul** (`:core:ui:compileDebugKotlin` exit 0), és a világos + `UI_MODE_NIGHT_YES` párosítás kódban ellenőrizhető. |
| WCAG AA kontrasztmérés a jelölők és a szalag színein | **NEM FUTOTT** – eszköz nélkül nem mérhető. Ami igazolt: minden szín Material 3 téma-tokenpárból jön (`surfaceVariant`/`onSurfaceVariant`, `errorContainer`/`onErrorContainer`), hardcode-olt `Color(0xFF…)` nincs a komponensekben. A tényleges mérés a T-604 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

---

## 2026-11-11 – T-206 · `navigation`: NavHost és alsó navigáció

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :navigation:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 15 új teszt zöld |
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` |
| `.\gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **408 teszt, 0 hiba, 0 kihagyott** (393 → 408) |
| `.\gradlew.bat assembleDebug --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (19 925 550 byte) |

A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések `tests`, `failures`,
`errors` és `skipped` attribútumainak összegzése adta (41 teszt-XML).

### Új tesztek (15) – `NavigationGraphTest`

| Teszt | Mit bizonyít |
|---|---|
| `navGraph_declaresExactlyTheSpecifiedRoutes_NFR03` | a tíz útvonal **pontosan** a FUNCTIONAL_SPEC 1. szerinti, se több, se kevesebb |
| `navGraph_hasNoDuplicateRoutePattern_NFR03` | nincs két cél ugyanazon az útvonalon |
| `navGraph_startsOnTheLibraryList_NFR03` | az indulási útvonal a Tudástár listája, és az fülkezdő |
| `bottomNav_hasTheFourSpecifiedDestinationsInOrder_NFR03` | négy fül, a specifikált sorrendben |
| `bottomNav_everyDestinationStartsOnATopLevelScreen_NFR03` | fül nem vihet mélyebb képernyőre |
| `bottomNav_everyScreenBelongsToExactlyOneTab_NFR03` | mind a tíz képernyő **pontosan egy** fülhöz tartozik – nincs „egyik fül sem kijelölt" állapot |
| `bottomNav_keepsTheOwningTabSelectedOnDeeperScreens_NFR03` | a részletezőn, a kártyákon és a technika-haladáson a saját fül marad kijelölve |
| `bottomNav_settingsKeepsTheProgressTabSelected_NFR03` | a `settings`-nek nincs önálló füle, a Haladásból nyílik |
| `bottomBar_isHiddenWhileAQuizIsRunning_TR06` | **a task lényegi pontja:** `quiz/run` → nincs bottom nav |
| `bottomBar_isHiddenOnTheQuizResult_TR06` | `quiz/result` → nincs bottom nav |
| `bottomBar_isVisibleOnEveryScreenOutsideTheRunningQuiz_TR06` | a rejtés **csak** erre a két képernyőre szól (a lista zárt: nem csúszhat bele több) |
| `bottomBar_isVisibleOnAnUnknownRoute_TR06` | ismeretlen útvonal nem veszi el a navigációt, nem zárja csapdába a felhasználót |
| `routeLookup_matchesThePatternNotTheFilledRoute_NFR03` | a keresés a `NavDestination.route`-ban kapott **mintára** illeszkedik |
| `routeBuilders_fillTheArgumentIntoTheDeclaredPattern_NFR03` | a `KyokushinRoutes` által épített útvonal és a gráfban deklarált minta nem csúszhat el |
| `argumentCarryingRoutes_useTheSharedArgumentNames_NFR03` | mind a négy argumentumos útvonal a `NavArgs` nevét használja |

### Miért sima JVM teszt, és nem Compose UI teszt?

A `KyokushinScreen` és a `BottomNavDestination` szándékosan **Android-mentes adat**:
az útvonalminta, a szakaszhoz tartozás és a „látszik-e a bottom nav" eldönthető a Compose
fa kirenderelése nélkül. A rejtés nem elszórt `if`-ekben él, hanem egyetlen helyen,
adatként (`KyokushinScreen.showsBottomBar`), és a `KyokushinNavHost` a `bottomBar` lambdát
csak akkor állítja össze, ha az `isBottomBarVisible` igazat ad. Így a teszt a tényleges
döntési pontot méri, nem egy mellékhatást. A `navigation` modulnak nincs Robolectric
függősége, és nem is kellett hozzáadni.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| A fülállapot megmaradásának **tényleges** mérése fülváltáskor | **NEM FUTOTT** – B-03. Ami igazolt: a fülre navigálás `popUpTo(startDestination) { saveState = true }` + `restoreState = true` + `launchSingleTop = true` beállítással történik, ami a Navigation Compose állapotmegőrző útja. A tényleges „beírom a keresőt, fület váltok, visszaváltok" bejárás emulátort kér – T-606. |
| Compose UI teszt a bottom nav elrejtésére (koppintás-bejárás) | **NEM FUTOTT** – B-03. A döntést a fenti három `TR06` teszt fedi le a tényleges döntési ponton. |
| `@Preview` **tényleges kirenderelése** (világos/sötét) | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel. Ami igazolt: a négy előnézet-függvény (`PlaceholderScreen` és `KyokushinBottomBar`, világos + sötét) **lefordul** (`:navigation:compileDebugKotlin` exit 0). |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

---

## 2026-11-12 – T-207 + T-211 · `feature/library`: lista ViewModel és tesztjei (G-02)

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:library:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 17 új teszt zöld |
| `./gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 1m 16s`, 259 task mind végrehajtva – **425 teszt, 0 hiba, 0 kihagyott** (408 → 425) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (20 424 842 byte) |

A futtatás Bash-ből történt (`export JAVA_HOME=".../jdk-17.0.21.9-hotspot"`,
`./gradlew.bat …`): a PowerShell ebben a session-ben le volt tiltva. A tesztszámot a
`*/build/test-results/**/TEST-*.xml` jelentések `tests` és `failures` attribútumainak
összegzése adta.

### Új tesztek (17) – `LibraryListViewModelTest`

A tételes lista a [`docs/done/T-211-feature-library-viewmodel-tesztek.md`](done/T-211-feature-library-viewmodel-tesztek.md)
„Elkészült" szakaszában van. Lefedett követelmények: FR-01 (kategóriákba szervezett
lista, tartalomból jövő kategórianév, `retired` kizárás, `datasetTrust`), FR-02
(kategóriaszűrés több kategóriával, `SavedStateHandle` forgatásra, szűrők törlése), FR-03
(ékezet- és kisbetű-érzéketlen rész-szó keresés, alias-találat jelzése, debounce).

### Miért Robolectric, ha a ViewModel Android-mentes?

Két pont miatt, és mindkettő a **tényleges** viselkedést méri:

1. a `SavedStateHandle` Android `Bundle` fölött validálja a tárolt értékeket – ezért
   derült ki, hogy a kategóriakijelölést `ArrayList`-ként kell tárolni, nem a `plus`/`minus`
   adta tetszőleges `List`-ként;
2. az üres állapot magyar üzenete a `core/ui` **valódi** `strings.xml`-jéből oldódik fel,
   tehát a teszt a specifikált szöveget ellenőrzi, nem egy kódba írt másolatát.

`@Config(sdk = [34])` – a `compileSdk = 37`-hez nincs `android-all` csomag.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt a Tudástár listájára | **NEM FUTOTT** – ebben a taskban **nincs** Compose kód: a T-207 kizárólag az állapotkezelés, a képernyő helyén a T-206 helyőrzője áll. A lista UI és a UI tesztje a T-208, illetve T-606 tárgya. |
| `@Preview` világos + sötét témára | **NEM ALKALMAZHATÓ** ebben a taskban – a ViewModel nem rajzol. A globális DoD „UI esetén" kikötése a T-208-nál esedékes. |
| Tényleges képernyőforgatás eszközön | **NEM FUTOTT** – B-03. Ami igazolt: ugyanabból a `SavedStateHandle`-ből újraépített ViewModel visszakapja a keresőszöveget és a kategóriakijelölést (`libraryList_savedStateHandle_restoresQueryAndCategories_FR02`) – ez az a mechanizmus, amin a forgatás túlélése áll. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

---

## 2026-11-12 – T-208 + T-210 · `feature/library`: lista UI és üres/hiba állapotok (G-03)

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:library:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 9 új teszt zöld |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **434 teszt, 0 hiba, 0 kihagyott** (425 → 434) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

A futtatás Bash-ből történt (`export JAVA_HOME=".../jdk-17.0.21.9-hotspot"`,
`./gradlew.bat …`). A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések
`tests` és `failures` attribútumainak összegzése adta (minden modulra).

### Új tesztek (9)

| Teszt | Mit bizonyít |
|---|---|
| `LibraryListAccessibilityTest.verifiedRecord_omitsTheStatusPart_FR05` | VERIFIED rekordnál a listatétel leírásából elmarad a státuszrész – ugyanaz a döntés, mint a `VerificationBadge` chipjénél |
| `LibraryListAccessibilityTest.awaitingReview_appendsTheSpokenStatus_FR05` | AWAITING_REVIEW rekordnál a leírás „..., Ellenőrzésre vár"-ra végződik |
| `LibraryListAccessibilityTest.incomplete_appendsTheSpokenStatus_FR05` | INCOMPLETE rekordnál a leírás „..., Hiányos"-ra végződik |
| `LibraryListAccessibilityTest.aliasMatch_appendsTheMatchedAliasAsASeparateSentence_FR03` | alias-találatnál az „Egyezés: {alias}" külön mondatként zárja a leírást |
| `LibraryListAccessibilityTest.nameMatch_hasNoAliasSentence_FR03` | saját névből jövő találatnál nincs „Egyezés" rész |
| `LibraryEmptyStateActionTest.noResult_offersClearFilters_notDiagnostics` | „nincs találat" oknál a „Szűrők törlése" akció fut, a diagnosztika nem |
| `LibraryEmptyStateActionTest.noContent_inDebugBuild_offersDiagnostics_FR32` | „nincs tartalom" oknál debug build mellett a diagnosztika akció fut |
| `LibraryEmptyStateActionTest.noContent_inReleaseBuild_hasNoAction_FR32` | „nincs tartalom" oknál `onDiagnostics = null` (release) esetén nincs akció – az `EmptyState` emiatt nem rajzol gombot |
| `LibraryEmptyStateActionTest.otherReasons_haveNoActionOnTheLibraryScreen` | a Tudástárhoz nem tartozó okok (`ProgressNoData` stb.) nem váltanak ki akciót |

### Miért tiszta függvény teszt, és nem Compose UI teszt?

A listatétel akadálymentes leírását ([`LibraryListItemAccessibility`](../feature/library/src/main/kotlin/hu/kyokushin/trainer/feature/library/list/LibraryListAccessibility.kt))
és az üres állapot akció-leképezését ([`libraryEmptyStateAction`](../feature/library/src/main/kotlin/hu/kyokushin/trainer/feature/library/list/LibraryListScreen.kt))
szándékosan `@Composable`-mentes, `Resources`-t fogadó (illetve sima) függvényként írtuk
meg – ugyanaz a minta, mint a `core/ui` `VerificationBadgeText` és `IllustrationContentDescription`
tesztjeinél. Így a FUNCTIONAL_SPEC 9. „egy bejárási egység" szabálya és a FR-32 debug
kapuja a **tényleges** `strings.xml` fölött, emulátor nélkül, Robolectric unit teszttel
bizonyítható – a tényleges kirenderelés/koppintás-bejárás emulátort kérne (lásd alább).

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt a listára (koppintás, TalkBack bejárás, tényleges 56×56 dp méret) | **NEM FUTOTT** – B-03. Ami igazolt: a leírás-összeállítás és az akció-leképezés logikája (fent), és hogy mind az öt `@Preview` függvény lefordul és render-hívás nélkül helyesen épül fel (`:feature:library:compileDebugKotlin` exit 0). |
| `@Preview` **tényleges kirenderelése** (világos/sötét, mind a négy állapot) | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel. Ami igazolt: a tíz előnézet-függvény (öt állapot × világos/sötét) lefordul. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
