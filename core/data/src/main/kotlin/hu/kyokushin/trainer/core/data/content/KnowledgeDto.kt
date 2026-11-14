package hu.kyokushin.trainer.core.data.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A kanonikus tudástárséma (ADR-011) deszerializálási konfigurációja.
 *
 * - `ignoreUnknownKeys = true`: a tartalom bővíthető anélkül, hogy egy régebbi app
 *   elszállna. A konverter bármikor felvehet új mezőt (`sha256`, `tags`, …); amit ez a
 *   verzió nem ismer, azt átlépi, nem elutasítja.
 * - Kiírási beállítás nincs: ez a `Json` **csak olvas**. A tartalom szerkesztési forrása a
 *   `knowledgebase/` CSV, az app nem ír tudástárfájlt.
 * - `isLenient` **nem** kell: a hibás szintaxis valódi hiba, és `V010` kóddal jelentendő –
 *   nem csendben megengedendő.
 */
internal val KnowledgeJson: Json = Json {
    ignoreUnknownKeys = true
}

/**
 * A `manifest.json` nyers alakja (ADR-011).
 *
 * **Minden mező `null`-tűrő, és nincs kötelező mező.** Ez nem lazaság: a hiányzó kötelező
 * mezőt a validátornak kell jelentenie (`V001` a sémaverzióra, `V002` a rekordmezőkre), a
 * hatókörének megfelelően. Ha a deszerializálás dobna egyetlen hiányzó mezőre, akkor
 * minden ilyen hiba egységesen `V010`-ként („a JSON nem értelmezhető") jelentkezne, és
 * elveszne a különbség a *szintaktikailag* hibás fájl és az *egyetlen* hiányos rekord
 * között – pedig az utóbbinál a többi 160 rekord használható.
 *
 * @property schemaVersion a séma szerkezeti verziója **semver stringként** (ADR-011);
 *   nem egész szám.
 * @property datasetVersion a tartalomkiadás verziója; a frissítés idempotenciája ezen
 *   dől el (FR-32).
 * @property editorialStatus a szerkesztői állapot (pl. `draft_review_required`) – a
 *   bizalmi szint forrása.
 * @property datasetTrust a tartalom önbevallása a bizalmi szintről. Beolvassuk a
 *   diagnosztika kedvéért, de **nem** ez képezi le a
 *   [hu.kyokushin.trainer.core.model.DatasetTrust]-ot: a tartalom nem emelheti a saját
 *   bizalmi szintjét (CR-02), ezért a leképezés az [editorialStatus] értékét adja át.
 * @property categories az egyetlen hiteles kategórialista.
 * @property illustrations a képlicenc- és megbízhatóság-nyilvántartás (CR-04).
 * @property synonymGroups a konverter által megállapított ütközéscsoportok. Beolvassuk,
 *   de nem adjuk át: a V006 szinonimavizsgálatot a validátor **újraszámolja** a
 *   normalizált nevekből, hogy a kvízkizárás ne a tartalom önbevallásán álljon (CR-07).
 */
@Serializable
internal data class ManifestDto(
    @SerialName("schema_version") val schemaVersion: String? = null,
    @SerialName("dataset_version") val datasetVersion: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("martial_art") val martialArt: String? = null,
    @SerialName("editorial_status") val editorialStatus: String? = null,
    @SerialName("dataset_trust") val datasetTrust: String? = null,
    @SerialName("notes") val notes: List<String?> = emptyList(),
    @SerialName("categories") val categories: List<CategoryDto> = emptyList(),
    @SerialName("illustrations") val illustrations: List<IllustrationDto> = emptyList(),
    @SerialName("synonym_groups") val synonymGroups: List<List<String?>> = emptyList(),
)

/**
 * Egy kategória nyers alakja a manifestből.
 *
 * @property id a kategória stabil azonosítója.
 * @property nameHu a magyar megjelenítendő név. **A tartalomból jön, nem a
 *   `strings.xml`-ből** (ADR-011): új kategória felvétele nem igényel kódmódosítást.
 * @property sortOrder a megjelenítési sorrend (DR-02); hiányában a tömbsorrend érvényes.
 * @property nameTranslations a [nameHu] nyitott kulcsú fordítás-térképe (FR-43,
 *   ADR-013) – tetszőleges ISO 639-1 nyelvkód, nem fix mezőkészlet. Hiányában üres map;
 *   a visszafelé kompatibilitás miatt egy régebbi `manifest.json` is betölthető marad.
 */
@Serializable
internal data class CategoryDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name_hu") val nameHu: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("name_translations") val nameTranslations: Map<String, String?> = emptyMap(),
)

/**
 * Egy illusztráció nyilvántartási bejegyzése a manifestből (ADR-003, ADR-011).
 *
 * @property ref a licenckulcs, amire a rekordok `illustration_ref` mezője hivatkozik.
 * @property file a fájl útvonala a tudástár gyökeréhez képest (pl.
 *   `illustrations/KY-0044.jpg`). A V005 szabály ennek az utolsó szegmensét keresi a
 *   csomagolt fájlnevek között.
 * @property license a licenc megnevezése; hiányában a kép nem használható (CR-03).
 * @property origin a kép eredete (pl. `ai_generated`).
 * @property reviewStatus a szakmai ellenőrzés állapota – a testhelyzet és a mozgásirány
 *   hitelessége. Független a [quizUsable] kaputól.
 * @property hasEmbeddedLabel a képbe bele van-e égetve a technika neve. Emberi
 *   megfigyelés, nem szövegfelismerés (ADR-011).
 * @property quizUsable a kép használható-e kvízkérdésben. Feliratos kép nem – akkor sem,
 *   ha szakmailag hiteles, mert elárulja a helyes választ.
 * @property hideableOnStudyCard a kép elrejthető-e a tanulókártyán a felfedésig.
 */
@Serializable
internal data class IllustrationDto(
    @SerialName("ref") val ref: String? = null,
    @SerialName("file") val file: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("sha256") val sha256: String? = null,
    @SerialName("origin") val origin: String? = null,
    @SerialName("license") val license: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("author") val author: String? = null,
    @SerialName("review_status") val reviewStatus: String? = null,
    @SerialName("has_embedded_label") val hasEmbeddedLabel: Boolean? = null,
    @SerialName("quiz_usable") val quizUsable: Boolean? = null,
    @SerialName("hideable_on_study_card") val hideableOnStudyCard: Boolean? = null,
)

/**
 * Az `entries.json` fájl nyers alakja.
 *
 * A fájl a manifest séma- és tartalomverzióját megismétli. Az **egyetlen hiteles** forrás
 * a manifest ([ManifestDto.schemaVersion], [ManifestDto.datasetVersion]); az itteni
 * értékeket csak beolvassuk, hogy ne ismeretlen mezőként tűnjenek el, és hogy egy későbbi
 * eltéréskereső szabály támaszkodhasson rájuk.
 *
 * @property entries a technikarekordok, a **fájlban szereplő sorrendben**. A sorrend
 *   jelentéshordozó: a V003 (duplikált `id`) szabály a későbbi rekordot veti el.
 */
@Serializable
internal data class EntriesFileDto(
    @SerialName("schema_version") val schemaVersion: String? = null,
    @SerialName("dataset_version") val datasetVersion: String? = null,
    @SerialName("entries") val entries: List<EntryDto> = emptyList(),
)

/**
 * Egy technikarekord nyers alakja az `entries.json`-ból (ADR-011).
 *
 * @property id stabil és örök azonosító (DR-06). Átnevezéskor nem változik, törléskor nem
 *   újrahasznosítható – az előrehaladás ehhez kapcsolódik (FR-33).
 * @property categoryId hivatkozás a manifest kategóriáira (V004).
 * @property japaneseRomaji a technika eredeti neve latin átírásban – ez a hiteles forma.
 * @property japaneseRomajiAscii a diakritikus jelek nélküli alak; a keresési index
 *   alapja (FR-03).
 * @property japaneseScript az eredeti japán írásmód, ha ismert. A kanonikus tartalomban
 *   mindenhol `null`: a japán írásjegyek nincsenek hitelesítve, és kódból nem pótolhatók.
 * @property hungarianName a magyar jelentés. Kötelező mező (V002).
 * @property descriptionHu rövid magyar leírás.
 * @property aliases további elfogadott elnevezések. Az elemek `null`-tűrők, mert a V009
 *   szabálynak az üres és a `null` aliast is jelentenie kell tudni.
 * @property tags szabad címkék; a sémában szerepelnek, a validátor nem használja.
 * @property illustrationRef a manifest `illustrations[].ref` kulcsa, vagy `null`. **Nem**
 *   útvonal: a kép metaadatai egyetlen helyen élnek (ADR-011).
 * @property reviewStatus a szerkesztői ellenőrzés állapota. Csak a `reviewed` érték
 *   jelent hitelesítést – a hitelesítési státuszt kódból soha nem emeljük (CR-01, CR-02).
 * @property quizExcluded emberi döntés: hiteles rekord, de a kvízből kimarad.
 * @property retired a tananyagból kivont tétel. Nem töröljük, csak jelöljük (ADR-004).
 * @property searchRomaji előre normalizált keresési alak a romajira (FR-03).
 * @property searchHungarian előre normalizált keresési alak a magyar névre (FR-03).
 * @property incomplete a konverter megállapítása a hiányosságról. **Az app validátora
 *   újraszámolja** (ADR-011), ezért ez az érték nem kerül át a validátor bemenetébe.
 * @property nameTranslations a [hungarianName] nyitott kulcsú fordítás-térképe (FR-43,
 *   ADR-013) – tetszőleges ISO 639-1 nyelvkód, nem fix mezőkészlet. Hiányában üres map.
 * @property descriptionTranslations a [descriptionHu] fordítás-térképe – lásd
 *   [nameTranslations].
 */
@Serializable
internal data class EntryDto(
    @SerialName("id") val id: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("japanese_romaji") val japaneseRomaji: String? = null,
    @SerialName("japanese_romaji_ascii") val japaneseRomajiAscii: String? = null,
    @SerialName("japanese_script") val japaneseScript: String? = null,
    @SerialName("hungarian_name") val hungarianName: String? = null,
    @SerialName("description_hu") val descriptionHu: String? = null,
    @SerialName("name_translations") val nameTranslations: Map<String, String?> = emptyMap(),
    @SerialName("description_translations") val descriptionTranslations: Map<String, String?> = emptyMap(),
    @SerialName("aliases") val aliases: List<String?> = emptyList(),
    @SerialName("tags") val tags: List<String?> = emptyList(),
    @SerialName("illustration_ref") val illustrationRef: String? = null,
    @SerialName("review_status") val reviewStatus: String? = null,
    @SerialName("quiz_excluded") val quizExcluded: Boolean? = null,
    @SerialName("retired") val retired: Boolean? = null,
    @SerialName("search_romaji") val searchRomaji: String? = null,
    @SerialName("search_hungarian") val searchHungarian: String? = null,
    @SerialName("incomplete") val incomplete: Boolean? = null,
)
