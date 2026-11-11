# T-602 · TR-07: rétegszabály-ellenőrzés automatizálva

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-110](T-110-core-quiz-seedelt-determinizmus.md) |
| Követelmény | NFR-03, NFR-07 |
| Teszt | TR-07 |

## Cél

A rétegszabály és a determinizmus betartásának gépi ellenőrzése.

## Teendők

- [ ] Teszt, amely a `core/model` és `core/quiz` forrásaiban tiltott importokat keres (`android.`, `androidx.`, `room`, `compose`).
- [ ] Teszt a tiltott hívásokra: `Random.Default`, `System.currentTimeMillis`, `Instant.now`, `Math.random`.
- [ ] A `feature/*` modulokban Room entitás / DAO / DTO típus tiltása.

## Kész, ha

- [ ] A teszt elbukik, ha valaki véletlenül behúz egy tiltott importot.

## Figyelj rá

- Ez a szabály önmagában nem tartja meg magát: a `core/model` és `core/quiz` Kotlin JVM modul volta a fordítási idejű védelem, ez a teszt a maradék esetekre való.

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
