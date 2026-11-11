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

- [ ] kotlinx.serialization DTO-k `@SerialName` snake_case leképezéssel, `ignoreUnknownKeys = true`.
- [ ] `ManifestDto`: `schema_version`, `dataset_version`, `editorial_status`, `dataset_trust`, `categories[]`, `illustrations[]`, `synonym_groups[]`.
- [ ] `EntryDto`: a 161 rekord mezői, köztük `illustration_ref`, `review_status`, `quiz_excluded`, `retired`.
- [ ] Assets-olvasás és a tényleges képfájllista összegyűjtése a validátor bemenetéhez.

## Kész, ha

- [ ] Minden DTO `internal` – nem szivárog ki a `core/data`-ból (L3).
- [ ] Ismeretlen extra mező nem okoz hibát; hiányzó kötelező mező igen (a validátor jelzi).

## Figyelj rá

- Az `ignoreUnknownKeys` azért kell, hogy a tartalom bővíthető legyen anélkül, hogy a régi app elszállna.
- A `schema_version` **string** semver, nem int (ADR-011).

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
