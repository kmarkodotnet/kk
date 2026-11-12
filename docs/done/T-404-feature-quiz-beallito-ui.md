# T-404 · feature/quiz: beállító UI

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-12 |
| Modul | `feature/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-403](T-403-feature-quiz-beallito-viewmodel.md) |
| Követelmény | FR-11, FR-12, FR-13, FR-22 |
| Teszt | TR-06 |

## Cél

A kvízbeállító képernyő.

## Teendők

- [x] Kategóriaválasztó a kvízképes darabszámmal („8 kvízképes / 12 technika”).
- [x] Kérdésszám szegmentált választó: 5 / 10 / 20.
- [x] Négy kérdéstípus jelölőkkel; a (C)/(D) letiltva + indoklás a jelenlegi képszámmal.
- [x] Készenléti összegzés és `shortfall` üzenet a gomb felett.

## Kész, ha

- [x] A letiltott típus mellett konkrét szám áll („jelenleg: 7”), nem általános szöveg.
- [x] `@Preview` a 0 kérdéses és a shortfall esetre is.

## Figyelj rá

- A mostani tartalomnál a (C)/(D) csak az `stances` kategória kiválasztásakor válik aktívvá – ezt a képernyőnek érthetővé kell tennie, nem rejtélyessé.

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
| Commit | lásd a következő commit hash-t a `docs/IMPLEMENTATION_PLAN.md`-ben (T-404 bejegyzés) |

**Futtatott parancsok** (Bash, `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `QuizSetupMessagesTest` (4 teszt) + a meglévő `QuizSetupViewModelTest` (16 teszt), 0 hiba |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **509 teszt, 0 hiba** (505 → 509) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` |

**Mi készült:** `QuizSetupScreen.kt` (`feature/quiz/.../setup/`) – stateless
`QuizSetupScreen` + a `QuizSetupViewModel`-t látó `QuizSetupRoute`, ugyanazzal a mintával,
mint a `LearnSetupScreen`/`LearnSetupRoute` (T-302). Kategórialista jelölőnégyzettel
(„`{kvízképes}` kvízképes / `{összes}` technika", FUNCTIONAL_SPEC 5.1/2.), szegmentált
kérdésszám-választó (`SingleChoiceSegmentedButtonRow`/`SegmentedButton`, Material3 1.4.0,
5/10/20, FR-11), négy kérdéstípus jelölőnégyzete (FR-12), letiltott sor alatt mindig
látható, konkrét darabszámos indoklással (FR-13), és az élő készenléti összegzés + a 0
kérdéses eset javaslata a „Kvíz indítása" gomb felett (FR-22). A gomb eredménye
(`QuizSetupViewModel.startEvent`) kifelé adott `onStartQuiz: (QuizStartResult) -> Unit`
lambda, nem közvetlen navigáció (L5).

Két `@Composable`-mentes segédfüggvény (`QuizSetupMessages.kt`, `QuizSetupMessagesTest.kt`
Robolectric-kel, a tényleges `strings.xml` fölött): `questionTypeLabelRes` (a négy
kérdéstípus felirata, szó szerint a FUNCTIONAL_SPEC 8. `quiz.type.*` kulcsaiból) és
`quizReadinessSummary` (a „ennyi kérdés áll össze" / hiány-szöveg döntése, szó szerint a
FUNCTIONAL_SPEC 5.1/5. mondatából és a `quiz.shortfall` kulcsból).

**Megjegyzés:** a `navigation` modul bekötése (a `QUIZ_SETUP` composable még
`PlaceholderScreen`, a `learn/cards → quiz/setup` kategória-átadás, és a
`quiz/setup → quiz/run` navigáció + `QuizPlan`-átadás) **szándékosan nem** ennek a
feladatnak a része – a `Teendők`/`Kész ha`/DoD listája nem kérte, és a `quiz/run`
képernyő (T-405, `QuizRunState`) előtt nem is oldható meg értelmesen. Lásd
`docs/CONVENTIONS.md` 5. szakasz új sora a pontos nyitott teendőről.
