package hu.kyokushin.trainer.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.SelfRating
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

    /**
     * Minden munkamenet ürítése (FR-35, T-502).
     *
     * A rá hivatkozó `quiz_attempt` sorok a `CASCADE` idegen kulcs miatt ezzel együtt
     * törlődnek – a `ProgressRepository.clearAllProgress()` ezért csak ezt és a
     * [StudyReviewDao.deleteAll] hívást futtatja egy tranzakcióban, nem mindhárom
     * felhasználói táblát külön. A tartalomtáblákhoz ez a lekérdezés nem nyúl.
     *
     * @return a törölt sorok száma.
     */
    @Query("DELETE FROM quiz_session")
    suspend fun deleteAll(): Int
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

    // --- Haladás: aggregált statisztika (FR-23–FR-27, DR-09, NFR-10, T-501) ---------
    //
    // Minden lekérdezés itt lent **aggregált SQL**: nincs mellette számláló tábla/oszlop,
    // amit az írásnak (`insertIgnoringDuplicate`) karban kellene tartania (DR-09). A
    // `ratio` számítása (és a „nincs adat vs. 0%" döntés, FR-27) sosem itt, SQL-ben
    // történik, hanem a mapperben ([toScore], `UserEntityMappers.kt`) – egyetlen helyen,
    // ugyanazzal a szabállyal, mint a `core/quiz` `Score`.

    /**
     * Összesített sikerarány **minden** próbálkozásból (FR-23).
     *
     * `COUNT(*)` és `COALESCE(SUM(correct), 0)` nélküli `WHERE`/`GROUP BY` esetén mindig
     * ad egy sort, még 0 próbálkozásnál is (`answered = 0, correctCount = 0`) – a hívó
     * ebből, nem egy `null` Flow-elemből dönt „nincs adat"-ot (FR-27).
     */
    @Query("SELECT COUNT(*) AS answered, COALESCE(SUM(correct), 0) AS correctCount FROM quiz_attempt")
    fun observeOverallScore(): Flow<OverallScoreRow>

    /**
     * Kategóriánkénti sikerarány (FR-24).
     *
     * A `category` tábla a `FROM`, nem a `quiz_attempt` – ez a döntő, hogy **minden**
     * kategória megjelenjen, azok is, amelyekben még nincs próbálkozás (FUNCTIONAL_SPEC
     * 6.1/3.). A két `LEFT JOIN` (`category → technique → quiz_attempt`) ezt a
     * technikán **keresztüli**, nem közvetlen kapcsolatot követi (a `quiz_attempt`-nek
     * nincs `categoryId` oszlopa, csak `techniqueId`-ja).
     */
    @Query(
        """
        SELECT c.id AS categoryId,
            COUNT(qa.id) AS answered,
            COALESCE(SUM(qa.correct), 0) AS correctCount
        FROM category c
        LEFT JOIN technique t ON t.categoryId = c.id
        LEFT JOIN quiz_attempt qa ON qa.techniqueId = t.id
        GROUP BY c.id
        ORDER BY c.sortOrder ASC, c.id ASC
        """,
    )
    fun observeCategoryScores(): Flow<List<CategoryScoreRow>>

    /**
     * Egyetlen technika összesített próbálkozás/helyes/utolsó-gyakorlás adata (FR-25).
     *
     * A `:techniqueId` bemenet a `SELECT`-ben **és** a `WHERE`-ben is szerepel: az első
     * előfordulás egyszerűen visszaadja a hívó paramétert eredményoszlopként, hogy a
     * [TechniqueScoreRow] ugyanazzal a típussal használható legyen itt és az
     * [observeNeedsPracticeScores]-nál is (ahol a `techniqueId` valódi `GROUP BY` oszlop)
     * – nem kell két, majdnem azonos projekciós típus. A lekérdezés mindig pontosan egy
     * sort ad, akkor is, ha a technikára még sosem érkezett válasz (`answered = 0`,
     * `lastPracticedAt = null`): a `WHERE` szűrés nélküli aggregátum ilyenkor is egy sor.
     */
    @Query(
        """
        SELECT :techniqueId AS techniqueId,
            COUNT(*) AS answered,
            COALESCE(SUM(correct), 0) AS correctCount,
            MAX(answeredAt) AS lastPracticedAt
        FROM quiz_attempt
        WHERE techniqueId = :techniqueId
        """,
    )
    fun observeTechniqueScore(techniqueId: TechniqueId): Flow<TechniqueScoreRow>

    /**
     * Egy technika próbálkozásainak kérdéstípus szerinti bontása (FR-25,
     * FUNCTIONAL_SPEC 6.2 „melyik típusban hibázik"). Lásd [observeTechniqueScore] a
     * `:techniqueId` kettős előfordulásának indokáért.
     */
    @Query(
        """
        SELECT :techniqueId AS techniqueId, questionType,
            COUNT(*) AS answered,
            COALESCE(SUM(correct), 0) AS correctCount
        FROM quiz_attempt
        WHERE techniqueId = :techniqueId
        GROUP BY questionType
        """,
    )
    fun observeTechniqueScoreByQuestionType(
        techniqueId: TechniqueId,
    ): Flow<List<TechniqueQuestionTypeScoreRow>>

    /**
     * A legfeljebb [limit] legtöbb gyakorlást igénylő technika (FR-26).
     *
     * `GROUP BY techniqueId` miatt **csak** olyan technika kerül bele, amelyre van
     * legalább egy próbálkozás – a sosem gyakorolt technika szándékosan hiányzik, nincs
     * hozzá külön `HAVING COUNT(*) >= 1` szűrő, mert a csoportosítás ezt már eleve
     * biztosítja. A rendezés (a) sikerarány **növekvő** (`CAST(SUM(correct) AS REAL) /
     * COUNT(*)`, ugyanazon csoport aggregátumaiból, nem egy második lekérdezésből), majd
     * (b) az utolsó gyakorlás **régisége** (`MAX(answeredAt) ASC` – a legkisebb, azaz
     * legrégebbi időbélyeg elöl). A `ratio` itt **csak rendezési kulcs**: a visszaadott
     * [TechniqueScoreRow] innentől a mapperben kapja meg a végleges
     * [hu.kyokushin.trainer.core.quiz.scoring.Score]-ját, ugyanazzal a „nincs adat"
     * szabállyal, mint minden más statisztika (FR-27, DR-09).
     */
    @Query(
        """
        SELECT techniqueId,
            COUNT(*) AS answered,
            COALESCE(SUM(correct), 0) AS correctCount,
            MAX(answeredAt) AS lastPracticedAt
        FROM quiz_attempt
        GROUP BY techniqueId
        ORDER BY (CAST(SUM(correct) AS REAL) / COUNT(*)) ASC, MAX(answeredAt) ASC
        LIMIT :limit
        """,
    )
    fun observeNeedsPracticeScores(limit: Int): Flow<List<TechniqueScoreRow>>

    /**
     * Kérdéstípus szerinti bontás **több** technikára egyszerre – a „gyakorlást igénylő"
     * lista ([observeNeedsPracticeScores]) tételeihez kell, hogy a
     * [hu.kyokushin.trainer.core.data.repository.TechniqueProgress] ugyanazt a
     * `byQuestionType` mezőt kapja, mint az egyetlen technikára szűrt
     * [observeTechniqueScoreByQuestionType]. Technikánkénti, egyenkénti lekérdezés N+1
     * probléma lenne (NFR-10); ez egyetlen `IN (:techniqueIds)` lekérdezés.
     */
    @Query(
        """
        SELECT techniqueId, questionType,
            COUNT(*) AS answered,
            COALESCE(SUM(correct), 0) AS correctCount
        FROM quiz_attempt
        WHERE techniqueId IN (:techniqueIds)
        GROUP BY techniqueId, questionType
        """,
    )
    fun observeQuestionTypeScoresFor(
        techniqueIds: List<TechniqueId>,
    ): Flow<List<TechniqueQuestionTypeScoreRow>>
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

    /**
     * Ugyanaz a lekérdezés, mint [getForTechnique], de megfigyelhetően (FR-28,
     * FUNCTIONAL_SPEC 6.2, T-506) – a technika-részletező a képernyőn marad, amíg a
     * felhasználó egy másik tanulókártyán újabb önértékelést adhat, tehát a lista
     * magától frissül, nem csak egyszeri lekérdezés. A legfrissebb önértékelés elöl.
     */
    @Query("SELECT * FROM study_review WHERE techniqueId = :techniqueId ORDER BY reviewedAt DESC, id DESC")
    fun observeForTechnique(techniqueId: TechniqueId): Flow<List<StudyReviewEntity>>

    @Query("SELECT COUNT(*) FROM study_review")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_review")
    suspend fun count(): Int

    /**
     * Hány önértékelés született az adott besorolással, megfigyelhetően (FR-28, T-301).
     *
     * **Minden önértékelés sor önmagáért számít** – nem a technikánkénti legutóbbi ítélet
     * összegzése. A `study_review` tábla append-only (lásd a fájl fejdokumentációját), és a
     * Haladás képernyő „Tudtam: `{n}` · Gyakorolnom kell: `{m}`" sora
     * (FUNCTIONAL_SPEC 6.1/5.) a leadott önértékelések összes darabszámát mutatja, nem a
     * technikák darabszámát. A „gyakorlást igénylő" lista (FR-25/26, T-501) ezzel szemben a
     * kvízsikerarányra épül, nem erre a számra – a kettő szándékosan nem keveredik (FR-28).
     */
    @Query("SELECT COUNT(*) FROM study_review WHERE selfRating = :rating")
    fun observeCountByRating(rating: SelfRating): Flow<Int>

    /**
     * Minden önértékelés ürítése (FR-35, T-502). A technikatáblákhoz nem nyúl.
     *
     * @return a törölt sorok száma.
     */
    @Query("DELETE FROM study_review")
    suspend fun deleteAll(): Int
}
