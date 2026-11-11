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
