package hu.kyokushin.trainer.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import java.time.Instant

/**
 * A **tartalomtáblák** (ARCHITECTURE 5.2): a csomagolt tudástárból upserttel töltődnek,
 * és a tartalomfrissítés újraírhatja őket. Minden típus `internal` – a Room entitás nem
 * szivárog ki a `core/data`-ból (ADR-001 L3/L4).
 *
 * **Idegen kulcsok itt tartalom → tartalom irányban mennek**, és ezeknél a `CASCADE`
 * megengedett: az aliasok és a szinonimacsoport-tagságok a technika *származtatott*
 * kísérői, nem önálló adat. A tilalom (ADR-004) a **felhasználói → tartalom** irányra
 * szól, és azt a `UserEntities.kt` tartja be.
 *
 * Fontos, hogy a technikát törlés helyett `retired = true` jelöléssel vonjuk vissza
 * (ADR-004), így ezek a cascade-ek a rendes működésben nem is aktiválódnak – a
 * `technique` sorok nem törlődnek.
 */

/** Egy technikakategória (DR-02). A `nameHu` tartalmi adat, nem `strings.xml` (ADR-011). */
@Entity(tableName = "category")
internal data class CategoryEntity(
    @PrimaryKey val id: CategoryId,
    val nameHu: String,
    val sortOrder: Int,
)

/**
 * Egy technika a tudástárból (DR-01).
 *
 * **A `quizEligible` szándékosan nincs oszlop.** A kvízjogosultság származtatott érték
 * (ADR-010): a `status`, a `quizExcluded`, a `retired`, a kérdéstípus mezőkövetelményei és
 * a tudástár bizalmi szintje együtt döntik el, a kvízmotorban. Tárolva két igazságforrás
 * lenne, és egy elavult `true` kvízbe engedhetne hiányos rekordot. (Az ARCHITECTURE 5.2
 * táblázata még felsorolja a `quizEligible` oszlopot – ez az ADR-010 utáni, szándékos
 * eltérés a dokumentumtól.)
 *
 * Az illusztráció három állapotát (nincs / helyőrző / valódi kép, ADR-003) négy nullázható
 * oszlop hordozza, [illustrationKind] vezérléssel. Sealed hierarchiát nem lehet egyetlen
 * oszlopba tenni; a visszaalakítás a mapper felelőssége, és a `kind` megléte dönti el,
 * melyik ágra kell mennie.
 */
@Entity(
    tableName = "technique",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            // Tartalom → tartalom hivatkozás, de itt sem cascade: egy kategória
            // eltüntetése nem törölhet technikákat, a technika visszavonása jelölés
            // (ADR-004). Hiányzó kategóriára hivatkozó technika így beszúráskor elbukik.
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        // A Tudástár listája és a keresés (FR-03, T-202) az ASCII-normalizált alakon
        // szűr és rendez – az index ezért ezen az oszlopon van, nem a `romaji`-n.
        Index(value = ["romajiAscii"]),
        // A `search_romaji` / `search_hungarian` oszlopokra szándékosan NINCS index: a
        // keresés részszó-egyezés (`LIKE '%…%'`, FR-03), aminél a bal oldali joker miatt
        // az SQLite nem tud indexet használni – a lekérdezés akkor is teljes táblát
        // olvas. Egy ott felvett index csak az import írási költségét növelné.
    ],
)
internal data class TechniqueEntity(
    @PrimaryKey val id: TechniqueId,
    val categoryId: CategoryId,
    val romaji: String,
    val romajiAscii: String,
    val hungarian: String,
    val japaneseScript: String? = null,
    val descriptionHu: String? = null,
    val status: VerificationStatus,
    val quizExcluded: Boolean,
    val retired: Boolean,
    /** A `manifest.json` `contentVersion` értéke, amelyből ez a sor származik (FR-32). */
    val datasetVersion: String,
    /**
     * A romaji név **előre normalizált** alakja: a keresés `LIKE '%…%'` feltétele ezen fut
     * (FR-03, T-202, ADR-012).
     *
     * Az értéket a `NameNormalizer.normalize(romaji)` állítja elő a leképezésben
     * (`ContentEntityMappers.toEntity`), tehát a normalizálás szabálya egyetlen helyen él
     * – ugyanaz, amit a validátor V006 vizsgálata és az [TechniqueAliasEntity.aliasNormalized]
     * használ. Nincs rá SQL-oldali újraszámolás: az SQLite `lower()` nem kezeli a
     * diakritikus jeleket, egy SQL-ben megírt második normalizáló pedig szétcsúszna az
     * elsőtől.
     *
     * Miért nem elég a [romajiAscii]? Mert az **megjelenítésre** szánt, kis-/nagybetűt
     * megtartó érték (`Mae Geri`), a keresés viszont összehasonlítási kulcsot kér.
     */
    @ColumnInfo(name = "search_romaji") val searchRomaji: String,
    /** A magyar jelentés előre normalizált alakja – lásd [searchRomaji] (FR-03, ADR-012). */
    @ColumnInfo(name = "search_hungarian") val searchHungarian: String,
    @ColumnInfo(name = "illustrationKind") val illustrationKind: IllustrationKind? = null,
    @ColumnInfo(name = "illustrationRef") val illustrationRef: String? = null,
    @ColumnInfo(name = "illustrationFileName") val illustrationFileName: String? = null,
    @ColumnInfo(name = "illustrationQuizUsable") val illustrationQuizUsable: Boolean? = null,
    @ColumnInfo(name = "illustrationHideableOnStudyCard")
    val illustrationHideableOnStudyCard: Boolean? = null,
)

/**
 * Az illusztráció-hivatkozás tárolt változatának megkülönböztetője (ADR-003).
 *
 * `null` oszlopérték = a rekord nem hivatkozik illusztrációra; [PLACEHOLDER] = hivatkozik,
 * de valódi kép nincs mögötte (V005 lefokozás); [ASSET] = validált, létező képfájl.
 * A `name` megy az adatbázisba, nem az `ordinal`.
 */
internal enum class IllustrationKind {
    ASSET,
    PLACEHOLDER,
}

/**
 * Egy technika további elfogadott elnevezése (dialektus, iskolánkénti eltérés).
 *
 * Összetett elsődleges kulcs `(techniqueId, alias)`: ugyanaz az alias ugyanannál a
 * technikánál nem duplázódhat, és az upsert így idempotens. Az [aliasNormalized] azért
 * tárolt oszlop és nem futásidőben számolt érték, mert a keresés (FR-03) minden
 * összehasonlításnál ezt használja – a normalizálás szabálya a tartalomimport
 * felelőssége, nem a lekérdezésé.
 */
@Entity(
    tableName = "technique_alias",
    primaryKeys = ["techniqueId", "alias"],
    foreignKeys = [
        ForeignKey(
            entity = TechniqueEntity::class,
            parentColumns = ["id"],
            childColumns = ["techniqueId"],
            // Tartalom → tartalom: az alias a technika kísérője, önálló élete nincs.
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["aliasNormalized"])],
)
internal data class TechniqueAliasEntity(
    val techniqueId: TechniqueId,
    val alias: String,
    val aliasNormalized: String,
)

/**
 * Egy keresési találat sora: a technika + a **mivel egyezett** információ (FR-03, T-202).
 *
 * Nem `@Entity`, hanem lekérdezés-projekció: a [nameMatched] és a [matchedAlias] nem
 * tárolt adat, hanem a `LIKE` feltétel kimenete a keresőmintára. Azért egyetlen
 * lekérdezésből jön, mert különben a UI-nak kellene kitalálnia, melyik alias egyezett –
 * az pedig a mintaillesztés második, Kotlin oldali leírása lenne.
 *
 * @property nameMatched a technika saját neve (romaji vagy magyar) illeszkedett-e. Ha
 *   igaz, a UI **nem** írja ki az „Egyezés: `{alias}`" sort, mert a találat önmagát
 *   magyarázza (FUNCTIONAL_SPEC 3.1).
 * @property matchedAlias az illeszkedő alias eredeti (nem normalizált) alakja, vagy
 *   `null`, ha egyetlen alias sem illeszkedett. Több illeszkedő alias esetén az ábécé
 *   szerint első – a kiírt szöveg így determinisztikus (NFR-07).
 */
internal data class TechniqueSearchRow(
    @Embedded val technique: TechniqueEntity,
    val nameMatched: Boolean,
    val matchedAlias: String?,
)

/**
 * Egy szinonimacsoport **tagsága** (V006, CR-07).
 *
 * A validátor `synonymGroups` kimenete `List<Set<TechniqueId>>`; relációs alakban ez
 * `(groupId, techniqueId)` párok halmaza. Így a kvízgenerátor jelölthalmaza az
 * adatbázisból is feltölthető, és a „azonos jelentésű kifejezés nem lehet egymás
 * distraktora" szabály nem igényli a teljes tudástár memóriában tartását.
 *
 * A [groupId] **nem stabil azonosító**: a tartalomimport minden futáskor újraszámolja a
 * csoportokat, ezért a tábla tartalma importkor teljesen cserélődik. Felhasználói adat
 * nem hivatkozik rá – ez az egyetlen tábla, amelynél a teljes ürítés megengedett.
 */
@Entity(
    tableName = "synonym_group",
    primaryKeys = ["groupId", "techniqueId"],
    foreignKeys = [
        ForeignKey(
            entity = TechniqueEntity::class,
            parentColumns = ["id"],
            childColumns = ["techniqueId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["techniqueId"])],
)
internal data class SynonymGroupMemberEntity(
    val groupId: Int,
    val techniqueId: TechniqueId,
)

/**
 * A betöltött tartalom állapota – **pontosan egy sor**, `id = 1` (ARCHITECTURE 5.2).
 *
 * Az egysoros tábla azért jobb a `SharedPreferences`-nél, mert így a tartalomimport és az
 * állapotfrissítés **ugyanabban a tranzakcióban** történhet: nem fordulhat elő, hogy a
 * technikák már az új kiadásból vannak, a kijelzett `contentVersion` viszont a régi.
 *
 * A [datasetTrust] a manifest szerkesztői állapotából származik, és kódból soha nem
 * emelkedik (CR-02). A validációs jelentés itt csak **összegzésként** jelenik meg; a
 * részletes észrevételek nem perzisztálódnak, azokat a fejlesztői diagnosztika az
 * importkor keletkezett jelentésből kapja (FR-45).
 */
@Entity(tableName = "content_state")
internal data class ContentStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val schemaVersion: String?,
    val contentVersion: String?,
    val datasetTrust: DatasetTrust,
    val importedAt: Instant,
    val acceptedCount: Int,
    val rejectedCount: Int,
    val blockingIssueCount: Int,
    val warningIssueCount: Int,
    /** A teljes tudástár elutasítva (pl. nem támogatott `schemaVersion`, V001 / V010). */
    val datasetRejected: Boolean,
) {
    internal companion object {
        /** Az egyetlen érvényes elsődleges kulcs: a tábla szándékosan egysoros. */
        const val SINGLETON_ID: Int = 1
    }
}
