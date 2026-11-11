# T-113 · core/quiz: QuizScoring

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-03 |
| Modul | `core/quiz` |
| Méret | S (1–3 ó) |
| Függ | [T-103](T-103-core-model-felhasznaloi-adatmodellek.md) |
| Követelmény | FR-20, FR-27, DR-09 |
| Teszt | TR-04 |

## Cél

Eredményszámítás tiszta függvényként, nullable aránnyal.

## Teendők

- [ ] `object QuizScoring { fun score(attempts: List<QuizAttempt>): Score }`.
- [ ] `Score(answered, correct, ratio: Double?)` – `ratio` **null**, ha `answered == 0`.
- [ ] Csak a ténylegesen megválaszolt kérdések számítanak; a meg nem válaszolt nem hibás.

## Kész, ha

- [ ] 0 válasz → `ratio == null` (nem `0.0`).
- [ ] Nincs állapot, nincs mellékhatás, nincs óra-hívás.

## Figyelj rá

- A nullable típus azért fontos, mert megakadályozza, hogy a UI véletlenül 0%-ot írjon ki ott, ahol „nincs adat” a helyes (FR-27).

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
| Commit | PENDING |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | 245 teszt, 0 hiba (core/model 35 + core/quiz 210; 9 új a `QuizScoringTest`-ben) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A `Score` nem csak hordozza a nullable arányt, hanem **kikényszeríti**: az
`init` blokk visszautasítja a `Score(0, 0, 0.0)`-t és az `answered > 0` melletti `null`-t is,
így a FR-27 szabálya a típus szintjén él, nem csak a `score()` törzsében. A `score()` a
tervben nem szereplő, de az US-03 4. elfogadási kritériumához tartozó **dedupot** is elvégzi
a `(sessionId, questionId)` páron (a legkorábbi `answeredAt` nyer) – a tárolás `UNIQUE`
indexe (ADR-005) mellett ez a második védelem a dupla koppintás ellen, és a T-114 erre a
viselkedésre írhatja a TR-04 dupla-válasz esetét. A T-114-nek nincs több hátralévő
előkészítése; a `Score` ratio **arány 0..1**, nem százalék – a formázás és a „nincs adat"
magyar szöveg a T-411 eredményképernyőjére tartozik.
