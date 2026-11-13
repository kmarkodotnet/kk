# T-609 · docs/QUIZ_RULES.md kiadott változata

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | `docs` |
| Méret | M (3–6 ó) |
| Függ | [T-112](T-112-tr-02-tr-03-generator-tesztek.md) |
| Követelmény | FR-12…FR-22, CR-07 |
| Teszt | — |

## Cél

A kvízszabályok dokumentálása a **tényleges** motorhoz mérve.

## Teendők

- [ ] A vázlat 7 fejezetének kitöltése a megvalósított `QuizGenerator` alapján.
- [ ] Konkrét példák: distraktorválasztás, fallback, seed-viselkedés.
- [ ] A `dryRun` ↔ `generate` egyezés szabálya.
- [ ] A képkapuk (`quiz_usable`) hatása.

## Kész, ha

- [ ] A dokumentum minden állítása visszavezethető egy létező tesztre (T-112).
- [ ] `/kyo-doc-sync` nem talál eltérést a dokumentum és a kód között.

## Figyelj rá

- A dokumentum csak az implementáció után íródhat meg hitelesen – ezért van a végén.

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

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-13 |
| Commit | 5f46b6f |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 642 teszt, 0 hiba (változatlan – tisztán dokumentációs módosítás) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk`, `BUILD SUCCESSFUL` |

**Megjegyzés:** A `docs/QUIZ_RULES.md` a tényleges `DefaultQuizGenerator`/
`DistractorSelector`/`QuizCandidateFilter`/`QuizEligibility`/`QuizScoring` kódot írja
le, minden állítást egy létező, zöld teszthez kötve (`DefaultQuizGeneratorTest`,
`DistractorSelectorTest`, `RealContentQuizGenerationTest`, `QuizPlanInvariantTest`,
`QuizScoringTest`/`QuizScoringTr04Test` – mind a T-112/T-113/T-114 alatt készült). A
`/kyo-doc-sync` parancs egy audit-utasítás, nem determinisztikus check – az
összevetést a dokumentum megírása közben, forrásról forrásra végeztem el. Rögzített,
kódból levezetett, korábban dokumentálatlan tény: a `DatasetTrust` mindkét értéke
átengedi a `QuizEligibility` kaput, tehát a lektoráltsági szint ma nem zár ki
technikát a kvízből, csak a UI szalagját vezérli. Nincs új teszt (a task fejléce
szerint `Teszt | —`).
