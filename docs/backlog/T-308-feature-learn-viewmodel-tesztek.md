# T-308 · feature/learn: ViewModel tesztek

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09, US-10, US-11 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-306](T-306-feature-learn-onertekeles.md) |
| Követelmény | FR-07, FR-09, FR-10 |
| Teszt | — |

## Cél

A tanulási logika lefedése.

## Teendők

- [ ] Seedelt sorrend determinizmusa.
- [ ] Felfedési állapot megőrzése léptetés és visszalépés után.
- [ ] Önértékelés rögzítése és az automatikus továbblépés.
- [ ] Feliratos képnél a felfedés előtti elrejtés.

## Kész, ha

- [ ] Fake repository és fix `Clock`; a tesztek determinisztikusak.

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
