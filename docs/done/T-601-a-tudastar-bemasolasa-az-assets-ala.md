# T-601 · A tudástár bemásolása az assets alá

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-04 |
| Modul | `app` |
| Méret | S (1–3 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md) |
| Követelmény | FR-29, AC-08 |
| Teszt | — |

## Cél

A `knowledgebase_transfer/` tartalmának beillesztése az alkalmazásba.

## Teendők

- [x] `manifest.json`, `entries.json` és az `illustrations/` bemásolása `app/src/main/assets/knowledge/` alá.
- [x] Build-ellenőrzés: az APK tartalmazza a fájlokat, és az import lefut.
- [x] A másolás módja dokumentálva (kézi másolás vagy Gradle task).

## Kész, ha

- [x] `assembleDebug` után az app elindul és betölti a 161 rekordot.
- [x] Az APK mérete elfogadható a 10 képpel.

## Figyelj rá

- A `knowledgebase/` marad a szerkesztési forrás, a `knowledgebase_transfer/` a generált átadási forma – az assets ennek a másolata.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre (`…loadsAllEntries_FR29`, `…secondCallIsUpToDate_FR30`)
- [x] Nem sért rétegszabályt (ADR-001, L1–L6) – az új teszt csak a publikus `ContentImporter` interfészt látja, Room/DAO/DTO nélkül
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07) – nincs új üzleti kód, csak asset-másolás és teszt
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul – nincs új UI-szöveg ebben a taskban
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – nem alkalmazható, nincs UI-elem a taskban
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
| Dátum | 2026-11-13 |
| Commit | lásd `docs/IMPLEMENTATION_PLAN.md` változásnapló (ez a fájl a commitot megelőzően készült el) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew :app:testDebugUnitTest --tests "hu.kyokushin.trainer.di.KnowledgeAssetImportTest" --rerun-tasks` | 0 | 2 teszt, 0 hiba |
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | 628 teszt (626 → 628), 0 hiba, teljes projekt |
| `./gradlew assembleDebug` | 0 | `app-debug.apk`, 23 060 928 bájt (≈ 22,0 MiB) |

**Megjegyzés:** A másolás módja **kézi** (nem Gradle copy-task) – a Teendők listája
mindkettőt megengedte, a kézi egyszerűbb és auditálhatóbb, ugyanaz a minta, mint a
`knowledgebase_transfer/` saját, `convert.py`-jal előállított és commitolt mivolta.
Dokumentálva a `knowledgebase_transfer/README.md` „Hogyan kerül az appba" szakaszában
és egy új `app/src/main/assets/knowledge/README.md`-ben. A „betölti a 161 rekordot"
kritériumot valódi eszköz/emulátor nélkül (B-03) egy új Robolectric+Hilt teszt
(`app`, `KnowledgeAssetImportTest`) bizonyítja, a valódi `AssetManager`-en és a teljes
`ContentImporter`-en át, nem egy fájlrendszeri fixtúrával. Részletek:
`docs/TEST_REPORT.md` 2026-11-13 „T-601" szakasza, `docs/CONVENTIONS.md` 3. szakasz új
sora.
