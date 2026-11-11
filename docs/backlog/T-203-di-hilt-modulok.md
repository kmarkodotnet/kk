# T-203 · di: Hilt modulok

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05 |
| Modul | `di` |
| Méret | M (3–6 ó) |
| Függ | [T-201](T-201-core-data-techniquerepository.md) |
| Követelmény | NFR-03, NFR-07 |
| Teszt | — |

## Cél

A kötések egy helyen – köztük a tesztelhetőség két feltétele: injektált `Clock` és RNG.

## Teendők

- [ ] `DatabaseModule` (`AppDatabase`, DAO-k), `RepositoryModule` (`@Binds` interfész → impl).
- [ ] `QuizModule` (`QuizGenerator`, `KnowledgeValidator`, `RandomFactory`).
- [ ] `TimeModule`: `Clock` – prod rendszeróra, teszt fix óra.
- [ ] `DispatcherModule`: `@IoDispatcher`, `@DefaultDispatcher` qualifierek.

## Kész, ha

- [ ] Az app elindul, a Hilt gráf felépül.
- [ ] Üzleti kódban sehol nincs közvetlen `Clock.systemUTC()` vagy `Random.Default` példányosítás.

## Figyelj rá

- A dispatcher injektálása nélkül a ViewModel tesztek nem lennének determinisztikusak.

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
