# T-606 · TR-06: AVD és UI tesztek

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | `app` |
| Méret | L (6–12 ó) |
| Függ | [T-412](T-412-feature-quiz-hibak-ujragyakorlasa.md), [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-06…FR-10, FR-16…FR-20 |
| Teszt | TR-06 |

## Cél

A Compose UI tesztek futtatása emulátoron. **Ez oldja fel a B-03 blokkolót.**

## Teendők

- [x] AVD létrehozása (`android sdk install system-images/...`, `avdmanager create avd`).
- [x] UI teszt: a négy fő navigációs cél elérése.
- [x] UI teszt: tanulókártya felfedése és önértékelés.
- [x] UI teszt: egy teljes kvíz végigvitele az összegzésig.
- [x] `./gradlew connectedDebugAndroidTest` futtatása és a kimenet rögzítése.

## Kész, ha

- [x] A tesztek ténylegesen lefutnak, vagy a `TEST_REPORT.md` pontosan dokumentálja, miért nem.
- [x] A `docs/ENVIRONMENT.md` B-03 blokkolója lezárva.

## Figyelj rá

- Ha az emulátor nem indul (virtualizáció, hely), az nem elhallgatandó: dokumentált blokkoló marad.

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

Meglévő API 37 AVD használatba véve; B-03 lezárva. Hat ténylegesen lefutott eszközteszt, navigáció, kártya, teljes kvíz, kategóriaátadás, nagy betű, TalkBack.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest assembleDebug lint ktlintCheck connectedDebugAndroidTest --console=plain` | 0 | 647 unit teszt, 6 eszközteszt, 0 hiba; debug APK, lint és formázás sikeres |
| `.\gradlew.bat :app:connectedDebugAndroidTest ktlintCheck --console=plain` | 0 | Végső kártyaforgatási kiegészítéssel is 6 eszközteszt, 0 hiba |

Részletes bizonyítékok és NEM FUTOTT tételek: [TEST_REPORT](../TEST_REPORT.md),
[elfogadási jegyzőkönyv](../ACCEPTANCE.md). Nincs új Clock/Random hívás,
repository-réteghatár nem sérült; a meglévő magyar strings.xml és világos/sötét
Preview-k megmaradtak. A teszt-only magyar feliratok a valós UI-folyamatot azonosítják.
