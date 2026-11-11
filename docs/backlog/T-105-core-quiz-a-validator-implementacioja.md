# T-105 · core/quiz: a validátor implementációja

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-01 |
| Modul | `core/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-104](T-104-core-quiz-a-validator-szerzodese.md) |
| Követelmény | DR-05, FR-31, CR-02, CR-07 |
| Teszt | TR-01 |

## Cél

A V001…V010 szabályok tényleges végrehajtása a kanonikus séma felett.

## Teendők

- [ ] `V001` támogatott `schema_version` (string semver, ismeretlen major → teljes elutasítás).
- [ ] `V002` kötelező mezők; `V003` egyedi `id`; `V004` létező `category_id`.
- [ ] `V005` képhivatkozás: hiányzó fájl → `Asset` helyett `Placeholder` lefokozás (nem rekordelvetés).
- [ ] `V006` szinonimacsoportok a normalizált `hungarian_name` / `japanese_romaji` / `aliases` alapján.
- [ ] `V007`/`V008` kvízképesség: `INCOMPLETE` mindig kizárva; a bizalmi kaput az ADR-010 formula adja.
- [ ] `V009` hiányzó ékezetmentes alak származtatása; `V010` parse-hiba kezelése.

## Kész, ha

- [ ] Egyetlen hibás rekord sem okoz kivételt, és nem dönti el az egész betöltést (kivéve V001/V010).
- [ ] A validátor **soha nem emel** státuszt: `AWAITING_REVIEW` → `VERIFIED` átmenet kódból lehetetlen.

## Figyelj rá

- A státuszemelés tilalma (CR-02) nem stílus kérdése: ez akadályozza meg, hogy lektorálatlan tartalom hitelesként jelenjen meg.
- A normalizálás (kisbetű, ékezettelenítés) ugyanaz a függvény legyen, amit a keresés használ (T-202) – egy helyen definiálva.

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
