# T-602 · TR-07: rétegszabály-ellenőrzés automatizálva

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-110](T-110-core-quiz-seedelt-determinizmus.md) |
| Követelmény | NFR-03, NFR-07 |
| Teszt | TR-07 |

## Cél

A rétegszabály és a determinizmus betartásának gépi ellenőrzése.

## Teendők

- [x] Teszt, amely a `core/model` és `core/quiz` forrásaiban tiltott importokat keres (`android.`, `androidx.`, `room`, `compose`).
- [x] Teszt a tiltott hívásokra: `Random.Default`, `System.currentTimeMillis`, `Instant.now`, `Math.random`.
- [x] A `feature/*` modulokban Room entitás / DAO / DTO típus tiltása.

## Kész, ha

- [x] A teszt elbukik, ha valaki véletlenül behúz egy tiltott importot.

## Figyelj rá

- Ez a szabály önmagában nem tartja meg magát: a `core/model` és `core/quiz` Kotlin JVM modul volta a fordítási idejű védelem, ez a teszt a maradék esetekre való.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul (N/A – ennek a feladatnak nincs felhasználói szövege)
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára (N/A – nincs UI)
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
| Commit | (lásd a következő, dokumentációs commit) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew :core:quiz:test --tests "hu.kyokushin.trainer.core.quiz.architecture.*" --rerun-tasks` | 0 | 13 teszt (`LayeringRuleDetectorTest`: 10, `LayeringRuleTest`: 3), 0 hiba |
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | 641 teszt (628 → 641, +13), 0 hiba, teljes projekt |
| `./gradlew assembleDebug` | 0 | `app-debug.apk`, `BUILD SUCCESSFUL` |

**Megjegyzés:** a szabálylogika (`LayeringRuleDetector`, `core/quiz` teszt
forráskészlet) sima `String`-eken dolgozik, elválasztva a fájlrendszer-bejárástól –
ez adja a „Kész, ha" pont tényleges bizonyítását: `LayeringRuleDetectorTest` 10
szintetikus esettel mutatja meg, hogy a detektor pozitív esetben (tiltott
import/hívás a bemenetben) talál, negatív esetben (megengedett domain-import, vagy a
hiányt magyarázó KDoc/sorvégi megjegyzés) nem téveszt – ez nem csak azt bizonyítja,
hogy a mai forrás tiszta, hanem azt is, hogy a detektor tényleg elkapna egy valódi
hibát. A valódi forrásfák ellenőrzése (`LayeringRuleTest`) a már létező
`kyo.repoRoot` rendszertulajdonságot használja (`core/quiz/build.gradle.kts`,
ugyanaz a minta, mint a `RealKnowledgeBaseFixture`-nél). Fordítás közben előjött a
`docs/CONVENTIONS.md` 6. szakaszában már rögzített „KDoc `/*` karakterpár" csapda (egy
KDoc-ban szó szerint leírt `feature/*` glob beágyazott blokk-megjegyzést nyitott) –
megoldva a szöveg átfogalmazásával, új CONVENTIONS-sor nem kellett, a meglévő csapda
leírása pontosan illett rá. Részletek: `docs/TEST_REPORT.md` 2026-11-13 „T-602" szakasza.
