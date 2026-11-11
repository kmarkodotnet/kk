# ADR-012 – SQL-oldali keresés előre normalizált oszlopokon (séma 1 → 2)

- **Állapot:** elfogadva (2026-11-11, T-202)
- **Kontextus:** FR-03, US-06, FUNCTIONAL_SPEC 3.1, DR-08, DR-07, ADR-004, ADR-011
- **Érinti:** `core/data` Room séma (`version = 1` → `2`), `TechniqueRepository`,
  `ContentImporter` (T-117), `core/data/schemas/…/2.json`

## Kontextus

A T-201 a keresést szerződés szerint megvalósította (ékezet- és kisbetű-érzéketlen
részszó-egyezés romajira, magyar jelentésre és aliasra, a `NameNormalizer`-rel), de a
szűrés a **beolvasott soron**, Kotlin oldalon futott. Ez két dolgot nem tudott megadni:

1. a szűrés nem az adatbázisban történt, tehát minden keresés a teljes aktív táblát és a
   teljes aliastáblát beolvasta és normalizálta újra (FR-03, NFR-10);
2. nem derült ki, **melyik alias** egyezett, pedig a listatételnek ki kell írnia:
   „Egyezés: `{alias}`" (FUNCTIONAL_SPEC 3.1, US-06 3. elfogadási kritérium).

A normalizálás szabálya nem tehető át SQL-be: az SQLite `lower()` a nem ASCII betűket nem
kisbetűsíti, és diakritikus jeleket nem bont le. A `NameNormalizer` (`core/quiz`) a
normalizálás **egyetlen** definíciója – a validátor V006 szinonimavizsgálata és a
`romajiAscii` származtatása is ezt használja. Egy SQL-ben megírt második normalizáló azt
jelentené, hogy a keresés olyan egyezést talál, amit a szinonimakizárás nem tart azonosnak
(CR-07 csendes kilyukadása).

## Döntés

1. A `technique` tábla két **előre normalizált** oszlopot kap: `search_romaji` és
   `search_hungarian`. Az értéket a `NameNormalizer.normalize(...)` állítja elő a
   `ContentEntityMappers.toEntity()`-ben, tehát az importálás útvonalán, egyetlen helyen.
   Az aliasoknál a már létező `technique_alias.aliasNormalized` oszlop ugyanezt teszi.
2. A keresés szűrése **SQL-ben** fut, `LIKE '%…%' ESCAPE '\'` feltétellel a három
   normalizált oszlopon; az alias-ág `EXISTS` altkérdés (a `JOIN` több aliasra duplikálná
   a technikát). A keresőszöveg `%` és `_` karakterei escape-elve literálisan
   illeszkednek, nem jokerként.
3. Az **egyező alias** ugyanabból a lekérdezésből jön (korrelált altkérdés, ábécé szerint
   első illeszkedő alias), és a `nameMatched` kifejezés mondja meg, kell-e egyáltalán
   kiírni. A repository ezt a `TechniqueMatch` domain típusban adja ki
   (`observeTechniqueMatches`); az `observeTechniques` ennek a vetülete.
4. A `search_*` oszlopokra **nincs index**: a bal oldali joker miatt az SQLite indexet nem
   használhat, egy index csak az import írási költségét növelné.
5. A séma `version` 1 → **2**, explicit `Migration`-nel
   (`KyokushinMigrations.MIGRATION_1_2`), exportált `2.json`-nal, és a migrációra
   Room `MigrationTestHelper` teszttel. `fallbackToDestructiveMigration()` **továbbra is
   tilos** (DR-08).

## A migráció és a visszatöltés

```sql
ALTER TABLE technique ADD COLUMN search_romaji     TEXT NOT NULL DEFAULT '';
ALTER TABLE technique ADD COLUMN search_hungarian  TEXT NOT NULL DEFAULT '';
UPDATE technique SET search_romaji = lower(romajiAscii),
                     search_hungarian = lower(hungarian);
UPDATE content_state SET contentVersion = NULL;
```

- `ALTER TABLE … ADD COLUMN`, nem tábla-újraépítés: a `technique` sorai megmaradnak, és a
  `technique_alias` / `synonym_group` cascade idegen kulcsai sem kerülnek veszélybe
  (ADR-004).
- A `search_romaji` áthidaló értéke **pontos**: a `romajiAscii` már diakritikus jel
  nélküli alak, ezért `lower()` ugyanazt adja, mint a `NameNormalizer`. A
  `search_hungarian` SQL-oldali `lower()`-e viszont csak közelítés (az ékezetek
  megmaradnak) – ezt szándékosan **nem** pótoljuk SQL-beli karaktertáblával, mert az a
  normalizálás második definíciója lenne.
- Ezért a migráció a `content_state.contentVersion`-t `NULL`-ra állítja: a következő
  `ContentImporter.importIfNeeded()` nem tér vissza `UpToDate`-tel, hanem újra felviszi a
  csomagolt tudástárat, és az upsert a két oszlopot a `NameNormalizer` kimenetével írja
  felül. Az áthidaló érték így legfeljebb az app következő indulásáig él.
- A `content_state` **tartalmi** metaadat, nem felhasználói adat: a migráció a
  `quiz_session`, `quiz_attempt` és `study_review` táblákhoz egyáltalán nem nyúl (DR-07,
  FR-33). Ezt a `KyokushinMigrationTest` sorszinten ellenőrzi.

## Elvetett alternatívák

| Alternatíva | Miért nem |
|---|---|
| A keresés marad Kotlin oldalon (T-201 állapot) | nem teljesíti az FR-03 „SQL-oldali, előre normalizált oszlop" elvárását (T-202), és minden kereséskor újranormalizálja a teljes táblát |
| FTS4/FTS5 virtuális tábla | a részszó-egyezés (`%dachi%`) nem token-prefix keresés, az FTS ezt tokenizáló nélkül nem adja; plusz egy párhuzamos, szinkronban tartandó tábla néhány száz rekord mellett |
| Normalizálás SQL-ben (`replace()` lánc a magyar ékezetekre) | a `NameNormalizer` második definíciója; az első szétcsúszás csendes CR-07 hiba |
| A migráció nem nyitja a verziókaput, csak SQL-ben töltene vissza | a `search_hungarian` ékezetes maradna, és a „zart labu" keresés az első import előtt nem találna |
| `fallbackToDestructiveMigration()` | törölné a felhasználó előrehaladását (DR-07, FR-33) – a projekt vasszabálya tiltja |
| Az egyező alias kiszámítása a UI-ban | a mintaillesztés második leírása Compose oldalon, és a `TechniqueMatch` nélkül a UI nem is tudná, mi illeszkedett |

## Következmények

- A `TechniqueEntity` két új, kötelező (nem nullázható) oszlopot hordoz; a fixtúráknak is
  ki kell tölteniük, ugyanazzal a normalizálóval.
- A `core/data/schemas/` alatt mostantól két verzió él (`1.json`, `2.json`), mindkettő
  verziókövetett. Az exportált sémát a `test` source set assetjeként kötjük be, hogy a
  `MigrationTestHelper` Robolectric alatt, emulátor nélkül megtalálja – a leszállított
  APK-ba nem kerül bele.
- A T-203 Hilt kötésének `addMigrations(*KyokushinMigrations.ALL)`-t kell adnia, destruktív
  tartalék nélkül.
- A Tudástár lista ViewModelje (T-207) az `observeTechniqueMatches`-t használja, és a
  `TechniqueMatch.matchedAlias` alapján írja ki az „Egyezés: `{alias}`" sort (a szöveg
  `strings.xml`-ben, a UI feladata).
