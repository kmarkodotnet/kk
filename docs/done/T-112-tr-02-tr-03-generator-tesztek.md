# T-112 · TR-02 + TR-03: generátor tesztek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-02, US-12 |
| Modul | `core/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-111](T-111-core-quiz-dryrun-keszenleti-osszegzes.md) |
| Követelmény | FR-13, FR-14, FR-22, CR-07, NFR-07 |
| Teszt | TR-02, TR-03 |

## Cél

A kvízmotor invariánsainak teljes lefedése. **Az M1 mérföldkő magja.**

## Teendők

- [ ] Invariánsok: 4 opció, 1 helyes, 3 különböző téves, nincs szinonima a tévesek közt.
- [ ] Determinizmus: azonos seed → azonos terv (kérdés- és opciósorrend is).
- [ ] Fallback: kevés jelölt → kevesebb kérdés `shortfall`-lal, nem hibás kérdés; 0 jelölt → üres terv.
- [ ] Kategóriaszűrés: szűrt kategórián kívüli technika nem jelenik meg sem helyesként, sem distraktorként.
- [ ] Típusjogosultság: feliratos/hiányzó kép esetén (C)/(D) kihagyva, `SkipReason`-nel.
- [ ] `dryRun` ↔ `generate` egyezés ugyanarra a kérésre.
- [ ] Teszt a valódi tartalomra: az `stances` kategória (C)/(D) kérdést ad, a többi nem.

## Kész, ha

- [ ] `./gradlew testDebugUnitTest` exit 0, minden fenti eset lefedve.
- [ ] `docs/TEST_REPORT.md` frissítve.

## Figyelj rá

- Ha egyetlen tesztcsomagot kell jól megírni ebben a projektben, ez az: a kvízhelyesség itt dől el.
- A tesztnevek `TR02` / `TR03` jelölést viselnek.

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
| Commit | cf9383c |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:quiz:test` | 0 | BUILD SUCCESSFUL |
| `.\gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 236 teszt, 0 hiba (core/model 35 + core/quiz 201; 34 új) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A kvízmotor főkódja **nem változott** – a task tisztán tesztet adott, és a
meglévő 202 teszt egyikét sem írta át. Három új osztály: `SeededShuffleTest` (7),
`QuizPlanInvariantTest` (20), `RealContentQuizGenerationTest` (7). A nyolc kvízmotor-invariáns
közös ellenőrzése a `GeneratorContractFixtures.assertQuizPlanInvariants(...)`-ban él, hogy a
szintetikus 21 kérés × 7 seed mátrix és a valódi 161 rekordos tartalom ugyanazt a garanciát
bizonyítsa.

Amit a következő fejlesztőnek tudni kell:

- A `Shortfall` okprioritása mostantól **tesztelt motordöntés**: `ALL_TYPES_SKIPPED` →
  `NOT_ENOUGH_IMAGES` → `NOT_ENOUGH_DISTRACTORS` → `NOT_ENOUGH_CANDIDATES`. Ha a UI (T-404,
  T-411) más sorrendet igényel, azt itt kell egyeztetni, és a teszt ki fogja kényszeríteni.
- A `NOT_ENOUGH_DISTRACTORS` ág csak **átfedő** szinonimacsoportokkal áll elő: egyetlen
  szinonimacsoport szimmetrikusan zár, és akkor a típus egésze esik ki
  (`NOT_ENOUGH_CANDIDATES`). Ezt a teszt kommentárja is rögzíti.
- A valódi tartalom V006 szinonimacsoport-listája **üres**, és ezt a
  `RealContentQuizGenerationTest` explicit `assert`-tel kimondja: ha a tartalom egyszer
  szinonimacsoportot hoz, az ne csendben kerüljön a motorba.
- A `DefaultQuizGenerator` forrásszövegére két teszt is támaszkodik (a `dryRun` törzse nem
  sorsol és nem épít kérdést; `generate` és `dryRun` is a `questionSlots(request,
  skippedTypes)` közös pontot hívja). Átnevezés vagy átszervezés esetén ezeket is frissíteni
  kell – ez szándékos: a két ág elválása így nem tud csendben megtörténni.
- Nem futott: `connectedDebugAndroidTest` (nincs eszköz/emulátor, B-03) és
  `lint ktlintCheck` (a ktlint konfigurálása a T-607).
