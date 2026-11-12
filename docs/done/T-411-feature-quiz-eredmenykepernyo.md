# T-411 · feature/quiz: eredményképernyő

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-16 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-113](T-113-core-quiz-quizscoring.md), [T-410](T-410-feature-quiz-elorehaladas-es-megszakitas.md) |
| Követelmény | FR-20, FR-27 |
| Teszt | TR-04, TR-06 |

## Cél

A kvíz összegzése, őszinte számokkal.

## Teendők

- [x] „`{helyes}` / `{megválaszolt}` helyes” + százalék.
- [x] 0 válasz → „Nem válaszoltál egyetlen kérdésre sem.” (nincs 0%).
- [x] Megszakított kvíz → „Megszakított kvíz – `{megválaszolt}` / `{kért}` kérdés.”
- [x] Hibás kérdések listája: típus, helyes technika (romaji + magyar + kis ábra), a saját válasz; tétel kattintható.
- [x] Gombok: Hibák újragyakorlása / Új kvíz / Vissza a Haladáshoz.

## Kész, ha

- [x] A sikerarány szövegesen is kiolvasható, nem csak diagramon (NFR-04).

## Figyelj rá

- A százalék a `QuizScoring`-ból jön (T-113), nem a UI-ban újraszámolva.

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
| Commit | `TBD` (lásd a következő, kizárólag a hash rögzítésére irányuló commit) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | Teljes projekt, 0 hiba. Új: `QuizResultViewModelTest` (9), `QuizResultMessagesTest` (5), `RoomProgressRepositoryTest` +3, `QuizRunViewModelTest` +2 – lásd `docs/TEST_REPORT.md` G-09 bejegyzése. |
| `./gradlew assembleDebug` | 0 | `app/build/outputs/apk/debug/app-debug.apk` elkészült |

**Megjegyzés:** A G-09 csoport (T-411/T-412/T-415) egy menetben készült el – részletek
`docs/TEST_REPORT.md` 2026-11-12 (G-09) bejegyzésében. A `ProgressRepository` egy új,
**olvasó** taggal bővült (`getSessionResult`), és a `QuizRunViewModel.onNext()` immár
lezárja a menetet az utolsó kérdésen (`finishSession` + `finishEvent`) – ez a G-07-ben
dokumentáltan nyitva hagyott rész. A `quiz/run`/`quiz/result` közötti tényleges
navigáció (`navigation` modul) továbbra sincs bekötve – lásd `docs/CONVENTIONS.md`
5. szakasz.
