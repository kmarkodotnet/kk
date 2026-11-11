# T-208 · feature/library: lista UI

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06, US-08 |
| Modul | `feature/library` |
| Méret | L (6–12 ó) |
| Függ | [T-207](T-207-feature-library-lista-viewmodel.md), [T-204](T-204-core-ui-techniqueillustration.md) |
| Követelmény | FR-01, FR-02, FR-03, FR-05 |
| Teszt | TR-06 |

## Cél

A Tudástár listaképernyő.

## Teendők

- [ ] Keresősáv törlés ikonnal; vízszintesen görgethető kategóriachip-sor találatszámmal, „Összes” chippel.
- [ ] `LazyColumn` stabil `key`-jel; listatétel: kis ábra, romaji (félkövér), magyar név, hitelesítési chip.
- [ ] Alias-találatnál: „Egyezés: `{alias}`” sor.
- [ ] Rendezés: kategória `sort_order`, azon belül romaji ábécé.

## Kész, ha

- [ ] A listatétel egy TalkBack bejárási egység, és ≥ 64 dp magas.
- [ ] `@Preview` világos + sötét + üres állapot.

## Figyelj rá

- A találatszám a chipeken azért hasznos, mert a felhasználó előre látja, melyik kategóriában van anyag.

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
