# T-503 · feature/progress: áttekintő képernyő

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-17 |
| Modul | `feature/progress` |
| Méret | L (6–12 ó) |
| Függ | [T-501](T-501-core-data-aggregalt-statisztika-lekerdezesek.md), [T-206](T-206-navigation-navhost-es-bottom-navigation.md) |
| Követelmény | FR-23, FR-24, FR-27 |
| Teszt | TR-06 |

## Cél

A Haladás fő képernyője.

## Teendők

- [x] Összesítő kártya: megválaszolt, helyes, sikerarány.
- [x] Nulla próbálkozás → „Még nincs adat. Indíts egy kvízt!” + gomb.
- [x] Kategóriánkénti bontás sávval és **szöveges** értékkel.
- [x] Próbálkozás nélküli kategória → „nincs adat”, nem 0%.

## Kész, ha

- [x] Sehol nem jelenik meg 0% ott, ahol valójában nincs adat.
- [x] A sávok értéke felolvasható (NFR-04).

## Figyelj rá

- A 0% és a „nincs adat” összekeverése demotiváló és félrevezető – ez az FR-27 lényege.

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
| Commit | (lásd a következő, dokumentációs commit) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 587 teszt, 0 hiba (578 → 587, +9: `ProgressMessagesTest` 4, `ProgressOverviewViewModelTest` 5) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk` (`app/build/outputs/apk/debug/`) |

**Megjegyzés:** a képernyő szándékosan csak a kerete (összesítő kártya + kategóriánkénti
bontás, FUNCTIONAL_SPEC 6.1/1–3.) – a „Gyakorlást igénylő" lista (T-504, FR-26) és az
önértékelési blokk (T-505, FR-28) hiányzik, ahogy a `docs/backlog/GROUPS.md` G-10
csoportja előírja. A navigáció bekötve: a `KyokushinNavHost` `progress/overview` ága
mostantól `ProgressOverviewRoute`-ot hívja (nem helyőrzőt); az „Alul: Beállítások link"
(FUNCTIONAL_SPEC 6.1/6.) is bekötve a `settings` (még helyőrző) útvonalra. Az ADR-010
táblázatának „Haladás" sora (a statisztika fejlécében lektorálatlanság-szalag) ezzel a
feladattal lett lezárva – korábban nem volt bekötve. Új CONVENTIONS-sor a „próbálkozás
nélküli kategóriánál nincs sáv" döntésről (FR-27 accessibility-csapda).
