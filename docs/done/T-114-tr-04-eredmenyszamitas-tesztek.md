# T-114 · TR-04: eredményszámítás tesztek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-03 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-113](T-113-core-quiz-quizscoring.md) |
| Követelmény | FR-18, FR-20, FR-27 |
| Teszt | TR-04 |

## Cél

Az eredményszámítás határesetei.

## Teendők

- [ ] 0 válasz → `null` arány; 100%; 0%; félbehagyott teszt (4/10 megválaszolva).
- [ ] Dupla válasz: ugyanarra a `questionId`-ra két `QuizAttempt` nem keletkezhet – a lista szintjén is ellenőrizve.
- [ ] Vegyes kérdéstípusok egy munkamenetben.

## Kész, ha

- [ ] Minden fenti eset lefedve, a tesztnevek `TR04` jelöléssel.

## Figyelj rá

- A „félbehagyott teszt” esete a leggyakrabban elrontott: a meg nem válaszolt kérdés **nem** hibás válasz.

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
| Dátum | 2026-11-11 |
| Commit | _lásd alább_ |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew testDebugUnitTest` | 0 | BUILD SUCCESSFUL – 262 teszt, 0 hiba (ebből a `QuizScoringTr04Test` 17 új eset) |
| `./gradlew assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A TR-04 készlet külön fájlba került
(`core/quiz/src/test/kotlin/hu/kyokushin/trainer/core/quiz/scoring/QuizScoringTr04Test.kt`),
a T-113 kilenc alaptesztje érintetlen – a `QuizScoringTest` a szerződést, a `QuizScoringTr04Test`
a TR-04 mátrixát fedi. A „dupla válasz a lista szintjén is ellenőrizve" pont úgy valósult meg,
hogy minden dedupot érintő eset egy közös `assertAnsweredMatchesDistinctQuestions` segítővel
is állítja: `score.answered == attempts.map { sessionId to questionId }.toSet().size`, és
`answered <= attempts.size`. A `QuizScoring` **nem igényelt módosítást**: minden TR-04 eset
első futásra zöld volt – a T-113 szerződése elbírta a teljes mátrixot. Új task-javaslat nem
keletkezett. A százalékformázás és a „nincs adat" magyar szöveg továbbra is a T-411 tárgya;
a `ratio` arány (0.0–1.0), nem százalék.
