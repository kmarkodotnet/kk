# ADR-013 – Többnyelvűsítés: nyílt fordítás-map a tartalomban, rendszernyelv-követés

- **Állapot:** elfogadva (2026-11-14, felhasználói döntés, OQ-07 feloldása)
- **Kontextus:** FR-43, `docs/REQUIREMENTS.md` 3.7 és 10. szakasz (OQ-07),
  `docs/BACKLOG.md` E10 (US-29), T-711. Érinti: `core/model`, `core/data`
  (`KnowledgeDto`, `ContentEntities`, Room séma `2 → 3`), `core/ui`,
  `feature/learn`, `feature/library`, `feature/progress`,
  `knowledgebase_transfer/tools/convert.py`.

## Kontextus

A `CLAUDE.md` vasszabálya és a mesterprompt 9. pontja eddig kizárta a
többnyelvűséget: minden felhasználói szöveg és tartalom kizárólag magyar volt. A
felhasználó 2026-11-14-én explicit felülírta ezt – a T-711 task szerint úgy, hogy
**tetszőleges számú nyelv** támogatható legyen, ne csak a task leírásában szereplő
angol/német. Egy párhuzamos munka emellett már elkészítette a 161 technika és 9
kategória valódi angol/német fordítását a szerkesztési forrásban
(`knowledgebase/kyokushin_knowledge_hu.csv`/`.json`, lásd `docs/CONTENT_TRANSLATIONS.md`),
szaknyelvi forrásokra hivatkozva, `translation_review_status: draft_review_required`
jelöléssel.

Két külön réteget kell megkülönböztetni:
1. **UI-kezelőszövegek** (gombok, képernyőcímek) – ezt az Android erőforrás-rendszer
   natívan, kódmódosítás nélkül támogatja tetszőleges nyelvre.
2. **Technikatartalom** (név, leírás, kategórianév) – ehhez nincs natív Android
   mechanizmus, mert ez nem erőforrás, hanem a csomagolt tudástár-JSON-ból
   betöltött, Room-ban tárolt adat (ADR-011).

## Döntés

### 1. UI-kezelőszövegek: Android erőforrás-qualifier, változtatás nélkül

Minden `strings.xml` mellé `values-<lang>/strings.xml` kerül (jelenleg `en`, `de`),
ugyanazokkal a kulcsokkal. **Új nyelv felvétele ezen a rétegen nulla kódmódosítás** –
ez maga az Android erőforrás-feloldás natív viselkedése, nincs szükség egyedi
megoldásra. Az alapértelmezett (qualifier nélküli) `values/strings.xml` **marad
magyar**: egy nem támogatott eszköznyelv esetén így a felhasználó magyar szöveget
lát, nem angolt vagy üreset.

### 2. Technikatartalom: nyílt, tetszőleges kulcsú fordítás-map, nem rögzített mezőkészlet

**Elvetve:** fix mezőnév nyelvenként (`english_name`, `german_name`, …) a kanonikus
`entries.json`/domain modellben. Ez a `knowledgebase/kyokushin_knowledge_hu.csv`
szerkesztési forrásban **megfelelő** (Excelben kényelmesen szerkeszthető, fix
oszlopok), de az appba kerülő kanonikus sémában **nem**: minden új nyelv új mezőt és
kódmódosítást igényelne, pont az ellentéte annak, hogy „bármennyi nyelv lehet".

**Elfogadva:** a kanonikus `entries.json` rekord és a `manifest.json` kategória két
új, **nyitott kulcsú** mezőt kap:

```json
{
  "name_translations": { "en": "Front kick", "de": "Vorwärtstritt" },
  "description_translations": { "en": "...", "de": "..." }
}
```

A kulcs tetszőleges ISO 639-1 nyelvkód lehet; a mező teljesen hiányozhat (régi
`entries.json`, `ignoreUnknownKeys = true` miatt visszafelé kompatibilis, ADR-011
stílusában). A `hu` kulcs **sosem** szerepel a mapben – a meglévő `hungarian_name`/
`description_hu`/`name_hu` már az alapnyelv, és ez az egyetlen kötelező, mindig
jelenlévő forrás.

A `knowledgebase_transfer/tools/convert.py` ezt a CSV `english_name`/`german_name`/
`description_en`/`description_de` (és a kategóriáknál a forrás JSON `name_en`/
`name_de`) oszlopaiból építi fel – a konverter oldalán **is** nyitott: ha a CSV új
nyelvi oszlopot kap (pl. `french_name`), a `csv.DictReader` azt automatikusan látja,
csak a map-építő logikának kell egy új kulcsot felvennie, nem egy új mezőt a teljes
sémában végig.

### 3. Domain modell: segédfüggvény, nem új kötelező mező

`core/model/Technique.kt` és `Category.kt`:

```kotlin
val nameTranslations: Map<String, String> = emptyMap()
val descriptionTranslations: Map<String, String> = emptyMap()  // csak Technique

fun Technique.localizedName(languageTag: String): String =
    nameTranslations[languageTag] ?: hungarian
fun Technique.localizedDescription(languageTag: String): String? =
    descriptionTranslations[languageTag] ?: descriptionHu
```

Tiszta Kotlin, Android-mentes (ADR-001 L1): a visszaesés **mindig** a magyar mezőre
esik, soha nem üres, soha nem más nyelvű alapértelmezés – ez a CLAUDE.md tartalmi
integritás szellemét követi (nincs kitalált/hiányzó-helyettesítő szöveg).

### 4. Tárolás: JSON-string oszlop a Room táblán, nem relációs tábla, nem SQL-szűrés célja

**Elvetve:** új relációs tábla (`technique_translation(techniqueId, lang, name, …)`,
mintázva a `technique_alias`-ra). Ez a mintázat ott helyes, ahol SQL-oldali
szűrés/keresés kell (ADR-012) – a fordítás viszont **sosem** keresési vagy szűrési
feltétel, csak megjelenítési adat egy már ismert technikához.

**Elfogadva:** a `TechniqueEntity`/`CategoryEntity` két új oszlopa (`nameTranslations`,
Technique-nél `descriptionTranslations`), **JSON stringként szerializálva** egy
`@TypeConverter` páron (`KyokushinTypeConverters.kt`, kotlinx.serialization). Ez a
legkisebb beavatkozású megoldás: a fordítások minden meglévő lekérdezésen (lista,
keresés, `getById`) "ingyen" utaznak, **nincs** szükség új DAO-ra, JOIN-ra vagy a
`toTechnique(aliases = …)` mintához hasonló külön betöltésre.

Séma `version 2 → 3`, `KyokushinMigrations.MIGRATION_2_3`:
```sql
ALTER TABLE technique ADD COLUMN nameTranslations        TEXT NOT NULL DEFAULT '{}';
ALTER TABLE technique ADD COLUMN descriptionTranslations TEXT NOT NULL DEFAULT '{}';
ALTER TABLE category  ADD COLUMN nameTranslations        TEXT NOT NULL DEFAULT '{}';
```
Nincs szükség a `content_state.contentVersion` visszaállítására (szemben
ADR-012-vel): a meglévő sorok az üres map-es default mellett azonnal érvényesek, a
tényleges fordítást a következő, `dataset_version`-léptetett import hozza be upserttel.

### 5. Nyelvválasztás: rendszerbeállítás követése, nincs in-app választó

`core/ui`: `@Composable fun currentLanguageTag(): String`, az
`androidx.compose.ui.text.intl.Locale.current.language`-re épül – ez már követi az
Android rendszer- vagy alkalmazásnyelv-beállítását, nincs szükség saját
`Resources`/`LocaleList` kezelésre vagy állapot-perzisztálásra.

### 6. A kvízmotor nyelve kimarad

A `core/quiz` motor (kérdésszöveg, disztraktor-kizárás, `NameNormalizer` bemenete) és
a `feature/quiz` képernyők **Hungarian-only** maradnak. Ez önálló pedagógiai döntés
(milyen nyelven kérdezzen a kvíz, hogyan viselkedjen a szinonima-kizárás
többnyelvű névkészleten) – nem ez a kör dönt róla.

### 7. A fordítás lektorálási állapota (`translation_review_status`) átmegy, de UI-jelölő nélkül

A CSV `translation_review_status` mezője átmegy a konverteren és a DTO-n (jövőbeli
felhasználásra), de ebben a körben **nem** kap domain modell mezőt vagy UI-jelölőt.
Egy „nem lektorált fordítás" jelölő a meglévő `VerificationBadge`/`UnreviewedBanner`
(`core/ui/status/`) mintáját követő, önálló, kis követő feladat.

## Mérlegelt alternatívák

| Alternatíva | Miért nem |
|---|---|
| Fix mezőnév nyelvenként (`english_name`, `german_name`) a kanonikus sémában | Minden új nyelv új mezőt és kódmódosítást igényelne – nem „tetszőleges számú nyelv". |
| Relációs fordítás-tábla (`technique_translation`) | A fordítás nem SQL-szűrési cél; a relációs tábla új DAO-t, JOIN-ot és külön betöltési logikát igényelne minden meglévő lekérdezési útvonalon, nulla funkcionális nyereségért. |
| Saját in-app nyelvválasztó | A felhasználó explicit a rendszerbeállítás-követést kérte. |
| A kvízmotor is lokalizálva | Önálló, mélyebb pedagógiai döntés (kérdésnyelv, szinonima-kizárás többnyelvű névkészleten) – külön feladat. |

## Következmények

- Új nyelv felvétele **tartalom**-szinten: a szerkesztési CSV-be egy új oszlop (pl.
  `french_name`), a `convert.py` map-építő logikájának egy új kulcsa – a domain
  modell, a Room séma és a UI kódja **nem** változik.
- Új nyelv felvétele **UI-kezelőszöveg**-szinten: egyetlen új `values-<lang>/
  strings.xml` modulonként – nulla Kotlin-kódmódosítás.
- A Room séma `3.json`-ja és a `KyokushinMigrationTest` bővül egy `MIGRATION_2_3`
  teszttel.
- A `docs/KNOWLEDGE_BASE.md`-t és a `kyo-knowledge-base` skillt ehhez kell igazítani.
