# T-303 · feature/learn: kártyaállapot

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-302](T-302-feature-learn-tanulas-beallitasa.md) |
| Követelmény | FR-09, NFR-07 |
| Teszt | — |

## Cél

A kártyasorrend és a pozíció megőrzése.

## Teendők

- [ ] Seedelt keverés a munkamenet elején, egyszer sorsolva.
- [ ] A sorrend, az aktuális index és a **kártyánkénti felfedettség** `SavedStateHandle`-ben.
- [ ] Előző/következő léptetés határokkal.

## Kész, ha

- [ ] Forgatás után ugyanaz a kártya, ugyanazzal a sorrenddel és felfedettséggel.

## Figyelj rá

- A felfedettség kártyánként tárolandó: visszalépéskor a már felfedett kártya felfedve marad.

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
