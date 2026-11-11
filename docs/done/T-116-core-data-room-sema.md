# T-116 · core/data: Room séma

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-04, US-13 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-103](T-103-core-model-felhasznaloi-adatmodellek.md) |
| Követelmény | DR-04, DR-07, DR-08, FR-18 |
| Teszt | TR-05 |

## Cél

Az adatbázis: tartalom- és felhasználói táblák, a kritikus megszorításokkal.

## Teendők

- [x] Tartalomtáblák: `category`, `technique`, `technique_alias`, `synonym_group`, `content_state`.
- [x] Felhasználói táblák: `quiz_session`, `quiz_attempt`, `study_review`.
- [x] **`UNIQUE(sessionId, questionId)` index** a `quiz_attempt`-en (ADR-005).
- [x] A felhasználói táblák idegen kulcsa a tartalomra: **nincs** `CASCADE DELETE` (ADR-004).
- [x] `@TypeConverter`: `Instant` ↔ `Long`, enum ↔ **`String`** (nem ordinal).
- [x] `fallbackToDestructiveMigration()` **tilos**; sémaexport bekapcsolva (`room.schemaLocation`).

## Kész, ha

- [x] A séma JSON exportálódik a `core/data/schemas/` alá.
- [x] Minden DAO `suspend` vagy `Flow`; `allowMainThreadQueries` nincs.

## Figyelj rá

- Az enum `String`-ként tárolása azért kell, hogy az enum-értékek átrendezése ne rontsa el a meglévő adatot.
- A cascade hiánya a TR-05 garancia alapja: tartalomtörlés nem viheti el az előrehaladást.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [–] UI esetén: **nem értelmezhető** (nincs UI ebben a feladatban) – `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
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
| Dátum | 2026-11-11 |
| Commit | `e9a3ec9` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | 52 teszt, 0 hiba, 0 kihagyott |
| `.\gradlew.bat testDebugUnitTest` | 0 | összesen 314 teszt, 0 hiba, 0 kihagyott (36 új) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:**

- **Szándékos eltérés az ARCHITECTURE 5.2 táblázatától:** a `technique` táblán **nincs**
  `quizEligible` oszlop. A kvízjogosultság származtatott érték (ADR-010); tárolva két
  igazságforrás lenne, és egy elavult `true` kvízbe engedhetne hiányos rekordot.
- Az ADR-004 két megengedett megoldása közül a **„nincs FK kényszer"** lett a választás a
  `quiz_attempt.techniqueId` és a `study_review.techniqueId` oszlopon. A `NO_ACTION` is
  megakadályozná a cascade törlést, de **beszúrási** kényszert adna: egy próbálkozás csak
  akkor rögzülhetne, ha a technika sora abban a pillanatban létezik – ez a két életciklust
  (DR-07) újra összekötné. FK nélkül a felhasználói sor akkor is érvényes marad, ha a
  technika visszavont vagy egy jövőbeli migráció fizikailag eltávolítja.
- Az egyetlen `CASCADE` felhasználói adat → felhasználói adat irányban van
  (`quiz_attempt.sessionId` → `quiz_session`): egy menet nélküli válasz értelmezhetetlen,
  és az előrehaladás törlése (FR-28, T-502) így egyetlen `DELETE` lesz.
- A `QuizAttemptDao.insertIgnoringDuplicate` **`-1`-et** ad vissza ütközéskor (nem dob):
  ebből lesz a `ProgressRepository.recordAttempt` `false` válasza (ADR-005). A T-401 erre
  építsen, ne a kivételre.
- A `QuizSessionDao.incrementAnsweredCount` az adatbázisban növel (`answeredCount + 1`),
  nem beolvasott értéket ír vissza. A hívó csak akkor növelje, ha a próbálkozás beszúrása
  tényleg létrehozott sort – így a számláló és a sorok száma nem csúszhat el.
- A `core/data/build.gradle.kts` kapott egy `testOptions { unitTests {
  isIncludeAndroidResources = true } }` blokkot: ez kell a Robolectric + in-memory Room
  tesztekhez. A T-118 (TR-05) ezen az úton emulátor nélkül futhat; a Robolectric
  `@Config(sdk = [34])`-kel működik (a `compileSdk = 37` szintre nincs android-all).
- Entitás ↔ domain **mapper még nincs** – szándékosan: a tartalom oldali leképezés a T-117
  (`ContentImporter`), a felhasználói oldali a T-301/T-401 (`ProgressRepository`) tárgya.
- `version = 1`, migráció még nem létezik. A `2.json` megjelenésekor a `1.json` az
  összehasonlítás alapja, és a sémaváltás explicit `Migration`-t + ADR-t igényel (DR-08).
