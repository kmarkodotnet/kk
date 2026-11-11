# T-117 · core/data: ContentImporter

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-04 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-115](T-115-core-data-a-kanonikus-sema-beolvasasa.md), [T-116](T-116-core-data-room-sema.md) |
| Követelmény | FR-29, FR-30, FR-31, FR-33 |
| Teszt | TR-05 |

## Cél

A betöltési folyamat: DTO → validátor → upsert, az előrehaladás sérülése nélkül.

## Teendők

- [ ] `importIfNeeded()`: csak akkor fut, ha a csomagolt `dataset_version` ≠ a tárolt.
- [ ] **Upsert**, nem `DELETE ALL` + `INSERT`.
- [ ] A csomagban már nem szereplő technika: `retired = true`, **nem** fizikai törlés.
- [ ] A `ValidationReport` eltárolása a debug diagnosztikához (`lastValidationReport()`).
- [ ] Háttérszálon fut (injektált `@IoDispatcher`), a UI `Loading` állapotot mutat.

## Kész, ha

- [ ] Ugyanazt a verziót kétszer betöltve nincs duplikáció.
- [ ] A `quiz_attempt` és `study_review` sorok száma változatlan marad egy frissítés után.

## Figyelj rá

- Ez a task a FR-33 garancia megvalósítása. A TR-05 teszt (T-118) pontosan ezt méri.
- A `retired` jelölés miatt minden tartalomlekérdezésnek szűrnie kell rá – a repository felelőssége (T-201).

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
