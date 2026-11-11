# T-111 · core/quiz: dryRun() készenléti összegzés

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-12 |
| Modul | `core/quiz` |
| Méret | S (1–3 ó) |
| Függ | [T-110](T-110-core-quiz-seedelt-determinizmus.md) |
| Követelmény | FR-13, FR-22 |
| Teszt | TR-03 |

## Cél

A kvízbeállító képernyő előre megmondja, hány kérdés állítható össze – ugyanazzal a motorral.

## Teendők

- [ ] `dryRun(request): QuizReadiness` – kérdésszám típusonként, kihagyott típusok okkal, várható `shortfall`.
- [ ] A `dryRun` ne generáljon teljes tervet, ha nem kell; de **ugyanazt a szűrő- és jogosultsági logikát** használja.

## Kész, ha

- [ ] A `dryRun` által ígért kérdésszám megegyezik azzal, amit a `generate()` ugyanarra a kérésre előállít.

## Figyelj rá

- Ha a `dryRun` és a `generate` eltérne, a beállító képernyő hazudna a felhasználónak. Ezért a két ág közös kódon osztozik, és erre külön teszt van (T-112).

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
| Commit | `9703870` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – 202 teszt, 0 hiba (core/model 35 + core/quiz 167) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:**

- A `dryRun()` már nem a `generate()` tervéből származtat: a két ág a privát
  `questionSlots(request, skippedTypes)` listán osztozik (minden meg nem hagyott típus
  minden olyan helyes jelöltje, amely mellé tényleg összeáll négy opció). A `dryRun`
  innen már csak összegez – nincs `Random`, nincs keverés, nincs `QuizQuestion`.
- Az egyezés bizonyítéka nem csak teszt, hanem érvelés is: a `generate` a helyes technika
  szerint deduplikál, ezért minden különböző helyes technika pontosan egy kérdést ad, a
  keverés sorrendjétől függetlenül – a kérdésszám `min(requestedCount, a slotok különböző
  helyes technikáinak száma)`. **Aki a `generate` dedup- vagy megállási szabályát módosítja,
  köteles az `expectedQuestionCount` formuláját is átvezetni.**
- Az `availableByType` **típusonkénti kapacitás**, nem a terv felosztása: az értékek összege
  nagyobb lehet az `expectedQuestionCount`-nál (ugyanaz a technika több típusban is lehet
  helyes válasz, de egy kvízen belül legfeljebb egyszer kerül elő). A `feature/quiz`
  beállító képernyőjén (T-403) ezért „ennyi érhető el típusonként" a helyes szöveg, nem
  „ennyi kérdés lesz abból a típusból".
- A `QuizReadiness` új invariánsokat kényszerít ki (diszjunkt kulcsok a `skippedTypes`-szal,
  nincs nulla kapacitású bejegyzés, `availableByType.isEmpty() == (expectedQuestionCount == 0)`,
  az értékek összege felső korlát).
- Helyreigazítás: a `QuizPlanContractTest` korábbi
  `availableByType.values.sum() == expectedQuestionCount` elvárása a tisztázott szemantika
  szerint téves volt, ezért kapacitás-ellenőrzésre cserélődött.
- A `QuizCandidateFilter.buildableQuestionCounts(request)` a `dryRun`-nak már nem bemenete;
  diagnosztikai célra megmarad, jelenleg csak teszt hívja. Ha a T-112 után sem kell, törölhető.
