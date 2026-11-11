# T-410 · feature/quiz: előrehaladás és megszakítás

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-15 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-405](T-405-feature-quiz-quizrunstate.md) |
| Követelmény | FR-16, FR-19 |
| Teszt | TR-06 |

## Cél

A kvíz elhagyásának kezelt útja.

## Teendők

- [ ] Lineáris előrehaladásjelző és „`{index}` / `{összes}`”.
- [ ] Bezárás (×) → megerősítő párbeszéd: „Megszakítod a kvízt? Az eddigi válaszaid megmaradnak.”
- [ ] A rendszer vissza-gombja ugyanezt váltja ki.
- [ ] Megszakításkor `finishedAt` marad `null`, `answeredCount` a tényleges érték.
- [ ] A kérdések között nincs visszalépés.

## Kész, ha

- [ ] A megszakított kvíz válaszai beszámítanak a Haladásba.

## Figyelj rá

- A megerősítés nélküli kilépés a leggyakoribb adatvesztési panasz forrása – ezért explicit.

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
