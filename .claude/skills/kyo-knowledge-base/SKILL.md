---
name: kyo-knowledge-base
description: A Kyokushin tudástár JSON sémája, importja, validálása, verziózása és migrációja. Használd amikor az assets/knowledge manifest.json vagy techniques.json fájlt, a séma mezőit, az importálót, a validátort, a kategóriákat, az illusztráció-hivatkozásokat, a contentVersion/schemaVersion kezelését vagy a tudástárfrissítés előrehaladás-megőrzését érinti a munka.
---

# Tudástár: séma, import, validáció

## 1. Fájlok és felelősségek

```
app/src/main/assets/knowledge/
  manifest.json        # séma- és tartalomverzió, kategóriák, illusztráció-nyilvántartás
  techniques.json      # a technikarekordok
app/src/main/assets/knowledge/illustrations/
  <illustration fájlok>.webp
```

A **csomagolt JSON csak tartalom**. Felhasználói adat (kvízpróbálkozás, önértékelés)
SOHA nem kerül a JSON-ba – az Room-ban él. Ez a kettéválasztás a tudástárfrissítés
feltétele (lásd 6. pont).

## 2. manifest.json

```json
{
  "schemaVersion": 1,
  "contentVersion": "0.1.0-demo",
  "datasetKind": "demo",
  "categories": [
    { "id": "tachi", "nameHu": "Állások", "sortOrder": 1 }
  ],
  "illustrations": [
    { "ref": "placeholder", "file": null, "license": "N/A", "source": null, "author": null }
  ]
}
```

- `schemaVersion`: **egész szám**, a séma szerkezeti verziója. Az app csak az általa ismert
  verziót fogadja el; ismeretlen verzió → betöltés elutasítása hibadiagnosztikával, nem crash.
- `contentVersion`: semver-szerű **string**, a tartalom kiadásának verziója.
- `datasetKind`: `"demo"` vagy `"production"`. Demó esetén a UI globális jelzést ad.
- `categories`: az egyetlen hiteles kategórialista. Magyar nevek itt vannak, nem a kódban.
- `illustrations`: licencnyilvántartás. Minden `techniques[].illustration` érték itt
  szerepelő `ref` legyen, vagy `null`.

## 3. techniques.json rekord

```json
{
  "id": "technique-<kategoria>-<sorszam>",
  "categoryId": "uke",
  "romaji": "...",
  "hungarian": "...",
  "japaneseScript": null,
  "aliases": [],
  "illustration": null,
  "descriptionHu": null,
  "difficulty": null,
  "beltRank": null,
  "verified": false,
  "quizEligible": false,
  "source": null
}
```

Kötelező: `id`, `categoryId`, `romaji`, `hungarian`, `verified`, `quizEligible`.
Opcionális/nullozható: a többi. Az `aliases` üres lista, nem `null`.

**Az `id` stabil és örök.** Átnevezéskor az `id` nem változik. Törléskor az `id` nem
újrahasznosítható (lásd 6. pont).

## 4. Validációs szabályok (mind kötelező)

A validátor **tiszta Kotlin**, Android-függőség nélkül tesztelhető. Kimenete nem kivétel,
hanem strukturált eredmény: `ValidationReport(accepted, rejected, issues)`.

| Kód | Szabály | Súlyosság | Hatás |
|---|---|---|---|
| `V001` | `schemaVersion` támogatott | BLOKKOLÓ | teljes betöltés elutasítva |
| `V002` | kötelező mező nem üres/blank | BLOKKOLÓ (rekord) | rekord elvetve |
| `V003` | `id` egyedi a fájlon belül | BLOKKOLÓ (rekord) | mindkét/későbbi rekord elvetve |
| `V004` | `categoryId` létezik a manifestben | BLOKKOLÓ (rekord) | rekord elvetve |
| `V005` | `illustration` ref létezik a manifest `illustrations`-ben és a fájl is megvan | FIGYELM. | rekord megmarad, de kép nélkül; képes kvíztípus kizárva |
| `V006` | duplikált/szinonim `romaji`/`hungarian`/`aliases` ütközés | FIGYELM. | a párok kölcsönös distraktor-kizárási listára kerülnek |
| `V007` | `quizEligible = true` csak `verified = true` mellett | BLOKKOLÓ (mező) | `quizEligible` lekényszerítve `false`-ra |
| `V008` | `quizEligible = true` esetén a szükséges mezők megvannak | BLOKKOLÓ (mező) | `quizEligible` lekényszerítve `false`-ra |
| `V009` | `aliases` nem tartalmaz üres stringet, nem ütközik a saját `romaji`/`hungarian`-jával | FIGYELM. | alias eldobva |
| `V010` | a JSON egyáltalán parse-olható | BLOKKOLÓ | betöltés elutasítva, diagnosztika |

**Soha ne omlasszd össze az appot érvénytelen tartalom miatt.** Hibás rekord: kihagyva a
kvízből, a tudástárban vagy nem jelenik meg, vagy explicit „hiányos" jelöléssel jelenik meg.
Debug buildben legyen elérhető fejlesztői diagnosztikai felület/napló a teljes
`ValidationReport`-tal.

## 5. Import pipeline

```
assets JSON --kotlinx.serialization--> DTO
   --validátor--> ValidationReport (accepted / rejected + issues)
   --mapper--> domain modell
   --upsert--> Room tartalomtáblák (technique, category)
```

Szabályok:
- A parse **tolerálja** a nem ismert extra mezőket (`ignoreUnknownKeys = true`), de NEM
  tolerálja a hiányzó kötelező mezőket.
- A betöltés idempotens: ugyanazt a `contentVersion`-t kétszer betölteni ne duplikáljon.
- A betöltés akkor fut, ha a csomagolt `contentVersion` ≠ a tárolt `contentVersion`.
- Upsert, nem `DELETE ALL` + `INSERT`: így a felhasználói adat idegen kulcsai nem sérülnek.

## 6. Tartalomfrissítés és az előrehaladás megőrzése

Ez az MVP egyik legfontosabb garanciája (mesterprompt 9. pont, teszt TR-05).

- A felhasználói adat (`quiz_attempt`, `study_review`, `quiz_session`) `techniqueId`-t tárol.
- A tartalomfrissítés **nem törli** a tartalomtáblákat, csak upsertel.
- **Eltűnt technika:** a rekordot ne töröld fizikailag; jelöld `retired = true` jelleggel
  (vagy tartsd meg a tartalomtáblában a megjelenítésből kizárva), hogy a korábbi
  próbálkozások hivatkozása értelmes maradjon. A haladás képernyőn a visszavont technikák
  külön, „már nem része a tananyagnak" megjegyzéssel vagy elhagyva jelennek meg – de a
  rekord nem veszik el.
- A felhasználói adat idegen kulcsa **ne** `CASCADE DELETE` legyen a tartalomra.
- Átnevezés: `id` marad, `romaji`/`hungarian` változik → az előrehaladás automatikusan követi.
- Minden `schemaVersion` vagy Room-sémaváltás ADR-t és migrációt kap: `docs/adr/`,
  valamint dokumentált Room `Migration` (destruktív migráció a felhasználói adatra tilos).

## 7. Új technika hozzáadásának menete (kódmódosítás nélkül)

1. Illusztráció bemásolása `assets/knowledge/illustrations/` alá.
2. `manifest.json` → `illustrations[]` bejegyzés licenccel és forrással.
3. `techniques.json` → új rekord, `verified: false`, `quizEligible: false`.
4. `contentVersion` léptetése.
5. `/kyo-validate-kb` futtatása – nulla BLOKKOLÓ hiba a cél.
6. Szakmai ellenőrzés után ember állítja `verified: true`-ra (és ha a mezők elegendőek,
   `quizEligible: true`-ra).

Ez a folyamat **nem igényel UI- vagy kvízmotor-kódmódosítást** – ez elfogadási kritérium.

## 8. Illusztráció utólagos hozzáadása egy meglévő technikához (T-707)

Ha egy már felvett technikarekordhoz utólag – pl. a felhasználótól kapott képből – kerül
illusztráció, a fenti 7. pont menete érvényes, **egyetlen fontos kiegészítéssel**: a
`convert.py` `ILLUSTRATION_PROVENANCE` konstansa **globális**, minden képre ugyanazt az
`origin: "ai_generated"` eredetet írja rá – ha a kép valójában felhasználó-adta fénykép,
ezt a `knowledgebase_transfer/manifest.json`-ban **kézzel kell korrigálni**, átmásolás előtt.
A kép eredetének pontosítása **nem** hitelesítés – a `review_status` ettől függetlenül
`"human_review_required"` marad. Részletek, a pontos mezőtáblázat és a javasolt követő
task: [`docs/KNOWLEDGE_BASE.md` 6a. pont](../../../docs/KNOWLEDGE_BASE.md#6a-illusztráció-utólagos-hozzáadása-egy-meglévő-technikához).

## 9. Többnyelvűsítés (T-711, FR-43)

A `techniques.json`/`entries.json` rekord és a `manifest.json` kategória két **nyitott
kulcsú** fordítás-mapet kap: `name_translations`, `description_translations` (csak a
technikán) – tetszőleges ISO 639-1 nyelvkóddal, **nem** fix mezőnév nyelvenként. A `hu`
kulcs sosem szerepel benne – a meglévő `hungarian_name`/`description_hu`/`name_hu` az
alapnyelv, és hiányzó fordításra a domain modell (`Technique.localizedName`/
`localizedDescription`, `Category.localizedName`) mindig erre esik vissza, soha nem
üresre. Új nyelv felvétele a szerkesztési CSV egy új oszlopával és a `convert.py`
map-építő logikájának egy új sorával jár – a séma, a validátor és a UI kódja nem
változik. A kvízmotor nyelve (kérdés/válasz szöveg) szándékosan kimarad – lásd
[`docs/KNOWLEDGE_BASE.md` 10a. pont](../../../docs/KNOWLEDGE_BASE.md#10a-többnyelvűsítés-fr-43-adr-013-t-711)
és [ADR-013](../../../docs/adr/ADR-013-multi-language-content.md).
