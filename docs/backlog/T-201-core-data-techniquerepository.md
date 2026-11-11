# T-201 · core/data: TechniqueRepository

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-07 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md) |
| Követelmény | NFR-03, DR-07 |
| Teszt | — |

## Cél

A tartalom olvasási felülete a feature modulok felé – domain modellel, nem entitással.

## Teendők

- [ ] `interface TechniqueRepository`: `observeCategories()`, `observeTechniques(query, categories)`, `getTechnique(id)`, `getQuizCandidates(...)`, `observeContentState()`.
- [ ] Implementáció `internal`, Hilt köti be (T-203).
- [ ] Entity → domain mapperek; a `retired = true` rekordok alapból kiszűrve.
- [ ] `Flow` olvasásra, `suspend` pontszerű lekérdezésre.

## Kész, ha

- [ ] A publikus API-ban nem szerepel Room entitás, DAO vagy DTO típus (L4).
- [ ] A `retired` szűrés a repositoryban van, nem a hívónál.

## Figyelj rá

- A `retired` szűrés elfelejtése csendes hiba lenne (visszavont technikák megjelennének) – ezért a repository felelőssége, nem a UI-é.

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
