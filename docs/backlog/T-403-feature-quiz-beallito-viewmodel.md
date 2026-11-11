# T-403 · feature/quiz: beállító ViewModel

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-12 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-111](T-111-core-quiz-dryrun-keszenleti-osszegzes.md), [T-402](T-402-core-data-kvizjeloltek-lekerdezese.md) |
| Követelmény | FR-11, FR-12, FR-13, FR-22 |
| Teszt | TR-03 |

## Cél

A kvízkonfiguráció és az élő készenléti összegzés.

## Teendők

- [ ] Kategóriák, kérdésszám (5/10/20), kérdéstípusok állapota.
- [ ] Minden változtatásra `dryRun()` futtatása és az összegzés frissítése.
- [ ] Típusonkénti letiltás + indoklás a `SkipReason` alapján.
- [ ] Indítás: seed sorsolása az injektált RNG-ből, `QuizSession` létrehozása, `QuizPlan` generálása.

## Kész, ha

- [ ] Ha a `dryRun` 0 kérdést ad, az indító gomb letiltva és 3 konkrét javaslat jelenik meg.
- [ ] A kiírt kérdésszám megegyezik azzal, amit a kvíz ténylegesen ad.

## Figyelj rá

- Itt tilos a `dryRun`-t „optimistán” kerekíteni: a beállító képernyő ígérete kötelező érvényű.

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
