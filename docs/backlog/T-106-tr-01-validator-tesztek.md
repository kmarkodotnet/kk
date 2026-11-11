# T-106 · TR-01: validátor tesztek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-01 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-105](T-105-core-quiz-a-validator-implementacioja.md) |
| Követelmény | DR-05, FR-31 |
| Teszt | TR-01 |

## Cél

A validátor minden szabályának lefedése. **Ez az első valódi zöld tesztfuttatás a projektben.**

## Teendők

- [ ] Táblázatos (parametrizált) tesztkészlet minden `V` kódra, mindegyik a kód nevét viseli.
- [ ] Külön teszt: érvénytelen bemenet **nem dob** kivételt.
- [ ] Külön teszt: a validátor nem emel státuszt (`AWAITING_REVIEW` bemenet → sosem `VERIFIED` kimenet).
- [ ] Teszt a valódi tartalomra: a `knowledgebase_transfer/` 161 rekordja 0 BLOKKOLÓ hibával megy át.

## Kész, ha

- [ ] `./gradlew testDebugUnitTest` **exit 0**, és a kimenet már NEM `NO-SOURCE`.
- [ ] A `docs/TEST_REPORT.md` frissítve a tényleges teszt- és hibaszámmal.

## Figyelj rá

- A tesztnevek tartalmazzák a `TR01` jelölést, pl. `validate_duplicateId_isRejected_TR01()`.
- A valódi tartalomra futó teszt védi meg a projektet attól, hogy a validátor csak szintetikus adaton működjön.

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
