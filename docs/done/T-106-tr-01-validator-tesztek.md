# T-106 · TR-01: validátor tesztek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-01 |
| Modul | `core/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-105](T-105-core-quiz-a-validator-implementacioja.md) |
| Követelmény | DR-05, FR-31 |
| Teszt | TR-01 |

## Cél

A validátor minden szabályának lefedése. **Ez az első valódi zöld tesztfuttatás a projektben.**

## Teendők

- [ ] Táblázatos (parametrizált) tesztkészlet minden `V` kódra, mindegyik a kód nevét viseli.
- [ ] Külön teszt: érvénytelen bemenet **nem dob** kivételt.
- [ ] Külön teszt: a validátor nem emel státuszt (`AWAITING_REVIEW` bemenet → sosem `VERIFIED` kimenet).
- [ ] Teszt a valódi tartalomra: a `knowledgebase_transfer/` 161 rekordja 0 BLOKKOLÓ hibával megy át.

## Kész, ha

- [ ] `./gradlew testDebugUnitTest` **exit 0**, és a kimenet már NEM `NO-SOURCE`.
- [ ] A `docs/TEST_REPORT.md` frissítve a tényleges teszt- és hibaszámmal.

## Figyelj rá

- A tesztnevek tartalmazzák a `TR01` jelölést, pl. `validate_duplicateId_isRejected_TR01()`.
- A valódi tartalomra futó teszt védi meg a projektet attól, hogy a validátor csak szintetikus adaton működjön.

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
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 105 teszt, 0 hiba (a T-105 utáni 65-ről +40) |
| `.\gradlew.bat cleanTest testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – ugyanaz a 105 teszt újrafuttatva |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk`, 19 655 673 bájt |

**Mi készült el:**

- `KnowledgeValidationRuleTableTest` – parametrizált (`@RunWith(Parameterized::class)`)
  szabálytáblázat **31 nevesített esettel**; minden eset neve a lefedett V-kóddal kezdődik,
  így a JUnit-jelentés önmagában megmondja, melyik szabály bukott el.
- `KnowledgeValidationRuleCoverageTest` – gépi lefedettségi őr: egy új `IssueCode`
  felvétele eset nélkül **bukó teszttel** jár, nem csendben lefedetlen szabállyal.
- `KnowledgeValidatorGuaranteesTest` – a két szerződési garancia külön osztályban:
  8 romlási alak egyike sem dob kivételt (FR-31), és `reviewed` manifest + `verified = true`
  mellett sem emel státuszt a validátor (CR-02).
- `RealKnowledgeBaseValidationTest` – a `knowledgebase_transfer/` **valós 161 rekordja
  állandó tesztként**: 0 BLOKKOLÓ hiba, `accepted = 161`, `rejected = 0`, 10 `Asset`
  (ebből 7 kvízben használható), 0 szinonimacsoport. A T-105 ideiglenes, utána törölt
  próbája helyére ez lépett.
- `ValidationFixtures.kt` – a közös bemenetépítők (`manifest()`, `validEntry()`), hogy a
  táblázat és a `DefaultKnowledgeValidatorTest` egyedi esetei bitre ugyanazt az
  „egyébként érvényes" alapállapotot lássák. A meglévő 25 teszt **nem** íródott újra, csak
  a duplikált privát segédei kerültek ide.

**A `V010`:** nem a validátor állítja elő, hanem a `core/data` importréteg a
deszerializálás meghiúsulásakor (T-115). A táblázat ezért **tagadó esettel** rögzíti
(`V010_isNeverProducedByTheValidator`): nincs olyan `KnowledgeInput`, amiből V010
keletkezhetne, tehát a validátortól nem szabad V010-viselkedést várni.

**Megjegyzés:**

- *Eltérés a tervtől:* a `core/quiz` **teszt** forráskészlete új, csak teszt hatókörű
  függőséget kapott (`libs.kotlinx.serialization.json`, tiszta JVM, verzió a version
  catalogból), mert a valós tartalmon futó TR-01 teszt JSON-t olvas. A `main` forráskészlet
  továbbra sem lát szerializációt – a validátor bemenete `KnowledgeInput`, nem fájl
  (ADR-001 L1 sértetlen). A tartalomimport igazi helye változatlanul a `core/data` (T-115);
  a teszt-oldali JSON → `KnowledgeInput` leképezés szándékosan minimális, és ha T-115
  elkészül, a két leképezés eltérése maga is hiba.
- A `knowledgebase_transfer/` útvonalát a `kyo.repoRoot` rendszertulajdonság adja át
  (`core/quiz/build.gradle.kts` → `tasks.withType<Test>`), nem relatív `../..` hivatkozás:
  az csendben elromlana, ha a modul máshová kerül. A tartalom `app/src/main/assets/`-be
  másolása a T-601 dolga.
- *Nyitott forrásadat-hiba (nem ebben a körben javítandó):* a `KY-0020` rekord
  `japanese_romaji_ascii` értéke a forrás CSV-ben és JSON-ban is `"Kirotsuke"` a
  `"Ki o tsuke"` helyett. Forrásadat-hiba, nem validátor-hiba; az appot nem érinti, mert a
  validátor újraszámolja az ASCII alakot. Javasolt új task: a `knowledgebase/` forrás és a
  `knowledgebase_transfer/tools/convert.py` kimenetének javítása.
- `./gradlew lint ktlintCheck` **nem futott** – a ktlint konfigurációja még nincs beállítva
  (T-607). `./gradlew connectedDebugAndroidTest` **nem futott** – nincs eszköz/emulátor
  (B-03), és a T-106 nem tartalmaz instrumentált tesztet.
