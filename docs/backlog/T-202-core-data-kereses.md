# T-202 · core/data: keresés

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-06 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-201](T-201-core-data-techniquerepository.md) |
| Követelmény | FR-03 |
| Teszt | — |

## Cél

Ékezet- és kisbetű-érzéketlen keresés romajira, magyar névre és aliasokra egyszerre.

## Teendők

- [ ] A normalizáló függvény **ugyanaz**, amit a validátor használ (T-105) – egy helyen definiálva.
- [ ] A keresés az előre normalizált `search_romaji` / `search_hungarian` oszlopokon és a `technique_alias.aliasNormalized`-en fut.
- [ ] Részszó-egyezés (`LIKE '%...%'`), nem csak prefix.
- [ ] Alias-találat esetén a találat jelzi, melyik alias egyezett (a UI kiírja).

## Kész, ha

- [ ] „zart labu” megtalálja a „Zárt lábú állás”-t; „DACHI” megtalálja az összes `… dachi` technikát.
- [ ] A keresés nem blokkolja a UI szálat.

## Figyelj rá

- Az `aliases` jelenleg minden rekordnál üres, ezért az alias-ág tesztje szintetikus adattal készül (R-05 kockázat).

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
