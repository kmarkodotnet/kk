# T-405 · feature/quiz: QuizRunState

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-15 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-403](T-403-feature-quiz-beallito-viewmodel.md) |
| Követelmény | FR-19, NFR-07, ADR-007 |
| Teszt | TR-04 |

## Cél

A futó kvíz állapota, ami túléli a konfigurációváltást.

## Teendők

- [ ] `QuizRunState(sessionId, seed, questions, currentIndex, answers, shortfall)` – szerializálva a `SavedStateHandle`-be.
- [ ] Az `answers` map a megválaszoltság **egyetlen igazsága**.
- [ ] Visszaállításkor nincs újragenerálás; ha mégis kell, a seed azonos tervet ad.

## Kész, ha

- [ ] Forgatás után ugyanaz a kérdés, **ugyanaz az opciósorrend**, megőrzött válaszokkal.

## Figyelj rá

- A `questions` csak ID-ket tartalmaz (ADR-006), ezért az állapot kicsi és tartalomfrissítés után is helyes nevet jelenít meg.

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
