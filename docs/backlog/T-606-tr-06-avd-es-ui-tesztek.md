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

- [ ] AVD létrehozása (`android sdk install system-images/...`, `avdmanager create avd`).
- [ ] UI teszt: a négy fő navigációs cél elérése.
- [ ] UI teszt: tanulókártya felfedése és önértékelés.
- [ ] UI teszt: egy teljes kvíz végigvitele az összegzésig.
- [ ] `./gradlew connectedDebugAndroidTest` futtatása és a kimenet rögzítése.

## Kész, ha

- [ ] A tesztek ténylegesen lefutnak, vagy a `TEST_REPORT.md` pontosan dokumentálja, miért nem.
- [ ] A `docs/ENVIRONMENT.md` B-03 blokkolója lezárva.

## Figyelj rá

- Ha az emulátor nem indul (virtualizáció, hely), az nem elhallgatandó: dokumentált blokkoló marad.

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
