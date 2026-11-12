# T-402 · core/data: kvízjelöltek lekérdezése

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-12 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-201](T-201-core-data-techniquerepository.md) |
| Követelmény | FR-11, FR-13 |
| Teszt | TR-03 |

## Cél

A generátor bemenetének összeállítása az adatbázisból.

## Teendők

- [ ] `getQuizCandidates(categories): QuizCandidateSet` – technikák + szinonimacsoportok + képkapuk.
- [ ] A `retired`, `quiz_excluded` és `INCOMPLETE` rekordok kizárva.
- [ ] A `dataset_trust` érték átadása a `content_state`-ből (ADR-010).

## Kész, ha

- [ ] A generátor semmilyen adatbázis-típust nem lát: tiszta domain objektumokat kap.

## Figyelj rá

- A szinonimacsoportokat a validátor állította elő és a `synonym_group` tábla őrzi – itt kell visszaolvasni, különben a CR-07 kizárás nem érvényesül.

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
| Dátum | 2026-11-12 |
| Commit | `e7b188a` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 451 teszt, 0 hiba (10 új, a G-01 csoport egészére) |
| `./gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL, `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés – két szándékos eltérés a „Teendők" szó szerinti szövegétől:**

1. **`getQuizCandidates` már T-201 óta létezik** (a `retired` szűréssel, a
   szinonimacsoportokkal és a `dataset_trust` `observeContentState()`-en keresztüli
   elérésével együtt) – ennek a tasknak a tényleges, hátramaradt munkája az
   `illustrationDigests` feltöltése volt.
2. **A `quiz_excluded` és az `INCOMPLETE` rekordok SZÁNDÉKOSAN nincsenek kiszűrve ebben
   a metódusban**, pedig a „Teendők" ezt kéri. Ennek oka az ADR-010 és a
   `QuizCandidateSet` dokumentált szerződése: „A készlet nem dönt kvízképességről… Ez a
   típus csak adatot szállít, szabályt nem." – a `quizExcluded`/`INCOMPLETE`/`retired`
   tényleges kizárását a `QuizEligibility.isEligible` egyetlen kapuja végzi
   kérdéstípusonként (ez már a T-201 lezárásakor is dokumentált döntés volt). Egy második
   szűrés itt **duplikálná** ezt a szabályt, és a kettő idővel szétcsúszhatna – pontosan
   azt kockáztatva, amit az ADR-010 el akar kerülni. A `retired` kizárás marad, mert az
   nem kvízjogosultsági, hanem tartalom-életciklus kérdés (ADR-004) – ez már T-201 óta
   SQL-ben van.
3. **`illustrationDigests` feltöltve** (ez volt a tényleges hiány, lásd
   `docs/CONVENTIONS.md`, `docs/TEST_REPORT.md` és `docs/IMPLEMENTATION_PLAN.md` korábbi
   bejegyzéseit): a térkép a csomagolt illusztrációfájl **nevét** adja ujjlenyomatként,
   nem a kép bájttartalmának SHA-256 hashét – a sémában nincs ilyen oszlop, és becsült
   ujjlenyomatot a projekt nem gyárt (CLAUDE.md „Tartalmi integritás"). A fájlnév-egyezés
   korrekt: az `IllustrationRef.Asset` csak a csomagban tényleg létező fájlra hivatkozik
   (V005), tehát két technika, amely ugyanarra a fájlnévre hivatkozik, szó szerint
   ugyanazt a képfájlt jeleníti meg – ezt zárja ki a (D) `JP_TO_IMAGE` típus
   distraktorszűrése. Amit ez nem fog el: két *eltérő* fájlnevű, de bitre azonos kép – ez
   a `knowledgebase_transfer` konverter I003 diagnosztikájának hatóköre, nem a motoré.
   Új teszt: `RoomTechniqueRepositoryTest.getQuizCandidates_fillsIllustrationDigestsForTechniquesSharingThePackagedFile_FR13`.
- A `QuizCandidateSet.illustrationDigests` KDoc-ja (`core/quiz`) is pontosításra került:
  nem állítja többé, hogy a tárolt érték „SHA-256 hex" – ez volt a tényleges
  dokumentációs csapda, amivel a következő fejlesztő összezavarodhatott volna.
- **Javasolt új task, ha a tényleges tudástárba illusztráció kerül:** ha a jövőben
  szükség lenne a bitre azonos, de eltérő fájlnevű képek felismerésére is (I003 eset),
  az a sémához egy valódi `sha256` oszlop felvételét igényelné (`technique` tábla,
  `version` emelés, explicit `Migration`, ADR) – ez önálló, önmagában is M méretű feladat
  lenne, nem fért bele ebbe a csoportba.
