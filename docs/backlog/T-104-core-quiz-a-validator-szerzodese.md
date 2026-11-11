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
