# T-414 · feature/quiz: ViewModel tesztek

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-13, US-15, US-16 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-412](T-412-feature-quiz-hibak-ujragyakorlasa.md) |
| Követelmény | FR-18, FR-19, FR-21, FR-22 |
| Teszt | TR-04 |

## Cél

A kvíz UI-logikájának lefedése emulátor nélkül.

## Teendők

- [x] Dupla válasz → egy `QuizAttempt`, változatlan eredmény. *(már lefedve a G-07
  csoportban, `QuizRunViewModelTest`.)*
- [x] `SavedStateHandle` visszaállítás → azonos kérdés és opciósorrend. *(már lefedve
  a G-07 csoportban, `QuizRunViewModelTest`.)*
- [x] Megszakítás → `finishedAt == null`, a válaszok megmaradnak. *(a zéró-válaszos ág
  már lefedve a G-09 csoportban; a RÉSZLEGES válaszos ág ebben a feladatban készült el:
  `quizResult_abortedSessionWithPartialAnswers_scoresOnlyTheAnsweredQuestions_FR19`.)*
- [x] Shortfall és 0 kérdés esetén a helyes UI állapot. *(ebben a feladatban készült
  el: `quizRun_zeroQuestionShortfallPlan_showsEmpty_FR22`,
  `quizRun_content_withShortfallPlan_showsActualQuestionCount_FR22`.)*
- [x] Újragyakorlás → új `sessionId`, új seed, a régi eredmény változatlan. *(az új
  seed/esemény már lefedve a T-412-ben; a sessionId-különbség és a régi eredmény
  változatlansága ebben a feladatban kapott explicit asszertációt a meglévő
  `quizResult_onRetryWrong_withFourDistinctWrongTechniques_startsNewSessionWithNewSeed_FR21`
  tesztben.)*

## Kész, ha

- [x] Fake repository, fix `Clock`, rögzített seed; a tesztek determinisztikusak.

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
| Commit | `e22e78e` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL in 1m 30s`, teljes projekt, 294 actionable task, 0 hiba – lásd `docs/TEST_REPORT.md` T-414 bejegyzése. |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` elkészült. |

**Megjegyzés:** A feladatot egy korábbi kör a Claude API session rate limitje miatt
commit előtt félbehagyta; ez a kör nem kezdte újra, csak a `git diff`-fel felmért,
valóban hiányzó négy esetet pótolta (lásd a „Teendők” pontjainál a hivatkozott
tesztneveket) – a G-07/G-09 csoportok meglévő tesztjeit nem duplikálta. Részletek:
`docs/TEST_REPORT.md` „2026-11-12 – T-414 kész” bejegyzése, `docs/CONVENTIONS.md`.
