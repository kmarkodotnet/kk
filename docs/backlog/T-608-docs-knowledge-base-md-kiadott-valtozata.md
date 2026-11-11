# T-608 · docs/KNOWLEDGE_BASE.md kiadott változata

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | `docs` |
| Méret | M (3–6 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md) |
| Követelmény | FR-30, CR-04, DR-08 |
| Teszt | — |

## Cél

A tudástár dokumentációjának véglegesítése a tényleges sémához mérve.

## Teendők

- [ ] A `BACKLOG`-ban vázolt 9 fejezet kitöltése az ADR-011 kanonikus sémája szerint.
- [ ] Új technika és új kép hozzáadásának lépésről lépésre leírása.
- [ ] A hitelesítési folyamat (ki és hogyan állít `reviewed`-ra).
- [ ] Licenctáblázat és a képek eredete.
- [ ] Migráció: `schema_version` emelés, átnevezés, visszavonás.

## Kész, ha

- [ ] A leírt folyamat alapján egy új technika hozzáadható **kódmódosítás nélkül** (AC-08).
- [ ] A dokumentum a tényleges `manifest.json` mezőit írja le, nem a tervezettet.

## Figyelj rá

- Nyitott kérdés marad: ki a szakmai hitelesítő (OQ-02) – ezt a felhasználónak kell megneveznie.

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
