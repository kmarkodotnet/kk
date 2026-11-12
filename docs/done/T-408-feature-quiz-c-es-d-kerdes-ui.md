# T-408 · feature/quiz: (C) és (D) kérdés UI

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-407](T-407-feature-quiz-a-es-b-kerdes-ui.md), [T-204](T-204-core-ui-techniqueillustration.md) |
| Követelmény | FR-12, FR-13, NFR-04 |
| Teszt | TR-06 |

## Cél

A két kép-alapú kérdéstípus megjelenítése.

## Teendők

- [x] (C): illusztráció a kérdésben + 4 romaji gomb.
- [x] (D): „Melyik ábra: **`{romaji}`**?” + 2×2 képrács.
- [x] A képrács elemei ≥ 48 dp érintési célpontok, kiválasztás jól látható.
- [x] A képek leírása **pozíció alapú** („első válaszlehetőség illusztrációja”).

## Kész, ha

- [x] A (D) opciók `contentDescription`-je nem tartalmazza a technika nevét.
- [x] Keskeny képernyőn (≈400 dp) is használható a rács.

## Figyelj rá

- Ez a két típus jelenleg csak az `stances` kategóriában aktiválható – de a kódnak készen kell állnia, mert a tartalom bővül.

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
| Dátum | 2026-11-12 |
| Commit | *(a következő commit rögzíti)* |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | 542 teszt, 0 hiba (535 → 542: +3 `core/ui` `IllustrationContentDescriptionTest`, +4 `feature/quiz` `QuizImageOptionGridTest`) |
| `./gradlew assembleDebug` | 0 | `app/build/outputs/apk/debug/app-debug.apk` elkészült |

**Megjegyzés:** A (C) kérdésábra akadálymentes leírásához a `core/ui` `IllustrationLabel`
sealed típusa (T-204) egy új, pozíció nélküli ággal bővült (`QuizPrompt`) – a meglévő
`Technique` (nevet mond) és `Decorative` (kvízben nem használható, mert a kép itt
információt hordoz) ágak nem fedték a helyzetet. Részletek: `docs/CONVENTIONS.md` 4.
szakasz új sora és `docs/TEST_REPORT.md` 2026-11-12 (T-408) bejegyzése. A
`quiz/setup` → `quiz/run` navigációs bekötés és a kvíz normál befejezése továbbra sem
ennek a feladatnak a tárgya (lásd `docs/TEST_REPORT.md` „NEM KÉSZÜLT EL" sorai).
