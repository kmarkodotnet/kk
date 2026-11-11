# T-501 · core/data: aggregált statisztika lekérdezések

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-17, US-18 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-401](T-401-core-data-progressrepository-kvizresz.md) |
| Követelmény | FR-23, FR-24, FR-25, FR-26, FR-27, DR-09 |
| Teszt | — |

## Cél

A haladás számai – **számolva**, nem redundáns számlálókból.

## Teendők

- [ ] Összesített: megválaszolt, helyes.
- [ ] Kategóriánkénti: `GROUP BY categoryId`, próbálkozásszámmal.
- [ ] Technikánkénti: próbálkozás, helyes, utolsó gyakorlás ideje, kérdéstípus-bontás.
- [ ] „Gyakorlást igénylő”: sikerarány növekvő, majd utolsó gyakorlás régisége; csak ≥ 1 próbálkozással; limit 10.
- [ ] Üres eset: a lekérdezés `null`-t vagy üres listát ad, nem 0-t.

## Kész, ha

- [ ] Nincs külön számláló tábla vagy mező, amit frissíteni kellene (DR-09).
- [ ] A lekérdezések `Flow`-t adnak, és nem a fő szálon futnak.

## Figyelj rá

- A „nincs adat” és a „0%” megkülönböztetése már az adatrétegben kezdődik: a `null` arány végigmegy a `QuizScoring`-on a UI-ig (FR-27).

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
