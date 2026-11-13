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

---

## 2026-11-12 – T-209 · `feature/library`: részletező ViewModel + UI

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:library:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 7 új teszt zöld (`LibraryDetailViewModelTest`) |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **441 teszt, 0 hiba, 0 kihagyott** (434 → 441), minden task frissen futtatva (0 `UP-TO-DATE`) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (20 426 186 bájt, frissen generálva) |

A futtatás Bash-ből történt (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`,
`./gradlew.bat …`). A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések
`tests` attribútumainak összegzése adta (minden modulra, `test` és `testDebugUnitTest`
feladatra együtt).

### Új tesztek (7) – `LibraryDetailViewModelTest`

| Teszt | Mit bizonyít |
|---|---|
| `libraryDetail_loadsAllPresentFields_FR04` | a `getTechnique` eredménye a `Content` állapot összes jelenlévő mezőjébe (romaji, magyar név, kategória) helyesen kerül át |
| `libraryDetail_optionalFieldsAbsent_stayNullOrEmpty_FR04` | hiányzó opcionális mezők (japán íráskép, leírás, aliasok) `null`/üres marad – a UI ettől nem rajzol „nincs adat" szakaszt |
| `libraryDetail_incompleteRecord_missingFieldsNameTheIllustration_FR05` | `INCOMPLETE` rekordnál a hiányzó mezők listája `[ILLUSTRATION]` (US-08 3. kritérium) |
| `libraryDetail_verifiedRecord_hasNoMissingFields_FR05` | hitelesített rekordnál a hiányzó mezők listája üres |
| `libraryDetail_unknownTechniqueId_showsErrorInsteadOfCrashing_US07` | ismeretlen azonosítóra `Error(library_detail_not_found)` érkezik, nincs kivétel a hívónál |
| `libraryDetail_repositoryThrows_showsErrorAndRetryRecovers_US07` | a repository kivétele `Error(error_load_failed)`-et ad, az „Újra" (`onRetry`) helyreállítja a `Content` állapotot |
| `libraryDetail_exposesDatasetTrustForUnreviewedBanner_US08` | a `datasetTrust` a tartalom bizalmi szintjéből jön, nem felhasználói beállításból (ADR-010) |

### Miért nincs saját Compose UI teszt

Ugyanaz a döntés, mint a T-208/T-210-nél: a `LibraryDetailScreen.kt` nyolc `@Preview`
függvénye (négy állapot × világos/sötét) a `:feature:library:compileDebugKotlin`
sikeres fordításával igazoltan felépül és lefordul, de a tényleges kirenderelés és a
koppintás-bejárás (kategóriachip, TalkBack sorrend) emulátort vagy Android Studio
előnézet-renderelőt igényel (B-03). A ViewModel logikáját – ami a tartalmi helyesség
döntő része – a fenti 7 Robolectric unit teszt fedi, a Compose fa kirenderelése nélkül.

A `navigation` modul `KyokushinNavHost.kt`-jának módosítása (a `LIBRARY_DETAIL` ág
lecserélése, a kategóriavisszaadás `previousBackStackEntry.savedStateHandle` mintája)
a `:navigation:testDebugUnitTest`-tel igazoltan fordul, és a meglévő 15
`NavigationGraphTest` teszt (útvonalminták, argumentumnevek) zöld marad – ez a teszt
nem Compose-fát épít, csak az adatszintű szerződést (route-ok, argumentumnevek)
ellenőrzi, ugyanúgy, mint a T-206 óta.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt a részletezőre (koppintás a kategóriachipen, tényleges vissza-navigáció, TalkBack bejárás) | **NEM FUTOTT** – B-03. Ami igazolt: a nyolc `@Preview` lefordul, és a kategória-visszaadás string-kulcsa (`LibraryListViewModel.KEY_CATEGORIES`) a navigációs gráf teszttel (route-szint) és a lista ViewModel tesztjeivel (a kulcs tényleges hatása a szűrésre) együtt igazolt – csak a két képernyő közötti tényleges koppintás-bejárás nem. |
| `@Preview` **tényleges kirenderelése** (világos/sötét, mind a négy állapot) | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel. Ami igazolt: a nyolc előnézet-függvény (négy állapot × világos/sötét) lefordul. |
| „Már gyakoroltam" összegzés tesztje | **NEM ALKALMAZHATÓ** – a funkció maga nincs megírva, mert nincs `ProgressRepository` (T-301/T-401 hátra); lásd `docs/CONVENTIONS.md` 8. és az `IMPLEMENTATION_PLAN.md` 4.4 sora. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

---

## 2026-11-12 – G-01 csoport · `core/data`: `ProgressRepository` (T-301, T-401, T-402, T-502)

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **451 teszt, 0 hiba, 0 kihagyott** (441 → 451), minden task frissen futtatva (0 `UP-TO-DATE`) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (20 426 186 bájt) |

A futtatás Bash-ből történt (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`,
`./gradlew.bat …`). A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések
`tests` attribútumainak modulonkénti összegzése adta: `core/data` 109, `app` 4,
`core/quiz` 227, `core/model` 35, `core/ui` 28, `feature/library` 33, `navigation` 15 –
összesen 451, mindenhol 0 hiba/0 error.

### Új tesztek (10) – `RoomProgressRepositoryTest` (9) + `RoomTechniqueRepositoryTest` (1)

| Teszt | Mit bizonyít |
|---|---|
| `startSession_usesInjectedClockAndSeedNotSystemCalls_NFR07` | a `startedAt`, a `requestedCount` és a `seed` az injektált `Clock`/paraméterekből kerül a `quiz_session` sorba, nem rendszerhívásból |
| `startSession_differentSeedsProduceDifferentSessionIds_ADR007` | két menetindítás különböző seeddel különböző `SessionId`-t kap |
| `recordAttempt_firstAnswerIsRecordedAndIncrementsAnsweredCount_FR18` | az első válasz rögzül, az `answeredAt` az injektált `Clock`-ból jön, és a menet `answeredCount`-ja nő |
| `recordAttempt_secondAnswerToSameQuestionReturnsFalseAndDoesNotIncrement_TR04` | gyors dupla koppintás: a második hívás `false`, az első ítélet megmarad, a számláló nem nő duplán |
| `finishSession_unfinishedSessionKeepsNullFinishedAtUntilClosed_FR19` | a menet `finishedAt`-ja `null` marad `finishSession` hívásáig, majd az injektált `Clock` időbélyegére áll |
| `recordStudyReview_isStoredWithInjectedTimestamp_FR10` | az önértékelés `reviewedAt`-ja az injektált `Clock`-ból jön |
| `observeStudyReviewSummary_countsEachRatingSeparatelyAndNeverTouchesQuizData_FR28` | a „Tudtam"/„Gyakorolnom kell" darabszám helyesen bontott, és az önértékelés sosem ír a kvízmunkamenet-táblákba |
| `clearAllProgress_removesUserDataButKeepsContentUntouched_FR35` | a törlés után a felhasználói táblák üresek, a tartalom (`technique`, `category`) változatlan |
| `progressRepository_publicApiExposesNoRoomOrDatabaseTypes_NFR03` | a `ProgressRepository` publikus API-ja (visszatérési és paramétertípusok) nem hivatkozik Room entitásra/DAO-ra (TR-07, reflexiós teszt, a `TechniqueRepository` megfelelőjének mintájára) |
| `getQuizCandidates_fillsIllustrationDigestsForTechniquesSharingThePackagedFile_FR13` | két technika, amely ugyanarra a csomagolt fájlnévre hivatkozik, egyező ujjlenyomatot kap; eltérő fájlnév eltérőt, illusztráció nélküli technika nem kap bejegyzést |

### Két szándékos eltérés a T-402 „Teendők" szó szerinti szövegétől

1. **`quiz_excluded`/`INCOMPLETE` szűrés nincs a repositoryban.** A `QuizCandidateSet`
   dokumentált szerződése szerint a készlet nem dönt kvízképességről – azt a
   `QuizEligibility.isEligible` egyetlen kapuja végzi kérdéstípusonként (ADR-010). Egy
   második szűrés itt duplikálná ezt a szabályt. A `retired` kizárás marad (SQL-ben,
   T-201 óta): az tartalom-életciklus kérdés (ADR-004), nem kvízjogosultság.
2. **`illustrationDigests` fájlnév-alapú, nem SHA-256 hex.** A sémában nincs bájt-szintű
   hash-oszlop, és becsültet nem gyártunk (CLAUDE.md „Tartalmi integritás"). A fájlnév
   egyenlőség elegendő és korrekt: lásd `docs/CONVENTIONS.md` 1. szakasz új sora.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

---

## 2026-11-12 – G-04 csoport · `feature/learn`: `learn/setup` (T-302) és kártyaállapot (T-303)

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 20 új teszt (`LearnSetupViewModelTest` 13, `LearnCardsViewModelTest` 7), 0 hiba |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **471 teszt, 0 hiba, 0 kihagyott** (451 → 471), minden task frissen futtatva (0 `UP-TO-DATE`) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (20 429 638 bájt) |

A futtatás Bash-ből történt (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`,
`./gradlew.bat …`). A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések
`tests` attribútumainak modulonkénti összegzése adta, és minden modulban
`failures="0" errors="0"`.

### Új tesztek (20) – `LearnSetupViewModelTest` (13) + `LearnCardsViewModelTest` (7)

| Teszt | Mit bizonyít |
|---|---|
| `learnSetup_whileImportRunning_showsLoading_FR06` | gátolt import alatt `Loading`, utána `Content` (honest mérés, `Job` kapuval) |
| `learnSetup_importThrows_showsErrorAndRetryRecovers_FR06` | kivétel esetén `Error` `@StringRes`-sel (nem a kivétel angol szövege), `onRetry()` helyreállít |
| `learnSetup_categoryNamesAndCountsComeFromContent_FR06` | a kategórianév és a darabszám a tartalomból jön, a hitelesítési státusztól **függetlenül** (egy `AWAITING_REVIEW` technika is számít) |
| `learnSetup_retiredTechniqueIsNotCounted_FR06` | a visszavont technika nem növeli a darabszámot |
| `learnSetup_exposesDatasetTrustForUnreviewedBanner_FR06` | a `datasetTrust` a tartalom állapotából jön, nem felhasználói beállításból |
| `learnSetup_noCategorySelected_cannotStartWithReason_FR06` | kijelölés nélkül `canStart = false`, a „válassz kategóriát" indoklással |
| `learnSetup_selectingCategoryWithTechniquesEnablesStart_FR06` | kategória kijelölésével a gomb aktiválódik, indoklás nélkül |
| `learnSetup_toggleCategoryTwiceClearsSelection_FR06` | ismételt kapcsolás visszaállítja a kijelölésmentes állapotot |
| `learnSetup_multipleCategoriesSelectedTogether_FR06` | több kategória egyszerre kijelölhető (FR-06) |
| `learnSetup_categoryWithoutDisplayableTechniques_cannotStartWithOtherReason_FR06` | technika nélküli kategória kijelölésekor a **másik** indoklás jelenik meg (nincs megjeleníthető technika, nem a „válassz kategóriát") |
| `learnSetup_defaultHideModeIsHideJapanese_FR07` | az alapértelmezett elrejtési mód a japán név |
| `learnSetup_selectingHideModeUpdatesState_FR07` | az elrejtési mód módosítható |
| `learnSetup_savedStateHandle_restoresSelectionAndHideModeAfterRecreate_FR06` | ugyanazzal a `SavedStateHandle`-lel újraépített ViewModel visszaadja a kijelölést és az elrejtési módot (forgatás/mentett állapot) |
| `learnCards_seedComesFromInjectedRandom_notSystemDefault_NFR07` | a sorrend pontosan a befecskendezett `Random`-ból sorsolt seedből adódik (két független `Random(1)` példány azonos eredményt ad) |
| `learnCards_sameSeed_producesIdenticalOrderAfterRecreate_FR09` | ugyanaz a `SavedStateHandle` (már tartalmazza a mentett seedet) → azonos sorrend, akkor is, ha az újraépített ViewModel MÁSIK `Random`-ot kap |
| `learnCards_positionSurvivesRecreateWithSameSavedStateHandle_FR09` | a pozíció túléli az újraépülést (forgatás/mentett állapot) |
| `learnCards_nextAndPrevious_respectBounds_FR09` | az elején „Előző", a végén „Következő" no-op, a `hasPrevious`/`hasNext` helyesen jelez |
| `learnCards_revealIsPerCard_notGlobal_FR09` | a felfedettség kártyánkénti: a következő kártya alapállapotban rejtett, a visszalépett korábbi kártya felfedve marad |
| `learnCards_revealedStateSurvivesRecreateWithSameSavedStateHandle_FR09` | a felfedettség is túléli az újraépülést |
| `learnCards_selectionWithoutTechniques_showsEmpty_FR09` | technika nélküli kategóriaválasztás `Empty` állapotot ad, nem omlik össze (NFR-06) |

### Szándékos eltérés: a `navigation` modul nincs megváltoztatva

A `learn/setup` képernyő **nincs** bekötve a `KyokushinNavHost`-ba (a placeholder marad),
és a `learn/cards` útvonalminta nem kapott argumentumot a kijelölt kategóriák/elrejtési
mód átadásához. A `LearnSetupScreen` a „Kártyák indítása" gombon már kiadja a szükséges
adatot lambdaként (`onStartCards: (Set<CategoryId>, HideMode) -> Unit`), és a
`LearnCardsViewModel` már kész elfogadni ugyanezt a bemenetet a `SavedStateHandle`
`categoryIds`/`hideMode` kulcsain (a `LibraryDetailViewModel.techniqueId` mintájára) –
de a tényleges route-bekötés (argumentumalak, `KyokushinNavHost` módosítása) a T-304
feladat, amikor a `learn/cards` célképernyő UI-ja is elkészül, és eldönthető, pontosan
milyen formában kell átadni az adatot. Lásd `docs/CONVENTIONS.md` 1. és 4.1 szakasz az
új sorokért.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt (tényleges renderelés/koppintás a `learn/setup` képernyőn, TalkBack bejárás) | **NEM FUTOTT** – B-03. Ami igazolt: a nyolc `@Preview` lefordul. |
| `@Preview` **tényleges kirenderelése** (világos/sötét, mind a négy ág) | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel. Ami igazolt: a nyolc előnézet-függvény lefordul. |
| A `learn/cards` Compose UI tesztje | **NEM ALKALMAZHATÓ** – a T-303 nem ad UI-t, csak ViewModelt (ugyanaz a kettéválasztás, mint T-207/T-208 között); a T-304 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

## 2026-11-12 – G-05 csoport · `feature/learn`: kártya UI (T-304), feliratos kép elrejtése (T-305), záróképernyő (T-307)

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 11 új teszt (`LearnCardPoliciesTest` 8, `LearnCardsViewModelTest` nettó +3: 7 → 10), 0 hiba |
| `./gradlew.bat :navigation:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 2 új teszt (`NavigationGraphTest`), 0 hiba |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **484 teszt, 0 hiba, 0 kihagyott** (471 → 484), minden task frissen futtatva (0 `UP-TO-DATE`) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

A futtatás Bash-ből történt (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`,
`./gradlew.bat …`). A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések
`tests` attribútumainak modulonkénti összegzése adta, és minden modulban
`failures="0" errors="0"`.

### Mi készült

A `learn/cards` Compose UI (`LearnCardsScreen.kt`): felső sáv (pozíció/összes, az
**aktuális kártya saját** kategórianeve, bezárás), a kártya (ábra, a nem elrejtett név,
„Koppints a felfedéshez" felfedésig, felfedés után mind a három elem), alsó Előző/Következő
sáv, üres-kijelölés védelmi háló és a záróképernyő (T-307). A `LearnCardsViewModel`
(T-303) kibővült: `repository.observeCategories()` a kártya saját kategórianevéhez,
`LearnCardsUiState.Finished` ág (a `lastIndex` utáni pozíció **nem** szorítódik vissza az
utolsó kártyára, hanem a záróképernyőt adja), és `onRestart()` (pozíció 0-ra, felfedettség
törölve, **azonos** seed/sorrend). A `navigation` modul bekötötte a `learn/setup`-ot és a
`learn/cards`-ot (eddig helyőrzők voltak): a `learn/cards` route két lekérdezőparamétert
kapott (`categoryIds`, `hideMode`) a `LearnCardsViewModel` saját kulcsneveivel, amiket a
Navigation Compose automatikusan a cél `SavedStateHandle`-jébe tesz.

### Új tesztek (13) – `LearnCardPoliciesTest` (8) + `LearnCardsViewModelTest` nettó (+3) + `NavigationGraphTest` (2)

| Teszt | Mit bizonyít |
|---|---|
| `imagePolicy_captionedAssetNotYetRevealed_mustHide_T305` | feliratos kép (`hideableOnStudyCard == false`), felfedés előtt: rejtendő |
| `imagePolicy_captionedAssetRevealed_noLongerHidden_T305` | ugyanaz, felfedés után: már nem rejtendő |
| `imagePolicy_plainAsset_neverHidden_T305` | normál kép sosem rejtendő, felfedés előtt sem |
| `imagePolicy_placeholderOrMissingIllustration_neverHidden_T305` | `Placeholder`/`null` esetén nincs mit elrejteni |
| `illustrationLabelPolicy_beforeReveal_isDecorative_notTheTechniqueName_FR07` | felfedés előtt az ábra leírása **dekoratív**, nem a technika neve (a TalkBack nem árulja el a rejtett nevet) |
| `illustrationLabelPolicy_afterReveal_namesTheTechnique_FR07` | felfedés után a leírás a teljes nevet adja |
| `stateDescription_beforeReveal_isHidden_T304` | a **tényleges** `strings.xml` fölött: felfedés előtt a `stateDescription` „elrejtve” |
| `stateDescription_afterReveal_isRevealed_T304` | ugyanaz felfedés után: „felfedve” |
| `learnCards_previousAtStart_isNoOp_FR09` | (a korábbi `…respectBounds…` teszt első fele, önálló tesztként) az „Előző” az elején no-op |
| `learnCards_nextAfterLastCard_showsFinishedScreen_FR06` | az utolsó kártya utáni „Következő” a záróképernyőre visz, **nem** marad az utolsó kártyán (szándékos viselkedésváltás a T-303-hoz képest) |
| `learnCards_restartAfterFinished_returnsToFirstCardWithoutReveal_FR06` | „Újrakezdés” a záróképernyőről az első, felfedetlen kártyára visz |
| `learnCards_cardCarriesItsOwnCategoryName_FR06` | a kártya a saját technikájának kategórianevét hordozza, nem a kijelölés egészét |
| `learnCards_routeArgumentNames_matchTheViewModelKeys_NFR03` | a `learn/cards` route argumentumnevei a `LearnCardsViewModel` kulcsaival egyeznek |
| `routeBuilders_learnCards_fillsCategoryIdsAndHideMode_NFR03` | a `KyokushinRoutes.learnCards(...)` helyesen tölti ki a route mintát |

### Szándékos, dokumentált eltérés: a „Kvíz indítása” nem köti be tényleges adatátvétellel a `quiz/setup`-ot

A `learn/cards` záróképernyőjének „Kvíz indítása ezekkel a kategóriákkal" gombja a
`LearnCardsRoute`-ból kifelé **átadja** a kiválasztott kategóriákat
(`onStartQuiz: (Set<CategoryId>) -> Unit`) – ez a T-307 „Kész ha” pontja a modul saját
határán. A `quiz/setup` (T-403/T-404) viszont még nem létezik, ezért a `navigation` modul
jelenleg csak átnavigál a `quiz/setup` helyőrzőre, a kategóriahalmazt **nem** továbbítja –
ugyanaz a döntés, mint a T-302/T-303 párnál a `learn/cards` argumentumaival
(`docs/CONVENTIONS.md` 4.1 szakasz). Amikor a T-403/T-404 elkészül, a tényleges
átvétel (route-argumentum vagy `SavedStateHandle` előretöltés) azzal a feladattal
köthető be.

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt (tényleges renderelés/koppintás a `learn/cards` képernyőn, TalkBack bejárás) | **NEM FUTOTT** – B-03. Ami igazolt: mind a 12 `@Preview` (6 állapot × világos/sötét) lefordul, és a Compose-mentes döntési szabályok (`LearnCardPoliciesTest`) valódi `strings.xml` fölött futnak. |
| `@Preview` **tényleges kirenderelése** | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

## 2026-11-12 – G-06 csoport · `feature/learn`: önértékelés (T-306), ViewModel tesztek (T-308)

**Környezet:** Windows 11 Pro, Bash (Git Bash),
`JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`, Gradle wrapper 9.9.1.

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 17s` – `feature/learn` **36 teszt, 0 hiba** (5 új `LearnCardsViewModelTest` eset; korábban 10 → 15 ebben az osztályban) |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 34s`, 277 actionable task (277 executed) – **489 teszt, 0 hiba, 0 kihagyott** (484 → 489) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 3s` – `app/build/outputs/apk/debug/app-debug.apk` |

A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések `tests` attribútumainak
modulonkénti összegzése adta (`core/model` 35, `core/quiz` 227, `core/data` 109,
`core/ui` 28, `feature/learn` 36, `feature/library` 33, `navigation` 17, `app` 4 = 489),
minden modulban `failures="0" errors="0"`.

### Mi készült

**T-306:** a `LearnCardsViewModel` egy `progressRepository: ProgressRepository`
konstruktorparaméterrel bővült (a Hilt gráf már ismeri – `RepositoryModule`), és három
új metódust kapott: `onSelfRate(rating)` (ment egy `StudyReview`-t a jelenlegi kártya
technikájára, majd hívja a meglévő `onNext()`-et – az utolsó kártyán ez is a
záróképernyőre vezet), valamint a rövidítései, `onKnew()`/`onNeedsPractice()`. A mentés
időbélyege **nem** itt, hanem a `RoomProgressRepository`-ban keletkezik az injektált
`Clock`-ból (NFR-07) – a ViewModel ezért szándékosan nem kapott saját `Clock`-ot
(CONVENTIONS 4.1: „Nem injektálunk, amire nincs szükség"). Védelmi háló (ugyanaz a minta,
mint az egyszeri kvízválasznál, ADR-005): az `onSelfRate` a jelenlegi `uiState.value`
felfedettségét is ellenőrzi, és no-op egy fel nem fedett kártyán.

A `LearnCardsScreen.kt` egy új `LearnCardSelfRatingRow`-t kapott: „Gyakorolnom kell” /
„Tudtam” gombpár, `enabled = card.revealed`, mindkettő ≥48 dp. Letiltott állapotban egy
mindig-látható magyarázó szöveg jelenik meg alattuk (`learn_cards_self_rate_disabled_reason`)
– ugyanaz a minta, mint a `learn/setup` letiltott „Kártyák indítása” gombjánál –, hogy a
letiltás oka a TalkBack normál bejárási sorrendjében is kimondva legyen (T-306 „Kész ha”).

**T-308:** a tanulási logika négy tétele közül három már a T-303/T-305 tesztkészletében
lefedve volt (seedelt sorrend determinizmusa, felfedési állapot megőrzése léptetés/
visszalépés után, feliratos kép elrejtése felfedésig) – ezekhez nem kellett új teszt. Az
„Önértékelés rögzítése és az automatikus továbblépés” tétel az, amit a T-306 most
implementált; ehhez 5 új teszt került a `LearnCardsViewModelTest`-be, egy új
`FakeLearnProgressRepository` tesztdublőrrel (`LearnTestFixtures.kt`, a kvízmenethez
tartozó tagjai `UnsupportedOperationException`-t dobnak, hogy egy véletlen felhasználásuk
azonnal kiderüljön).

### Új tesztek (5) – `LearnCardsViewModelTest`

| Teszt | Mit bizonyít |
|---|---|
| `learnCards_onKnew_whenRevealed_recordsStudyReviewAndAdvances_FR10` | felfedett kártyán a „Tudtam” gomb `StudyReview(KNEW)`-t ment és a következő kártyára lép |
| `learnCards_onNeedsPractice_whenRevealed_recordsStudyReviewAndAdvances_FR10` | ugyanaz a „Gyakorolnom kell” gombra, `NEEDS_PRACTICE` besorolással |
| `learnCards_onKnew_whenNotRevealed_isNoOp_FR10` | fel nem fedett kártyán az önértékelés nem ír rekordot és nem lép tovább (védelmi háló, ADR-005 mintája) |
| `learnCards_onNext_withoutSelfRating_doesNotRecordStudyReview_FR10` | a sima „Következő” (nem az önértékelés gombjai) nem ír `StudyReview`-t – az önértékelés nélküli továbblépés megengedett |
| `learnCards_onKnew_onLastCard_advancesToFinishedScreen_FR10` | az utolsó kártyán is menti az önértékelést, és a záróképernyőre lép, nem akad el |

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI teszt (tényleges koppintás a „Tudtam”/„Gyakorolnom kell” gombon, TalkBack bejárás) | **NEM FUTOTT** – B-03. Ami igazolt: a meglévő `@Preview`-k (felfedés előtt/után) a két gombbal együtt lefordulnak, és a letiltási indoklás szövege a felfedés előtti állapotban megjelenik. |
| `@Preview` **tényleges kirenderelése** | **NEM FUTOTT** – az előnézet-renderelő Android Studiót igényel. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

## 2026-11-12 – T-403 · `feature/quiz`: beállító ViewModel (Fázis 6 kezdete)

**Környezet:** Windows 11 Pro, Bash (Git Bash),
`JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`, Gradle wrapper 9.9.1.

**Előzmény:** egy korábbi subagent megírta a `QuizSetupViewModel`/`QuizSetupUiState`/
`QuizStartResult`/`QuizTestFixtures` kódot és a `feature/quiz/build.gradle.kts`
kiegészítését, de a Claude API heti rate limitje miatt félbeszakadt a
`QuizSetupViewModelTest.kt` írása közben, **commit előtt**. Ez a kör nem kezdte újra: a
meglévő kódot a `core/quiz`/`core/data` tényleges szerződésével (`QuizGenerator`,
`QuizReadiness`, `QuizRequest`, `QuizCandidateSet`, `QuizCandidateFilter`,
`QuizEligibility`, `TechniqueRepository`, `ProgressRepository`, `ContentImporter`)
összevetve ellenőrizte, befejezte a tesztfájlt, és csak az így talált két hibát javította.

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 14s` – `feature/quiz` **16 teszt, 0 hiba** (`QuizSetupViewModelTest`) |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 31s`, 294 actionable task (294 executed) – **505 teszt, 0 hiba, 0 kihagyott** (489 → 505) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 3s` – `app/build/outputs/apk/debug/app-debug.apk` |

A tesztszámot a `*/build/test-results/**/TEST-*.xml` jelentések `tests` attribútumainak
modulonkénti összegzése adta (`core/model` 35, `core/quiz` 227, `core/data` 109,
`core/ui` 28, `feature/learn` 36, `feature/library` 33, `feature/quiz` 16, `navigation`
17, `app` 4 = 505), minden modulban `failures="0" errors="0"`.

### Mi készült

A `QuizSetupViewModel` (`feature/quiz/.../setup/QuizSetupViewModel.kt`) egy
`candidateSetFlow`-ban tartja életben a **minden kategóriára szóló** jelöltkészletet
(`repository.observeTechniques().map { repository.getQuizCandidates() }`), és a
kategóriaszűrést a `QuizRequest.categoryFilter`-en keresztül memóriában végzi – így a
`suspend getQuizCandidates()` nem fut újra minden jelölőnégyzet-kattintásra. **Minden**
kijelölésváltásra (`combine(selectedCategoriesFlow, requestedCountFlow, selectedTypesFlow,
candidateSetFlow, observeContentState())`) a `buildState` egy valódi
`DefaultQuizGenerator.dryRun(liveRequest)`-et futtat; a `QuizSetupUiState.Content`
`expectedQuestionCount`/`shortfall`/`canStart` mezői **közvetlenül** ennek a kimenete,
nincs második, a motortól eltérő számítás (T-403 „Figyelj rá"). A típusonkénti letiltás
oka (FR-13, `QuizQuestionTypeUiModel.disabledReasonRes`) egy **próba**-kéréssel készül,
amelyben minden típus engedélyezett (`liveRequest.copy(enabledTypes =
QuestionType.entries.toSet())`), hogy a felhasználó saját kapcsolóállása ne tudja
elfedni a tartalmi okot (`QuizCandidateFilter.skipReasons`). Az indítás (`onStartQuiz`)
az injektált `Random`-ból (a `di/QuizModule.kt` már T-108 óta biztosítja) sorsol egy
seedet, megnyitja a munkamenetet (`ProgressRepository.startSession`), legenerálja a
tervet (`QuizGenerator.generate`) **ugyanazzal** a seeddel, és egy `Channel`/`Flow`
egyszeri eseményen (`startEvent`) adja ki a `QuizStartResult`-ot – a tényleges navigáció
és a `QuizRunState` a T-404/T-405 tárgya.

### A megszakított kör munkájában talált és javított két hiba

1. **Kitalált `strings.xml` szöveg a FUNCTIONAL_SPEC 8. fejezete ellenében.** A korábbi
   kör a letiltott „Kvíz indítása" gomb alá **három külön** bejegyzést
   (`quiz_setup_suggestion_more_categories`/`_more_types`/`_fewer_questions`) és egy
   `startDisabledSuggestions: List<Int>` mezőt tett be, de a FUNCTIONAL_SPEC 8. a
   `quiz.notEnoughData` kulcs alatt **egyetlen, szó szerinti** mondatot ad, amely
   mindhárom javaslatot egy mondatban hordozza. A CONVENTIONS 4. szabálya szerint ez a
   kötelező forrás, nem szabad újat kitalálni, ahol a mesterforrás már pontos szöveget
   ad. Javítva: `QuizSetupUiState.Content.notEnoughDataMessageRes: Int?` (egyetlen,
   nullázható `@StringRes`), `strings.xml`-ben egyetlen
   `quiz_setup_message_not_enough_data` bejegyzés a szó szerinti szöveggel, a három
   kitalált bejegyzés törölve.
2. **Bukó teszt, hibás elvárással.** A `quizSetup_toggleCategory_narrowsLiveRequest_FR11`
   azt várta, hogy az „állások" kategóriára szűkítve (a fixtúra 3 technikájával) a
   `dryRun` 3 kérdést ad. Valójában **0**-t adott: egy négyopciós kérdéshez (1 helyes +
   3 distraktor) legalább **4** egymástól különböző, kvízképes technika kell egy
   típuson belül (`QuizQuestion.OPTION_COUNT`, `DistractorSelector`), és a
   `QuizCandidateFilter.optionPool` a disztraktorok forrását **is** a kategóriaszűrőn
   belülre korlátozza – 3 technikával tehát egyetlen kérdés sem áll össze, ami pontosan
   a motor dokumentált, nem-optimista viselkedése (T-403 „Figyelj rá"). A teszt
   elvárása volt hibás, nem a kód (CONVENTIONS 3.): a fixtúrát bővítettem egy negyedik,
   kvízképes „állások" technikával, hogy a szűkítés tényleges, nullától eltérő hatása
   bizonyítható legyen.

### Új tesztek (16) – `QuizSetupViewModelTest`

| Teszt | Mit bizonyít |
|---|---|
| `quizSetup_whileImportRunning_showsLoading_FR11` | import alatt `Loading` |
| `quizSetup_importThrows_showsErrorAndRetryRecovers_FR11` | import-kivétel → `Error`, `onRetry()` helyreállít |
| `quizSetup_defaultState_emptySelectionAllTypesTenQuestions_FR11` | alapállapot: üres kijelölés (= minden kategória), 10 kérdés, minden típus kijelölve |
| `quizSetup_categoryCounts_quizEligibleCountExcludesIncomplete_totalCountDoesNot_FR11` | a kategóriánkénti „kvízképes / összes" szám: `INCOMPLETE` csak az előbbit csökkenti |
| `quizSetup_retiredTechniqueIsNotCounted_FR11` | visszavont technika sem a kvízképes, sem a teljes számba nem számít |
| `quizSetup_noImages_disablesBothImageTypesWithReason_FR13` | kép nélkül a (C)/(D) típus letiltva, tartalmi indoklással; (A)/(B) nem |
| `quizSetup_enoughDistinctImages_enablesImageTypes_FR13` | 5 egyedi, kvízhasználható képpel a (D) típus engedélyezett |
| `quizSetup_expectedQuestionCount_isNotEmptyWithDefaultContent_FR22` | alapállapotban `expectedQuestionCount = 5`, `canStart = true` |
| `quizSetup_toggleCategory_narrowsLiveRequest_FR11` | kategóriakijelölés tényleges, nullától eltérő hatással szűkíti a `dryRun`-t |
| `quizSetup_allTypesDisabled_cannotStartWithNotEnoughDataMessage_FR22` | minden típus kikapcsolva → `canStart = false` + a FUNCTIONAL_SPEC szó szerinti `quiz.notEnoughData` üzenete |
| `quizSetup_canStart_hasNoNotEnoughDataMessage_FR22` | `canStart = true` esetén nincs indoklás |
| `quizSetup_selectRequestedCount_onlyAllowsFiveTenTwenty_FR11` | csak 5/10/20 engedett, egyéb érték kivételt dob |
| `quizSetup_toggleType_updatesSelection_FR12` | kérdéstípus ki/bekapcsolása |
| `quizSetup_savedStateHandle_restoresSelectionAfterRecreate_FR11` | a kijelölés (kategória, kérdésszám, típus) túléli a `SavedStateHandle` újraépítését |
| `quizSetup_onStartQuiz_drawsSeedFromInjectedRandom_startsSessionAndEmitsPlan_FR19` | seed az injektált `Random`-ból, `startSession` meghívva, a kiadott terv kérdésszáma egyezik a beállító képernyő ígéretével (T-403 „Kész ha" 2.) |
| `quizSetup_onStartQuiz_noopWhenCannotStart_FR22` | `canStart = false` esetén az indítás no-op (védelmi háló, ADR-005 mintája) |

### Rétegszabály

A `QuizSetupViewModel` kizárólag `core:model`, `core:data` (repository interfészek) és
`core:quiz` (generátor, jelöltszűrés, jogosultsági kapu) típusokat lát – nincs benne
Room entitás, DAO vagy DTO (ADR-001 L3/L4). A `Random` konstruktorparaméter, a kötés a
`di/QuizModule.kt`-ban (T-108 óta); nincs `Random.Default` vagy `Clock`-hívás az üzleti
kódban (NFR-07).

### NEM FUTOTT

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| Compose UI / `@Preview` | **NEM ALKALMAZHATÓ** – a T-403 nem ad UI-t, csak ViewModelt (ugyanaz a kettéválasztás, mint T-207/T-208 és T-302/T-303 között); a UI a T-404 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |

---

## 2026-11-12 – T-404 (`feature/quiz`: beállító UI)

| Parancs | Exit | Kimenet |
|---|---|---|
| `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` + `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `QuizSetupMessagesTest` (4 teszt, 0 hiba) a meglévő `QuizSetupViewModelTest` (16 teszt) mellett |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **509 teszt, 0 hiba** (505 → 509) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` |

### Mi készült

`QuizSetupScreen.kt` (`feature/quiz/.../setup/`): stateless `QuizSetupScreen` +
`QuizSetupRoute` (a `QuizSetupViewModel`-t látó egyetlen composable), ugyanazzal a
mintával, mint a `LearnSetupScreen`/`LearnSetupRoute`. Kategórialista jelölőnégyzettel
(„`{kvízképes}` kvízképes / `{összes}` technika", FUNCTIONAL_SPEC 5.1/2.), szegmentált
kérdésszám-választó (`SingleChoiceSegmentedButtonRow`/`SegmentedButton`, Material3
1.4.0, 5/10/20, FR-11), négy kérdéstípus jelölőnégyzete (FR-12) letiltott sor alatt
mindig látható, konkrét darabszámos indoklással (FR-13, T-404 „Kész ha" 1. pontja: nem
általános szöveg), és az élő készenléti összegzés + a 0 kérdéses eset javaslata a
„Kvíz indítása" gomb felett (FR-22). A gomb eredménye (`QuizSetupViewModel.startEvent`)
a `QuizSetupRoute`-ból kifelé adott `onStartQuiz: (QuizStartResult) -> Unit` lambda – a
`navigation` modul bekötése **szándékosan nincs** ebben a körben (lásd
`docs/CONVENTIONS.md` 5. szakasz új sora: a `quiz/run` még `PlaceholderScreen`, nem
fogadna `QuizPlan`-t, és a `learn/cards → quiz/setup` kategória-átadás is hátravan).

Két `@Composable`-mentes segédfüggvény (`QuizSetupMessages.kt`), hogy a szöveg-döntés a
tényleges `strings.xml` fölött, emulátor nélkül bizonyítható legyen (ugyanaz a minta,
mint a `LibraryListItemAccessibility`/`UnreviewedNotice`): `questionTypeLabelRes`
(a négy kérdéstípus felirata, szó szerint a FUNCTIONAL_SPEC 8. `quiz.type.*`
kulcsaiból) és `quizReadinessSummary` (a „ennyi kérdés áll össze" / hiány-szöveg
döntése, szó szerint a FUNCTIONAL_SPEC 5.1/5. mondatából és a `quiz.shortfall`
kulcsból).

### Új tesztek (4) – `QuizSetupMessagesTest` (Robolectric, a tényleges `strings.xml` fölött)

| Teszt | Mit bizonyít |
|---|---|
| `quizReadinessSummary_withoutShortfall_announcesExpectedCount_FR11` | shortfall nélkül a „A beállításokkal `{n}` kérdés állítható össze." szó szerinti szöveg |
| `quizReadinessSummary_withShortfall_announcesRequestedAndActualCount_FR22` | shortfall esetén a `quiz.shortfall` szó szerinti szövege, helyes paraméter-sorrenddel (kért, majd tényleges) |
| `quizReadinessSummary_zeroQuestions_stillAnnouncesTheCount_FR22` | 0 kérdés esetén is a számot közlő szöveg jön (a „0" eset nem keverendő a `Shortfall`-lal) |
| `questionTypeLabelRes_returnsTheFunctionalSpecWordingForEachType_FR12` | mind a négy kérdéstípus felirata szó szerint egyezik a FUNCTIONAL_SPEC 8.-cal |

### Rétegszabály, akadálymentesség

A `QuizSetupScreen.kt` kizárólag `core:model`, `core:ui` (`ErrorState`, `UnreviewedBanner`,
`KyokushinTrainerTheme`) és `core:quiz` (`Shortfall`/`ShortfallReason`, csak az
előnézetekhez) típusokat lát – nincs Room entitás, DAO vagy DTO (ADR-001 L3/L4), nincs
közvetlen `Clock`/`Random` hívás (NFR-07, a UI réteg egyáltalán nem is lát ilyet). Minden
felhasználói szöveg `strings.xml`-ben (`feature/quiz/src/main/res/values/strings.xml`),
magyarul. A kategória- és kérdéstípus-sorok `Modifier.toggleable(role = Role.Checkbox)`-ot
kapnak a teljes sorra (nem csak a `Checkbox`-ra), a gomb és minden sor `heightIn(min = 48.dp)`.
`@Preview` világos + sötét témára, három `Content`-variánsra (indítható, shortfall, 0
kérdés letiltva) + `Loading`/`Error`.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `navigation` modul bekötése (`QUIZ_SETUP` composable, `learn/cards → quiz/setup` kategória-átadás) | **NEM KÉSZÜLT EL** – szándékosan, lásd a „Mi készült" szakasz és `docs/CONVENTIONS.md` 5. szakasz új sora. Nem T-404 „Teendők" tárgya; a `quiz/run` oldali bekötés a T-405 utáni, önálló lépés. |

---

## 2026-11-12 – G-07 csoport (T-405, T-406, T-410: a futó kvíz állapotgépe)

| Parancs | Exit | Kimenet |
|---|---|---|
| `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` + `./gradlew.bat :core:quiz:test --rerun-tasks` | 0 | `BUILD SUCCESSFUL` |
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `QuizRunViewModelTest` (14 teszt) + a meglévő `QuizSetupMessagesTest` (4) és `QuizSetupViewModelTest` (16), 0 hiba |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **528 teszt, 0 hiba** (509 → 528) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` |

### Mi készült

**`core/quiz`:** `QuizRunStateCodec` (`generator` csomag) – `encodePlan`/`decodePlan`/
`encodeAnswers`/`decodeAnswers`, a `QuizPlan` és a `Map<QuestionId, TechniqueId>` JSON
kódolása/visszafejtése. Ez az **első** hely, ahol a modul `main` forráskészlete tényleges
`Json`-t használ (a `QuizPlan`/`QuizQuestion` a T-107 óta `@Serializable`, de eddig nem
volt kódoló/dekódoló függvény) – a `feature/quiz` ugyanis a `build.gradle.kts`-ében
kifejezetten tiltja a `kotlinx.serialization` típusokat (ADR-001, L3), ezért a ViewModel
csak ezt a négy, `String`-et be/kiadó függvényt hívja.

**`feature/quiz/run` (új csomag):**
- `QuizRunState` – `sessionId, seed, questions, currentIndex, answers, shortfall`
  (ARCHITECTURE 6.2 alakja), plain Kotlin `internal data class` (nem `@Serializable`).
- `QuizRunUiState` – `Loading`/`Empty`/`Content` (pozíció, összes, megválaszoltszám,
  az aktuális kérdésre adott válasz, utolsó kérdés-e, megerősítő párbeszég látható-e).
- `QuizRunViewModel` – `onAnswer` (T-406: ViewModel-szintű `answers`-map ellenőrzés mint
  első, `recordAttempt` `false` válasza mint második védelmi vonal; azonosító-alapú
  helyesség), `onNext` (T-410: csak előre, az utolsó kérdésen no-op – a normál befejezés
  a G-09/T-411 tárgya), `onRequestClose`/`onCancelClose`/`onConfirmClose` (T-410: a
  megerősítő párbeszég állapota; a megszakítás **nem** hív `finishSession`-t, a
  `finishedAt` a FUNCTIONAL_SPEC 5.2 szerint `null` marad).

**`feature/quiz/setup`:** a `QuizStartResult` egy `seed: Long` mezővel bővült (a
`QuizSetupViewModel.onStartQuiz` már sorsolta, csak eddig nem adta tovább) – ez kell a
`QuizRunState.seed`-hez. A meglévő `QuizSetupViewModelTest` egyik tesztje egy
`assertThat(result.seed)...` sorral bővült, kódmódosítás más helyen nem történt.

### Új tesztek (19)

**`QuizRunStateCodecTest` (`core/quiz`, 5 teszt, sima JUnit):**

| Teszt | Mit bizonyít |
|---|---|
| `encodePlan_thenDecode_restoresIdenticalPlan_FR19` | a teljes `QuizPlan` (kérdések, `skippedTypes`, `shortfall`) bitre visszaáll |
| `encodePlan_thenDecode_preservesOptionOrder_FR19` | az opciósorrend is a kódolt állapot része (ADR-005/006) |
| `encodeAnswers_thenDecode_restoresIdenticalMap_FR18` | a `Map<QuestionId, TechniqueId>` bitre visszaáll |
| `encodeAnswers_emptyMap_roundTrips_FR19` | az üres válasz-map is érvényes kör |
| `decodePlan_rejectsManipulatedEmptyIdentifier` | egy manipulált, üres azonosítót hordozó mentett állapot kivételt dob, nem jut be a domainbe |

**`QuizRunViewModelTest` (`feature/quiz`, 14 teszt, Robolectric `@Config(sdk = [34])`):**

| Teszt | Mit bizonyít |
|---|---|
| `quizRun_withValidPlan_showsFirstQuestion_FR16` | az első kérdés és a kezdő progresszállapot helyes |
| `quizRun_missingInput_showsEmpty_NFR06` | hiányzó bemenetre `Empty`, nem omlik össze, nem ragad `Loading`-ban |
| `quizRun_afterRecreateWithSameSavedStateHandle_sameQuestionSameOptionOrder_FR19` | forgatás után ugyanaz a kérdés, ugyanaz az opciósorrend (T-405 „Kész ha") |
| `quizRun_answersSurviveRecreateWithSameSavedStateHandle_FR19` | a rögzített válasz is túléli az újraépülést |
| `quizRun_onAnswer_correctness_isIdentifierBased_notTextual_FR18` | a helyesség `selected == correctTechniqueId`, nem szöveg |
| `quizRun_onAnswer_wrongOption_recordsIncorrectById_FR18` | hibás válasz azonosító alapon `correct = false`-ot rögzít |
| `quizRun_onAnswer_rapidDoubleTapOnSameQuestion_recordsOnlyFirstAttempt_FR18` | gyors dupla koppintásra egyetlen próbálkozás, az első válasz marad érvényben |
| `quizRun_onAnswer_whenRepositoryRejects_doesNotChangeState_FR18` | a `recordAttempt` `false` válasza nem módosítja az állapotot (T-406 „Teendők" 2.) |
| `quizRun_onAnswer_unknownQuestionId_isNoOp` | ismeretlen kérdésazonosítóra no-op, nincs rögzítés |
| `quizRun_onNext_advancesPosition_FR16` | a „Következő" előre lép |
| `quizRun_onNext_atLastQuestion_isNoOp` | az utolsó kérdésen túl nem megy (T-410 „nincs visszalépés", a befejezés T-411 tárgya) |
| `quizRun_onRequestClose_showsConfirmation_FUNCTIONAL_SPEC_5_2` | a bezárás megnyitja a megerősítő párbeszéget |
| `quizRun_onCancelClose_hidesConfirmationAndStaysOnQuiz` | a „Folytatás" becsukja a párbeszéget, a kvíz folytatódik |
| `quizRun_onConfirmClose_keepsRecordedAnswersAndEmitsCloseEvent_FR19` | a megszakítás megtartja a rögzített válaszokat, **nem** hív `finishSession`-t, és egyszeri eseményt küld |

### Rétegszabály, determinizmus

A `feature/quiz/run` csomag kizárólag `core:model` és `core:quiz` (generátor) típusokat
lát, a `ProgressRepository` interfészen keresztül ír – nincs Room entitás, DAO, DTO vagy
`kotlinx.serialization` típus a `feature/quiz`-ben (ADR-001, L3 – a tiltást a modul saját
`build.gradle.kts` fejkommentje is kimondja). Nincs közvetlen `Clock`/`Random` hívás: az
időbélyeget a `ProgressRepository` adja (NFR-07), a seed már a bemenet része (nem itt
sorsolt). Minden felhasználói szöveg: **nincs új szöveg** ebben a körben, mert ez a
csoport nem ad Compose UI-t (lásd alább).

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| Compose UI / `@Preview` / `contentDescription` / ≥48 dp | **NEM ALKALMAZHATÓ** – a G-07 csoport (T-405/T-406/T-410) szándékosan kizárólag az állapotgépet adja, Compose UI nélkül (ugyanaz a kettéválasztás, mint T-303 → T-304 a `feature/learn`-ben). A kérdés-UI a T-407/T-408, a visszajelzés a T-409, a progresszsáv/`AlertDialog`/`BackHandler` tényleges Compose rajzolása egy még hátralévő UI-feladat tárgya. |
| `navigation` modul bekötése (`QuizStartResult` eljuttatása a `QuizRunViewModel.SavedStateHandle`-jébe) | **NEM KÉSZÜLT EL** – szándékosan, lásd a „Mi készült" szakasz és `docs/CONVENTIONS.md` 5. szakasz új sora: a bemenet nem route-argumentumként, hanem a cél `NavBackStackEntry.savedStateHandle`-jébe írva jut majd el, amikor a navigációs bekötés elkészül. |

---

## 2026-11-12 – G-08 csoport kész (T-407, T-409, `docs/backlog/GROUPS.md`, `feature/quiz`, Fázis 6.4 – az (A)/(B) kérdés UI és a válasz-visszajelzés)

**Környezet:** Windows 11 Pro, Bash (Git Bash),
`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`, Gradle
wrapper 9.9.1 (`./gradlew.bat`).

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:quiz:compileDebugKotlin` | 0 | `BUILD SUCCESSFUL in 14s` – csak a már ismert, nem blokkoló `hiltViewModel()` deprecation figyelmeztetés (CONVENTIONS 4.1). |
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 24s` – a `feature/quiz` modul 41 tesztje, 0 hiba. |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 44s`, 294 actionable task (294 executed). **Teljes projekt: 535 teszt, 0 hiba** (528 → 535). |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 6s`, 300 actionable task; `app/build/outputs/apk/debug/app-debug.apk` elkészült. |

A 535 az összes `build/test-results/**/TEST-*.xml` `tests` attribútumának összege (nem
becslés); a `feature/quiz` modulon belüli bontás a JUnit XML-ekből:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…feature.quiz.run.QuizOptionFeedbackResolverTest` (új) | **5** | 0 |
| `…feature.quiz.run.QuizRunViewModelTest` (14 → **16**) | 16 | 0 |
| `…feature.quiz.setup.QuizSetupMessagesTest` | 4 | 0 |
| `…feature.quiz.setup.QuizSetupViewModelTest` | 16 | 0 |

### Mi készült

Ez a csoport (G-08) a `docs/backlog/GROUPS.md` szerint egy menetben készült, mert a két
feladat **ugyanazt** a képernyőt adja (`QuizRunScreen.kt`): a szöveges (A)/(B) kérdés UI
(T-407) és a rá adott válasz-visszajelzés (T-409).

**`QuizRunViewModel` kiegészítése (a G-07 állapotgépe fölött, nem helyette):** a G-07
csoport (T-405/T-406/T-410, lásd fentebb) szándékosan nem oldotta fel a
`QuizQuestion.optionTechniqueIds` azonosítóit megjelenítendő adatra – ezt a saját KDoc-ja
kifejezetten a kérdés-UI-t adó feladatra (ez a csoport) hagyta. A feloldás a
ViewModelben készült el, nem a Compose rétegben (hogy az L5 rétegszabály ne sérüljön): a
`QuizRunViewModel` egy harmadik konstruktorparamétert kapott (`TechniqueRepository`), az
`uiState`-et építő `combine` egy harmadik forrással bővült
(`techniqueRepository.observeTechniques()`), és a `QuizRunUiState.Content` egy új
`options: List<QuizOptionDisplay>` mezőt kapott – a `question.optionTechniqueIds`
**ugyanabban a sorrendben** (ADR-007, a seedelt keverés sorrendje, nem rendezve újra)
feloldott romaji/magyar/ábra hármasa. Ha egy opció technikája nem oldható fel (védelmi
háló, NFR-06), az eredmény `QuizRunUiState.Empty`, nem kivétel és nem hiányos opciólista.

**`QuizRunScreen.kt` (új, `feature/quiz/run`):** `QuizRunRoute` (Hilt ViewModel +
`closeEvent` figyelése) + stateless `QuizRunScreen`. Felépítés:
- `QuizRunTopBar` – „`{index}` / `{összes}`" szöveg + bezárás (×) `IconButton` + lineáris
  `LinearProgressIndicator` (FUNCTIONAL_SPEC 5.2/1.).
- `QuizTextQuestion` – (A)/(B) kiemelt kérdéstárgy (prefix + félkövér érték, két
  `Text`-ben, lásd `docs/CONVENTIONS.md` 4.1 új sora) + a négy `QuizOptionRow`. A (C)/(D)
  típus (`IMAGE_TO_JP`/`JP_TO_IMAGE`) ága szándékosan `Unit` – a T-408 tárgya.
- `QuizOptionRow` – a válasz-visszajelzés (T-409): `Modifier.selectable` (`selected`
  szemantika, FUNCTIONAL_SPEC 9.) + feltételes `Modifier.semantics { stateDescription }`;
  az ikon (pipa/kereszt) + szöveg („Helyes"/„Helytelen") + szín hármasa minden kiemelt
  opción (a kiválasztott ÉS a mindig kiemelt helyes opción is – lásd lent).
- `QuizExplanationCard` – a helyes technika romaji + magyar + ábra hármasa, kattintható
  (`onOpenTechnique(techniqueId)` kifelé adott lambda, L5 – a Tudástár részletezőjére
  navigálás a hívó dolga, US-14/3. kritérium).
- A „Következő"/„Eredmény" gomb (a válasz megadása után jelenik meg; a tényleges
  befejezés a G-09/T-411 tárgya, itt az `onNext()` az utolsó kérdésen dokumentáltan no-op).
- `BackHandler` + `AlertDialog` a megszakítás megerősítésére (FUNCTIONAL_SPEC 5.2/5.) – a
  `QuizRunViewModel` KDoc-ja (T-410 lezárása) ezt kifejezetten ide, a kérdés-UI-t adó
  feladatra utalta.

**Új, `@Composable`-mentes, sima JUnit-tal tesztelt döntési logika**
(`QuizOptionFeedback.kt`, `run` csomag): `QuizOptionFeedback` enum
(`UNANSWERED`/`SELECTED_CORRECT`/`SELECTED_INCORRECT`/`CORRECT_UNSELECTED`/`NEUTRAL`) +
`QuizOptionFeedbackResolver.resolve(optionId, correctTechniqueId, selectedTechniqueId)` –
Robolectric/Compose-renderelés nélkül bizonyítja, melyik opció melyik ágat kapja.

**Függőségek (`feature/quiz/build.gradle.kts`):** két új, már a version catalogban
szereplő, de a modulban korábban nem használt tétel: `libs.androidx.activity.compose`
(`BackHandler` – a T-410 lezárása már előre jelezte, hogy ez lesz az első
előfordulása a modulban) és `libs.compose.material.icons.extended` (pipa/kereszt/nyíl
ikon, ugyanaz a katalógusbejegyzés, mint a `navigation` modulban).

**`strings.xml` (`feature/quiz`):** 19 új bejegyzés a „Futó kvíz" szakaszban – a
progresszszöveg, a prompt prefix/érték (A/B), a „Helyes"/„Helytelen" felirat (FUNCTIONAL_
SPEC 8. `quiz.answer.correct`/`wrong` szó szerint), a **külön**, kisbetűs
`stateDescription` szövegek (FUNCTIONAL_SPEC 9.), a magyarázó kártya felirata, a
„Következő"/„Eredmény" gomb, a megszakítás-megerősítés (FUNCTIONAL_SPEC 8.
`quiz.abort.title`/`message` szó szerint + az 5.2 leíró szövegéből a „Megszakítás"/
„Folytatás" gombfelirat), és a védelmi háló (`Empty`) üzenete.

### Új tesztek (7)

**`QuizOptionFeedbackResolverTest` (`feature/quiz`, 5 teszt, sima JUnit, Robolectric nélkül):**

| Teszt | Mit bizonyít |
|---|---|
| `resolve_beforeAnswer_everyOptionIsUnanswered_FR17` | válasz előtt minden opció `UNANSWERED` |
| `resolve_selectedTheCorrectOption_isSelectedCorrect_FR17` | a kiválasztott helyes opció `SELECTED_CORRECT` |
| `resolve_selectedAWrongOption_thatOptionIsSelectedIncorrect_FR17` | a kiválasztott hibás opció `SELECTED_INCORRECT` |
| `resolve_wrongSelected_correctOptionIsStillHighlighted_NFR04` | hibás válasz mellett is a helyes opció `CORRECT_UNSELECTED` (mindig kiemelve) |
| `resolve_wrongSelected_otherUnselectedWrongOptionsAreNeutral_FR17` | a harmadik, nem kiválasztott hibás opció `NEUTRAL` |

**`QuizRunViewModelTest` bővítése (`feature/quiz`, 2 új teszt, Robolectric `@Config(sdk = [34])`):**

| Teszt | Mit bizonyít |
|---|---|
| `quizRun_content_resolvesOptionDisplayFromTechniqueRepository_FR16` | az opciók romaji/magyar neve a `TechniqueRepository`-ból jön, **ugyanabban a sorrendben**, mint a `QuizQuestion.optionTechniqueIds` |
| `quizRun_content_missingOptionTechnique_showsEmpty_NFR06` | ha egy opció technikája nem oldható fel, az eredmény `Empty`, nem egy hiányos `Content` |

### Rétegszabály, akadálymentesség

A `feature/quiz` továbbra is kizárólag `core:model`/`core:ui`/`core:quiz`/`core:data`
(repository-interfész) típusokat lát – a `QuizRunScreen.kt` sosem hivatkozik Room
entitásra/DAO-ra/DTO-ra, a `TechniqueRepository`-t is csak a ViewModel látja (ADR-001,
L3/L4). Nincs közvetlen `Clock`/`Random` hívás. Akadálymentesség: minden kiemelt opció
ikon+szöveg+szín hármasával jelez (nem csak színnel, NFR-04), a `selectable` adja a
`selected` szemantikát, a `stateDescription` a FUNCTIONAL_SPEC 9. szerinti kisbetűs
„helyes"/„helytelen", minden gomb/opció ≥48 dp (`heightIn(min = …)`), `@Preview` világos
+ sötét témára nyolc állapotra (betöltés, üres, (A) válasz előtt/helyes/helytelen, (B)
válasz előtt, megszakítás-megerősítés, hosszú magyar név).

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `quiz/setup` → `quiz/run` navigációs bekötés (`KyokushinNavHost`) | **NEM KÉSZÜLT EL** – szándékosan, lásd `docs/CONVENTIONS.md` 5. szakasz; ez nem ennek a csoportnak (G-08) a tárgya, és a T-408 sem bővíti. |
| Kvíz **normál** befejezése (eredményképernyőre lépés, `finishSession`) | **NEM KÉSZÜLT EL** – szándékosan, a G-09 csoport (T-411) tárgya; az `onNext()` az utolsó kérdésen dokumentáltan no-op marad. |

---

## 2026-11-12 – T-408 kész (`feature/quiz`, Fázis 6.2 – a (C)/(D) kérdéstípus UI-ja)

**Környezet:** Windows 11 Pro, Bash (Git Bash),
`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`, Gradle
wrapper 9.9.1 (`./gradlew.bat`).

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :core:ui:testDebugUnitTest :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 22s` – mindkét modul zöld. |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 37s`, 294 actionable task (294 executed). **Teljes projekt: 542 teszt, 0 hiba** (535 → 542). |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 4s`, 300 actionable task; `app/build/outputs/apk/debug/app-debug.apk` elkészült. |

A 542 az összes `build/test-results/**/TEST-*.xml` `tests` attribútumának összege (nem
becslés, a teljes modulfa feletti futásból). A két érintett modul bontása:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…core.ui.illustration.IllustrationContentDescriptionTest` (6 → **9**) | 9 | 0 |
| `…feature.quiz.run.QuizImageOptionGridTest` (új) | **4** | 0 |

### Mi készült

A G-08 csoport (T-407/T-409) lezárásakor a `QuizRunScreen.kt`-ban a `IMAGE_TO_JP`/
`JP_TO_IMAGE` kérdéstípus ága dokumentáltan üres `Unit` volt – ezt ez a feladat tölti ki.
A `docs/backlog/GROUPS.md` szerint T-408 szándékosan **önálló** feladat (nem csoporttag):
„a képes kérdéstípusok akadálymentessége önálló gondolkodást kíván".

**`QuizRunScreen.kt` (bővítve, `feature/quiz/run`):**
- `QuizImageToJpQuestion` – (C) típus: „Melyik technika ez?" + a HELYES opció
  illusztrációja kérdésként, 4 `QuizOptionRow` romaji felirattal (a már meglévő
  (A)/(B) opciógomb és `QuizOptionFeedbackResolver` újrahasznosítva).
- `QuizJpToImageQuestion` + `QuizImageOptionCard` – (D) típus: „Melyik ábra:
  **`{romaji}`**?" + 2×2 képrács (`Column` két `Row`-ja, `Modifier.weight(1f)`, nem
  `LazyVerticalGrid` – 4 fix elemhez felesleges függőség). Minden kártya a már meglévő
  visszajelzés-logikát (ikon+szöveg+szín+`stateDescription`) hívja, a felirat helyén
  képpel.
- `QuizImageOptionGrid` (új, `@Composable`-mentes `object`): a 2×2 rács pozíció-
  hozzárendelése (1-alapú, az `options` lista EREDETI sorrendjéből – nem rendezi újra).

**`core/ui` – `IllustrationLabel.QuizPrompt` (új sealed ág, bővítve a T-204-ben lezárt
típuson):** a (C) kérdésábra a HELYES opció illusztrációja – ha a leírása a technika
nevét mondaná ki, a TalkBack idő előtt elárulná a választ (NFR-04). A meglévő két ág
(`Technique` – nevet mond, `QuizOption` – pozíciót mond) közül sem illett ide, és
`Decorative` a dokumentáció szerint kvízben nem használható (a kép itt információt
hordoz). Az új, pozíció nélküli `QuizPrompt` ág egy harmadik, semleges szöveget ad („A
kvízkérdés ábrája", `illustration_a11y_quiz_prompt`) – a (D) rács változatlanul a
T-204-ben már elkészült `IllustrationLabel.QuizOption(position)`-t használja.

**`strings.xml`:** `feature/quiz` – 2 új bejegyzés (`quiz_run_prompt_image_to_jp`,
`quiz_run_prompt_jp_to_image_prefix`; a `quiz_run_prompt_value` újrahasznosítva a (D)
kiemelt romaji értékéhez). `core/ui` – 1 új bejegyzés
(`illustration_a11y_quiz_prompt`).

### Új tesztek (7)

**`IllustrationContentDescriptionTest` bővítése (`core/ui`, 3 új teszt, Robolectric
`@Config(sdk = [34])`, a tényleges `strings.xml` fölött):**

| Teszt | Mit bizonyít |
|---|---|
| `assetDescription_forQuizPrompt_omitsTechniqueName_NFR04` | a (C) kérdésábra leírása nem tartalmazza a technika nevét |
| `assetDescription_forQuizPrompt_differsFromQuizOptionDescription_NFR04` | a kérdésábra leírása nem keverhető össze a (D) pozíció alapú opcióleírásával |
| `placeholderDescription_forQuizPrompt_omitsTechniqueNameAndMatchesAsset_NFR04` | a helyőrző-ág is névmentes, és megegyezik a kép-ággal |

**`QuizImageOptionGridTest` (`feature/quiz`, 4 új teszt, sima JUnit, Robolectric
nélkül):**

| Teszt | Mit bizonyít |
|---|---|
| `rows_fourOptionsTwoColumns_splitsIntoTwoRowsOfTwo_FR12` | 4 opció, 2 oszlop → pontosan 2×2 |
| `rows_assignsSequentialOneBasedPositions_inOriginalOrder_NFR04` | a pozíció 1-alapú és az `options` lista EREDETI sorrendjét követi |
| `rows_notDivisibleByColumns_lastRowIsShorter_FR12` | nem kerek elemszámnál az utolsó sor rövidebb |
| `rows_rejectsNonPositiveColumnCount_FR12` | 0 vagy negatív oszlopszám `require()`-rel elutasított |

### Rétegszabály, akadálymentesség

A `feature/quiz` továbbra is kizárólag `core:model`/`core:ui`/`core:quiz`/`core:data`
(repository-interfész) típusokat lát, nincs új függőség. Nincs közvetlen `Clock`/
`Random` hívás. A (D) minden képopciója ≥48 dp (`heightIn(min = …)`), a (C) kérdésábra
leírása és a (D) opciók leírása egyaránt névmentes (NFR-04); 8 új `@Preview` pár
(világos/sötét: (C) válasz előtt/helyes, (D) válasz előtt/helyes/helytelen) + 1 pár
`widthDp = 400`-zal a (D) rácshoz (T-408 „Kész ha": „Keskeny képernyőn (≈400 dp) is
használható a rács").

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `quiz/setup` → `quiz/run` navigációs bekötés (`KyokushinNavHost`) | **NEM KÉSZÜLT EL** – szándékosan, lásd `docs/CONVENTIONS.md` 5. szakasz; nem ennek a feladatnak a tárgya. |
| Kvíz **normál** befejezése (eredményképernyőre lépés, `finishSession`) | **NEM KÉSZÜLT EL** – szándékosan, a G-09 csoport (T-411) tárgya. |

## 2026-11-12 – G-09 csoport kész (T-411, T-412, T-415 · `feature/quiz`, Fázis 6.4 – eredményképernyő, hibák újragyakorlása, lektorálatlanság jelzése)

**Környezet:** Windows 11 Pro, Bash (Git Bash),
`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`, Gradle
wrapper 9.9.1 (`./gradlew.bat`).

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :core:data:compileDebugKotlin :feature:quiz:compileDebugKotlin` | 0 | `BUILD SUCCESSFUL in 7s` – a főkód fordul (a `hiltViewModel()` deprecation figyelmeztetés dokumentált, nem blokkoló). |
| `./gradlew.bat :core:data:compileDebugUnitTestKotlin :feature:learn:compileDebugUnitTestKotlin :feature:quiz:compileDebugUnitTestKotlin` | 0 | `BUILD SUCCESSFUL in 3s` (az első futás egy hiányzó `SessionId` importot jelzett a `RoomProgressRepositoryTest`-ben – javítva, a második futás zöld). |
| `./gradlew.bat :core:data:testDebugUnitTest :feature:learn:testDebugUnitTest :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 24s`, 94 actionable task (11 executed, 83 up-to-date a nem érintett `core/*` modulokon) – a három érintett modul mind zöld. |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 35s`, **294 actionable task, mind a 294 végrehajtva** (teljes, őszinte projektfutás). |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 4s`, 300 actionable task; `app/build/outputs/apk/debug/app-debug.apk` elkészült. |

A `build/test-results/**/TEST-*.xml` `tests` attribútumainak összege a teljes projekten
**560** (nem becslés). Az érintett új/bővült osztályok:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…core.data.repository.RoomProgressRepositoryTest` (9 → **12**) | 12 | 0 |
| `…feature.quiz.run.QuizRunViewModelTest` (15 → **17**) | 17 | 0 |
| `…feature.quiz.result.QuizResultViewModelTest` (új) | **9** | 0 |
| `…feature.quiz.result.QuizResultMessagesTest` (új) | **5** | 0 |

### Mi készült

**T-411 – `quiz/result` eredményképernyő.** `QuizResultViewModel` (új, `feature/quiz/result`):
egyszeri `ProgressRepository.getSessionResult(sessionId)` visszaolvasás, `QuizScoring.score`
(T-113) a sikerarányhoz, a hibás próbálkozások feloldása `TechniqueRepository.getTechnique`-tel
(a **visszavont** technikát is visszaadja, DR-07 – egy később visszavont technikára mutató
régi eredmény is helyesen megjelenik). `QuizResultUiState` (`Loading`/`NotFound`/`Content`),
`QuizResultScreen` (Compose): nagy összegzés (`quiz_result_score_fraction` + külön
`quiz_result_score_percent` – NFR-04 „nem csak diagramon, szövegesen is kiolvasható"),
hibás kérdések listája (típus, helyes technika romaji+magyar+ábra, saját válasz,
kattintható `Surface` → `onOpenTechnique`), három gomb.
A „Nagy összegzés" három kölcsönösen kizáró ága (`Scored`/`NoAnswers`/`Aborted`) egy
`@Composable`-mentes `quizResultSummary` függvényben él (`QuizResultMessages.kt`,
ugyanaz a minta, mint a `QuizSetupMessages.kt`) – sima JUnittal tesztelve, Compose/
Robolectric nélkül.

**A kvíz normál befejezése (`QuizRunViewModel`, a G-07 óta nyitva hagyott rész):**
`onNext()` az utolsó kérdésen immár `progressRepository.finishSession(sessionId)`-t hív és
egy új `finishEvent: Flow<SessionId>`-et ad ki; a `QuizRunRoute` egy `onFinish` lambdával
bővült. A `FakeQuizRunProgressRepository.finishSession` a G-07-ben szándékosan kivételt
dobott (annak bizonyítására, hogy ott senki nem hívja) – itt már **rögzíti** a hívásokat.

**Új `ProgressRepository` tag (`getSessionResult`, `core/data`):** `QuizSessionResult`
(`session` + `attempts`) – **egyetlen, már ismert menet** visszaolvasása, nem a T-501
aggregált statisztikája (lásd a `ProgressRepository.kt` KDoc-ja a kettő elhatárolásáról).
`RoomProgressRepository` a `QuizSessionDao.getById` + `QuizAttemptDao.getBySession`
(mindkettő már létező DAO-metódus) eredményét fordítja domain típusra egy új,
`ContentEntityMappers.kt` mintájú `UserEntityMappers.kt`-ban (`QuizSessionEntity.toQuizSession()`,
`QuizAttemptEntity.toQuizAttempt()`) – ez volt az első olvasó tag, amelynek entitás→domain
fordításra volt szüksége (eddig a repository csak írt/törölt, lásd `docs/CONVENTIONS.md`
1. szakasz).

**T-412 – „Hibák újragyakorlása".** A kvízmotor `QuizRequest`-je csak kategória szerint
szűr, nincs technikaszintű allow-list. A `QuizResultViewModel.onRetryWrong()` ezért egy
**kizárólag** a hibás próbálkozások helyes technikáit tartalmazó `QuizCandidateSet`-et
épít (társ-technika hozzáadása **nélkül**) – így a motor correct-answer poolja ÉS a
distraktorpoolja is garantáltan csak ebből a halmazból áll össze, tehát az új munkamenet
**kizárólag** a hibás technikákat kérdezi (US-16 AC-1: „ugyanazokra a technikákra").
Ennek ára a motor már dokumentált, nem-optimista szabálya (`docs/CONVENTIONS.md` 1.
szakasz: „legalább 4 technika kell egy négyopciós kérdéshez"): 4-nél kevesebb **egyedi**
hibás technikával egyetlen kérdés sem állna össze – egy előzetes `dryRun()` (ugyanaz a
minta, mint a `quiz/setup`-nál) ezért megakadályozza egy üres munkamenet megnyitását. A
régi munkamenet érintetlen marad (nincs rá írás az `onRetryWrong`-ban), az új próbálkozások
önálló, jogos rekordok.

**T-415 – a lektorálatlanság jelzése.** A `quiz/setup` figyelmeztető sora **már a
T-404-ben elkészült** (`UnreviewedBanner(trust = state.datasetTrust)` a `QuizSetupScreen`-ben)
– ez a feladat csak az eredményképernyőn adta hozzá, ugyanazzal a komponenssel (ADR-010):
nem elrejthető, és `DatasetTrust.REVIEWED` mellett automatikusan eltűnik, kódmódosítás
nélkül.

### Rétegszabály, akadálymentesség

A `feature/quiz/result` kizárólag `core:model`/`core:ui`/`core:quiz`/`core:data`
(repository-interfész) típusokat lát, Room/DAO/entitás sosem szivárog a UI-ig. Nincs
közvetlen `Clock`/`Random` hívás (a `Random` az `onRetryWrong` seedsorsolásához injektált,
ugyanaz a kötés, mint a `QuizSetupViewModel`-nél). Minden gomb és a hibás tétel sora
`heightIn(min = 48.dp)`; 12 új `@Preview` (6 állapot × világos/sötét: betöltés, nem
található, hibás kérdésekkel, hibátlan, megszakított, 0 válasz).

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `quiz/setup` ↔ `quiz/run` ↔ `quiz/result` navigációs bekötés (`KyokushinNavHost`) | **NEM KÉSZÜLT EL** – szándékosan, lásd `docs/CONVENTIONS.md` 5. szakasz; nem ennek a feladatnak (G-09, `feature/quiz`-re korlátozva) a tárgya. |
| T-413 (akadálymentesség), T-414 (teljes ViewModel-teszt-lefedettség) | **NEM EBBEN A KÖRBEN** – önálló, nyitott taskok, nem ennek a csoportnak a tárgya. |

## 2026-11-12 – T-413 kész (`feature/quiz`: akadálymentesség)

**Környezet:** Windows 11 Pro, Bash (Git Bash),
`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`, Gradle
wrapper 9.9.1 (`./gradlew.bat`).

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 39s`, 294 actionable task, mind végrehajtva (teljes, őszinte projektfutás) – lefutott egyszer a módosítás ELŐTT (baseline, zöld), majd újra UTÁNA. |
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 22s`, 76 actionable task – a módosított modul önállóan is zöld. |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, 300 actionable task; `app/build/outputs/apk/debug/app-debug.apk`. |

A `build/test-results/**/TEST-*.xml` (`testDebugUnitTest` ÉS a JVM modulok `test`
feladata együtt) `tests` attribútumainak összege a teljes projekten **566** (560 → 566,
+6 az új `QuizOptionAccessibilityMappingTest`-tel), **0 hiba, 0 error**. Az érintett
osztály:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…feature.quiz.run.QuizOptionAccessibilityMappingTest` (új) | **6** | 0 |

### Mi készült

A feladat kiindulásakor a `feature/quiz` mindhárom képernyője a Teendők nagy részét
**már tartalmazta** a T-407/T-408/T-409/T-411/T-412 saját UI-munkájából (opciók
`selected`/`stateDescription`, (D) pozíció alapú képleírás – `IllustrationLabel.
QuizOption`, `IllustrationContentDescriptionTest` –, a kérdés→opciók→visszajelzés→
Következő bejárási sorrend, 48 dp célpontok a legtöbb gombon). Ezt a feladat
kódolvasással, egyenként igazolta.

**Talált és javított valódi hiány:** a `QuizCloseConfirmationDialog`
(`QuizRunScreen.kt`) `AlertDialog`-jának „Mégsem"/„Megszakítás" `TextButton`-jai a
Material 3 alapértelmezett (~40 dp) magasságukat kapták, nem a projekt 48 dp-es
szabályát (NFR-04) – egy szerkezetileg azonos `TextButton` a `QuizResultScreen`-ben már
korábban helyesen `heightIn(min = 48.dp)` volt. Mindkét dialógusgomb explicit
`Modifier.heightIn(min = MIN_TOUCH_TARGET)`-et kapott.

**Új teszt-lefedettség:** a válasz-visszajelzés ikon/szöveg/`stateDescription`
leképező függvényei (`quizOptionIcon`/`quizOptionFeedbackTextRes`/
`quizOptionStateDescriptionRes`, `QuizRunScreen.kt`) `private`-ból `internal`-ra
nyíltak (ugyanaz a minta, mint a `QuizOptionFeedbackResolver`/`QuizImageOptionGrid`-nél,
T-408/T-409) – egy új `QuizOptionAccessibilityMappingTest` (6 teszt) emulátor nélkül
bizonyítja, hogy válasz előtt (`UNANSWERED`) és egy nem kiválasztott helytelen opciónál
(`NEUTRAL`) nincs ikon/szöveg/`stateDescription`, míg a helyes/helytelen ágak a
megfelelő, egymástól különböző string-erőforrást kapják.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| TR-06: TalkBackkel, tényleges eszközön/emulátorral végigvitt kvíz | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya; az itt dokumentált kódelemzés a helyettesítő bizonyíték. |
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – ugyanaz, B-03. A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `quiz/setup` ↔ `quiz/run` ↔ `quiz/result` navigációs bekötés (`KyokushinNavHost`) | **NEM KÉSZÜLT EL** – szándékosan, lásd `docs/CONVENTIONS.md` 5. szakasz; nem ennek a feladatnak a tárgya. |

## 2026-11-12 – T-414 kész (`feature/quiz`: ViewModel tesztek)

**Környezet:** Windows 11 Pro, Bash (Git Bash),
`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`, Gradle
wrapper 9.9.1 (`./gradlew.bat`).

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 1m 30s`, 294 actionable task, mind végrehajtva (teljes, őszinte projektfutás). |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, 300 actionable task (up-to-date – a tesztforrás-módosítás nem érinti az `app`/`assembleDebug` fő forráskészletét); `app/build/outputs/apk/debug/app-debug.apk` megvan. |

A `build/test-results/**/TEST-*.xml` (`testDebugUnitTest` ÉS a JVM modulok `test`
feladata együtt) `tests` attribútumainak összege a teljes projekten **569** (566 → 569,
+3), **0 hiba, 0 error**. Az érintett osztályok:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…feature.quiz.run.QuizRunViewModelTest` (17 → **19**) | 19 | 0 |
| `…feature.quiz.result.QuizResultViewModelTest` (9 → **10**) | 10 | 0 |

### Mi készült

A feladatot egy korábbi kör a Claude API session rate limitje miatt kódolás közben
félbehagyta, COMMIT ELŐTT – a munkafában a két tesztfájl és az
`IMPLEMENTATION_PLAN.md` módosítása már nem-commitolt állapotban várt. Ez a kör nem
kezdte újra: `git diff`-fel felmérte, hogy a T-414 „Teendők" listájának 1./2. pontja
(dupla válasz, `SavedStateHandle` visszaállítás) és a 3. pont zéró-válaszos ága már a
G-07/G-09 csoportokban (T-405/T-406/T-410/T-411/T-412) lefedett volt, és a
`QuizRunViewModelTest`/`QuizResultViewModelTest` meglévő eseteit **nem** duplikálta.

Négy, tényleg hiányzó eset készült el:

- **`QuizRunViewModelTest`** (+2, FR-22): egy nulla kérdéses, teljes shortfallú
  (`ShortfallReason.ALL_TYPES_SKIPPED`) `QuizPlan` a `QuizRunUiState.Empty`-t adja (a
  `buildUiState` a `questions` ürességét vizsgálja, nem a bemenet puszta jelenlétét);
  egy **nem teljes** shortfallú terv (kért 5, tényleges 2) a `Content.total`-ban a
  TÉNYLEGES kérdésszámot mutatja, nem a kértet.
- **`QuizResultViewModelTest`** (+1, FR-19, „félbehagyott teszt"): megszakításkor a
  **megmaradt** válaszokból (3 a kért 10-ből) a pontszám a tényleg megválaszolt 3-ból
  számol (`ratio = 2/3`), a hiányzó 7-et nem bünteti 0%-ként – elkülönítve a már
  meglévő, zéró-válaszos megszakítás-tesztől.
- **`QuizResultViewModelTest.quizResult_onRetryWrong_…`** (meglévő teszt +2 erősebb
  állítás, US-16 AC-1/AC-2): az újragyakorlás eseménye **új**, a megtekintettől
  különböző `sessionId`-t ad (nem a régi menet folytatása), és a `onRetryWrong()`
  hívása után a régi munkamenet `getSessionResult`-ja **változatlan** marad (nincs
  kettős könyvelés).

A `docs/IMPLEMENTATION_PLAN.md` fázis 6.6 sorának és a „Következő lépés" szakaszának
a félbehagyott kör által elkezdett szerkesztését ez a kör átnézte: a tartalom
konzisztens és a tényleges végeredménnyel egyező volt (19/10/5 teszteset, helyes
„T-413 és T-414 is 2026-11-12-én kész" megfogalmazás) – változtatás nélkül megtartva.

### Rétegszabály, akadálymentesség

Csak tesztfájl-módosítás; nincs új UI, string- vagy rétegszabály-érintettség. A
meglévő fake repository-k és injektált `Clock`/`Random` dublőrök változatlanok –
nincs közvetlen `Clock`/`Random` hívás.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `quiz/setup` ↔ `quiz/run` ↔ `quiz/result` navigációs bekötés (`KyokushinNavHost`) | **NEM KÉSZÜLT EL** – szándékosan, lásd `docs/CONVENTIONS.md` 5. szakasz; nem ennek a feladatnak a tárgya. |

---

## 2026-11-13 – T-501 kész: `core/data` aggregált statisztika `@Query`-k

| Parancs | Exit | Eredmény |
|---|---|---|
| `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` + `./gradlew.bat :core:data:testDebugUnitTest --tests "hu.kyokushin.trainer.core.data.repository.RoomProgressRepositoryTest" --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 17s`, 41 actionable task. A `TEST-...RoomProgressRepositoryTest.xml`: `tests="21" skipped="0" failures="0" errors="0"` (12 meglévő + 9 új). |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 36s`, 294 actionable task (teljes, őszinte projektfutás, minden modul). Minden `build/test-results/**/*.xml` `failures="0" errors="0"`. |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 6s`, 300 actionable task (11 végrehajtva, 289 up-to-date); `app/build/outputs/apk/debug/app-debug.apk` megvan. |

A teljes projekt `tests` attribútumainak összege (minden modul, a kanonikus
`testDebugUnitTest`/JVM-modul `test` eredménykönyvtárából, dupla számolás nélkül):
**569 → 578** (+9), **0 hiba, 0 error**. Az érintett osztály:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…core.data.repository.RoomProgressRepositoryTest` (12 → **21**) | 21 | 0 |

### Mi készült

A `ProgressRepository` négy új, **olvasó**, aggregált SQL `@Query`-re épülő taggal
bővült (FR-23…FR-27, DR-09): `observeOverallProgress`, `observeCategoryProgress`,
`observeTechniqueProgress(id)`, `observeNeedsPractice(limit)`. Részletek:
`docs/IMPLEMENTATION_PLAN.md` Fázis 3.7 sora és a 2026-11-13-i changelog bejegyzés.

Kilenc új teszt a `RoomProgressRepositoryTest`-ben, in-memory Room adatbázison:

- „nincs adat" (nem 0%) mindkét granularitáson (összesített, egyetlen technika) –
  FR-27.
- a kategóriabontás **minden** kategóriát ad, a próbálkozásmentest is – FR-24.
- a kategóriabontás helyesen összegez **két** technikát egy kategórián belül
  (`GROUP BY categoryId` bizonyítása) – FR-24.
- a technikánkénti bontás kérdéstípusonként **elkülönül**, és egy másik technika
  próbálkozása nem szivárog bele – FR-25.
- a „gyakorlást igénylő" lista **kihagyja** a sosem gyakorolt technikát – FR-26.
- a lista rendezése sikerarány növekvő, majd azonos aránynál a régebben gyakorolt
  elöl – FR-26.
- a `limit` paraméter tényleg korlátoz – FR-26.

A bővült interfész miatt a négy meglévő `ProgressRepository` tesztdublőr
(`FakeLearnProgressRepository`, `FakeQuizProgressRepository`,
`FakeQuizRunProgressRepository`, `FakeQuizResultProgressRepository`,
`feature/learn`/`feature/quiz` teszt source setek) mind kapott négy új, kivételt dobó
felülírást – ezek nélkül a build `ABSTRACT_MEMBER_NOT_IMPLEMENTED` hibával elhasalt
volna (ez a köztes állapot egyszer tényleg elő is fordult, és a teljes
`testDebugUnitTest` futás fogta meg).

### Rétegszabály, NFR-07, NFR-10

A `progressRepository_publicApiExposesNoRoomOrDatabaseTypes_NFR03` teszt (már
meglévő, reflexiós) a négy új metódus `Flow<OverallProgress>`/
`Flow<List<CategoryProgress>>`/`Flow<TechniqueProgress>`/`Flow<List<TechniqueProgress>>`
visszatérési típusán is lefutott, és nem talált Room/DAO/Entity szivárgást. Nincs
közvetlen `Clock`/`Random` hívás az új kódban (nincs is rá szükség: a próbálkozások
időbélyege már tárolt adat). Nincs N+1 lekérdezés: a „gyakorlást igénylő" lista
kérdéstípus-bontása egyetlen `IN (:techniqueIds)` lekérdezésből jön, nem
technikánkénti egyenkénti hívásból.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| UI (`contentDescription`, 48 dp célpont, `@Preview`) | **NEM ALKALMAZHATÓ** – tisztán `core/data` adatréteg-feladat, nincs UI-t érintő változás. |

---

## 2026-11-13 – T-503 kész: `feature/progress` áttekintő képernyő

| Parancs | Exit | Eredmény |
|---|---|---|
| `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` + `./gradlew.bat :feature:progress:testDebugUnitTest :navigation:test --console=plain` | 0 | `BUILD SUCCESSFUL in 8s`, 105 actionable task. `ProgressMessagesTest`: `tests="4"`; `ProgressOverviewViewModelTest`: `tests="5"` – mindkettő `failures="0" errors="0"`. |
| `./gradlew.bat testDebugUnitTest --rerun-tasks --console=plain` | 0 | `BUILD SUCCESSFUL in 39s`, 302 actionable task (teljes, őszinte projektfutás, minden modul). Minden `build/test-results/**/*.xml` `failures="0" errors="0"`. |
| `./gradlew.bat assembleDebug --console=plain` | 0 | `BUILD SUCCESSFUL in 2s`, 304 actionable task (16 végrehajtva, 288 up-to-date); `app/build/outputs/apk/debug/app-debug.apk` megvan. |

A teljes projekt `tests` attribútumainak összege (minden modul, a kanonikus
`testDebugUnitTest`/JVM-modul `test` eredménykönyvtárából, dupla számolás nélkül):
**578 → 587** (+9), **0 hiba, 0 error**. Az érintett osztályok (mindkettő új):

| Osztály | Teszt | Hiba |
|---|---|---|
| `…feature.progress.overview.ProgressMessagesTest` | 4 | 0 |
| `…feature.progress.overview.ProgressOverviewViewModelTest` | 5 | 0 |

### Mi készült

A Haladás áttekintő képernyője (FUNCTIONAL_SPEC 6.1, FR-23, FR-24, FR-27, US-17) – a
`docs/backlog/GROUPS.md` G-10 csoportja szerint szándékosan csak a **keret**: összesítő
kártya + kategóriánkénti bontás. A „Gyakorlást igénylő" lista (T-504) és az
önértékelési blokk (T-505) hiányzik, azokat a következő két feladat adja hozzá.

- `ProgressOverviewViewModel` a `ProgressRepository.observeOverallProgress()` +
  `observeCategoryProgress()` és a `TechniqueRepository.observeCategories()` +
  `observeContentState()` négyes `combine`-ja; a kategórianevet a
  `TechniqueRepository`, a pontszámot a `ProgressRepository` adja, `CategoryId`
  szerint párosítva.
- `progressScoreDisplay(score)` (`ProgressMessages.kt`) a „0 megválaszolt kérdés →
  nincs adat, nem 0%" (FR-27) elágazás egyetlen, `@Composable`-mentes megfogalmazása.
- `ProgressOverviewScreen`: lektorálatlanság-szalag (ADR-010 – a „Haladás" sor eddig
  nem volt bekötve), összesítő kártya (nulla próbálkozásnál a kártya helyén „Még nincs
  adat. Indíts egy kvízt!" + gomb, a `progress.noData` szöveg szó szerint a
  FUNCTIONAL_SPEC 8.-ból), kategóriánkénti bontás sávval és szöveges értékkel,
  „Beállítások" link.
- Navigációs bekötés: a `KyokushinNavHost` `progress/overview` ága mostantól
  `ProgressOverviewRoute`-ot hívja (nem helyőrzőt).

### Accessibility-csapda, amit a kód elkerül (FR-27, NFR-04)

Próbálkozás nélküli kategóriánál a sor **nem rajzol sávot**: egy `0f` értékű
`LinearProgressIndicator` a Material 3 beépített `ProgressBarRangeInfo` szemantikájával
„0 százalék"-ot jelentene be a TalkBacknek, miközben a látható szöveg „nincs adat"-ot
mond – pontosan az a félreértés, amit az FR-27 kizár. Új sor a
`docs/CONVENTIONS.md`-ben.

### Rétegszabály, NFR-07

A `feature/progress` build.gradle.kts-e (már meglévő, a modul létrehozásakor felvett
megjegyzés) tiltja a Room/DTO típusokat; az új kód kizárólag `core/model`/`core/quiz`
domain típust és a `core/data` repository-szerződést látja. Nincs közvetlen
`Clock`/`Random` hívás (nincs is rá szükség: a ViewModel csak olvas, nem ír). A
`ProgressOverviewViewModel`-nek nincs `SavedStateHandle`-je (a `progress/overview`
route nem vesz át argumentumot), ezért a ViewModel-teszt Robolectric nélkül, sima
JUnittal fut.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `@Preview` emulátoros/vizuális ellenőrzés | **NEM FUTOTT** – csak forráskódként ellenőrizve (`ProgressOverviewScreenLoadingPreview`/`WithDataPreview`/`NoDataPreview`, világos + sötét); a tényleges renderelés emulátort igényel (B-03). |

---

## 2026-11-13 – T-504/T-505/T-506 kész (G-10 csoport): a Haladás áttekintő két alszakasza
## és a technika-részletező

| Parancs | Exit | Eredmény |
|---|---|---|
| `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` + `./gradlew.bat :feature:progress:compileDebugKotlin :core:data:compileDebugKotlin :navigation:compileDebugKotlin --console=plain` | 0 | `BUILD SUCCESSFUL in 21s` – csak a már ismert `hiltViewModel()` deprecation figyelmeztetés, hiba nincs. |
| `./gradlew.bat :feature:progress:testDebugUnitTest :core:data:testDebugUnitTest :navigation:testDebugUnitTest :feature:learn:testDebugUnitTest :feature:quiz:testDebugUnitTest --console=plain --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 29s`, 198 actionable task. Minden érintett modul `build/test-results/testDebugUnitTest/*.xml` `failures="0" errors="0"`. |
| `./gradlew.bat testDebugUnitTest --console=plain --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 40s`, 311 actionable task (teljes, őszinte projektfutás, minden modul). Minden `build/test-results/**/*.xml` `failures="0" errors="0"`. |
| `./gradlew.bat assembleDebug --console=plain` | 0 | `BUILD SUCCESSFUL in 5s`, 304 actionable task (24 végrehajtva, 280 up-to-date); `app/build/outputs/apk/debug/app-debug.apk` megvan. |

A teljes projekt `tests` attribútumainak összege (minden modul, a kanonikus
`testDebugUnitTest`/JVM-modul `test` eredménykönyvtárából, dupla számolás nélkül):
**587 → 606** (+19), **0 hiba, 0 error**. Az érintett osztályok:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…feature.progress.ProgressFormattingTest` (új) | 2 | 0 |
| `…feature.progress.overview.ProgressNeedsPracticeAccessibilityTest` (új) | 2 | 0 |
| `…feature.progress.overview.ProgressOverviewViewModelTest` (+4) | 9 | 0 |
| `…feature.progress.technique.ProgressTechniqueLabelsTest` (új) | 2 | 0 |
| `…feature.progress.technique.ProgressTechniqueViewModelTest` (új) | 7 | 0 |
| `…core.data.repository.RoomProgressRepositoryTest` (+2) | 23 | 0 |

### Mi készült

A G-10 csoport (`docs/backlog/GROUPS.md`) a T-503 kereteként megépített Haladás
áttekintő két hiányzó alszakaszát adja hozzá, és megépíti a technika-részletezőt:

- **T-504 – „Gyakorlást igénylő" lista (FR-26).** `ProgressOverviewUiState.Content`
  kapott egy `needsPractice: List<ProgressNeedsPracticeItem>` mezőt; a
  `ProgressOverviewViewModel` a `ProgressRepository.observeNeedsPractice(10)` listáját
  a `TechniqueRepository.observeTechniques()`-ből feloldott romaji/magyar
  név/ábrával párosítja, `techniqueId` szerint (nem index szerint). Üres lista →
  „Még nincs elég adat a javaslathoz." a lista helyén, nem üres `Column`. Tétel
  kattintható → `onOpenTechnique` → `progress/technique/{techniqueId}`.
- **T-505 – önértékelési blokk (FR-28).** `ProgressOverviewUiState.Content` kapott egy
  `studyReviewSummary: StudyReviewSummary` mezőt (a `ProgressRepository.observeStudyReviewSummary()`-ból,
  T-301-ben már megírt forrás) – külön, elhatárolt szakasz „Tudtam: `{n}` ·
  Gyakorolnom kell: `{m}`" + a FUNCTIONAL_SPEC 8. szó szerinti magyarázó sora.
- **T-506 – technika-részletező (FR-25, DR-07).** Új `feature/progress/technique`
  csomag: `ProgressTechniqueViewModel` (`SavedStateHandle`-ből olvasott
  `techniqueId`, ugyanaz a minta, mint a `feature/library` `LibraryDetailViewModel`-nél)
  + `ProgressTechniqueScreen`. Azonosító adatok + „Megnyitás a Tudástárban" link,
  kvízstatisztika (próbálkozás/helyes/sikerarány/utolsó gyakorlás,
  kérdéstípus-bontás), önértékelési előzmény külön blokkban, és a visszavont technika
  explicit jelzése – a statisztika ekkor is látható marad (ADR-004, DR-07).
  Bekötve a `KyokushinNavHost` `progress/technique/{techniqueId}` ágába (a
  `PlaceholderScreen` helyén).

### `core/data` bővítés, amire a T-506-nak szüksége volt

A `ProgressRepository`-nak eddig nem volt tagja a **technikánkénti** önértékelési
előzményhez (csak az összesített `observeStudyReviewSummary()` létezett, T-505-höz). Új
tag: `observeStudyReviewHistory(techniqueId): Flow<List<StudyReview>>` – a
`StudyReviewDao.observeForTechnique` (új, `Flow`-visszatérésű testvérmetódus a már
létező `suspend getForTechnique` mellett, változatlanul, a meglévő tesztek nem
érintettek) + egy új `StudyReviewEntity.toStudyReview()` mapper
(`UserEntityMappers.kt`) adja. Ugyanaz a mintázat, mint a T-411 `getSessionResult`-ja
(CONVENTIONS 1. szakasz „Egy `ProgressRepository`-bővítés minden tesztdublőrjét
érinti"): mind az öt `ProgressRepository` tesztdublőr
(`FakeLearnProgressRepository`, `FakeQuizProgressRepository`,
`FakeQuizRunProgressRepository`, `FakeQuizResultProgressRepository`,
`FakeProgressOverviewRepository`) kapott egy új felülírást (a négy kvíz/tanulás-oldali
kivételt dob, az új `FakeProgressTechniqueDetailRepository` valósítja meg). Új
`RoomProgressRepositoryTest` esetek (+2): legfrissebb elöl, csak a kért technikára
szűrve; nincs önértékelés → üres lista, nem `null`.

### Rétegszabály, NFR-07

Az új kód kizárólag `core/model`/`core/quiz` domain típust és a `core/data`
repository-szerződést látja a `feature/progress`-ben; a `ProgressTechniqueViewModel`
`SavedStateHandle`-je a `navigation` modul `NavArgs.TECHNIQUE_ID`-jával csak **névben**
egyezik (nincs `navigation`-függés, L5). Nincs közvetlen `Clock`/`Random` hívás – az
utolsó gyakorlás/önértékelés dátumának megjelenítése (`formatLastPracticed`) egy már
**meglévő, tárolt** `Instant`-ot formáz a megadott (alapértelmezésben eszköz-)
időzónával, nem egy új időpontot kérdez le.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `@Preview` emulátoros/vizuális ellenőrzés | **NEM FUTOTT** – csak forráskódként ellenőrizve (mindhárom képernyő/állapot világos + sötét variánsa); a tényleges renderelés emulátort igényel (B-03). |

## 2026-11-13 – T-507/T-509 kész (G-11 csoport): `feature/progress` beállítások + ViewModel tesztek

| Parancs | Exit | Eredmény |
|---|---|---|
| `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` + `./gradlew.bat :feature:progress:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 20s`, 76 actionable task. A `feature/progress` modul minden tesztosztálya `failures="0" errors="0"` (összesen 33 eset a modulban, köztük az új `ProgressSettingsViewModelTest` 6 esete). |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 39s` (első futás) / `BUILD SUCCESSFUL in 40s` (megerősítő, második teljes futás egy kiegészítő `@Preview` után), 311 actionable task – a teljes projekt, minden modul. Minden `build/test-results/**/*.xml` `failures="0" errors="0"`. |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 4s` (majd egy újabb, megerősítő futás `BUILD SUCCESSFUL in 2s`), `app/build/outputs/apk/debug/app-debug.apk` megvan (~21 MB). |

A teljes projekt `tests` attribútumainak összege (minden modul, a kanonikus
`testDebugUnitTest`/JVM-modul `test` eredménykönyvtárából, dupla számolás nélkül):
**606 → 613** (+7), **0 hiba, 0 error**. Az érintett osztályok:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…feature.progress.settings.ProgressSettingsViewModelTest` (új) | 6 | 0 |
| `…feature.progress.overview.ProgressOverviewViewModelTest` (+1 → 10) | 10 | 0 |

### Mi készült

- **T-507 – Beállítások (FR-32, FR-35, NFR-09).** Új `feature/progress/settings`
  csomag: `ProgressSettingsUiState` (`Loading`/`Content` – séma-/tartalomverzió,
  szerkesztői állapot, rekord- és kategóriaszám, `ClearProgressStep`), a
  `ProgressSettingsViewModel` (`TechniqueRepository.observeContentState()` +
  `.observeCategories()` a tartalominfóhoz, `ProgressRepository.clearAllProgress()` a
  törléshez) és a `ProgressSettingsScreen`/`Route`. A kétlépcsős megerősítés
  (`NONE → FIRST_WARNING → FINAL_WARNING`) a `SavedStateHandle`-ben él (ugyanaz a
  minta, mint a `QuizRunViewModel` `closeConfirmationVisible` mezője): az első
  párbeszéd pontosan felsorolja, mi törlődik (kvízpróbálkozások, munkamenetek,
  önértékelések) és mi nem (a tudástár), mindkét párbeszéd kimondja, hogy a művelet
  nem visszavonható. A törlés gomb csak a végső megerősítés után hívja a
  `clearAllProgress()`-t, és amíg a hívás fut, újra nem indítható
  (`isClearing`/kétszeri-kattintás védelem). A „Verzió és forrás" szakasz a
  tartalomverziót és a `knowledgebase/README.md`-ből szó szerint átvett két
  korlátozó mondatot jeleníti meg – **nem** app-verziószámot: a `feature/progress`
  modulhatár miatt nem éri el az `app` modul `BuildConfig.VERSION_NAME`-jét (lásd
  `docs/CONVENTIONS.md` 5. szakasz, új sor). A `navigation` modul `SETTINGS` ága
  szándékosan továbbra is `PlaceholderScreen` – a tényleges bekötés egy önálló,
  `navigation`-feladat (ugyanaz a nyitott minta, mint a `quiz/setup`/`quiz/run`/
  `quiz/result`-nál).
- **T-509 – ViewModel tesztek (FR-26, FR-27, FR-35).** `ProgressSettingsViewModelTest`
  (6 eset): importált/nem importált („nincs adat") tartalominfó, a kétlépcsős
  megerősítés (a repository csak a végső megerősítés után híva, egyszer), „Mégsem"
  bármelyik lépésben (sosem hív törlést), és hogy a törlés nem változtatja meg a
  tartalominfót. Egy dedikált eset egyetlen `FakeResettableProgressRepository`
  példányt oszt meg egy `ProgressOverviewViewModel` és egy
  `ProgressSettingsViewModel` között, és bizonyítja, hogy a Beállításokon
  megerősített törlés az áttekintő statisztikáját (`overallScore`,
  kategóriánkénti `score`) is nullázza, a tartalominfót (rekord-/kategóriaszám)
  nem érinti. A „gyakorlást igénylő" **rendezés megőrzését** (a tényleges rendezés a
  T-501 SQL `@Query`-je) egy új `ProgressOverviewViewModelTest` eset fedi
  (`progressOverview_needsPractice_preservesRepositoryOrder_FR26`, `+1 → 10`); a
  „nincs adat" esetek és az önértékelés-függetlenség a G-10 csoport tesztjei már
  lefedték, ezeket nem kellett megduplázni.

### Rétegszabály, NFR-07

A `ProgressSettingsViewModel` kizárólag a `ProgressRepository`/`TechniqueRepository`
szerződést és a `core/model` domain típusokat (`Category`, `DatasetTrust`) látja – nincs
Room entitás/DAO/DTO a `feature/progress`-ben (L3). Nincs közvetlen `Clock`/`Random`
hívás: a `ClearProgressStep` állapotgép tisztán felhasználói akciókra (kattintás)
reagál, nincs benne időfüggő vagy véletlen logika.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `@Preview` emulátoros/vizuális ellenőrzés | **NEM FUTOTT** – csak forráskódként ellenőrizve (5 állapotpár – betöltés, importált tartalom, nem importált tartalom, első/végső megerősítés – világos + sötét variánsa); a tényleges renderelés emulátort igényel (B-03). |

## 2026-11-13 – T-508 kész: `feature/progress` fejlesztői diagnosztika

| Parancs | Exit | Eredmény |
|---|---|---|
| `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"` + `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 1m 56s`, 312 actionable task – a teljes projekt, minden modul. Minden `build/test-results/**/*.xml` `failures="0" errors="0"`. |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL in 11s`, `app/build/outputs/apk/debug/app-debug.apk` megvan (~20 MB). |

A teljes projekt `tests` attribútumainak összege (minden modul, a kanonikus
`testDebugUnitTest`/JVM-modul `test` eredménykönyvtárából, dupla számolás nélkül):
**613 → 626** (+13), **0 hiba, 0 error**. Az érintett (új) osztályok:

| Osztály | Teszt | Hiba |
|---|---|---|
| `…feature.progress.diagnostics.ProgressDiagnosticsGroupingTest` (új) | 4 | 0 |
| `…feature.progress.diagnostics.ProgressDiagnosticsIssueLabelsTest` (új) | 5 | 0 |
| `…feature.progress.diagnostics.ProgressDiagnosticsViewModelTest` (új) | 4 | 0 |

### Mi készült

- **T-508 – Fejlesztői diagnosztika (FR-32, US-19).** Új `feature/progress/diagnostics`
  csomag: `ProgressDiagnosticsUiState` (`Loading`/`NoReport`/`Content` –
  súlyosságszűrő, teljes-elutasítás jelző, elfogadott/elvetett rekordszám,
  kódonkénti csoportok), `ProgressDiagnosticsViewModel` (a már meglévő
  `ContentImporter.lastValidationReport()` **egyszeri, pontszerű** `suspend`
  hívását egy belső `Loading`/`Loaded` állapotba zárja, nem `Flow`-t figyel – a
  jelentés dokumentáltan nem perzisztálódik), `ProgressDiagnosticsGrouping`
  (`@Composable`-mentes, `IssueCode` szerint `V001 < … < V010` rendezett
  csoportosítás és súlyosságszűrés – az ESETI, nem a kód alapértelmezett
  súlyossága szerint szűr), `ProgressDiagnosticsIssueLabels` (érintett rekord
  leírása ismert/ismeretlen azonosítóval, 1-alapú bejegyzés-sorszámmal) és a
  `ProgressDiagnosticsScreen`/`Route` (súlyosságszűrő `FilterChip`-sor, kódonkénti
  kártyák, „nincs ilyen súlyosságú észrevétel" üres állapot, „teljes tudástár
  elutasítva" figyelmeztetés). A `ValidationIssue.detail` szándékosan **nem**
  `strings.xml`-ből jön (a `core/quiz` KDoc-ja már korábban is kimondja: fejlesztői
  diagnosztikai szöveg, nem végfelhasználói UI-felirat).
- **A `feature/progress/settings` (T-507) debug-only menüpontja.** A korábban
  félkész `ProgressSettingsScreen` módosítás hiányzó darabja, a
  `ProgressSettingsDiagnosticsButton` composable (`OutlinedButton`, `MIN_TOUCH_TARGET`
  = 48 dp) most elkészült, és a `stringResource(R.string.progress_settings_diagnostics_button)`
  = „Tartalomdiagnosztika" (FUNCTIONAL_SPEC 6.3 szó szerinti felirata) jelenik meg
  rajta. Ugyanaz a `BuildConfig.DEBUG`-minta, mint a `feature/library` „Diagnosztika"
  gombjánál (T-210): `ProgressSettingsRoute` `onOpenDiagnostics = if (BuildConfig.DEBUG) ({}) else null` –
  release buildben a gomb egyáltalán nem rajzolódik ki, mert az `onOpenDiagnostics != null`
  feltétel hamis. Ehhez a `feature/progress/build.gradle.kts` kapott egy
  `buildFeatures.buildConfig = true` sort. A `settings → progress/diagnostics`
  tényleges navigációs bekötés a `navigation` modul tárgya, szándékosan nyitva
  marad (ugyanaz a dokumentált minta, mint a `quiz/setup`/`quiz/run`/
  `quiz/result`/`settings` útvonalaknál, `docs/CONVENTIONS.md` 5. szakasz).

### Rétegszabály, NFR-07

A `ProgressDiagnosticsViewModel` kizárólag a `core/data` `ContentImporter`
szerződését és a `core/quiz` validációs típusait (`ValidationReport`,
`ValidationIssue`, `IssueCode`, `Severity`) látja – nincs Room entitás/DAO/DTO a
`feature/progress`-ben (L3), és a `ContentImporter` már a `core/data`
`RepositoryModule`-ban kötve van (T-203), nem igényelt új DI modult. Nincs közvetlen
`Clock`/`Random` hívás: a súlyosságszűrő egy tiszta felhasználói választás, a
jelentés betöltése pedig a már injektált `ContentImporter`-en keresztül megy.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva; a T-607 tárgya. |
| `@Preview` emulátoros/vizuális ellenőrzés | **NEM FUTOTT** – csak forráskódként ellenőrizve (a diagnosztika négy állapota – betöltés, nincs jelentés, jelentés, teljes elutasítás – és a beállítások debug-gombos variánsa, mindegyik világos + sötét); a tényleges renderelés emulátort igényel (B-03). |

---

## 2026-11-13 – T-601: a tudástár bemásolása az assets alá (FR-29, AC-08)

### Mi készült

A `knowledgebase_transfer/manifest.json`, `entries.json` és `illustrations/` (10 kép)
bitre azonos másolata bekerült az `app/src/main/assets/knowledge/` alá (ellenőrizve:
`diff`/`cmp` mindhárom fájlcsoporton, eltérés nélkül). A `knowledgebase/` marad a
szerkesztési forrás, a `knowledgebase_transfer/` a köztes, ellenőrzött átadási forma –
a másolás módja **kézi** (nem Gradle-task), dokumentálva a
`knowledgebase_transfer/README.md` „Hogyan kerül az appba" szakaszában és egy új
`app/src/main/assets/knowledge/README.md`-ben.

Új teszt: `app` modul, `hu.kyokushin.trainer.di.KnowledgeAssetImportTest` (2 teszt) –
a teljes Hilt-gráfot Robolectric alatt felépítve (ugyanaz a minta, mint a
`HiltGraphTest`, T-203) **tényleg lefuttatja** a `ContentImporter.importIfNeeded()`-t a
valódi, csomagolt `AssetManager`-en át:

- `importIfNeeded_packagedKnowledgeAssets_loadsAllEntries_FR29` – `ImportResult.Imported`,
  `contentVersion = "0.1.0"`, `acceptedCount = 161`, `rejectedCount = 0`, `retiredCount = 0`.
- `importIfNeeded_calledTwice_secondCallIsUpToDate_FR30` – a második hívás
  `ImportResult.UpToDate("0.1.0")` (verziókapu a valódi csomagolt asseten).

Ez a **„Kész, ha: `assembleDebug` után az app elindul és betölti a 161 rekordot"**
kritérium bizonyítéka – valódi eszköz/emulátor nélkül (B-03 nyitott blokkoló), a
projekt bevált, emulátor nélküli Hilt-gráf-ellenőrzési mintájával.

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew :app:testDebugUnitTest --tests "hu.kyokushin.trainer.di.KnowledgeAssetImportTest" --rerun-tasks` | 0 | 2 teszt, 0 hiba |
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | **628 teszt** (626 → 628, +2), 0 hiba, 0 error, teljes projekt |
| `./gradlew assembleDebug` | 0 | `app-debug.apk`, **23 060 928 bájt** (≈ 22,0 MiB; a korábbi 18,69 MB-hoz képest +~2,2 MB, a 10 csomagolt kép mérete – elfogadható) |

APK-tartalom ellenőrzve (`unzip -l`): `assets/knowledge/manifest.json`,
`assets/knowledge/entries.json`, `assets/knowledge/illustrations/KY-0044.jpg` …
`KY-0053.jpg` (mind a 10) mind megtalálhatók a csomagolt APK-ban.

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| Valódi eszközön/emulátoron történő indítás és a 161 rekord UI-ban való megjelenésének vizuális ellenőrzése | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03). A `KnowledgeAssetImportTest` ugyanezt a betöltési láncot (valódi `AssetManager` → `ContentImporter` → Room) Robolectric alatt, emulátor nélkül bizonyítja, de a tényleges UI-renderelést és indítási élményt nem. A T-606 tárgya. |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |

---

## 2026-11-13 – T-602: TR-07 rétegszabály-ellenőrzés automatizálva (NFR-03, NFR-07)

### Mi készült

Három új JUnit teszt a `core/quiz` modulban (a task modulmezője szerint), amely a
rétegszabály (ADR-001) és a determinizmus (NFR-07) maradék, fordítás által nem elkapott
eseteit forráskód-szinten ellenőrzi:

- `LayeringRuleTest.coreModelAndCoreQuiz_haveNoForbiddenImport_TR07_NFR03` – a
  `core/model`/`core/quiz` `src/main/kotlin` fáján nincs `android.`/`androidx.`/`room`/
  `compose` importsor.
- `LayeringRuleTest.coreModelAndCoreQuiz_haveNoDirectClockOrRandomCall_TR07_NFR07` –
  ugyanott nincs közvetlen `Random.Default`/`System.currentTimeMillis`/`Instant.now`/
  `Math.random` hívás (a keresés előbb komment-mentesít, mert a két modul KDoc-jai
  dokumentáltan, szó szerint idézik ezeket a hívásokat a hiányukat magyarázva).
- `LayeringRuleTest.featureModules_doNotImportRoomEntityDaoOrDtoType_TR07_NFR03` – a
  négy `feature/*` modul `src/main/kotlin` fáján nincs `androidx.room` import, és nincs
  `Entity`/`Dao`/`Dto` végű típusnévre mutató import.

A szabálylogika (`LayeringRuleDetector`) sima `String`-eken dolgozik, elválasztva a
fájlrendszer-bejárástól. A task „Kész, ha" pontja („a teszt elbukik, ha valaki
véletlenül behúz egy tiltott importot") ezáltal **tényleg bizonyított**, nem csak
feltételezett: `LayeringRuleDetectorTest` 10 szintetikus esettel mutatja meg, hogy a
detektor pozitív esetben (tiltott import/hívás van a bemenetben) talál, negatív esetben
(megengedett domain-import, vagy a hiányt magyarázó KDoc/sorvégi megjegyzés) nem téveszt.
A valódi forrásfák ellenőrzése a `kyo.repoRoot` rendszertulajdonságon (már a
`core/quiz/build.gradle.kts`-ben beállítva a `RealKnowledgeBaseFixture`-höz) keresztül
megy – a teszt JVM munkakönyvtára a modul saját könyvtára, nem a repó gyökere.

Fordítási közben előjött a `docs/CONVENTIONS.md` 6. szakaszában már rögzített „KDoc `/*`
karakterpár" csapda: egy KDoc-ban szó szerint leírt `feature/*` glob a Kotlin beágyazott
blokk-megjegyzés szabálya miatt `Unclosed comment` hibát adott – a szöveg „feature
modulokban"-ra/„feature modulbeli"-re írása megoldotta.

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew :core:quiz:test --tests "hu.kyokushin.trainer.core.quiz.architecture.*" --rerun-tasks` | 0 | 13 teszt (`LayeringRuleDetectorTest`: 10, `LayeringRuleTest`: 3), 0 hiba |
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | **641 teszt** (628 → 641, +13), 0 hiba, 0 error, teljes projekt |
| `./gradlew assembleDebug` | 0 | `app-debug.apk`, `BUILD SUCCESSFUL` |

### NEM FUTOTT / NEM ALKALMAZHATÓ

| Parancs / ellenőrzés | Ok |
|---|---|
| `./gradlew connectedDebugAndroidTest` | **NEM FUTOTT** – nincs csatlakoztatott eszköz vagy emulátor (B-03); nem is érinti ezt a feladatot (nincs UI). |
| `./gradlew lint ktlintCheck` | **NEM FUTOTT** – a ktlint konfigurációja még nincs beállítva (T-607). |

## 2026-11-13 – T-603 kész: TR-08 manifest permission teszt (NFR-01)

Új teszt: `app/src/test/kotlin/hu/kyokushin/trainer/ManifestPermissionTest.kt`
(`mergedManifest_doesNotRequestInternetPermission_TR08`). Robolectric alatt
(`@Config(sdk = [34], application = Application::class)`) a `PackageManager`-től
lekéri a csomagolt (összevont) manifest `requestedPermissions` tömbjét, és
`doesNotContain(Manifest.permission.INTERNET)`-tel ellenőrzi – **nem** „nulla
permission"-t vár, mert a 2026-11-11-i build igazolta, hogy az AndroidX (Hilt)
transitive manifest merge-je saját, aláírásszintű permissiont ad hozzá
(`…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`), amit egy ilyen assertion hamisan
buktatna.

A teszt szándékosan a sima `Application`-t használja, nem a `HiltTestApplication`-t
(mint a `HiltGraphTest`): a valódi `KyokushinApplication` `@HiltAndroidApp`, aminek
felépítése a teljes Hilt gráfot igényelné – ennek a tesztnek viszont csak az összevont
manifestre van szüksége.

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew :app:testDebugUnitTest --tests "hu.kyokushin.trainer.ManifestPermissionTest" --rerun-tasks` | 0 | 1 teszt, 0 hiba |
| `./gradlew testDebugUnitTest --rerun-tasks` (T-607 végén, együtt) | 0 | **642 teszt** (641 → 642, +1), 0 hiba, teljes projekt |

## 2026-11-13 – T-607 kész: lint és kódformázás bekötve (NFR-11)

### Android Lint

Az AGP-ből a `lint` task modulonként már alapból él (`app`, `core/data`, `core/ui`,
`feature/*`, `navigation`, `di`); a `core/model`/`core/quiz` (tiszta Kotlin JVM
modulok, ADR-001 L1) nincs benne, és nem is kap `com.android.lint`-et, mert az a
rétegszabály által védett modulhatárt sértené egy kozmetikai jelentés-aggregálásért.

Az első (konfiguráció előtti) teljes futás **1 hibát és 5 figyelmeztetést** adott:

| Találat | Típus | Döntés |
|---|---|---|
| `PropertyEscape` a `local.properties`-en (gépspecifikus, gitignore-olt fájl, a `sdk.dir` szándékosan perjeles) | hiba | elnyomva (`app/build.gradle.kts` `lint { disable += "PropertyEscape" } `), indoklással a kódban |
| `RedundantLabel` a `MainActivity`-n (az `android:label` megegyezik az `application` tagéval) | figyelmeztetés | javítva (törölve a redundáns attribútum) |
| `OldTargetApi` ×2 (`targetSdk = 36` < `compileSdk = 37`) | figyelmeztetés | szándékos, dokumentált verziómátrix (ADR-008/ADR-009) – meghagyva |
| `NewerVersionAvailable` (`kotlinx-serialization-json` 1.12.0 → 1.13.0) | figyelmeztetés | meghagyva – verziófrissítés ADR-folyamat tárgya, nem lint-reaktív |
| `ObsoleteSdkInt` (`mipmap-anydpi-v26` felesleges, `minSdk = 26`) | figyelmeztetés | meghagyva – kozmetikai, nem blokkoló |

Emellett `checkDependencies = true` bekerült az `app` lint-konfigjába: ez az egyetlen
modul, amely minden másikra függ, tehát ez adja a reális, teljes-app lint-nézetet egy
9 modulos projektben (figyelmeztetést ad a `core/model`/`core/quiz` „external
dependency, not analyzed" státuszáról – ez elvárt, nem hiba).

### ktlint

A `org.jlleitschuh.gradle.ktlint` plugin (14.2.0, a plugins.gradle.org legfrissebb
stabil kiadása) a gyökér `build.gradle.kts` `subprojects {}` blokkjából minden
modulra bekötve. A szabályokat a gyökér `.editorconfig` adja. A bundle-özött ktlint
motor verziója (1.5.0, a plugin saját, tesztelt default értéke) **nincs felülírva** –
ez a dokumentáltan kompatibilis pár.

Az első (konfiguráció utáni, de formázás előtti) `ktlintCheck` futás **~3000 sornyi**
találatot adott a teljes forrásfán. A `ktlintFormat` ezt automatikusan a nagy
többségében kijavította (indentálás, sortörés, üres sor szabályok, import-sorrend,
trailing comma). Három, formázással nem javítható kategória kézi beavatkozást
igényelt:

1. **Sorhossz (max 120)** – kb. 40 előfordulás, túlnyomórészt az
   `override … = throw UnsupportedOperationException("…")` tesztdublőr-minta és
   néhány `@Preview(..., uiMode = …UI_MODE_NIGHT_YES)` annotáció. Mind tördelve (nem
   elnyomva) – lásd `docs/CONVENTIONS.md` 6. szakasz.
2. **KDoc Markdown-táblázat sora nem rövidíthető** – 3 hely
   (`DefaultKnowledgeValidator.resolveIllustration`, `DefaultQuizGenerator`,
   `KyokushinDatabase`): `@Suppress("ktlint:standard:max-line-length")` a dokumentált
   deklaráción, elszigetelt indoklással.
3. **Fájl-áttekintő KDoc + az első deklaráció saját KDoc-ja egymás után**
   (`standard:no-consecutive-comments`) – 7 fájl: az áttekintő komment `/**` → `/*`
   (tartalom változatlan).

Három `.editorconfig` szabály-kikapcsolás, mindegyik indokolva a fájlban és a
`docs/CONVENTIONS.md` 6. szakaszában: `ktlint_standard_function-naming` (a Compose
`@Composable` PascalCase konvenció ütközik a generikus lowerCamelCase-elvárással),
négy sortörés-szabály (`function-signature`, `multiline-expression-wrapping`,
`argument-list-wrapping`, `parameter-list-wrapping`, `class-signature`,
`property-wrapping`, `chain-method-continuation` – nem konvergálnak stabilan a
projekt meglévő kódján) és `ktlint_standard_filename` (négy, lezárt feladatok
dokumentációja által szó szerint idézett fájlnév). A `ktlint_code_style = android`
kísérletként beírva **érvénytelen** ktlint 1.5.0-ban (figyelmeztetéssel
`ktlint_official`-ra esik vissza) – törölve, dokumentálva.

### Futtatott parancsok

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew lint --rerun-tasks` | 0 | 9 Android modul, `PropertyEscape` elnyomva, `RedundantLabel` javítva, a többi figyelmeztetés dokumentáltan meghagyva |
| `./gradlew ktlintCheck --rerun-tasks` | 0 | teljes projekt, **77 task**, mindegyik tényleg futott (nincs `UP-TO-DATE` a bizonyítékban) |
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | **642 teszt**, 0 hiba, teljes projekt |
| `./gradlew assembleDebug --rerun-tasks` | 0 | `app-debug.apk`, `BUILD SUCCESSFUL` |

## 2026-11-13 – G-13 csoport kész: `docs/KNOWLEDGE_BASE.md` (T-608) és `docs/QUIZ_RULES.md` (T-609)

Tisztán dokumentációs feladatcsoport (`docs` modul, nincs kódváltozás) – a task fejléce
mindkettőnél `Teszt | —`-t ad, tehát nem tartozik hozzá új automatizált teszt; a „Kész,
ha" pont ehelyett azt kéri, hogy minden állítás visszavezethető legyen egy **létező**
tesztre, és hogy a dokumentum a tényleges kódot/sémát írja le, nem a tervezettet.

**Ellenőrzés módja:** mindkét dokumentumot közvetlenül a forráskódból és a csomagolt
tartalomból írtam (nem a korábbi VÁZLAT-ból kiindulva) – elolvasva a tényleges
`app/src/main/assets/knowledge/manifest.json`/`entries.json` mezőit, a
`KnowledgeInputMapper.kt`, `DefaultKnowledgeValidator.kt`, `ValidationIssue.kt`,
`DefaultQuizGenerator.kt`, `DistractorSelector.kt`, `QuizCandidateFilter.kt`,
`QuizEligibility.kt` és `QuizScoring.kt` forrását, és minden `docs/QUIZ_RULES.md`
állítást egy konkrét, meglévő teszt nevéhez kötve (`DefaultQuizGeneratorTest`,
`DistractorSelectorTest`, `RealContentQuizGenerationTest`, `QuizPlanInvariantTest`,
`QuizScoringTest`/`QuizScoringTr04Test` – mind a T-112/T-113/T-114 alatt készült,
meglévő, zöld tesztek). Ez a `/kyo-doc-sync` parancs által elvégzett összevetést
helyettesíti, mert a parancs maga egy audit-utasítás (Bash/Read/Grep/Glob), nem egy
determinisztikus check – a tényleges összevetést a dokumentum megírása közben, forrásról
forrásra végeztem el, nem utólag, külön lépésként.

**Mivel a változás tisztán dokumentáció**, a teljes tesztkészlet és a build a
változatlan, korábban (T-607) már bizonyított állapotában marad; az alábbi két parancs
ezt **újra, tényleges futtatással** igazolja (nem a korábbi eredményre hivatkozva):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | **642 teszt**, 0 hiba, teljes projekt (változatlan a T-607-es állapothoz képest – a módosítás nem érintett forráskódot) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk`, `BUILD SUCCESSFUL` (minden Android alprojekt `UP-TO-DATE` – nincs kódváltozás, amit újra kellett volna fordítani) |

**NEM FUTOTT:**

- `connectedDebugAndroidTest` – nincs emulátor/eszköz (B-03, változatlan).
- `./gradlew lint` / `./gradlew ktlintCheck` – nem kódváltozás, és a globális DoD listája
  (`docs/BACKLOG.md`) ezt a két parancsot nem írja elő kötelezőként (csak
  `assembleDebug` és `testDebugUnitTest`); a legutóbbi, T-607-beli futásuk
  (exit 0, teljes projekt) változatlanul érvényes, mert a `docs/*.md` fájlokat sem a
  lint, sem a ktlint nem vizsgálja.

**Megjegyzés a dokumentumok tartalmáról (nem hiba, de érdemes tudni):** a
`docs/QUIZ_RULES.md` megírása közben megerősítettem egy a kódból levezetett, nem
nyilvánvaló tényt: a `DatasetTrust` enum mindkét értéke (`REVIEWED`,
`PRACTICE_ON_UNREVIEWED`) átengedi a `QuizEligibility.isEligible` kaput – a bizalmi
szint tehát **ma nem zár ki technikát** a kvízből, csak a UI „Lektorálatlan
gyakorlóanyag" szalagját vezérli. Ez nem új hiba, csak egy a kódban már így élő,
korábban dokumentálatlan viselkedés, amit most a kiadott `QUIZ_RULES.md` explicit kimond.
