package hu.kyokushin.trainer.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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
