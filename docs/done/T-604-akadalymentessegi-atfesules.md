# T-604 · Akadálymentességi átfésülés

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-20 |
| Modul | minden feature |
| Méret | L (6–12 ó) |
| Függ | [T-413](T-413-feature-quiz-akadalymentesseg.md), [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | NFR-04 |
| Teszt | TR-06 |

## Cél

Végigmenni a négy fő képernyőn akadálymentességi szemmel.

## Teendők

- [x] Minden illusztráció `contentDescription`-jének ellenőrzése – különös tekintettel a kvízopciókra.
- [x] Érintési célpontok ≥ 48 dp.
- [x] TalkBack bejárási sorrend képernyőnként.
- [x] Kontraszt WCAG AA világos és sötét témában.
- [x] A helyesség/állapot nem csak színnel jelenik meg.

## Kész, ha

- [x] Egy teljes kvíz végigvihető TalkBackkel, a válasz felfedése nélkül.
- [x] A `FUNCTIONAL_SPEC` 9. fejezetének minden sora teljesül.

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
| Commit | `c8addec` |

WCAG-palettatesztek, szemantika- és célpontaudit, tényleges TalkBack szolgáltatással futó teljes accessibility kvízpróba. Az emberi felolvasásértékelés külön korlát, lásd TEST_REPORT.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest assembleDebug lint ktlintCheck connectedDebugAndroidTest --console=plain` | 0 | 647 unit teszt, 6 eszközteszt, 0 hiba; debug APK, lint és formázás sikeres |
| `.\gradlew.bat :app:connectedDebugAndroidTest ktlintCheck --console=plain` | 0 | Végső kártyaforgatási kiegészítéssel is 6 eszközteszt, 0 hiba |

Részletes bizonyítékok és NEM FUTOTT tételek: [TEST_REPORT](../TEST_REPORT.md),
[elfogadási jegyzőkönyv](../ACCEPTANCE.md). Nincs új Clock/Random hívás,
repository-réteghatár nem sérült; a meglévő magyar strings.xml és világos/sötét
Preview-k megmaradtak. A teszt-only magyar feliratok a valós UI-folyamatot azonosítják.
