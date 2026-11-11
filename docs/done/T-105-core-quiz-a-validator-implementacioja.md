# T-105 Â· core/quiz: a validÃ¡tor implementÃ¡ciÃ³ja

| | |
|---|---|
| Epik | **E1** â€“ Fundamentum: domain modell, kvÃ­zmotor, tudÃ¡stÃ¡r, perzisztencia |
| User story | US-01 |
| Modul | `core/quiz` |
| MÃ©ret | L (6â€“12 Ã³) |
| FÃ¼gg | [T-104](T-104-core-quiz-a-validator-szerzodese.md) |
| KÃ¶vetelmÃ©ny | DR-05, FR-31, CR-02, CR-07 |
| Teszt | TR-01 |

## CÃ©l

A V001â€¦V010 szabÃ¡lyok tÃ©nyleges vÃ©grehajtÃ¡sa a kanonikus sÃ©ma felett.

## TeendÅ‘k

- [x] `V001` tÃ¡mogatott `schema_version` (string semver, ismeretlen major â†’ teljes elutasÃ­tÃ¡s).
- [x] `V002` kÃ¶telezÅ‘ mezÅ‘k; `V003` egyedi `id`; `V004` lÃ©tezÅ‘ `category_id`.
- [x] `V005` kÃ©phivatkozÃ¡s: hiÃ¡nyzÃ³ fÃ¡jl â†’ `Asset` helyett `Placeholder` lefokozÃ¡s (nem rekordelvetÃ©s).
- [x] `V006` szinonimacsoportok a normalizÃ¡lt `hungarian_name` / `japanese_romaji` / `aliases` alapjÃ¡n.
- [x] `V007`/`V008` kvÃ­zkÃ©pessÃ©g: `INCOMPLETE` mindig kizÃ¡rva; a bizalmi kaput az ADR-010 formula adja.
- [x] `V009` hiÃ¡nyzÃ³ Ã©kezetmentes alak szÃ¡rmaztatÃ¡sa; `V010` parse-hiba kezelÃ©se.

## KÃ©sz, ha

- [x] Egyetlen hibÃ¡s rekord sem okoz kivÃ©telt, Ã©s nem dÃ¶nti el az egÃ©sz betÃ¶ltÃ©st (kivÃ©ve V001/V010).
- [x] A validÃ¡tor **soha nem emel** stÃ¡tuszt: `AWAITING_REVIEW` â†’ `VERIFIED` Ã¡tmenet kÃ³dbÃ³l lehetetlen.

## Figyelj rÃ¡

- A stÃ¡tuszemelÃ©s tilalma (CR-02) nem stÃ­lus kÃ©rdÃ©se: ez akadÃ¡lyozza meg, hogy lektorÃ¡latlan tartalom hiteleskÃ©nt jelenjen meg.
- A normalizÃ¡lÃ¡s (kisbetÅ±, Ã©kezettelenÃ­tÃ©s) ugyanaz a fÃ¼ggvÃ©ny legyen, amit a keresÃ©s hasznÃ¡l (T-202) â€“ egy helyen definiÃ¡lva.

## Definition of Done

A task-specifikus pontokon felÃ¼l a **globÃ¡lis DoD** is Ã©rvÃ©nyes (`docs/BACKLOG.md` â†’ â€žGlobÃ¡lis Definition of Doneâ€):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett kÃ¶vetelmÃ©nyre
- [x] Nem sÃ©rt rÃ©tegszabÃ¡lyt (ADR-001, L1â€“L6)
- [x] Nincs kÃ¶zvetlen `Clock` / `Random` hÃ­vÃ¡s Ã¼zleti kÃ³dban (NFR-07)
- [x] Minden felhasznÃ¡lÃ³i szÃ¶veg `strings.xml`-ben, magyarul
- [x] UI esetÃ©n: `contentDescription`, â‰¥ 48 dp cÃ©lpont, `@Preview` vilÃ¡gos + sÃ¶tÃ©t tÃ©mÃ¡ra
- [x] `docs/IMPLEMENTATION_PLAN.md` Ã¡llÃ¡sa frissÃ­tve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben â€žNEM FUTOTT â€“ okâ€ sorkÃ©nt szerepel

## KapcsolÃ³dÃ³ dokumentumok

- `docs/REQUIREMENTS.md` â€“ a hivatkozott kÃ¶vetelmÃ©nyek
- `docs/FUNCTIONAL_SPEC.md` â€“ a kÃ©pernyÅ‘ Ã©s a viselkedÃ©s leÃ­rÃ¡sa
- `docs/ARCHITECTURE.md` + `docs/adr/` â€“ a szerkezeti dÃ¶ntÃ©sek
- `.claude/skills/` â€“ `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## ElkÃ©szÃ¼lt

| | |
|---|---|
| DÃ¡tum | 2026-11-11 |
| Commit | `f5bc7d6` |

**Futtatott parancsok:**

| Parancs | Exit | EredmÃ©ny |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL`; 65 teszt, 0 hiba (core/model 35 + core/quiz 30) |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`; `app/build/outputs/apk/debug/app-debug.apk` â€“ 19 655 673 bÃ¡jt |

**MegjegyzÃ©s:**

1. **SzerzÅ‘dÃ©smÃ³dosÃ­tÃ¡s (indokolt eltÃ©rÃ©s):** a `ManifestInput.schemaVersion` Ã©s a
   `ValidationReport.schemaVersion` tÃ­pusa `Int?` â†’ `String?` lett. Az ADR-011 szerint a
   kanonikus sÃ©ma `schema_version` mezÅ‘je **semver string** (`"1.0.0"`), Ã©s a V001 szabÃ¡ly a
   **major** verziÃ³n dÅ‘l el â€“ egÃ©sz szÃ¡mmal a feladat â€žismeretlen major â†’ teljes elutasÃ­tÃ¡s"
   pontja nem megfogalmazhatÃ³, Ã©s a diagnosztika nem tudnÃ¡ megmondani, melyik verziÃ³t kaptuk.
   A T-104 szerzÅ‘dÃ©steszt ennek megfelelÅ‘en frissÃ¼lt (`"99.0.0"`).
2. **`NameNormalizer`** (`core/quiz/.../text/NameNormalizer.kt`) a normalizÃ¡lÃ¡s egyetlen
   helye: a V006/V009 szinonimavizsgÃ¡lat, a `romajiAscii` szÃ¡rmaztatÃ¡s Ã©s a **T-202 keresÃ©s**
   is ezt hÃ­vja (`core/data` â†’ `core/quiz` fÃ¼ggÃ©s megengedett, ADR-001 L2 csak a fordÃ­tott
   irÃ¡nyt tiltja). ValÃ³s adaton ellenÅ‘rizve: mind a 161 rekordnÃ¡l egyezik a konverter
   `search_romaji` Ã©rtÃ©kÃ©vel.
3. **`QuizEligibility`** az ADR-010 formula egyetlen megfogalmazÃ¡sa (`isEligible`,
   `eligibleTypes`, `hasFieldsFor`, `hasQuizUsableIllustration`). A **T-108 jelÃ¶ltszÅ±rÃ©s**
   ezt hasznÃ¡lja, ne Ã­rja Ãºjra.
4. **V005 kÃ©tfÃ©le hatÃ¡sa** szÃ¡ndÃ©kos: a *lÃ³gÃ³* hivatkozÃ¡s (a `ref` nincs a manifest
   nyilvÃ¡ntartÃ¡sÃ¡ban) tartalmi hiba â†’ `Placeholder` + `INCOMPLETE` stÃ¡tusz; a *hiÃ¡nyzÃ³ fÃ¡jl*
   vagy a licenc nÃ©lkÃ¼li kÃ©p csak csomagolÃ¡si/jogi hiÃ¡nyossÃ¡g â†’ `Placeholder`, a stÃ¡tusz
   vÃ¡ltozatlan, Ã­gy a szÃ¶veges kvÃ­z Ã©s a tanulÃ¡s megmarad.
5. **HiÃ¡nyzÃ³ `manifest.contentVersion`** rekordonkÃ©nti `V002`-t ad (`field = "datasetVersion"`),
   nem teljes elutasÃ­tÃ¡st: a `Technique.datasetVersion` kÃ¶telezÅ‘ mezÅ‘, tehÃ¡t egyetlen rekord
   sem kÃ©pezhetÅ‘ le, de a DATASET hatÃ³kÃ¶r a szerzÅ‘dÃ©s szerint a V001/V010-nek van fenntartva.
6. **KÃ©zi prÃ³ba a valÃ³s tudÃ¡stÃ¡ron** (`knowledgebase_transfer/`, 161 rekord, 9 kategÃ³ria,
   10 illusztrÃ¡ciÃ³): `accepted = 161`, `rejected = 0`, nulla Ã©szrevÃ©tel,
   `datasetTrust = PRACTICE_ON_UNREVIEWED`, 10 `Asset`, ebbÅ‘l 7 `quizUsable` (a 3 feliratos
   kÃ©p kiesik). A prÃ³ba ideiglenes, generÃ¡lt teszttel futott, amit utÃ¡na tÃ¶rÃ¶ltem.
7. **Felfedezett adathiba â€“ javasolt Ãºj task:** a konverter `japanese_romaji_ascii` Ã©rtÃ©ke a
   `KY-0020` rekordon hibÃ¡s (`"Kirotsuke"` a `"Ki o tsuke"` helyett). Az appot nem Ã©rinti,
   mert a validÃ¡tor ÃºjraszÃ¡molja az Ã©kezetmentes alakot, de a
   `knowledgebase_transfer/tools/convert.py` kimenete Ã©s a CSV forrÃ¡s eltÃ©rÃ©sÃ©t javÃ­tani kell.
8. **Nyitott dokumentÃ¡ciÃ³s eltÃ©rÃ©s:** a `kyo-knowledge-base` skill 2â€“4. pontja mÃ©g az
   `Int`-es `schemaVersion`-t Ã©s a szigorÃº V007-et Ã­rja; ezt az ADR-011 Ã©s az ADR-010 mÃ¡r
   felÃ¼lÃ­rta, az igazÃ­tÃ¡s a FÃ¡zis 3.3 (`docs/KNOWLEDGE_BASE.md`, T-608) dolga.

