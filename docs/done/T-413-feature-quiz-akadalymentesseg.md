# T-413 · feature/quiz: akadálymentesség

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14, US-20 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-408](T-408-feature-quiz-c-es-d-kerdes-ui.md), [T-409](T-409-feature-quiz-valasz-visszajelzes.md) |
| Követelmény | NFR-04 |
| Teszt | TR-06 |

## Cél

A kvíz TalkBackkel is helyesen használható – a válasz felfedése nélkül.

## Teendők

- [x] Opciók `selected` szemantikával; válasz után `stateDescription`.
- [x] A (D) opciók képleírása pozíció alapú, nevet nem tartalmaz.
- [x] Bejárási sorrend: kérdés → opciók → visszajelzés → Következő.
- [x] Minden érintési célpont ≥ 48 dp.

## Kész, ha

- [x] TalkBackkel végigvihető egy teljes kvíz anélkül, hogy a felolvasás elárulná a helyes választ.
  (Kódszinten igazolva – lásd az „Elkészült” szakaszt; tényleges TalkBack/emulátor-futás a
  B-03 blokkoló miatt nem történt, ugyanaz a korlát, mint a T-404/T-407/T-408/T-409/T-411/
  T-412-nél.)

## Figyelj rá

- Ez az a pont, ahol az akadálymentesség és a kvízhelyesség ütközhet – a megoldás a pozíció alapú leírás.

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
| Commit | `3675c9c` |

**Futtatott parancsok** (Bash, `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – teljes, őszinte projektfutás, 294 actionable task, mind végrehajtva. A projekt összesített teszteredménye (minden `test`/`testDebugUnitTest` XML tagjainak összege) **566 teszt, 0 hiba** (560 → 566, +6 az új `QuizOptionAccessibilityMappingTest`-tel). |
| `./gradlew.bat :feature:quiz:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – a módosított modul önmagában is zöld; az új teszt futását a `feature/quiz/build/test-results/testDebugUnitTest/TEST-....QuizOptionAccessibilityMappingTest.xml` igazolja (`tests="6" failures="0" errors="0"`). |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (≈20,9 MB). |

**Mi készült:** a feladat kiindulásakor a `feature/quiz` mindhárom képernyője
(`setup`/`run`/`result`) a Teendők nagy részét **már tartalmazta** – ezt a T-407/T-408/
T-409/T-411/T-412 feladatok a saját UI-juk megírásakor proaktívan beépítették (lásd
`docs/CONVENTIONS.md` 4.1 szakasz NFR-04-re hivatkozó sorai). Konkrétan, kódolvasással
igazolva:

- **„Opciók `selected` szemantikával; válasz után `stateDescription`"**: `QuizOptionRow`
  és `QuizImageOptionCard` (`QuizRunScreen.kt`) `Modifier.selectable(selected = …, role =
  Role.RadioButton, …)`-t és feltételes `Modifier.semantics { stateDescription = … }`-t
  használ. Ezt most **önálló, emulátor nélküli JUnit teszt is bizonyítja**: az ikon-,
  felirat- és `stateDescription`-leképező függvényeket (`quizOptionIcon`/
  `quizOptionFeedbackTextRes`/`quizOptionStateDescriptionRes`) `private`-ból `internal`-ra
  nyitottam (ugyanaz a minta, mint a `QuizOptionFeedbackResolver`-nél, T-409), és egy új
  `QuizOptionAccessibilityMappingTest` (6 teszt) igazolja, hogy válasz előtt
  (`UNANSWERED`) és egy nem kiválasztott helytelen opciónál (`NEUTRAL`) **nincs**
  `stateDescription`/ikon/szöveg, míg a helyes/helytelen ágak a megfelelő, egymástól
  **különböző** string-erőforrást kapják.
- **„A (D) opciók képleírása pozíció alapú, nevet nem tartalmaz"**: `IllustrationLabel.
  QuizOption(position)` + `IllustrationContentDescriptionTest` (T-408, `core/ui`) –
  változatlan, már korábban tesztelt.
- **„Bejárási sorrend: kérdés → opciók → visszajelzés → Következő"**: a `QuizRunContent`
  Compose-fastruktúrája (`QuizRunScreen.kt`) pontosan ezt a sorrendet követi: a kérdés-
  blokk, majd az opciók (a kérdéstípus-specifikus composable-ökben), majd – csak válasz
  után – a `QuizExplanationCard`, és a „Következő"/„Eredmény" gomb a görgetett területen
  KÍVÜL, mindig legutoljára. Ez réteg-sorrendi tény, nem futásidejű állapot, ezért
  kódolvasással igazolt; automatizált bizonyítás `createComposeRule` hiányában (lásd
  `docs/CONVENTIONS.md` 4.1) nem elérhető.
- **„Minden érintési célpont ≥ 48 dp"**: itt egy **valódi hiányt** találtam és javítottam:
  a `QuizCloseConfirmationDialog` (`QuizRunScreen.kt`) `AlertDialog`-jának „Mégsem"/
  „Megszakítás" `TextButton`-jai a Material 3 alapértelmezett (~40 dp) magasságukat
  kapták, **nem** a projekt 48 dp-es szabályát – ezzel szemben a `QuizResultScreen`
  „Vissza a Haladáshoz" `TextButton`-ja már korábban is helyesen `heightIn(min = 48.dp)`
  volt. Mindkét dialógusgomb explicit `Modifier.heightIn(min = MIN_TOUCH_TARGET)`-et
  kapott – ez az egyetlen funkcionális kódmódosítás, amit ez a feladat a meglévő
  Teendőkön felül igényelt.

**Megjegyzés:** a `quiz/setup`/`quiz/run`/`quiz/result` navigációs bekötése
(`KyokushinNavHost`) továbbra sincs megoldva – ez szándékosan nem ennek a feladatnak a
tárgya (lásd `docs/CONVENTIONS.md` 5. szakasz). A TR-06 (TalkBackkel, tényleges
eszközön/emulátorral végigvitt kvíz) a B-03 blokkoló miatt nem futott; az összes fent
leírt kódszintű bizonyíték ettől független. Ha egy jövőbeli feladat (T-604
„Akadálymentességi átfésülés”) emulátort kap, érdemes elsőként a kvízt TalkBackkel
manuálisan végigjátszani, és ezzel az itt dokumentált kódelemzést tényleges eszközön is
megerősíteni.
