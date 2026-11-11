# T-118 · TR-05: tartalomfrissítés tesztek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-04 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md) |
| Követelmény | FR-33, DR-06, DR-07, DR-08 |
| Teszt | TR-05 |

## Cél

A projekt egyik legfontosabb garanciája: a frissítés nem viszi el az előrehaladást.

## Teendők

- [ ] In-memory Room; v1 tartalom betöltése, próbálkozások rögzítése, v2 betöltése.
- [ ] Ellenőrzés: a próbálkozások száma és a számolt statisztika változatlan.
- [ ] Átnevezett technika: az `id` marad, az előrehaladás követi, az új név jelenik meg.
- [ ] Visszavont technika: `retired = true`, a próbálkozásai feloldhatók maradnak.
- [ ] Idempotencia: ugyanaz a verzió kétszer → nincs duplikáció.

## Kész, ha

- [ ] Minden fenti eset lefedve, `TR05` jelöléssel a tesztnevekben.
- [ ] `./gradlew testDebugUnitTest` exit 0.

## Figyelj rá

- Ez a teszt Robolectricet vagy in-memory Roomot igényel, tehát lassabb, mint a tiszta JVM tesztek – külön forráskészletbe is tehető.

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
