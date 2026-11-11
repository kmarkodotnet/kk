package hu.kyokushin.trainer.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * A felhasználói táblák DAO-i (DR-04). Minden függvény `suspend` vagy `Flow`.
 *
 * Itt nincs `@Upsert` és nincs `@Update` a próbálkozásokra: a kvízválasz és az
 * önértékelés **append-only** adat. Egy upsert az „első válasz számít" szabályt sértené
 * (ADR-005 mérlegelt alternatívái), egy update pedig átírná a már megítélt helyességet,
 * amire a statisztika épül (DR-09).
 */

@Dao
internal interface QuizSessionDao {

    @Insert
    suspend fun insert(session: QuizSessionEntity)

    @Query("SELECT * FROM quiz_session WHERE id = :id")
    suspend fun getById(id: SessionId): QuizSessionEntity?

    /**
     * A megválaszolt kérdések számlálója **az adatbázisban** nő, nem beolvasott értékből
     * visszaírva: a `read-modify-write` két párhuzamos coroutine között elveszíthetne egy
     * választ. A hívó akkor növeli, ha a próbálkozás beszúrása tényleg létrehozott sort
     * (lásd [QuizAttemptDao.insertIgnoringDuplicate]), így a számláló és a sorok száma
     * nem csúszhat el (FR-18, DR-09).
     */
    @Query("UPDATE quiz_session SET answeredCount = answeredCount + 1 WHERE id = :id")
    suspend fun incrementAnsweredCount(id: SessionId): Int

    /**
     * A menet lezárása (FR-19). Csak akkor ír, ha a menet még befejezetlen – egy lezárt
     * menet lezárási időpontja nem íródhat át.
     */
    @Query("UPDATE quiz_session SET finishedAt = :finishedAt WHERE id = :id AND finishedAt IS NULL")
    suspend fun markFinished(id: SessionId, finishedAt: Instant): Int

    @Query("SELECT COUNT(*) FROM quiz_session")
    suspend fun count(): Int
}

@Dao
internal interface QuizAttemptDao {

    /**
     * Egy kvízválasz rögzítése – **az egyszeri válasz kikényszerítésének helye** (FR-18).
     *
     * A `UNIQUE(sessionId, questionId)` index ütközésekor az
     * [OnConflictStrategy.IGNORE] miatt nem keletkezik kivétel: a beszúrás egyszerűen nem
     * történik meg, és a visszatérési érték `-1`. Ebből lesz a
     * `ProgressRepository.recordAttempt` `false` válasza, amiből a hívó tudja, hogy a
     * kérdésre már volt válasz, és az állapotot nem kell módosítani (ADR-005).
     *
     * @return az új sor azonosítója, vagy `-1`, ha a kérdésre ebben a menetben már volt válasz.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicate(attempt: QuizAttemptEntity): Long

    @Query("SELECT * FROM quiz_attempt WHERE sessionId = :sessionId ORDER BY answeredAt ASC, id ASC")
    suspend fun getBySession(sessionId: SessionId): List<QuizAttemptEntity>

    @Query("SELECT * FROM quiz_attempt WHERE techniqueId = :techniqueId ORDER BY answeredAt ASC, id ASC")
    suspend fun getByTechnique(techniqueId: TechniqueId): List<QuizAttemptEntity>

    @Query(
        "SELECT COUNT(*) FROM quiz_attempt WHERE sessionId = :sessionId AND questionId = :questionId",
    )
    suspend fun countForQuestion(sessionId: SessionId, questionId: QuestionId): Int

    @Query("SELECT COUNT(*) FROM quiz_attempt")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM quiz_attempt")
    suspend fun count(): Int
}

@Dao
internal interface StudyReviewDao {

    /**
     * Önértékelés rögzítése. Minden hívás **új sort** hoz létre, az ismételt önértékelés
     * nem ütközés, hanem a haladás időbeli alakulásának adata (DR-09).
     */
    @Insert
    suspend fun insert(review: StudyReviewEntity): Long

    @Query("SELECT * FROM study_review WHERE techniqueId = :techniqueId ORDER BY reviewedAt DESC, id DESC")
    suspend fun getForTechnique(techniqueId: TechniqueId): List<StudyReviewEntity>

    @Query("SELECT COUNT(*) FROM study_review")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_review")
    suspend fun count(): Int
}
