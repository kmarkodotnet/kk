# T-507 · feature/progress: beállítások

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-19 |
| Modul | `feature/progress` |
| Méret | M (3–6 ó) |
| Függ | [T-502](T-502-core-data-elorehaladas-torlese.md), [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-35, NFR-09 |
| Teszt | — |

## Cél

Tartalominformáció és adatkezelés.

## Teendők

- [ ] Tartalominfó: `schema_version`, `dataset_version`, `editorial_status`, rekordszám, kategóriaszám.
- [ ] „Előrehaladás törlése”: **kétlépcsős** megerősítés, pontos felsorolással, hogy mi törlődik.
- [ ] Verzió- és licencinformáció.

## Kész, ha

- [ ] A megerősítő szöveg kimondja, hogy a művelet nem visszavonható.
- [ ] A törlés nem érinti a tudástárat.

## Figyelj rá

- Az adatvédelmi ígéret (NFR-09) része, hogy a felhasználó törölni tudja, amit gyűjtöttünk róla – még ha az sosem hagyta el az eszközt.

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
