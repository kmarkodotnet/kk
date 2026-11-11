package hu.kyokushin.trainer.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.coroutines.flow.Flow

/**
 * A tartalomtáblák DAO-i.
 *
 * Minden függvény `suspend` vagy `Flow`-t ad vissza – blokkoló DAO nincs, és az
 * adatbázis nem engedi a főszálú lekérdezést (`allowMainThreadQueries` sehol nem szerepel).
 * A `Flow`-s lekérdezések önmagukban nem blokkolnak: a Room a gyűjtés dispatcherén futtatja
 * őket.
 *
 * Írásra kizárólag [Upsert] van, `DELETE` nincs a technikára: a tartalomfrissítés upsert,
 * és a kikerült technika `retired = true` jelölést kap, nem törlést (ADR-004). Az
 * egyetlen ürítés a `synonym_group` táblán megengedett, mert annak `groupId`-ja nem stabil
 * azonosító, és felhasználói adat nem hivatkozik rá.
 */

@Dao
internal interface CategoryDao {

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    /**
     * A kategóriák a tartalom által diktált sorrendben (DR-02): a lista **nem** ábécé
     * szerinti, mert a kategóriák oktatási logikát követnek. Az `id` csak holtversenyt tör.
     */
    @Query("SELECT * FROM category ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int
}

@Dao
internal interface TechniqueDao {

    @Upsert
    suspend fun upsertAll(techniques: List<TechniqueEntity>)

    @Query("SELECT * FROM technique WHERE id = :id")
    suspend fun getById(id: TechniqueId): TechniqueEntity?

    /** A listákban, a tanulásban és a kvízjelöltek között csak az aktív technika jelenik meg. */
    @Query("SELECT * FROM technique WHERE retired = 0 ORDER BY romajiAscii ASC")
    fun observeActive(): Flow<List<TechniqueEntity>>

    /**
     * Az aktív technikák egy kategóriahalmazon belül (FR-02, T-201).
     *
     * A kategóriaszűrés **SQL-ben** van, nem a repositoryban Kotlin oldalon: a szűrő a
     * `technique.categoryId` indexére esik, és így a lekérdezés nem olvassa be a teljes
     * táblát. Üres halmazra ez a lekérdezés nem hívható (az `IN ()` egyetlen sort sem
     * adna) – a „minden kategória" esetet a hívó az [observeActive]-val fedi le.
     */
    @Query(
        "SELECT * FROM technique WHERE retired = 0 AND categoryId IN (:categories) " +
            "ORDER BY romajiAscii ASC",
    )
    fun observeActiveInCategories(categories: List<CategoryId>): Flow<List<TechniqueEntity>>

    /**
     * A keresés **SQL-ben**: aktív technikák, amelyek illeszkednek a normalizált
     * keresőminta részszavára (FR-03, T-202, ADR-012).
     *
     * Három helyen illeszkedhet: a [TechniqueEntity.searchRomaji], a
     * [TechniqueEntity.searchHungarian] oszlopon, vagy a technika bármelyik aliasának
     * [TechniqueAliasEntity.aliasNormalized] alakján. Az alias-ág `EXISTS` altkérdés, nem
     * `JOIN`: a `JOIN` több aliasra ugyanazt a technikát többször adná vissza, és a
     * `DISTINCT` a `t.*` projekcióval drágább is, zavarosabb is.
     *
     * A `nameMatched` és a `matchedAlias` ugyanebben a lekérdezésben keletkezik, hogy a
     * találat meg tudja mondani, **melyik** alias egyezett (FUNCTIONAL_SPEC 3.1:
     * „Egyezés: `{alias}`"). Egy Kotlin oldali újraellenőrzés ugyanazt a mintaillesztést
     * írná le másodszor, más nyelven – és csendben szétcsúszhatna az SQL-beli ágtól.
     *
     * @param pattern **kész** `LIKE` minta (`%…%`), amelyben a joker karakterek már
     *   escape-elve vannak; az előállítása a repository `searchPattern` függvénye. Üres
     *   keresésre `%`, ami minden sorra illeszkedik – a kikapcsolt szűrő nem „nincs
     *   találat". A `\` escape karakter, ezért a mintában literális `\` nem fordulhat elő
     *   escape-elés nélkül.
     */
    @Query(
        """
        SELECT t.*,
            (t.search_romaji LIKE :pattern ESCAPE '\'
                OR t.search_hungarian LIKE :pattern ESCAPE '\') AS nameMatched,
            (
                SELECT a.alias FROM technique_alias a
                WHERE a.techniqueId = t.id AND a.aliasNormalized LIKE :pattern ESCAPE '\'
                ORDER BY a.alias ASC LIMIT 1
            ) AS matchedAlias
        FROM technique t
        WHERE t.retired = 0
            AND (
                t.search_romaji LIKE :pattern ESCAPE '\'
                OR t.search_hungarian LIKE :pattern ESCAPE '\'
                OR EXISTS(
                    SELECT 1 FROM technique_alias a
                    WHERE a.techniqueId = t.id AND a.aliasNormalized LIKE :pattern ESCAPE '\'
                )
            )
        ORDER BY t.romajiAscii ASC
        """,
    )
    fun observeActiveMatching(pattern: String): Flow<List<TechniqueSearchRow>>

    /**
     * Ugyanaz a keresés egy kategóriahalmazon belül (FR-02 + FR-03).
     *
     * Üres halmazra nem hívható (`IN ()` egyetlen sort sem ad) – a „minden kategória"
     * esetet a hívó az [observeActiveMatching]-gal fedi le.
     */
    @Query(
        """
        SELECT t.*,
            (t.search_romaji LIKE :pattern ESCAPE '\'
                OR t.search_hungarian LIKE :pattern ESCAPE '\') AS nameMatched,
            (
                SELECT a.alias FROM technique_alias a
                WHERE a.techniqueId = t.id AND a.aliasNormalized LIKE :pattern ESCAPE '\'
                ORDER BY a.alias ASC LIMIT 1
            ) AS matchedAlias
        FROM technique t
        WHERE t.retired = 0
            AND t.categoryId IN (:categories)
            AND (
                t.search_romaji LIKE :pattern ESCAPE '\'
                OR t.search_hungarian LIKE :pattern ESCAPE '\'
                OR EXISTS(
                    SELECT 1 FROM technique_alias a
                    WHERE a.techniqueId = t.id AND a.aliasNormalized LIKE :pattern ESCAPE '\'
                )
            )
        ORDER BY t.romajiAscii ASC
        """,
    )
    fun observeActiveMatchingInCategories(
        pattern: String,
        categories: List<CategoryId>,
    ): Flow<List<TechniqueSearchRow>>

    /** Pontszerű változat a kvízjelöltekhez: nem kell rá megfigyelés (T-201, T-402). */
    @Query("SELECT * FROM technique WHERE retired = 0 ORDER BY romajiAscii ASC")
    suspend fun getActive(): List<TechniqueEntity>

    @Query(
        "SELECT * FROM technique WHERE retired = 0 AND categoryId IN (:categories) " +
            "ORDER BY romajiAscii ASC",
    )
    suspend fun getActiveInCategories(categories: List<CategoryId>): List<TechniqueEntity>

    /** Minden sor, a visszavontakkal együtt – a Haladás képernyőnek és az importnak kell. */
    @Query("SELECT * FROM technique ORDER BY romajiAscii ASC")
    suspend fun getAll(): List<TechniqueEntity>

    @Query("SELECT id FROM technique")
    suspend fun getAllIds(): List<TechniqueId>

    /**
     * Visszavonás **törlés helyett** (ADR-004 2. pont): az aktuális kiadásból kimaradt
     * technikák sora megmarad, hogy a rá hivatkozó felhasználói rekordok hivatkozása
     * értelmes maradjon (FR-33, TR-05).
     *
     * @return ahány sort tényleg érintett.
     */
    @Query("UPDATE technique SET retired = 1 WHERE id IN (:ids) AND retired = 0")
    suspend fun markRetired(ids: List<TechniqueId>): Int

    @Query("SELECT COUNT(*) FROM technique")
    suspend fun count(): Int

    /**
     * Az aktív technikák száma, megfigyelhetően.
     *
     * A tartalomállapot (`ContentState`, T-201) ebből tudja megmondani, hogy van-e
     * egyáltalán megjeleníthető tartalom – a `content_state` tábla `acceptedCount`
     * összegzése erre nem elég, mert az a **legutóbbi import** mérlege, nem a tábla
     * aktuális tartalma (egy elutasított frissítés után a kettő eltér).
     */
    @Query("SELECT COUNT(*) FROM technique WHERE retired = 0")
    fun observeActiveCount(): Flow<Int>
}

@Dao
internal interface TechniqueAliasDao {

    @Upsert
    suspend fun upsertAll(aliases: List<TechniqueAliasEntity>)

    /**
     * Egy technika aliasainak cseréje importkor: az aliaslista szerkeszthető, tehát egy
     * eltávolított alias nem élhet túl egy upsertet. Ez tartalom → tartalom törlés, a
     * felhasználói adatot nem érinti (ADR-004).
     */
    @Query("DELETE FROM technique_alias WHERE techniqueId = :techniqueId")
    suspend fun deleteForTechnique(techniqueId: TechniqueId)

    @Query("SELECT * FROM technique_alias WHERE techniqueId = :techniqueId ORDER BY alias ASC")
    suspend fun getForTechnique(techniqueId: TechniqueId): List<TechniqueAliasEntity>

    /**
     * **Minden** alias egyetlen lekérdezésben, technikaazonosító szerint csoportosítható
     * sorrendben (NFR-10).
     *
     * A lista és a kvízjelöltek összeállítása technikánkénti [getForTechnique] hívással
     * N+1 lekérdezés lenne – 161 rekordnál 162 kérdés az adatbázishoz, minden
     * újrarendereléskor. Az aliastábla a teljes tudástárra néhány száz sor, ezért a
     * teljes beolvasás és a memóriabeli csoportosítás a kisebb költség.
     */
    @Query("SELECT * FROM technique_alias ORDER BY techniqueId ASC, alias ASC")
    fun observeAll(): Flow<List<TechniqueAliasEntity>>

    @Query("SELECT * FROM technique_alias ORDER BY techniqueId ASC, alias ASC")
    suspend fun getAll(): List<TechniqueAliasEntity>

    @Query("SELECT COUNT(*) FROM technique_alias")
    suspend fun count(): Int
}

@Dao
internal interface SynonymGroupDao {

    @Upsert
    suspend fun upsertAll(members: List<SynonymGroupMemberEntity>)

    /** A csoportokat az import minden futáskor újraszámolja, ezért teljes csere. */
    @Query("DELETE FROM synonym_group")
    suspend fun clear()

    @Query("SELECT * FROM synonym_group ORDER BY groupId ASC, techniqueId ASC")
    suspend fun getAll(): List<SynonymGroupMemberEntity>

    @Query("SELECT COUNT(*) FROM synonym_group")
    suspend fun count(): Int
}

@Dao
internal interface ContentStateDao {

    @Upsert
    suspend fun upsert(state: ContentStateEntity)

    /**
     * Az egysoros tábla megfigyelése. `null`, amíg egyetlen import sem futott le – ez az
     * első indítás rendes állapota, nem hiba.
     */
    @Query("SELECT * FROM content_state WHERE id = 1")
    fun observe(): Flow<ContentStateEntity?>

    @Query("SELECT * FROM content_state WHERE id = 1")
    suspend fun get(): ContentStateEntity?
}
