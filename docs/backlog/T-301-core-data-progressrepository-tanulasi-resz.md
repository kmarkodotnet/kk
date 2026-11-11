# T-301 · core/data: ProgressRepository – tanulási rész

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-11 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-116](T-116-core-data-room-sema.md) |
| Követelmény | FR-10, FR-28, DR-04 |
| Teszt | — |

## Cél

Az önértékelés rögzítése, a kvízstatisztikától elkülönítve.

## Teendők

- [ ] `recordStudyReview(techniqueId, rating)` – injektált `Clock`-kal.
- [ ] `observeStudyReviewSummary()` – „Tudtam” / „Gyakorolnom kell” darabszám.
- [ ] A `study_review` tábla **sosem** keveredik a `quiz_attempt` aggregációiba.

## Kész, ha

- [ ] Az önértékelés nem jelenik meg a kvízsikerarányban (FR-28).

## Figyelj rá

- Ez a szétválasztás elfogadási kritérium: az önértékelés szubjektív, a kvíz objektív.

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
