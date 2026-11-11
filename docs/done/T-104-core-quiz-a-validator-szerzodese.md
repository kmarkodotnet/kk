# T-104 · core/quiz: a validátor szerződése

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-01 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-102](T-102-core-model-technique-category-illustrationref.md) |
| Követelmény | DR-05, FR-31 |
| Teszt | TR-01 |

## Cél

A tudástár-validátor publikus felülete és kimeneti típusai – implementáció nélkül.

## Teendők

- [ ] `interface KnowledgeValidator { fun validate(input: KnowledgeInput): ValidationReport }`.
- [ ] `KnowledgeInput(manifest, entries, availableIllustrationFiles: Set<String>)` – a fájlrendszert **nem** ismeri.
- [ ] `ValidationReport(accepted, rejected, issues, synonymGroups, datasetTrust, datasetVersion, schemaVersion)`.
- [ ] `ValidationIssue(code: IssueCode, severity: Severity, entryId?, field?, detail)`; `enum IssueCode { V001..V010 }`, `enum Severity { BLOCKING, WARNING }`.

## Kész, ha

- [ ] A szerződés nem dob kivételt: a hiba a `ValidationReport` része.
- [ ] A `core/quiz` modul továbbra is Android-mentes.

## Figyelj rá

- A képfájlok létezése **bemenet** (`availableIllustrationFiles`), nem a validátor dolga – így tisztán JVM-en tesztelhető (TR-01).
- A `synonymGroups` a generátor bemenete lesz: ez a csatolási pont, ami a CR-07-et kikényszeríti.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [ ] `./gradlew assembleDebug` exit 0
- [ ] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [ ] Nem sért rétegszabályt (ADR-001, L1–L6)
- [ ] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [ ] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [ ] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [ ] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [ ] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-11 |
| Commit | `d931421` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – 40 teszt, 0 hiba (`core/model` 35 + `core/quiz` 5) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk`, 19 625 677 bájt |

**Megjegyzés:**

- **Eltérés az `ARCHITECTURE.md` 4.1 kódvázlatától, a task-fájl szerint:** a belépési pont
  `validate(input: KnowledgeInput)` egyetlen paraméterrel (nem `manifest` + `techniques`
  párral), a jelentés pedig `datasetTrust` + `datasetVersion` + `schemaVersion` mezőket
  hordoz a vázlat `DatasetKind` + `contentVersion` párja helyett – `DatasetKind` típus
  nem létezik a `core/model`-ben, a meglévő enum a `DatasetTrust`. Az `ARCHITECTURE.md`
  4.1 szakasza átvezetésre vár (lásd `docs/IMPLEMENTATION_PLAN.md` drift-pontja).
- **A `ValidationIssue.entryId` nyers `String?`, nem `TechniqueId?`** (a vázlatban
  `techniqueId: TechniqueId?` szerepelt). Indok: a `TechniqueId.of` üres azonosítóra
  kivételt dob, márpedig éppen az üres `id`-jú rekordot kell tudni jelenteni (V002). Az
  azonosíthatóság kedvéért a jelentés `entryIndex`-et is tart, mert a `RejectedRecord`
  index az egyetlen mindig létező azonosító.
- **Hozzáadott, a task-fájlban nem nevesített típus: `IssueScope`** (`DATASET` / `RECORD` /
  `FIELD`). Indok: a `Severity` önmagában nem különíti el a „teljes betöltés elutasítva"
  (V001), a „rekord elvetve" (V002) és a „mező lefokozva" (V007) esetet, holott a
  szabálytáblázat „Hatás" oszlopa háromfelé osztja. Enélkül a `core/data` nem tudná
  eldönteni, támaszkodhat-e az `accepted` listára. Erre épül a `ValidationReport.datasetRejected`.
- **A bemeneti típusok szándékosan nem validálnak** (`null`-tűrő mezők, nincs `require()`):
  a hibás tartalomnak reprezentálhatónak kell lennie, különben a jelentés elkészítése előtt
  dobna kivételt. A `Technique` ezzel szemben `require()`-rel validál, ezért a T-105
  implementációnak a rekordot a domain objektum létrehozása **előtt** kell elvetnie. Ezt a
  sorrendet a `technique_blankMandatoryField_throws_soValidatorMustRejectBeforeMapping_FR31`
  teszt rögzíti.
- **A `V010` kódot nem a validátor állítja elő**, hanem a bemenetet összeállító importréteg
  (`core/data`, T-115), amikor a deszerializálás meghiúsul és a validátor sosem fut le.
- **A `core/quiz` modul megkapta a `testDebugUnitTest` gyűjtőtaskot** (a `test` aliasa),
  mint korábban a `core/model`. Enélkül a kötelező DoD-parancs csendben kihagyta volna a
  modul tesztjeit. Minden további tiszta Kotlin JVM modulnál ugyanez a lépés kell.
- **A `romajiAscii` nincs a bemeneti sémában**: származtatott érték, a normalizálás a
  validátor (T-105) felelőssége.
- A TR-01 teljes, szabályonkénti táblázatos tesztkészlete a T-106 tárgya; itt csak a
  szerződés öt tesztje fut.
