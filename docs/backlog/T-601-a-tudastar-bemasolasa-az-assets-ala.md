# T-601 · A tudástár bemásolása az assets alá

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-04 |
| Modul | `app` |
| Méret | S (1–3 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md) |
| Követelmény | FR-29, AC-08 |
| Teszt | — |

## Cél

A `knowledgebase_transfer/` tartalmának beillesztése az alkalmazásba.

## Teendők

- [ ] `manifest.json`, `entries.json` és az `illustrations/` bemásolása `app/src/main/assets/knowledge/` alá.
- [ ] Build-ellenőrzés: az APK tartalmazza a fájlokat, és az import lefut.
- [ ] A másolás módja dokumentálva (kézi másolás vagy Gradle task).

## Kész, ha

- [ ] `assembleDebug` után az app elindul és betölti a 161 rekordot.
- [ ] Az APK mérete elfogadható a 10 képpel.

## Figyelj rá

- A `knowledgebase/` marad a szerkesztési forrás, a `knowledgebase_transfer/` a generált átadási forma – az assets ennek a másolata.

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
