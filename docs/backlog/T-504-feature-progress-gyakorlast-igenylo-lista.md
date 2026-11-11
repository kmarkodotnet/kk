# T-504 · feature/progress: gyakorlást igénylő lista

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-18 |
| Modul | `feature/progress` |
| Méret | M (3–6 ó) |
| Függ | [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-26 |
| Teszt | — |

## Cél

A gyenge pontok kiemelése.

## Teendők

- [ ] Legfeljebb 10 technika, a T-501 rendezése szerint.
- [ ] Tételenként: romaji, magyar név, kis ábra, sikerarány, utolsó gyakorlás.
- [ ] Tétel kattintható → technika-részletező.
- [ ] Üres eset: „Még nincs elég adat a javaslathoz.”

## Kész, ha

- [ ] Csak legalább egy próbálkozással rendelkező technika kerül be.

## Figyelj rá

- A még nem gyakorolt technika nem „gyenge pont” – azt a kvíz fogja behozni.

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
