# Tudástár – séma, bővítés, hitelesítés, licenc, migráció

> **Állapot: KIADOTT (T-608).** Ez a dokumentum a tényleges, csomagolt sémát írja le
> (`app/src/main/assets/knowledge/`), nem egy tervezett formátumot. Minden mezőnév, szabály
> és példa a kódból és a jelenlegi tartalomból lett kiolvasva – ha a kettő eltérne, a
> `/kyo-doc-sync` erre épít. A normatív szabályleírás ezen felül a
> **`.claude/skills/kyo-knowledge-base/SKILL.md`**-ben és a **`.claude/skills/kyokushin-content/SKILL.md`**-ben
> él; ha ez a dokumentum és a skill eltérne, a skillt is frissíteni kell (lásd a végén).

## Tartalomjegyzék

Angol/német tartalmi fordítások: [tárolás, mezők és lektorálási állapot](CONTENT_TRANSLATIONS.md).
Mind a 161 név és leírás a szerkesztési CSV/JSON magyar mezői mellett elérhető.

Terminológiai előellenőrzés: [T-708 – teljes forrásösszevetés és problémalisták](CONTENT_VERIFICATION.md),
[161 rekordos táblázat](CONTENT_VERIFICATION.csv). Ez nem változtat szakmai hitelesítési státuszt.

1. [Fájlszerkezet](#1-fájlszerkezet)
2. [`manifest.json` séma](#2-manifestjson-séma)
3. [`entries.json` rekordséma](#3-entriesjson-rekordséma)
4. [Leképezés a validátor bemenetére és a domain modellre](#4-leképezés-a-validátor-bemenetére-és-a-domain-modellre)
5. [Validációs szabályok (V001…V010)](#5-validációs-szabályok-v001v010)
6. [Új technika és új kép hozzáadása](#6-új-technika-és-új-kép-hozzáadása)
6a. [Illusztráció utólagos hozzáadása egy meglévő technikához](#6a-illusztráció-utólagos-hozzáadása-egy-meglévő-technikához)
7. [Hitelesítési folyamat](#7-hitelesítési-folyamat)
8. [Illusztrációk és licencelés](#8-illusztrációk-és-licencelés)
9. [Migráció: tartalomkiadás, séma, átnevezés, visszavonás](#9-migráció-tartalomkiadás-séma-átnevezés-visszavonás)
10. [Jelenlegi tartalmi állapot](#10-jelenlegi-tartalmi-állapot)

---

## 1. Fájlszerkezet

```
knowledgebase/                         ← SZERKESZTÉSI forrás (CSV + JSON + nyers képek)
  kyokushin_knowledge_hu.csv           ← a hiteles forrás; CSV és JSON eltérésnél a CSV nyer
  kyokushin_knowledge_hu.json
  pics/

knowledgebase_transfer/                ← GENERÁLT, köztes, ellenőrzött átadási forma
  manifest.json                        ← kategóriák, licencnyilvántartás, bizalmi szint
  entries.json                         ← a technikarekordok (ADR-011 kanonikus séma)
  illustrations/*.jpg
  VALIDATION_REPORT.md                 ← a konverter generálta jelentés
  tools/convert.py                     ← ez állítja elő a fentieket a `knowledgebase/`-ből

app/src/main/assets/knowledge/         ← az appba csomagolt, bitre azonos másolat (T-601)
  manifest.json
  entries.json
  illustrations/*.jpg
```

**Ez a dokumentum az `app/src/main/assets/knowledge/` alatti, csomagolt sémát írja le** –
ez az, amit a futó app betölt. A `knowledgebase_transfer/` ennek a köztes, verziókövetett
előállítási lépése; a `knowledgebase/` a tényleges szerkesztési felület. A három mappa
tartalma **szándékosan szinkronban tartott másolat**, nem három külön igazság: lásd a 6. és
9. pontot a frissítés pontos menetéről.

A csomagolt JSON **csak tartalom**. Felhasználói adat (kvízpróbálkozás, önértékelés,
munkamenet) soha nem kerül ebbe a fájlba – az a Room adatbázisban él, és az importáló
soha nem nyúl hozzá (lásd a 9. pont „Előrehaladás megőrzése" alpontját).

## 2. `manifest.json` séma

A kanonikus séma forrása **ADR-011**: a projekt a kapott tudástár sémáját használja
(`schema_version`, `dataset_version`, snake_case mezőnevek), nincs konverziós réteg vagy
párhuzamos „belső" formátum.

```json
{
  "schema_version": "1.0.0",
  "dataset_version": "0.1.0",
  "generated_at": "2026-11-11",
  "language": "hu-HU",
  "martial_art": "Kyokushin karate",
  "editorial_status": "draft_review_required",
  "dataset_trust": "practice_on_unreviewed",
  "source": {
    "csv": "knowledgebase/kyokushin_knowledge_hu.csv",
    "json": "knowledgebase/kyokushin_knowledge_hu.json",
    "images": "knowledgebase/pics",
    "converter": "knowledgebase_transfer/tools/convert.py"
  },
  "notes": ["..."],
  "categories": [
    { "id": "etiquette", "name_hu": "Etikett és köszönések", "sort_order": 1 }
  ],
  "illustrations": [
    {
      "ref": "KY-0044",
      "file": "illustrations/KY-0044.jpg",
      "content_type": "image/jpeg",
      "width": 1122,
      "height": 1402,
      "sha256": "be0189a3…",
      "origin": "ai_generated",
      "license": "project-internal",
      "source": "knowledgebase/pics (a projekt saját, generált készlete)",
      "author": null,
      "review_status": "human_review_required",
      "has_embedded_label": false,
      "quiz_usable": true,
      "hideable_on_study_card": true
    }
  ]
}
```

| Mező | Jelentés |
|---|---|
| `schema_version` | **Semver string**, nem egész szám. A támogatott halmaz jelenleg `{"1.0.0"}`; a támogatottság a **major** verzión dől el (`DefaultKnowledgeValidator.SUPPORTED_SCHEMA_MAJOR = 1`). Ismeretlen vagy olvashatatlan major verzió → **V001**, a teljes betöltés elutasítva. |
| `dataset_version` | A tartalomkiadás verziója. Ez dönti el a betöltés idempotenciáját (FR-30): ha a csomagolt érték megegyezik a már tárolttal, az importáló `ImportResult.UpToDate`-et ad, és se validáció, se adatbázisírás nem fut. |
| `editorial_status` | A tudástár **szerkesztői állapota egészben** (nem rekordonként). Ebből származtatja a validátor a [`DatasetTrust`](../core/model/src/main/kotlin/hu/kyokushin/trainer/core/model/DatasetTrust.kt) értéket: kizárólag a `"reviewed"` vagy `"production"` szó ad `REVIEWED`-et, minden más (beleértve a hiányzót és az elgépeltet is) a szigorúbb `PRACTICE_ON_UNREVIEWED`-et (ADR-010, CR-02 – bizalmi szintet kód soha nem emel). |
| `dataset_trust` | **Jelenleg nem olvassa be a validátor** (`ManifestInput`-nak nincs ilyen mezője) – dokumentációs/olvasói célt szolgál a manifestben, a tényleges kapu bemenete az `editorial_status`. Ha ez a mező a jövőben programatikusan is szerepet kapna, azt ADR-kiegészítésben kell rögzíteni. |
| `categories[]` | **Az egyetlen hiteles kategórialista.** `id` (stabil azonosító), `name_hu` (magyar megnevezés – nem a kódban/`strings.xml`-ben), `sort_order` (UI-sorrend; hiányában a tömbbeli sorrend). |
| `illustrations[]` | A kép-licencnyilvántartás (lásd a 8. pontot). Minden `entries[].illustration_ref` érték itt szereplő `ref`-re hivatkozik, vagy `null`. |

## 3. `entries.json` rekordséma

```json
{
  "schema_version": "1.0.0",
  "dataset_version": "0.1.0",
  "entries": [
    {
      "id": "KY-0001",
      "category_id": "etiquette",
      "japanese_romaji": "Osu",
      "japanese_romaji_ascii": "Osu",
      "japanese_script": null,
      "hungarian_name": "Tiszteletteljes köszönés és válasz",
      "description_hu": "A Kyokushinban gyakran használt köszönés, megerősítés és a kitartás kifejezése.",
      "aliases": [],
      "tags": [],
      "illustration_ref": null,
      "review_status": "editorial_review_required",
      "quiz_excluded": false,
      "retired": false,
      "search_romaji": "osu",
      "search_hungarian": "tiszteletteljes koszones es valasz",
      "incomplete": false
    }
  ]
}
```

| Mező | Kötelező? | Jelentés |
|---|---|---|
| `id` | igen | **Stabil és örök** azonosító (`KY-NNNN`). Átnevezéskor nem változik; törlés/visszavonás után sem használható fel újra (lásd 9. pont). |
| `category_id` | igen | A manifest `categories[].id` egyikére kell hivatkoznia (V004). |
| `japanese_romaji` | igen | A technika neve latin átírásban – a hiteles forma. |
| `japanese_romaji_ascii` | nem (ha hiányzik, a validátor származtatja) | Diakritikus jelek nélküli, de a szóalakot megtartó alak (`NameNormalizer.toAscii`). A fájlban **jelen van** (a konverter előre kitölti), de a validátor ettől függetlenül saját maga is számolja és mindig felülírja a domain mezőt – lásd 4. pont. |
| `japanese_script` | nem | Az eredeti japán írásmód, ha ismert. A jelenlegi tartalomban mindenhol `null`. |
| `hungarian_name` | igen | A magyar jelentés. |
| `description_hu` | nem | Rövid magyar leírás. |
| `aliases` | nem, de **mindig lista** (sosem `null`) | További elfogadott elnevezések. A V009 szabály tisztítja (üres/önütköző/duplikált elem eldobva). |
| `tags` | nem | A jelenlegi tartalomban mindenhol üres. Egyetlen validációs szabály sem használja (lásd `KnowledgeInputMapper` – szándékosan nem kerül át a validátor bemenetébe). |
| `illustration_ref` | nem | A manifest `illustrations[].ref` egyikére hivatkozik, vagy `null`, ha a rekordnak nincs képe. **Nem útvonal** – lásd 8. pont, miért. |
| `review_status` | igen (a validátor ettől függ) | `"reviewed"` vagy bármi más (jelenleg `"editorial_review_required"`). **Csak** a szó szerinti `"reviewed"` érték jelent hitelesítést (`KnowledgeInputMapper.REVIEW_STATUS_REVIEWED`); minden más a szigorúbb olvasatot kapja. |
| `quiz_excluded` | nem (alapértelmezés `false`) | Szerkesztői döntés: a rekord hiteles és ép, de pedagógiai okból nem kvízelhető. A kvízmotor ezt feltétlenül kizáró feltételként kezeli (`QuizEligibility.isEligible`). |
| `retired` | nem (alapértelmezés `false`) | A technika kikerült a tananyagból, de a rekord megmarad (ADR-004) – lásd 9. pont. |
| `search_romaji`, `search_hungarian` | nem | Előre normalizált keresési alak (FR-03). A `core/data` keresése ezt használja; **nem** kerül át a validátor bemenetébe, mert az maga is normalizál (`NameNormalizer`) – két hely, két igazság lenne. |
| `incomplete` | nem | **A konverter saját megállapítása** a forrás-CSV-ből. A validátor **újraszámolja** a saját V002/V005 szabályaiból, és ezt a fájlbeli értéket nem olvassa be – a két „hiányos" fogalom szándékosan független (a konverteré diagnosztikai, a validátoré a tényleges `VerificationStatus.INCOMPLETE`). |

**Amire nincs mező, és miért:**

- **`verified` / `quiz_eligible` nincs a fájlban.** A `review_status` helyettesíti a
  `verified`-et (lásd fent); a `quiz_eligible` pedig **számított** érték (ADR-010 formula,
  lásd `docs/QUIZ_RULES.md`), sosem tartalmi adat – tárolása két igazságforrást jelentene.
- **`belt_rank` / `difficulty` nincs a fájlban és a `library/detail` képernyő sem jeleníti
  meg.** Ez dokumentált drift a `FUNCTIONAL_SPEC.md`-vel szemben (lásd
  `docs/CONVENTIONS.md` 8. szakasz) – az ADR-011 kanonikus séma nem ad ilyen mezőt a
  technikáknak, és kódból kitalálni tilos (CLAUDE.md tartalmi integritás).
- **`source` (technikaszintű)** sincs a rekordon – csak az illusztrációknak van `source`
  mezőjük (8. pont). A `library/detail` ezért soha nem rajzol ki technikaszintű forrást.

## 4. Leképezés a validátor bemenetére és a domain modellre

A csomagolt, snake_case JSON **nem közvetlenül** kerül a validátorba. Egyetlen,
mellékhatás-mentes leképezési réteg (`core/data` `KnowledgeInputMapper.kt`) alakítja át a
validátor camelCase bemeneti típusaira (`core/quiz` `KnowledgeInput`/`TechniqueInput`):

| Fájlmező (`entries.json`) | Validátor bemenete (`TechniqueInput`) | Megjegyzés |
|---|---|---|
| `id` | `id` | |
| `category_id` | `categoryId` | |
| `japanese_romaji` | `romaji` | |
| `hungarian_name` | `hungarian` | |
| `japanese_script` | `japaneseScript` | |
| `aliases` | `aliases` | |
| `illustration_ref` | `illustration` | |
| `description_hu` | `descriptionHu` | |
| `review_status == "reviewed"` | `verified` | **Számolt logikai érték**, nem közvetlen olvasás. |
| — | `quizEligible = null` | A fájlban nincs ilyen mező; a validátor bemenetében szándékosan `null` marad. |
| `quiz_excluded` | `quizExcluded` | |
| `retired` | `retired` | |
| `japanese_romaji_ascii`, `search_romaji`, `search_hungarian`, `tags`, `incomplete` | *(nem kerül át)* | Lásd a 3. pont táblázatát: mind származtatott vagy nem használt mező. |

A manifest leképezésénél két tudatos eltérés van:

- `dataset_version` → `ManifestInput.contentVersion` (a validátor szótárában a
  *tartalomkiadás* verziója `contentVersion`, a *séma* verziója `schemaVersion`).
- `editorial_status` → `ManifestInput.datasetKind` (a bizalmi szint a szerkesztői
  állapotból jön, nem egy önbevallásos `dataset_trust`-ból – CR-02).

A validátor (`DefaultKnowledgeValidator`) ebből a bemenetből állítja elő a domain
`Technique`-et (`core/model`):

| Validátor bemenet / döntés | Domain mező (`Technique`) |
|---|---|
| `romaji` | `romaji` |
| `NameNormalizer.toAscii(romaji)` (**mindig újraszámolva**, nem a fájl `japanese_romaji_ascii`-jából) | `romajiAscii` |
| `hungarian` | `hungarian` |
| a kötelező mezők és a kép épek → `VerificationStatus.AWAITING_REVIEW`, ha `verified == true` → `VerificationStatus.VERIFIED`, ha a képhivatkozás lóg vagy kötelező mező hiányzik → `VerificationStatus.INCOMPLETE` (ADR-002) | `status` |
| a képhivatkozás feloldása a manifest nyilvántartása + a csomagban tényleg létező fájlok ellen (lásd 5. pont V005) | `illustration: IllustrationRef?` (ADR-003) |
| `quizExcluded`, `retired` | ugyanaz a mező, változatlanul |

**Fontos, nem nyilvánvaló következmény:** mivel a bemeneti `quizEligible` mindig `null`
(a fájlban nincs ilyen mező), a validátor **V007/V008 szabálya a jelenlegi tartalmon soha
nem fut le** – ezek a szabályok csak akkor aktívak, ha egy jövőbeli tartalomforrás
tényleg küldene `quiz_eligible`-szerű kérést a fájlban. A tényleges kvízjogosultságot ma
kizárólag a `QuizEligibility` formula számolja ki, futásidőben, a `status`/`quizExcluded`/
`retired`/mezőteljesség/`DatasetTrust` alapján – lásd `docs/QUIZ_RULES.md` 1. pontját.

## 5. Validációs szabályok (V001…V010)

A validátor **tiszta Kotlin** (`core/quiz/.../validation/DefaultKnowledgeValidator.kt`),
Android-függőség nélkül tesztelhető (TR-01: `DefaultKnowledgeValidatorTest`,
`KnowledgeValidatorContractTest`, `KnowledgeValidationRuleTableTest`,
`RealKnowledgeBaseValidationTest`). Kimenete sosem kivétel, hanem egy strukturált
`ValidationReport(accepted, rejected, issues, synonymGroups, datasetTrust, …)`.

| Kód | Szabály | Súlyosság | Hatókör | Hatás |
|---|---|---|---|---|
| **V001** | `schema_version` major verziója támogatott (`{1}`) | BLOKKOLÓ | `DATASET` | A teljes betöltés elutasítva; `accepted` üres. |
| **V002** | Kötelező mező (`id`, `categoryId`, `romaji`, `hungarian`, és a manifest `contentVersion`-je) nem üres/blank | BLOKKOLÓ | `RECORD` | A rekord elvetve. |
| **V003** | `id` egyedi a fájlon belül | BLOKKOLÓ | `RECORD` | A **későbbi** (nagyobb indexű) rekord elvetve. |
| **V004** | `categoryId` szerepel a manifest `categories[]` között | BLOKKOLÓ | `RECORD` | A rekord elvetve. |
| **V005** | A képhivatkozás feloldható: a `ref` szerepel a manifest `illustrations[]`-ben, **van** licence, és a fájl tényleg a csomagban van | ismeretlen `ref`: BLOKKOLÓ; hiányzó licenc/fájl: FIGYELM. | `FIELD` | Ismeretlen `ref` → a rekord `INCOMPLETE`-re lefokozva, `Placeholder` képpel. Hiányzó licenc/fájl → a kép `Placeholder`-re fokozva, a rekord státusza **nem** romlik, csak a képes kvíztípusokból esik ki. |
| **V006** | Két rekord `romaji`/`hungarian`/`aliases`-e normalizálva (`NameNormalizer`) egyezik | FIGYELM. | `FIELD` | A pár kölcsönös distraktor-kizárási csoportba kerül (`synonymGroups`, CR-07). |
| **V007** | `quizEligible = true` nem hitelesített (`INCOMPLETE`) rekordon | BLOKKOLÓ | `FIELD` | A jelzés lefokozva – **a jelenlegi tartalmon sosem fut le**, mert a fájlban nincs `quiz_eligible` mező (lásd 4. pont). |
| **V008** | `quizEligible = true`, de nincs kvízben használható kép | BLOKKOLÓ | `FIELD` | Ugyanaz a megjegyzés, mint V007-nél. |
| **V009** | `aliases` elemei nem üresek, nem egyeznek a saját `romaji`/`hungarian`-jával, nem duplikáltak | FIGYELM. | `FIELD` | A hibás alias eldobva, a többi megmarad. |
| **V010** | A JSON egyáltalán parse-olható | BLOKKOLÓ | `DATASET` | **Ezt nem a validátor állítja elő**, hanem a `core/data` importréteg, amikor a deszerializálás meghiúsul – ilyenkor a validátor sosem fut le. |

**A feldolgozás sorrendje egy rekordon belül:** V002 → V003 → V004 (ha bármelyik BLOKKOLÓ
RECORD-hatókörű, a rekord itt elvetve, a többi szabály már nem fut rá) → V009 (alias
tisztítás) → V005 (kép feloldása, és ebből a státusz) → domain objektum építése → V007/V008
(kvízképesség-lefokozás) → **a teljes elfogadott listán** V006 (szinonimafelderítés).

**Amit a validátor soha nem tesz:**

- **Nem dob kivételt.** Ha a domain `Technique` konstruktora bármilyen, itt nem előre
  látott invariánst sértene, az is V002-ként *jelentésbe* kerül, nem a hívó nyakába (FR-31).
- **Nem emel státuszt.** `VerificationStatus.VERIFIED` kizárólag a tartalom
  `review_status: "reviewed"` értékéből keletkezhet; a hibás képhivatkozás *lefokozza*
  `INCOMPLETE`-re. Felfelé nincs kódút (CR-02, ADR-002).
- **Nem nyúl fájlrendszerhez, órához, RNG-hez** – a csomagban tényleg létező fájlnevek
  listáját (`availableIllustrationFiles`) és a bizalmi szintet **bemenetként** kapja, nem
  maga állapítja meg.

## 6. Új technika és új kép hozzáadása

Ez a folyamat **nem igényel UI- vagy kvízmotor-kódmódosítást** (FR-30, AC-08):

1. **Szerkesztés a forrásban:** új sor felvétele a `knowledgebase/kyokushin_knowledge_hu.csv`-be
   (ez a hiteles forrás – ha a párhuzamos JSON eltér, a konverter `X003` kóddal jelzi és a
   CSV nyer). Ha új kép is jár a technikához: a képfájl bemásolása a
   `knowledgebase/pics/` alá.
2. **Konverter futtatása:**
   ```bash
   python knowledgebase_transfer/tools/convert.py
   ```
   Ez előállítja/frissíti a `knowledgebase_transfer/manifest.json`, `entries.json`,
   `illustrations/` és `VALIDATION_REPORT.md` fájlokat. A konverter nem dob kivételt
   tartalmi hibára: jelenti (`VALIDATION_REPORT.md`), és a rekordot megjelöli.
3. **Új kép felirat-ellenőrzése (ha van kép):** nézd meg a képet. Ha rajta látszik a
   technika azonosítója, japán vagy magyar neve, vedd fel a `convert.py`
   `IMAGES_WITH_EMBEDDED_LABEL` halmazába – ennek elmulasztása hibás (a választ eláruló)
   kvízkérdést eredményezne (lásd 8. pont).
4. **Átmásolás az app csomagjába** (T-601 mintája szerint, nincs Gradle copy-task):
   ```bash
   cp knowledgebase_transfer/manifest.json app/src/main/assets/knowledge/manifest.json
   cp knowledgebase_transfer/entries.json app/src/main/assets/knowledge/entries.json
   cp knowledgebase_transfer/illustrations/*.jpg app/src/main/assets/knowledge/illustrations/
   ```
5. **`dataset_version` léptetése** a `knowledgebase/` forrásban (a konverter ezt átemeli
   mindkét generált manifestbe) – enélkül az `ContentImporter.importIfNeeded()`
   `ImportResult.UpToDate`-et ad, és az új rekord soha nem töltődik be (FR-30
   idempotencia-kapu a `dataset_version` egyezésén dől el).
6. **Ellenőrzés:**
   ```bash
   ./gradlew.bat testDebugUnitTest
   ./gradlew.bat assembleDebug
   ```
   A `core/quiz` oldali validátor-tesztek (TR-01) és az `app` modul
   `KnowledgeAssetImportTest` (T-601 – a **valódi**, csomagolt assetet tölti be a valódi
   `ContentImporter`-rel) ezen a ponton bizonyítják, hogy az új rekord BLOKKOLÓ hiba
   nélkül betöltődik.
7. **Új rekord kezdő állapota:** `review_status: "editorial_review_required"` (nem
   `"reviewed"`) – a modell soha nem állíthatja hitelesnek a saját generált tartalmát
   (CLAUDE.md, `kyokushin-content` skill 1. pont). A szakmai hitelesítés emberi döntés,
   lásd 7. pont.

A 2–6. lépés **kódmódosítás nélkül** megy végig: a `techniques.json`/`manifest.json`
mezői, a validátor szabályai és a `ContentImporter` upsert-logikája eleve ezt a forgatókönyvet
szolgálják ki. Kódmódosítást **csak** egy valódi sémaváltozás (új kötelező mező, új
`schema_version` major) igényelne – ez a 9. pont tárgya, és nem ez a rendes bővítési út.

## 6a. Illusztráció utólagos hozzáadása egy meglévő technikához

Ez a szakasz a 6. pont egy **szűkebb, gyakoribb esete** (T-707, FR-41): amikor egy már
meglévő, `illustration_ref: null` technikarekordhoz utólag – például a felhasználótól
kapott képből – kerül illusztráció, nem egy teljesen új technikasor felvétele.

**A fájlrendszeri út és a pipeline azonos a 6. ponttal, nincs "kézi" rövidítés.** A kép
soha nem kerül kézzel az `app/src/main/assets/knowledge/illustrations/` alá – az mindig a
`convert.py` kimenete. A teendő:

1. A képfájl bemásolása a `knowledgebase/pics/` alá, **a technika `id`-jával megegyező
   fájlnévvel** (pl. `KY-0120` technikához `knowledgebase/pics/KY-0120.jpg`; `.jpg`/
   `.jpeg`/`.png`/`.webp` mind felismert).
2. A technikarekord `illustration_ref` mezőjének kitöltése a CSV-ben (ha még üres),
   ugyanarra az `id`-ra.
3. Lásd 6. pont 2., 3., 4., 6. lépése változatlanul (konverter futtatása, felirat-ellenőrzés,
   átmásolás az app csomagjába, build/teszt ellenőrzés), és az 5. lépés (`dataset_version`
   léptetése).

### A valódi különbség: a kép eredetének (`origin`/`license`/`source`/`author`) pontossága

**Kódból feltárt, dokumentált hiányosság (T-707 vizsgálata, nincs hozzá külön backlog-tétel
még):** a `convert.py` jelenleg **egyetlen, globális** `ILLUSTRATION_PROVENANCE`
konstansból tölti ki MINDEN illusztráció `origin`/`license`/`source`/`author` mezőjét
(`"ai_generated"` / `"project-internal"` / …) – függetlenül attól, hogy a kép tényleg honnan
származik. A `knowledgebase/pics/manifest.json` per-kép forrásfájl csak a `review_status`-t
tárolja per-kép szinten, az eredetet nem – így a jelenlegi pipeline-nak **nincs** olyan
bemenete, amiből egy felhasználó-adta (nem AI-generált) kép helyes eredetét automatikusan
levezetné.

**Ha a felhasználó saját, nem AI-generált fényképet ad egy technikához, a jelenlegi
pipeline tévesen `"ai_generated"`/`"project-internal"` eredetet írna rá.** Ezért utólagos,
felhasználó által adott kép esetén **kötelező egy kézi lépés**: a `convert.py` futtatása
után, az app csomagjába átmásolás **előtt**, nyisd meg a
`knowledgebase_transfer/manifest.json`-t, és az érintett `illustrations[]` bejegyzésen írd
át kézzel:

| Mező | Felhasználó-adta kép esetén |
|---|---|
| `origin` | A tényleges eredet (pl. `"user_provided"` vagy `"photograph"`) – **ne** hagyd `"ai_generated"`-en, ha nem az. |
| `license` | A felhasználó által megadott/jóváhagyott licenc – **ne** találj ki egyet (CLAUDE.md tartalmi integritás). |
| `source` | A kép tényleges forrása (pl. "a felhasználó saját fotója, dátum"). |
| `author` | Ha a felhasználó megnevezi magát/a fotóst; egyébként marad `null`. |
| `review_status` | Változatlanul `"human_review_required"` marad – **az eredet pontosítása nem hitelesítés** (lásd alább). |

Ezt a kézi javítást **minden** `dataset_version`-léptetéskor meg kell ismételni, amíg a
`convert.py`/`pics/manifest.json` nem kap per-kép eredet-felülírást – a konverter
újrafuttatásakor a globális konstans felülírná a kézi javítást.

**Javasolt, fel nem vett követő task:** `convert.py` bővítése, hogy a
`knowledgebase/pics/manifest.json` (vagy egy új mező benne) per-kép `origin`/`license`/
`source`/`author` adatot olvasson be, és csak hiányzó adat esetén essen vissza a globális
`ILLUSTRATION_PROVENANCE`-ra. Ez jelen task hatókörén (`docs`, `core/data`) kívül esik,
mert a `knowledgebase_transfer` Python-eszköztárát módosítaná.

### A hitelesítési státusz nem emelkedik automatikusan

A kép megléte – akár AI-generált, akár felhasználó-adta – **soha nem** állítja a
technikarekord `review_status`-át `"reviewed"`-re, és a kép saját `review_status`-a is
`"human_review_required"` marad, amíg a 7. pont szerinti szakmai ellenőrzés meg nem történik
(CLAUDE.md tartalmi integritás, CR-02, ADR-002). A kép eredetének pontosítása (fent) és a
szakmai hitelesítés **két külön lépés**; az első sosem helyettesíti a másodikat.

### Formátum/méret előzetes ellenőrzése – nincs ilyen automatizmus, ez szándékos döntés

A `DefaultKnowledgeValidator` (V005, lásd 5. pont) **csak** azt ellenőrzi, hogy a hivatkozott
fájl a csomagban megvan-e, és hogy a hivatkozás nem lóg-e a semmibe (BLOKKOLÓ, ha a `ref`
nincs regisztrálva; FIGYELMEZTETÉS, ha regisztrált, de a fájl hiányzik – státuszlefokozás
nélkül). A kép **formátumát** (dekódolható-e Androidon) vagy **méretét** (fájlméret,
felbontás) import előtt **senki nem vizsgálja** – `KnowledgeAssetSource`/`KnowledgeDto`/
`KnowledgeInputMapper` csak fájlnév-egyezést néz.

Ebben a körben **szándékosan nem készült** hozzá ellenőrző szkript vagy Gradle-feladat: a
hiányosság valós, de a pótlása önálló döntést igényelne (melyik képfeldolgozó függőséget
kapja a `knowledgebase_transfer` Python-eszköztára, milyen méretkorlátot szabjon) – ez
túlmutat egy dokumentációs taskon, és a task Teendője is csak feltételesen kérte ("csak ha
ez tényleg hiányzik... és akkor is csak ha ez nem túl nagy falat"). **Gyakorlati
védőháló helyette:** a `./gradlew testDebugUnitTest` (`KnowledgeAssetImportTest`, T-601)
mindig a **valódi**, csomagolt asseteket tölti be a valódi `ContentImporter`-rel – egy
teljesen hibás (nem kép) fájl itt hasalna el, mielőtt kiadásra kerülne.

## 7. Hitelesítési folyamat

**Két külön szintű hitelesítés van, és ezt a kettőt nem szabad összekeverni:**

| Szint | Mező | Hatása |
|---|---|---|
| **Rekordszintű** | `entries.json` → `review_status` | Csak a `"reviewed"` érték számít hitelesítésnek. Ez dönti el a `VerificationStatus` (`VERIFIED`/`AWAITING_REVIEW`/`INCOMPLETE`) UI-jelölőjét (badge) a Tudástárban. **Nem** ez dönti el a kvízjogosultságot – lásd lent. |
| **Teljes tudástár szintje** | `manifest.json` → `editorial_status` | Ez dönti el a `DatasetTrust` (`REVIEWED`/`PRACTICE_ON_UNREVIEWED`) értéket, azaz a „Lektorálatlan gyakorlóanyag" szalag (ADR-010) megjelenését minden képernyőn. |

**Fontos, kódból levezetett tény, amire a tartalomszerkesztőnek figyelnie kell:**
a kvízjogosultság (`QuizEligibility.isEligible`) **sem** a rekord `review_status`-ától,
**sem** a manifest `editorial_status`-ától nem függ közvetlenül – csak attól, hogy a
rekord **nem** `INCOMPLETE` (azaz nincs lógó képhivatkozása és minden kötelező mezője
megvan), nincs `quiz_excluded`/`retired` jelölve, és megvannak a kérdéstípushoz szükséges
mezők. A `DatasetTrust` két lehetséges értéke (`REVIEWED`, `PRACTICE_ON_UNREVIEWED`)
**mindkettő** átengedi a kaput (ADR-010 szándékos döntése: a kvíz lektorálatlan
tartalommal is futhat, feltűnő jelzéssel) – ezért a `review_status` átállítása **nem
változtatja meg**, hogy egy rekord kvízképes-e, csak azt, hogy a Tudástárban megjelenik-e
rajta az „Ellenőrzésre vár" jelölő.

**A hitelesítés menete, lépésről lépésre:**

1. A szakmai hitelesítő (dojo/szenszei/szövetség – **a konkrét személyt vagy szervezetet a
   felhasználónak kell megneveznie, ez nyitott kérdés, lásd OQ-02 lent**) átnézi a rekordot
   a `knowledgebase/kyokushin_knowledge_hu.csv`-ben: a `japanese_romaji`, a `hungarian_name`,
   és – ha van kép – a hozzá tartozó illusztráció testhelyzete és mozgásiránya.
2. Ha a rekord helyes, a hitelesítő (vagy a nevében dolgozó tartalomszerkesztő) átírja a
   CSV `review_status` oszlopát `"reviewed"`-re.
3. Ha egy kép szakmailag helyesnek bizonyul, a hozzá tartozó manifest-bejegyzés
   `review_status` mezője (`human_review_required` → pl. `reviewed`) frissül – ez **nem**
   azonos a technikarekord `review_status`-ával: a kép és a technika review-ja két külön
   nyilvántartás (lásd 8. pont).
4. A `convert.py` újrafuttatása, átmásolás az app csomagjába, `dataset_version` léptetése –
   ugyanaz a menet, mint a 6. pontban.
5. Ha **az egész tudástár** elérte a lektorált állapotot (minden rekord `reviewed`), a
   `manifest.json` `editorial_status` mezője állítható `"reviewed"`-re – ettől a
   `DatasetTrust.REVIEWED` lesz, és a „Lektorálatlan gyakorlóanyag" szalag automatikusan,
   **kódmódosítás nélkül** eltűnik minden képernyőről (AC-08, ADR-010).

**Nyitott kérdés (OQ-02, változatlanul nyitva):** ki a szakmai hitelesítő (szenszei, dojo
vagy szövetség)? A CLAUDE.md tartalmi integritás szabálya szerint ezt **nem lehet
kitalálni** – a folyamat lépései a fentiek szerint rögzítettek, de a konkrét személyt vagy
szervezetet a felhasználónak kell megneveznie, mielőtt bármely rekord `review_status`-a
valóban `"reviewed"`-re állhatna.

## 8. Illusztrációk és licencelés

A kép metaadatai **egyetlen helyen** élnek: a `manifest.json` `illustrations[]`
nyilvántartásában. A technikarekord csak az `illustration_ref` mezőn hivatkozik rá – soha
nem útvonalon. Ennek az az oka, hogy a licenc, az eredet és a kvízhasználhatóság
egy helyen marad, nincs két igazság róla (ADR-011, CR-04).

### Licenctáblázat (a jelenlegi 10 illusztráció)

| Mező | Jelenlegi érték | Jelentés |
|---|---|---|
| `origin` | `"ai_generated"` | Minden illusztráció a projekt saját, AI-generált oktatási képkészlete (`knowledgebase/pics`), **nem** harmadik fél appjából vagy weboldaláról kinyert kép (CR-04 tiltása nem sérül). |
| `license` | `"project-internal"` | A projekt saját, belső felhasználásra szánt képe. |
| `source` | `"knowledgebase/pics (a projekt saját, generált készlete)"` | |
| `author` | `null` | Nincs megnevezett egyéni szerző (generált kép). |
| `review_status` | `"human_review_required"` (mind a 10 képen) | A testhelyzet és a mozgásirány szakmai felülvizsgálata **még nem történt meg** – ez független a technikarekord `review_status`-ától. |

### A `quiz_usable` és `hideable_on_study_card` kapu – miért nem elég a `review_status`

A beérkezett 10 illusztrációból **háromba bele van égetve** a technika azonosítója, japán
és magyar neve: **KY-0045** (Heisoku dachi), **KY-0050** (Zenkutsu dachi), **KY-0052**
(Kiba dachi). Ez a feliratos kép:

- a (C) „ábra → japán név" kérdésnél **leírja a helyes választ**;
- a (D) „japán név → ábra" kérdésnél **elárulja**, melyik opció a helyes;
- a tanulókártyán elrejtett néven **felfedi a választ** felfedés előtt.

Ez **független** attól, hogy a kép szakmailag hiteles-e: egy tökéletesen ellenőrzött,
feliratos kép is használhatatlan kvízben. Ezért minden illusztráció **két önálló kaput**
hordoz:

| Mező | Mit jelent | Mire hat |
|---|---|---|
| `review_status` (az illusztráción) | szakmailag ellenőrzött-e a testhelyzet/mozgásirány | az „ellenőrzésre vár" jelölés a képen |
| `has_embedded_label` → `quiz_usable`, `hideable_on_study_card` | felfedi-e a kép a választ | a (C)/(D) kvíztípus és a tanulókártya felfedés-előtti megjelenítése |

A `has_embedded_label` megállapítása **emberi megfigyelés** (a `convert.py`
`IMAGES_WITH_EMBEDDED_LABEL` halmaza, dátumozva: 2026-11-11, mind a 10 kép vizuálisan
átnézve) – **nem** automatikus szövegfelismerés. Új kép felvételekor ezt a listát kézzel
kell karbantartani (lásd 6. pont, 3. lépés). A hosszú távú jobb megoldás: felirat nélküli
képeket generálni, és a nevet az app rajzolja a kép mellé – akkor ez a lista kiürül, és
mind a 10 (és minden jövőbeli) kép kvízképes lehet.

A domain modellben (`IllustrationRef.Asset`) ez két `Boolean` mező: `quizUsable` és
`hideableOnStudyCard`. Hiányzó (`null`) bemeneti érték esetén a validátor mindig a
**szigorúbb** olvasatot választja (`quizUsable == true` kell az explicit igenhez;
`hideableOnStudyCard != false`, tehát hiányzó mező esetén alapértelmezésben felfedés
mögé rejtendő) – soha nem a megengedőbb irányba (ADR-011 kiegészítés).

### Tilalmak (változatlanul érvényesek, `kyokushin-content` skill 4. pontja)

- **Tilos** harmadik fél alkalmazásából, weboldalról, könyvből kinyert, licenc nélküli kép.
- **Tilos** generatív képmodellel előállított „karate technika rajz" hiteles tananyagként –
  a testhelyzet és a mozgásirány tipikusan hibás; ezért van minden képen
  `review_status: human_review_required`, amíg szakmai felülvizsgálat nem történt.
- **Megengedett** az explicit képhelyőrző (`illustration_ref: null`, vagy a V005 szabály
  által lefokozott `Placeholder`). Ilyenkor a (C)/(D) kvíztípus nem aktiválható arra a
  technikára.

## 9. Migráció: tartalomkiadás, séma, átnevezés, visszavonás

Négy, egymástól élesen eltérő „migráció" van ebben a projektben – fontos nem összekeverni
őket:

### 9.1 Rendes tartalomkiadás (`dataset_version` léptetés) – **kódmódosítás nélkül**

Ez a 6. pontban leírt, mindennapi folyamat: új/módosított rekord, `dataset_version`
léptetés, átmásolás, `ContentImporter.importIfNeeded()` upsertel. Ha a `dataset_version`
nem változik, a betöltés **nem fut** (FR-30 idempotencia) – ez a leggyakoribb hiba, ha egy
„frissítés" nem látszik az appban.

### 9.2 Technika átnevezése – **kódmódosítás nélkül, az `id` nem változik**

Az `id` **stabil és örök**: átnevezéskor csak a `japanese_romaji`/`hungarian_name` (és az
ebből származó `search_*`/`*_ascii` mezők) változnak a forrásban, az `id` marad. Mivel a
Room `quiz_attempt`/`study_review` táblák `techniqueId`-t tárolnak, nem szöveget, a
felhasználó korábbi előrehaladása **automatikusan** az új névvel jelenik meg – ehhez sem
UI-, sem adatbázis-kódmódosítás nem kell, csak a 6. pont szerinti tartalomkiadás.

### 9.3 Technika visszavonása (`retired`) – **kódmódosítás nélkül**

Ha egy technika kikerül a tananyagból, **nem törlendő** a forrásból úgy, hogy az `id`
eltűnjön a fájlból *és* fizikailag töröljön egy korábban betöltött rekordot – a helyes
lépés a `retired: true` beállítása az `entries.json`-ban (vagy a forrás CSV-ben, konverter
által átemelve). A `ContentImporter`:

- **upsertel**, soha nem `DELETE ALL + INSERT`-el (ADR-004);
- a csomagból **kimaradt** (korábban elfogadott, most nem szereplő) technikát maga jelöli
  `retired = true`-ra (`ImportResult.Imported.retiredCount`) – ha egy rekordot egyszerűen
  kihagynak a fájlból, a hatás ugyanaz, mint explicit `retired: true`-t írni;
- a `quiz_attempt`/`study_review` táblák `techniqueId` oszlopán **nincs** `CASCADE DELETE`
  (sem FK-kényszer egyáltalán) – a visszavont technika korábbi próbálkozásai megmaradnak,
  hivatkozásuk értelmes marad (FR-33, TR-05).
- Egy visszavont technika **nem** jelenik meg a Tudástár listában, a Tanulás kártyái között
  és a kvízjelöltek között (`QuizEligibility.isEligible` explicit kizárja), de a Haladás
  képernyőn a korábbi eredménye látható marad.
- A visszavont technika `id`-ja **soha nem használható fel újra** más fogalomra.

### 9.4 `schema_version` emelés – **ez IGEN kódmódosítást igényel, nem rutinfeladat**

Ez a `manifest.json`/`entries.json` **szerkezeti** formátumának major-verziós váltása (ma
`"1.0.0"`, támogatott major: `1`). Ez **nem** azonos a 9.1 pont rutin tartalomkiadásával,
és **nem** azonos a Room adatbázis sémaverziójával (`docs/CONVENTIONS.md` 2. szakasz – a
Room séma jelenleg a 2-es verzión van, saját `Migration`-jeivel, `ADR-012` mintával). A
tartalom-séma major verziójának emelése:

- új, `DefaultKnowledgeValidator.SUPPORTED_SCHEMA_MAJOR` bővítést vagy cserét igényel (a
  validátor kódjában);
- valószínűleg új `TechniqueInput`/`ManifestInput` mezőket és egy frissített
  `KnowledgeInputMapper`-t igényel, ha a séma új kötelező mezőt vezet be;
- önálló ADR-t kap (az ADR-011 mintájára), amely rögzíti, mi változott és miért;
- a meglévő V001 szabály a **régi** major verziójú csomagot (helyesen) elutasítja, amíg a
  kód nem bővül – ez a szándékos védőháló, nem hiba.

Nincs ilyen emelés folyamatban; ha egy jövőbeli tartalomforrás `schema_version: "2.0.0"`-t
adna, az app ma **V001**-gyel, a teljes betöltés elutasításával válaszolna, a korábbi
tartalommal futva tovább (FR-31) – ez a dokumentált, biztonságos alapállás.

## 10. Jelenlegi tartalmi állapot

| | |
|---|---|
| Rekord | **161** (hiányos: 0) |
| Kategória | **9** (`etiquette`, `commands`, `stances`, `blocks`, `punches`, `kicks`, `body_parts`, `numbers`, `kata`) |
| Illusztráció | **10**, mind a `stances` kategóriában (KY-0044…KY-0053) |
| `dataset_version` | `0.1.0` |
| `editorial_status` | `draft_review_required` → `DatasetTrust.PRACTICE_ON_UNREVIEWED` – **minden rekord szakmai lektorálásra vár**, a „Lektorálatlan gyakorlóanyag" szalag mindenhol látható (ADR-010) |
| Kvízben használható kép | **7** a 10-ből (3 kiesik a beégetett felirat miatt – lásd 8. pont) |
| Szöveges kvíz ((A)/(B)) | mind a 9 kategóriában indítható (mindegyikben ≥ 4 kvízképes jelölt van) |
| Képes kvíz ((C)/(D)) | **csak** a `stances` kategóriában; a többi 8 kategóriában a kvízbeállító letiltja és megindokolja (FR-13) |

Részletes, generált bontás kategóriánként: `knowledgebase_transfer/VALIDATION_REPORT.md`.

**Ez tudatosan nem „demó" adatkészlet a korábbi (OQ-01-re adott) terv szerint** – ez a
tényleges, szakmai lektorálásra váró tartalom, amelyen a kvíz **futhat**, feltűnő
jelöléssel (ADR-010 felülírta a korábbi, szigorúbb szabályt, amely mellett a 161 rekordból
nulla lett volna kvízképes).

---

## Nyitott kérdések és eltérések

- **OQ-02 (nyitott):** a szakmai hitelesítő személye/szervezete nincs megnevezve – lásd a
  7. pont végét. Ezt a felhasználónak kell megneveznie, kódból vagy dokumentációból nem
  pótolható.
- **`dataset_trust` manifest-mező jelenleg olvasatlan** a validátor által (lásd 2. pont) –
  ha ez félreértést okozna egy jövőbeli tartalomszerkesztőnél, a tényleges kapu bemenete az
  `editorial_status`.
- A `.claude/skills/kyo-knowledge-base/SKILL.md` 2–3. pontja még a **tervezett** (camelCase,
  `techniques.json`, `verified: Boolean` a fájlban) sémát írja le, amit az ADR-011 felülírt.
  Ez a dokumentum a tényleges sémát írja le; a skill frissítése a dokumentációs drift
  listán szerepel (`docs/CONVENTIONS.md` 8. szakasz) – nem ennek a tasknak (T-608) a
  hatóköre, mert a skill nem a kiadott dokumentáció, hanem a fejlesztői segédanyag.
