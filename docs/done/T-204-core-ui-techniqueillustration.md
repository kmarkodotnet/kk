# T-204 · core/ui: TechniqueIllustration

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-07, US-08 |
| Modul | `core/ui` |
| Méret | M (3–6 ó) |
| Függ | [T-102](T-102-core-model-technique-category-illustrationref.md) |
| Követelmény | CR-05, NFR-04 |
| Teszt | — |

## Cél

Az illusztráció megjelenítése mindhárom esetére, helyes akadálymentes leírással.

## Teendők

- [x] `Asset` → a kép; `Placeholder` → semleges helyőrző + „Ábra készül”; `null` → nincs képblokk.
- [x] `contentDescription` paraméterezhető: a Tudástárban a technika neve, a kvízopcióknál **pozíció alapú** leírás.
- [x] Képarány megtartása, `max-width` a konténerhez; nagy képeknél méretezés.
- [x] Jelzés, ha a kép szakmai ellenőrzésre vár.

## Kész, ha

- [x] A kvízopciónál használt leírás **nem tartalmazza** a technika nevét (NFR-04).
- [x] `@Preview` mindhárom esetre, világos és sötét témában.

## Figyelj rá

- A `contentDescription` a legkönnyebben elrontható akadálymentességi pont: a (D) kvízopciónál a „Kiba dachi illusztrációja” leírás TalkBackkel elárulná a választ.

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
| Dátum | 2026-11-11 |
| Commit | <COMMIT> |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – a `core/ui` 10 új tesztje zöld (`:core:ui:testDebugUnitTest` ténylegesen lefutott) |
| `.\gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – teljes készlet újrafúratva: **375 teszt, 0 hiba** (365 → 375) |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

### Mivel igazolt a „Kész, ha” két pontja

1. **„A kvízopciónál használt leírás nem tartalmazza a technika nevét (NFR-04).”**
   Két, egymást erősítő módon. (1) **Fordítási időben:** a komponens nem
   `contentDescription: String`-et kap, hanem az `IllustrationLabel` szándéktípust, és a
   `QuizOption(position)` ágon a technika nevére **nincs is paraméter** – a név
   átadása nem elrontható, hanem kifejezhetetlen. (2) **Tesztben:** az
   `assetDescription_forQuizOption_omitsTechniqueName_NFR04` és a
   `placeholderDescription_forQuizOption_omitsTechniqueName_NFR04` a **tényleges**
   `strings.xml` fölött (Robolectric) állítja, hogy a leírás sem a romajit, sem a
   magyar nevet nem tartalmazza – tehát a magyar szöveg későbbi átírása is kibukik.
2. **„`@Preview` mindhárom esetre, világos és sötét témában.”** Négy előnézet-függvény,
   mindegyiken két `@Preview` (alap + `UI_MODE_NIGHT_YES`): `Asset`, `Placeholder`,
   `null`, és ráadásul a kvízopció-eset is. A renderelés Android Studiót igényel és
   **nem futott** (TEST_REPORT „NEM FUTOTT”); amit a build igazol, az az, hogy az
   előnézet-függvények **lefordulnak** (`:core:ui:compileDebugKotlin` exit 0).

**Megjegyzés:**

- **A téma átkerült az `app`-ból a `core/ui`-ba.** Új hely:
  `core/ui/.../core/ui/theme/Theme.kt`, csomag `hu.kyokushin.trainer.core.ui.theme`.
  Indok: a megosztott komponensek `@Preview`-jai és a későbbi funkciómodulok képernyői
  is a témát használják, `:app`-ra viszont senki nem függhet visszafelé (ADR-001) – a
  téma `app`-ban hagyása minden következő Compose taskot megblokkolt volna. A
  `MainActivity` importja, az `ARCHITECTURE.md` modultérképe és a 6.3 szakasza
  frissítve. A paletta változatlan.
- **A `null` eset szerződése.** A komponens `null` hivatkozásnál **nem bocsát ki UI-t**
  (a task „Teendők” pontja szerint). A US-07 2. elfogadási kritériuma viszont a
  részletezőn helyőrzőt vár „nincs képe” esetén is – ezt a **hívónak** kell megoldania
  azzal, hogy a `null`-t `IllustrationRef.Placeholder`-ként adja át. **T-209-nek ezt meg
  kell tennie**, a listán (T-208) viszont a hely marad üresen.
- **A képek még nincsenek az assetsben.** A komponens a
  `knowledge/illustrations/<fileName>` útvonalról tölt; a fájlokat a **T-601** teszi
  oda. Addig az `Asset` ág a „hiányzó fájl → helyőrző” tartalékon fut (FUNCTIONAL_SPEC
  10.), tehát nincs összeomlás, de a T-601 előtt valódi kép nem fog megjelenni.
- **Az `awaitingReview` nem származtatott érték.** A komponens nem nézi meg a technika
  hitelesítési státuszát: a hívó dönt (CR-02 – kód soha nem emel státuszt). A
  jelenlegi tartalomban **minden** kép `review_status: human_review_required`, tehát a
  T-208/T-209 gyakorlatilag mindenhol `awaitingReview = true`-t fog átadni.
  **Javasolt új task** (nem ebben a körben javítva): az `IllustrationRef.Asset` nem
  hordozza a kép saját `review_status`-át, csak a technika `VerificationStatus`-a érhető
  el – ha a kettőt külön kell jelezni (egy hiteles technika AI-generált képpel), a
  modellt bővíteni kell egy `awaitingReview: Boolean` mezővel az `Asset`-ben, ADR-003
  kiegészítéssel.
- **A KDoc-csapda harmadszor is előkerült:** `` `feature/*` `` egy KDoc-ban beágyazott
  blokk-megjegyzést nyit, és a fájl végén `Unclosed comment`-tel áll meg. A T-607
  (lint/ktlint) érdemes szabályt tenni rá.
- **A `core/ui` Robolectric-konvenciója:** `@Config(sdk = [34])` +
  `isIncludeAndroidResources = true` – ugyanaz, mint a `core/data`-ban és az `app`-ban.
  `@Config(manifest = Config.NONE)` **nem** működik (`compileSdk = 37`-hez a Robolectric
  4.17 nem talál SDK-képet).
