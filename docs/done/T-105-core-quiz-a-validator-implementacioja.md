# T-105 · core/quiz: a validátor implementációja

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-01 |
| Modul | `core/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-104](T-104-core-quiz-a-validator-szerzodese.md) |
| Követelmény | DR-05, FR-31, CR-02, CR-07 |
| Teszt | TR-01 |

## Cél

A V001…V010 szabályok tényleges végrehajtása a kanonikus séma felett.

## Teendők

- [x] `V001` támogatott `schema_version` (string semver, ismeretlen major → teljes elutasítás).
- [x] `V002` kötelező mezők; `V003` egyedi `id`; `V004` létező `category_id`.
- [x] `V005` képhivatkozás: hiányzó fájl → `Asset` helyett `Placeholder` lefokozás (nem rekordelvetés).
- [x] `V006` szinonimacsoportok a normalizált `hungarian_name` / `japanese_romaji` / `aliases` alapján.
- [x] `V007`/`V008` kvízképesség: `INCOMPLETE` mindig kizárva; a bizalmi kaput az ADR-010 formula adja.
- [x] `V009` hiányzó ékezetmentes alak származtatása; `V010` parse-hiba kezelése.

## Kész, ha

- [x] Egyetlen hibás rekord sem okoz kivételt, és nem dönti el az egész betöltést (kivéve V001/V010).
- [x] A validátor **soha nem emel** státuszt: `AWAITING_REVIEW` → `VERIFIED` átmenet kódból lehetetlen.

## Figyelj rá

- A státuszemelés tilalma (CR-02) nem stílus kérdése: ez akadályozza meg, hogy lektorálatlan tartalom hitelesként jelenjen meg.
- A normalizálás (kisbetű, ékezettelenítés) ugyanaz a függvény legyen, amit a keresés használ (T-202) – egy helyen definiálva.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
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
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL`; 65 teszt, 0 hiba (core/model 35 + core/quiz 30) |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`; `app/build/outputs/apk/debug/app-debug.apk` – 19 655 673 bájt |

**Megjegyzés:**

1. **Szerződésmódosítás (indokolt eltérés):** a `ManifestInput.schemaVersion` és a
   `ValidationReport.schemaVersion` típusa `Int?` → `String?` lett. Az ADR-011 szerint a
   kanonikus séma `schema_version` mezője **semver string** (`"1.0.0"`), és a V001 szabály a
   **major** verzión dől el – egész számmal a feladat „ismeretlen major → teljes elutasítás"
   pontja nem megfogalmazható, és a diagnosztika nem tudná megmondani, melyik verziót kaptuk.
   A T-104 szerződésteszt ennek megfelelően frissült (`"99.0.0"`).
2. **`NameNormalizer`** (`core/quiz/.../text/NameNormalizer.kt`) a normalizálás egyetlen
   helye: a V006/V009 szinonimavizsgálat, a `romajiAscii` származtatás és a **T-202 keresés**
   is ezt hívja (`core/data` → `core/quiz` függés megengedett, ADR-001 L2 csak a fordított
   irányt tiltja). Valós adaton ellenőrizve: mind a 161 rekordnál egyezik a konverter
   `search_romaji` értékével.
3. **`QuizEligibility`** az ADR-010 formula egyetlen megfogalmazása (`isEligible`,
   `eligibleTypes`, `hasFieldsFor`, `hasQuizUsableIllustration`). A **T-108 jelöltszűrés**
   ezt használja, ne írja újra.
4. **V005 kétféle hatása** szándékos: a *lógó* hivatkozás (a `ref` nincs a manifest
   nyilvántartásában) tartalmi hiba → `Placeholder` + `INCOMPLETE` státusz; a *hiányzó fájl*
   vagy a licenc nélküli kép csak csomagolási/jogi hiányosság → `Placeholder`, a státusz
   változatlan, így a szöveges kvíz és a tanulás megmarad.
5. **Hiányzó `manifest.contentVersion`** rekordonkénti `V002`-t ad (`field = "datasetVersion"`),
   nem teljes elutasítást: a `Technique.datasetVersion` kötelező mező, tehát egyetlen rekord
   sem képezhető le, de a DATASET hatókör a szerződés szerint a V001/V010-nek van fenntartva.
6. **Kézi próba a valós tudástáron** (`knowledgebase_transfer/`, 161 rekord, 9 kategória,
   10 illusztráció): `accepted = 161`, `rejected = 0`, nulla észrevétel,
   `datasetTrust = PRACTICE_ON_UNREVIEWED`, 10 `Asset`, ebből 7 `quizUsable` (a 3 feliratos
   kép kiesik). A próba ideiglenes, generált teszttel futott, amit utána töröltem.
7. **Felfedezett adathiba – javasolt új task:** a konverter `japanese_romaji_ascii` értéke a
   `KY-0020` rekordon hibás (`"Kirotsuke"` a `"Ki o tsuke"` helyett). Az appot nem érinti,
   mert a validátor újraszámolja az ékezetmentes alakot, de a
   `knowledgebase_transfer/tools/convert.py` kimenete és a CSV forrás eltérését javítani kell.
8. **Nyitott dokumentációs eltérés:** a `kyo-knowledge-base` skill 2–4. pontja még az
   `Int`-es `schemaVersion`-t és a szigorú V007-et írja; ezt az ADR-011 és az ADR-010 már
   felülírta, az igazítás a Fázis 3.3 (`docs/KNOWLEDGE_BASE.md`, T-608) dolga.
