# T-110 · core/quiz: seedelt determinizmus

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-02, US-15 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-109](T-109-core-quiz-distraktorvalasztas.md) |
| Követelmény | NFR-07 |
| Teszt | TR-02 |

## Cél

Ugyanaz a bemenet + seed → bitre ugyanaz a terv.

## Teendők

- [ ] Fisher–Yates keverés kizárólag a seedelt RNG-ből (`kotlin.random.Random(seed)`).
- [ ] Minden jelöltbejárás **rendezett** (`sortedBy { it.id.value }`) – `Set` iterációs sorrendre épülő logika tilos.
- [ ] Az opciók keverése is a seedből; a kérdéssorrend is.
- [ ] Nincs `Random.Default`, nincs rendszeróra-hívás a modulban.

## Kész, ha

- [ ] Ugyanazzal a `QuizRequest`-tel kétszer hívva a `QuizPlan` minden mezője megegyezik, az opciók sorrendjével együtt.
- [ ] A modulban egyetlen `Random.Default` / `currentTimeMillis` / `Instant.now()` előfordulás sincs.

## Figyelj rá

- Ez nem kozmetika: enélkül a forgatás utáni állapot-visszaállítás (T-405) más kvízt adna vissza.
- A `HashSet`/`HashMap` bejárási sorrendje JVM-verziónként változhat – ezért kötelező a rendezett bejárás.

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
| Commit | <COMMIT> |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | 196 teszt, 0 hiba (core/model 35 + core/quiz 161); `DefaultQuizGeneratorTest` 15 teszt |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** `SeededShuffle` (az egyetlen keverő belépési pont, az RNG-t paraméterként
kéri) és `DefaultQuizGenerator` készült el. A `generate()` egyetlen `Random(request.seed)`
példányból sorsol: Fisher–Yates a (típus, helyes válasz) jelöltekre adja a kérdéssorrendet,
majd kérdésenként a négy opcióra. A keverés **csak permutál** – az opciók halmaza a T-109
`selectOptionIds` kimenete. Ugyanaz a helyes technika egy terven belül legfeljebb egyszer
szerepel, és kevés jelöltnél kevesebb kérdés lesz tényleges `Shortfall`-lal
(`ALL_TYPES_SKIPPED` → `NOT_ENOUGH_IMAGES` → `NOT_ENOUGH_DISTRACTORS` →
`NOT_ENOUGH_CANDIDATES` prioritás; ezt a sorrendet a specifikáció nem írja elő, a motor
dönti el – ha a T-112 mást kíván, ott kell egyeztetni).

**A következő fejlesztőnek:** a `dryRun()` egyelőre a `generate()` tervéből származtat.
Helyes (definíció szerint nem tud eltérni a `generate()`-től), de kérdéseket épít, ezért a
beállító képernyő élő összegzéséhez nem optimális – a kérdésépítés nélküli változat és az
`availableByType` pontos szemantikája a **T-111** dolga. A `Random(seed)` nem tiltott hívás:
a seed a kérésben jön, a hívó injektált RNG-ből sorsolja (ADR-007); a tiltás a seed nélküli
véletlenre és a rendszerórára vonatkozik, és ezt forrásszkennelő teszt kényszeríti ki
(`quizModuleMainSources_containNoUnseededRandomOrClock_NFR07`) – ez a teszt a **T-602**
automatizált rétegszabály-ellenőrzésének is előfutára.
