# T-115 · core/data: a kanonikus séma beolvasása

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-04 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-105](T-105-core-quiz-a-validator-implementacioja.md) |
| Követelmény | FR-29, DR-05 |
| Teszt | TR-05 |

## Cél

A `manifest.json` és `entries.json` beolvasása a kanonikus séma szerint (ADR-011).

## Teendők

- [x] kotlinx.serialization DTO-k `@SerialName` snake_case leképezéssel, `ignoreUnknownKeys = true`.
- [x] `ManifestDto`: `schema_version`, `dataset_version`, `editorial_status`, `dataset_trust`, `categories[]`, `illustrations[]`, `synonym_groups[]`.
- [x] `EntryDto`: a 161 rekord mezői, köztük `illustration_ref`, `review_status`, `quiz_excluded`, `retired`.
- [x] Assets-olvasás és a tényleges képfájllista összegyűjtése a validátor bemenetéhez.

## Kész, ha

- [x] Minden DTO `internal` – nem szivárog ki a `core/data`-ból (L3).
- [x] Ismeretlen extra mező nem okoz hibát; hiányzó kötelező mező igen (a validátor jelzi).

## Figyelj rá

- Az `ignoreUnknownKeys` azért kell, hogy a tartalom bővíthető legyen anélkül, hogy a régi app elszállna.
- A `schema_version` **string** semver, nem int (ADR-011).

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [–] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – **nem alkalmazható**: a T-115 nem tartalmaz UI-t
- [x] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

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
| Commit | COMMIT_HASH |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | 16 teszt, 0 hiba |
| `.\gradlew.bat testDebugUnitTest` | 0 | 278 teszt, 0 hiba, 0 kihagyott |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app-debug.apk` (19 719 165 bájt) |

**Megjegyzés:**

- A beolvasó belépési pontja a `KnowledgeContentReader` (`internal`): kimenete
  `Parsed(KnowledgeInput)` vagy `Unreadable(ValidationReport)`. A T-117 `ContentImporter`
  mindkét ágon `ValidationReport`-ot kap, és a `datasetRejected` kapun dönt – nincs
  kivételkezelési teendő a hívó oldalon.
- A `V010` **itt** készült el (olvashatatlan, szerkezetileg hibás vagy a csomagból hiányzó
  fájl), így a V001…V010 kódkészlet teljes. A hiányzó *kötelező mező* szándékosan nem
  V010: azt a validátor jelenti rekordszinten (V002), hogy egyetlen hiányos rekord ne
  vigye el a többi 160-at.
- A DTO → `KnowledgeInput` leképezés **szándékosan azonos** a `core/quiz` teszt-oldali
  `RealKnowledgeBaseFixture.kt`-jével (a T-106 kérése). A szinkront a
  `RealKnowledgeContentReaderTest` tartja: ugyanazokat az elvárásokat állítja a valódi 161
  rekordra, mint a `RealKnowledgeBaseValidationTest`. Ha a séma változik, **mindkettőt** kell
  követni – a fixture a T-601 után is megmarad validátor-oldali garanciának.
- Az `entries.json` megismétli a manifest `schema_version` / `dataset_version` értékét. A
  beolvasó beolvassa (`EntriesFileDto`), de **a manifest a hiteles forrás**, és a két hely
  eltérését egyelőre semmi nem jelzi. Ha ez kell, önálló szabálykódot (és taskot) igényel,
  nem csendes felülírást.
- Nem futott: az `AndroidKnowledgeAssetSource` vékony delegáló osztályára nincs unit teszt –
  a `core/data` modulnak nincs saját `assets/` könyvtára (a tartalom másolása a T-601).
  Rögzítve a `docs/TEST_REPORT.md`-ben.
- Változatlanul nyitott forrásadat-hiba (T-105/T-106 óta): a `KY-0020` rekord
  `japanese_romaji_ascii` értéke `"Kirotsuke"` a `"Ki o tsuke"` helyett. Az appot nem
  érinti (a validátor újraszámolja), a `knowledgebase_transfer/tools/convert.py` kimenetét
  kell javítani.
