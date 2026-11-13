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

- [x] `README.md`: előfeltételek, futtatás, build, használat, **ismert korlátok**.
- [x] `docs/TEST_REPORT.md` lezárása: minden tényleges futtatás, és ami nem futott, annak az oka.
- [x] `./gradlew assembleDebug` → a debug APK pontos útvonala és mérete.
- [x] Az AC-01…AC-08 elfogadási kritériumok egyenkénti végigellenőrzése, bizonyítékkal.
- [x] A következő három legfontosabb teendő megfogalmazása az éles tudástárhoz.

## Kész, ha

- [x] Minden elfogadási kritériumnál szerepel, hogy teljesül-e, és mivel igazolható.
- [x] Semmilyen build- vagy teszteredmény nincs leírva tényleges futtatás nélkül.

## Figyelj rá

- Az „ismert korlátok” szakasz őszintesége itt a legfontosabb: lektorálatlan tartalom, 3 feliratos kép, képes kvíz egyetlen kategóriában.

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

README, TEST_REPORT, ACCEPTANCE és ENVIRONMENT frissítve. APK útvonal, méret, SHA-256; AC-01…AC-08 egyenként rögzítve. AC-08 tartalmi kézi próbája részben igazoltként, nem kész tesztként szerepel.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest assembleDebug lint ktlintCheck connectedDebugAndroidTest --console=plain` | 0 | 647 unit teszt, 6 eszközteszt, 0 hiba; debug APK, lint és formázás sikeres |
| `.\gradlew.bat :app:connectedDebugAndroidTest ktlintCheck --console=plain` | 0 | Végső kártyaforgatási kiegészítéssel is 6 eszközteszt, 0 hiba |

Részletes bizonyítékok és NEM FUTOTT tételek: [TEST_REPORT](../TEST_REPORT.md),
[elfogadási jegyzőkönyv](../ACCEPTANCE.md). Nincs új Clock/Random hívás,
repository-réteghatár nem sérült; a meglévő magyar strings.xml és világos/sötét
Preview-k megmaradtak. A teszt-only magyar feliratok a valós UI-folyamatot azonosítják.
