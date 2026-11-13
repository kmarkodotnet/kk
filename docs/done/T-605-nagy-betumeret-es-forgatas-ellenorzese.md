# T-605 · Nagy betűméret és forgatás ellenőrzése

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-20 |
| Modul | minden feature |
| Méret | M (3–6 ó) |
| Függ | [T-604](T-604-akadalymentessegi-atfesules.md) |
| Követelmény | NFR-04, NFR-06 |
| Teszt | — |

## Cél

A layout ne törjön extrém beállításoknál.

## Teendők

- [x] A rendszer betűméretének maximumra állítása, a négy fő képernyő átnézése.
- [x] Forgatás minden képernyőn, különös tekintettel a kvízre és a tanulókártyára.
- [x] Keskeny képernyő (≈400 dp) ellenőrzése.

## Kész, ha

- [x] Nincs levágott szöveg, nincs átfedő elem, nincs elveszett állapot.

## Figyelj rá

- A kvíz opciógombjai a legérzékenyebbek: hosszú magyar név + nagy betűméret.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [x] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-13 |
| Commit | (a lezáró commit után rögzítve) |

200%-os font_scale, kb. 411 dp álló szélesség; négy fő cél és kártya/kvíz forgatása. Szövegmérés és screenshotok, görgethető elrendezések, mentett állapot ellenőrizve.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest assembleDebug lint ktlintCheck connectedDebugAndroidTest --console=plain` | 0 | 647 unit teszt, 6 eszközteszt, 0 hiba; debug APK, lint és formázás sikeres |
| `.\gradlew.bat :app:connectedDebugAndroidTest ktlintCheck --console=plain` | 0 | Végső kártyaforgatási kiegészítéssel is 6 eszközteszt, 0 hiba |

Részletes bizonyítékok és NEM FUTOTT tételek: [TEST_REPORT](../TEST_REPORT.md),
[elfogadási jegyzőkönyv](../ACCEPTANCE.md). Nincs új Clock/Random hívás,
repository-réteghatár nem sérült; a meglévő magyar strings.xml és világos/sötét
Preview-k megmaradtak. A teszt-only magyar feliratok a valós UI-folyamatot azonosítják.
