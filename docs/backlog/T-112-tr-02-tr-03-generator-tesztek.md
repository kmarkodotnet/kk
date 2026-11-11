# T-112 · TR-02 + TR-03: generátor tesztek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-02, US-12 |
| Modul | `core/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-111](T-111-core-quiz-dryrun-keszenleti-osszegzes.md) |
| Követelmény | FR-13, FR-14, FR-22, CR-07, NFR-07 |
| Teszt | TR-02, TR-03 |

## Cél

A kvízmotor invariánsainak teljes lefedése. **Az M1 mérföldkő magja.**

## Teendők

- [ ] Invariánsok: 4 opció, 1 helyes, 3 különböző téves, nincs szinonima a tévesek közt.
- [ ] Determinizmus: azonos seed → azonos terv (kérdés- és opciósorrend is).
- [ ] Fallback: kevés jelölt → kevesebb kérdés `shortfall`-lal, nem hibás kérdés; 0 jelölt → üres terv.
- [ ] Kategóriaszűrés: szűrt kategórián kívüli technika nem jelenik meg sem helyesként, sem distraktorként.
- [ ] Típusjogosultság: feliratos/hiányzó kép esetén (C)/(D) kihagyva, `SkipReason`-nel.
- [ ] `dryRun` ↔ `generate` egyezés ugyanarra a kérésre.
- [ ] Teszt a valódi tartalomra: az `stances` kategória (C)/(D) kérdést ad, a többi nem.

## Kész, ha

- [ ] `./gradlew testDebugUnitTest` exit 0, minden fenti eset lefedve.
- [ ] `docs/TEST_REPORT.md` frissítve.

## Figyelj rá

- Ha egyetlen tesztcsomagot kell jól megírni ebben a projektben, ez az: a kvízhelyesség itt dől el.
- A tesztnevek `TR02` / `TR03` jelölést viselnek.

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
