# T-107 · core/quiz: a generátor szerződése

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-02 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-102](T-102-core-model-technique-category-illustrationref.md) |
| Követelmény | FR-14, FR-15, FR-22 |
| Teszt | TR-02 |

## Cél

A kvízgenerátor be- és kimeneti típusai. ADR-006: a kérdés csak azonosítókat tárol.

## Teendők

- [x] `QuizRequest(candidates, enabledTypes, categoryFilter, requestedCount, seed, datasetTrust)`.
- [x] `QuizQuestion(id, type, correctTechniqueId, optionTechniqueIds)` – **se szöveg, se kép**.
- [x] `QuizPlan(questions, skippedTypes: Map<QuestionType, SkipReason>, shortfall: Shortfall?)`.
- [x] `enum SkipReason { NOT_ENOUGH_CANDIDATES, NOT_ENOUGH_IMAGES, TYPE_DISABLED }`.
- [x] `QuizReadiness` a `dryRun()` kimenetének (T-111).

## Kész, ha

- [x] A `QuizQuestion` szerializálható (a `SavedStateHandle`-be kerül, T-405).
- [x] Nincs olyan mező, amibe megjelenítendő szöveg kerülhetne.

## Figyelj rá

- Azért csak ID: ha két technikának azonos a magyar neve, a szöveges összehasonlítás hamis találatot adna (DR-03, ADR-006).
- A `skippedTypes` és a `shortfall` nem hibajelzés, hanem a UI-nak szóló indoklás (FR-22).

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul – *nem érintett:* a `SkipReason`/`ShortfallReason` enum, a szöveget a UI állítja elő (T-404)
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – *nem érintett:* ez tiszta Kotlin modul, nincs benne Compose
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
| Commit | <commit> |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 135 teszt, 0 hiba, 0 kihagyott (core/model 35 + core/quiz 100) |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` |

**Megjegyzés:**

- **Eltérés a tervtől (tudatos):** a `core/quiz` **főkódja** megkapta a
  `kotlin-serialization` plugint és a `kotlinx-serialization-json` függőséget. A T-106
  jegyzete szerint addig csak a teszt forráskészlet látott szerializációt – ezt a
  „Kész, ha" pont kényszerítette át: a `QuizQuestion`-nak szerializálhatónak kell lennie,
  mert a T-405 a `SavedStateHandle`-be menti (ADR-007).
- A `core/model` **érintetlen** maradt: a value class azonosítók privát konstruktora
  megvan, és a szerializálás a `core/quiz`-ban élő `TechniqueIdSerializer` /
  `QuestionIdSerializer` sztring-szerializátorokon megy, amelyek a `TechniqueId.of(...)`
  gyárat hívják. A visszafejtés tehát nem kerülheti meg a domain invariánst – erre teszt
  van (`corruptSavedState_cannotBypassQuestionInvariants_FR14`).
- **Két típus a tervlistán felül keletkezett,** mert a szerződés nélkülük hiányos lenne:
  * `QuizCandidateSet` – a `QuizRequest.candidates` típusa; a technikákon túl viszi a V006
    **szinonimacsoportokat** (CR-07, a validátor `ValidationReport.synonymGroups`
    kimenete) és az illusztrációk **tartalmi ujjlenyomatát** (a T-109 „vizuálisan azonos
    kép" szabályához). Ez egyben a `TechniqueRepository.getQuizCandidates(...)`
    visszatérési típusa (`docs/ARCHITECTURE.md` 5.3), amit a T-402 fog megvalósítani.
  * `ShortfallReason` – a `Shortfall` okát a `kyo-quiz-engine` skill 5. pontja előírja, és
    más a hatóköre, mint a `SkipReason`-nak (teljes terv vs. egy kérdéstípus).
- **A T-108 ide illeszkedik:** a jelöltszűrés bemenete `QuizRequest.candidates` +
  `QuizRequest.categoryFilter` + `QuizRequest.datasetTrust`, és a meglévő
  `QuizEligibility.isEligible(technique, questionType, datasetTrust)` formulát kell
  használnia – nem duplikálva. A kihagyás okát a `SkipReason`-ban kell visszaadni.
- **A TR-02 még nem zöld,** és nem is lehet: a generálási invariánsok (pontosan egy helyes,
  három különböző téves, szinonimakizárás, seed-determinizmus, graceful fallback) tényleges
  algoritmust igényelnek – T-108…T-110, a tesztek T-112. A `QuizGeneratorContractTest`
  csak azt bizonyítja, hogy az interfész kivétel nélkül teljesíthető.
- **Nem javított, menet közben talált apróság:** a `core/quiz` a `core:model`-t
  `implementation` hatókörrel használja, miközben a publikus API-ja (`QuizQuestion`,
  `QuizRequest`) `core:model` típusokat tesz közzé. A fogyasztóknak (T-402, T-403, T-405)
  ezért saját `core:model` függőséget kell felvenniük. Javasolt új task: a Kotlin JVM
  magmodulok `api` hatókörre állítása, ha a Kotlin JVM plugin `java-library`-t alkalmaz –
  ez a T-203 (Hilt modulok) vagy a T-607 (lint/formázás) körül a legkisebb kockázattal
  elvégezhető.
