# T-206 · navigation: NavHost és bottom navigation

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05 |
| Modul | `navigation` |
| Méret | M (3–6 ó) |
| Függ | [T-205](T-205-core-ui-jelolok-es-allapotkomponensek.md) |
| Követelmény | NFR-03 |
| Teszt | TR-06 |

## Cél

A négy fő cél és a képernyők közti váltás.

## Teendők

- [ ] Útvonalak: `library/list`, `library/detail/{id}`, `learn/setup`, `learn/cards`, `quiz/setup`, `quiz/run/{sessionId}`, `quiz/result/{sessionId}`, `progress/overview`, `progress/technique/{id}`, `settings`.
- [ ] Bottom nav: Tudástár, Tanulás, Kvíz, Haladás – ikonnal és magyar felirattal.
- [ ] A bottom nav **elrejtve** a `quiz/run` és `quiz/result` képernyőn.
- [ ] Az `app` modul `KyokushinApp()` composable-je ezt használja a Fázis 1 helyőrző helyett.

## Kész, ha

- [ ] Mind a négy cél elérhető, az állapot megmarad fülváltáskor.
- [ ] A kvíz futás közben nem hagyható el véletlenül.

## Figyelj rá

- Ez a task cseréli le a Fázis 1-ben készült ideiglenes kezdőképernyőt.

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
