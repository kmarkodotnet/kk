# T-501 · core/data: aggregált statisztika lekérdezések

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-17, US-18 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-401](T-401-core-data-progressrepository-kvizresz.md) |
| Követelmény | FR-23, FR-24, FR-25, FR-26, FR-27, DR-09 |
| Teszt | — |

## Cél

A haladás számai – **számolva**, nem redundáns számlálókból.

## Teendők

- [x] Összesített: megválaszolt, helyes.
- [x] Kategóriánkénti: `GROUP BY categoryId`, próbálkozásszámmal.
- [x] Technikánkénti: próbálkozás, helyes, utolsó gyakorlás ideje, kérdéstípus-bontás.
- [x] „Gyakorlást igénylő”: sikerarány növekvő, majd utolsó gyakorlás régisége; csak ≥ 1 próbálkozással; limit 10.
- [x] Üres eset: a lekérdezés `null`-t vagy üres listát ad, nem 0-t.

## Kész, ha

- [x] Nincs külön számláló tábla vagy mező, amit frissíteni kellene (DR-09).
- [x] A lekérdezések `Flow`-t adnak, és nem a fő szálon futnak.

## Figyelj rá

- A „nincs adat” és a „0%” megkülönböztetése már az adatrétegben kezdődik: a `null` arány végigmegy a `QuizScoring`-on a UI-ig (FR-27).

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul (N/A – nincs UI-t vagy felhasználói szöveget érintő változás ebben a feladatban)
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára (N/A – tisztán `core/data` adatréteg)
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
| Commit | `9789511` |

**Futtatott parancsok** (Bash, `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :core:data:testDebugUnitTest --tests "hu.kyokushin.trainer.core.data.repository.RoomProgressRepositoryTest" --rerun-tasks` | 0 | `BUILD SUCCESSFUL`, `RoomProgressRepositoryTest`: 21 teszt (12 meglévő + 9 új), 0 hiba |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL`, teljes projekt, 578 teszt (569 → 578), 0 hiba |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A `ProgressRepository` négy új, olvasó taggal bővült
(`observeOverallProgress`/`observeCategoryProgress`/`observeTechniqueProgress(id)`/
`observeNeedsPractice(limit)`, FR-23…FR-27, DR-09) – mind aggregált SQL `@Query`-ből
(`QuizAttemptDao`, `UserDaos.kt`), nincs új számlálótábla/oszlop, nincs sémaváltozás.
A kategóriabontás a `category` táblából indul két `LEFT JOIN`-nal, hogy a
próbálkozásmentes kategória is „nincs adat"-tal jelenjen meg. A `ratio`/„nincs adat"
döntés a `core/quiz` `Score` típuson megy keresztül (mapperek: `toScore`/
`toTechniqueProgress`, `UserEntityMappers.kt`), sosem SQL-ben számolva – a FR-27 szabály
egyetlen helyen él. Két Room-projekció (`TechniqueScoreRow`/
`TechniqueQuestionTypeScoreRow`) kettős felhasználású (egyetlen technikára és a
„gyakorlást igénylő" listára is, `SELECT :param AS oszlop` trükkel), hogy ne kelljen
két majdnem azonos típus. A bővült interfész miatt mind a négy `ProgressRepository`
tesztdublőr (`FakeLearnProgressRepository`, `FakeQuizProgressRepository`,
`FakeQuizRunProgressRepository`, `FakeQuizResultProgressRepository`) négy új,
kivételt dobó felülírást kapott. Részletek: `docs/CONVENTIONS.md` 1. szakasz új
sorai, `docs/IMPLEMENTATION_PLAN.md` Fázis 3.7 + a 2026-11-13-i changelog bejegyzés.
Nyitva marad (önálló, nem ennek a feladatnak a tárgya): a `feature/progress` UI
(T-503…T-506) és a `library/detail` (T-209) „már gyakoroltam" összegzésének bekötése.
