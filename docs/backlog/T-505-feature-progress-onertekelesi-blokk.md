# T-505 · feature/progress: önértékelési blokk

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-11, US-17 |
| Modul | `feature/progress` |
| Méret | S (1–3 ó) |
| Függ | [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-28 |
| Teszt | — |

## Cél

A tanulókártya-önértékelés elkülönített megjelenítése.

## Teendők

- [ ] Külön, vizuálisan elhatárolt szakasz: „Tudtam: `{n}` · Gyakorolnom kell: `{m}`”.
- [ ] Magyarázó sor: „Az önértékelés nem számít bele a kvíz sikerarányába.”

## Kész, ha

- [ ] Az önértékelés számai sehol nem keverednek a kvízstatisztikával.

## Figyelj rá

- A magyarázó sor nem díszítés: enélkül a felhasználó joggal hinné, hogy a két szám összefügg.

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
