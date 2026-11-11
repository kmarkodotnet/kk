# T-304 · feature/learn: kártya UI

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09, US-10 |
| Modul | `feature/learn` |
| Méret | L (6–12 ó) |
| Függ | [T-303](T-303-feature-learn-kartyaallapot.md), [T-204](T-204-core-ui-techniqueillustration.md) |
| Követelmény | FR-07, FR-08 |
| Teszt | TR-06 |

## Cél

A tanulókártya megjelenítése és felfedése.

## Teendők

- [ ] Felső sáv: „`{n}` / `{összes}`”, kategória, bezárás.
- [ ] Kártya: ábra, a nem elrejtett név nagy betűvel, az elrejtett helyén „Koppints a felfedéshez”.
- [ ] Felfedés után **mindhárom** látszik: romaji + magyar név + ábra.
- [ ] Alsó sáv: Előző / Következő.

## Kész, ha

- [ ] `stateDescription`: felfedés előtt „elrejtve”, utána „felfedve”.
- [ ] Nagy érintési felület a felfedéshez (≥ 48 dp).

## Figyelj rá

- A felfedés explicit interakció legyen – a véletlen felfedés elrontja a tanulást (FR-07).

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
