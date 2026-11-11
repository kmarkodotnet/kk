# T-204 · core/ui: TechniqueIllustration

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-07, US-08 |
| Modul | `core/ui` |
| Méret | M (3–6 ó) |
| Függ | [T-102](T-102-core-model-technique-category-illustrationref.md) |
| Követelmény | CR-05, NFR-04 |
| Teszt | — |

## Cél

Az illusztráció megjelenítése mindhárom esetére, helyes akadálymentes leírással.

## Teendők

- [ ] `Asset` → a kép; `Placeholder` → semleges helyőrző + „Ábra készül”; `null` → nincs képblokk.
- [ ] `contentDescription` paraméterezhető: a Tudástárban a technika neve, a kvízopcióknál **pozíció alapú** leírás.
- [ ] Képarány megtartása, `max-width` a konténerhez; nagy képeknél méretezés.
- [ ] Jelzés, ha a kép szakmai ellenőrzésre vár.

## Kész, ha

- [ ] A kvízopciónál használt leírás **nem tartalmazza** a technika nevét (NFR-04).
- [ ] `@Preview` mindhárom esetre, világos és sötét témában.

## Figyelj rá

- A `contentDescription` a legkönnyebben elrontható akadálymentességi pont: a (D) kvízopciónál a „Kiba dachi illusztrációja” leírás TalkBackkel elárulná a választ.

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
