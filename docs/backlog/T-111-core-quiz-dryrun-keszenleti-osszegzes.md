# T-111 · core/quiz: dryRun() készenléti összegzés

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-12 |
| Modul | `core/quiz` |
| Méret | S (1–3 ó) |
| Függ | [T-110](T-110-core-quiz-seedelt-determinizmus.md) |
| Követelmény | FR-13, FR-22 |
| Teszt | TR-03 |

## Cél

A kvízbeállító képernyő előre megmondja, hány kérdés állítható össze – ugyanazzal a motorral.

## Teendők

- [ ] `dryRun(request): QuizReadiness` – kérdésszám típusonként, kihagyott típusok okkal, várható `shortfall`.
- [ ] A `dryRun` ne generáljon teljes tervet, ha nem kell; de **ugyanazt a szűrő- és jogosultsági logikát** használja.

## Kész, ha

- [ ] A `dryRun` által ígért kérdésszám megegyezik azzal, amit a `generate()` ugyanarra a kérésre előállít.

## Figyelj rá

- Ha a `dryRun` és a `generate` eltérne, a beállító képernyő hazudna a felhasználónak. Ezért a két ág közös kódon osztozik, és erre külön teszt van (T-112).

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
