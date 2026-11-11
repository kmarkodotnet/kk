# T-610 · Átadás: README, tesztjelentés, APK, elfogadási kritériumok

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | `docs` |
| Méret | M (3–6 ó) |
| Függ | [T-606](T-606-tr-06-avd-es-ui-tesztek.md) |
| Követelmény | NFR-11, NFR-12, AC-01…AC-08 |
| Teszt | — |

## Cél

A projekt lezárása és átadható állapotba hozása.

## Teendők

- [ ] `README.md`: előfeltételek, futtatás, build, használat, **ismert korlátok**.
- [ ] `docs/TEST_REPORT.md` lezárása: minden tényleges futtatás, és ami nem futott, annak az oka.
- [ ] `./gradlew assembleDebug` → a debug APK pontos útvonala és mérete.
- [ ] Az AC-01…AC-08 elfogadási kritériumok egyenkénti végigellenőrzése, bizonyítékkal.
- [ ] A következő három legfontosabb teendő megfogalmazása az éles tudástárhoz.

## Kész, ha

- [ ] Minden elfogadási kritériumnál szerepel, hogy teljesül-e, és mivel igazolható.
- [ ] Semmilyen build- vagy teszteredmény nincs leírva tényleges futtatás nélkül.

## Figyelj rá

- Az „ismert korlátok” szakasz őszintesége itt a legfontosabb: lektorálatlan tartalom, 3 feliratos kép, képes kvíz egyetlen kategóriában.

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
