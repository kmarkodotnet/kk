package hu.kyokushin.trainer.core.data.repository

import androidx.room.withTransaction
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.QuizAttemptEntity
import hu.kyokushin.trainer.core.data.db.QuizSessionEntity
import hu.kyokushin.trainer.core.data.db.StudyReviewEntity
import hu.kyokushin.trainer.core.data.db.toQuizAttempt
import hu.kyokushin.trainer.core.data.db.toQuizSession
import hu.kyokushin.trainer.core.data.db.toScore
import hu.kyokushin.trainer.core.data.db.toTechniqueProgress
import hu.kyokushin.trainer.core.data.di.IoDispatcher
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * A [ProgressRepository] egyetlen megvalósítása Room felett (ARCHITECTURE 5.1, NFR-03).
 *
 * `internal`, és a Hilt köti be (`@Binds`). A kötésnek a `core/data`-ban kell élnie, mert a
 * konstruktor paramétere, a [KyokushinDatabase] is `internal` – ugyanaz az indok, mint a
 * [RoomTechniqueRepository]-nál.
 *
 * ## A `SessionId` előállítása (ADR-007, NFR-07)
 *
 * A [startSession] generál egy új, globálisan egyedi azonosítót, de **nem** `UUID.randomUUID()`
 * vagy `System.currentTimeMillis()` hívással – azok közvetlen rendszerhívások lennének
 * üzleti kódban, amit az NFR-07 tilt. Helyette a már **injektált** forrásokat kombinálja:
 * az [clock] aktuális időbélyegét és a hívó által átadott, injektált RNG-ből sorsolt
 * [seed]-et (ADR-007 – a kvízmotor seedje egyébként is magán a menet rekordján és a
 * `SavedStateHandle`-en keresztül is egyedi kell legyen). A kettő együtt gyakorlati
 * bizonyossággal egyedi: két menetindítás ugyanabban a milliszekundumban csak akkor
 * ütközne, ha az injektált RNG is pontosan ugyanazt a 64 bites seedet sorsolná – ez nem a
 * repository felelőssége, hanem az RNG forrásminőségéé.
 *
 * ## Tranzakciós határok
 *
 * A [recordAttempt] a próbálkozás beszúrását és a menet `answeredCount` növelését egy
 * [androidx.room.withTransaction] blokkban végzi: a kettő csak együtt konzisztens (lásd
 * [hu.kyokushin.trainer.core.data.db.QuizSessionDao.incrementAnsweredCount]
 * dokumentációját). A [clearAllProgress] a két felhasználói tábla ürítését egy
 * tranzakcióban futtatja (FR-35) – ez zárja ki, hogy egy megszakított törlés árva
 * `quiz_attempt` sort hagyjon egy már törölt menethez.
 *
 * @property database az adatbázis; a DAO-k rajta keresztül érhetők el.
 * @property clock az időbélyegek egyetlen forrása (NFR-07): `startedAt`, `answeredAt`,
 *   `finishedAt`, `reviewedAt` mind innen jön, soha nem `Instant.now()`-ból.
 * @property ioDispatcher a háttér-I/O dispatcher; a kötést a `di` modul adja.
 */
internal class RoomProgressRepository @Inject constructor(
    private val database: KyokushinDatabase,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProgressRepository {

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId =
        withContext(ioDispatcher) {
            val now = clock.instant()
            val id = SessionId.of("session-${now.toEpochMilli()}-$seed")
            database.quizSessionDao().insert(
                QuizSessionEntity(
                    id = id,
                    startedAt = now,
                    requestedCount = requestedCount,
                    seed = seed,
                ),
            )
            id
        }

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean = withContext(ioDispatcher) {
        database.withTransaction {
            val rowId = database.quizAttemptDao().insertIgnoringDuplicate(
                QuizAttemptEntity(
                    sessionId = attempt.sessionId,
                    questionId = attempt.questionId,
                    techniqueId = attempt.techniqueId,
                    questionType = attempt.questionType,
                    selectedTechniqueId = attempt.selectedTechniqueId,
                    correct = attempt.correct,
                    answeredAt = clock.instant(),
                ),
            )
            // `-1` = ütközés a `UNIQUE(sessionId, questionId)` indexen: a kérdésre már
            // volt válasz. A menet számlálóját ekkor nem szabad növelni (ADR-005).
            val recorded = rowId != -1L
            if (recorded) {
                database.quizSessionDao().incrementAnsweredCount(attempt.sessionId)
            }
            recorded
        }
    }

    override suspend fun finishSession(id: SessionId) {
        withContext(ioDispatcher) {
            database.quizSessionDao().markFinished(id, clock.instant())
        }
    }

    override suspend fun getSessionResult(sessionId: SessionId): QuizSessionResult? =
        withContext(ioDispatcher) {
            val sessionEntity = database.quizSessionDao().getById(sessionId) ?: return@withContext null
            val attemptEntities = database.quizAttemptDao().getBySession(sessionId)
            QuizSessionResult(
                session = sessionEntity.toQuizSession(),
                attempts = attemptEntities.map { it.toQuizAttempt() },
            )
        }

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        withContext(ioDispatcher) {
            database.studyReviewDao().insert(
                StudyReviewEntity(
                    techniqueId = techniqueId,
                    selfRating = rating,
                    reviewedAt = clock.instant(),
                ),
            )
        }
    }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> =
        combine(
            database.studyReviewDao().observeCountByRating(SelfRating.KNEW),
            database.studyReviewDao().observeCountByRating(SelfRating.NEEDS_PRACTICE),
        ) { known, needsPractice ->
            StudyReviewSummary(knownCount = known, needsPracticeCount = needsPractice)
        }.flowOn(ioDispatcher)

    override fun observeOverallProgress(): Flow<OverallProgress> =
        database.quizAttemptDao().observeOverallScore()
            .map { row -> OverallProgress(row.toScore()) }
            .flowOn(ioDispatcher)

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> =
        database.quizAttemptDao().observeCategoryScores()
            .map { rows -> rows.map { CategoryProgress(categoryId = it.categoryId, score = it.toScore()) } }
            .flowOn(ioDispatcher)

    override fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress> =
        combine(
            database.quizAttemptDao().observeTechniqueScore(id),
            database.quizAttemptDao().observeTechniqueScoreByQuestionType(id),
        ) { overall, byType -> overall.toTechniqueProgress(byType) }
            .flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>> =
        database.quizAttemptDao().observeNeedsPracticeScores(limit)
            .flatMapLatest { rows ->
                if (rows.isEmpty()) {
                    // Nincs legalább egy próbálkozással rendelkező technika – a hívó
                    // üres listát kap, nem `null`-t (lásd a `ProgressRepository` KDoc-ját).
                    flowOf(emptyList<TechniqueProgress>())
                } else {
                    val ids = rows.map { it.techniqueId }
                    database.quizAttemptDao().observeQuestionTypeScoresFor(ids).map { breakdown ->
                        val byTechnique = breakdown.groupBy { it.techniqueId }
                        rows.map { row -> row.toTechniqueProgress(byTechnique[row.techniqueId].orEmpty()) }
                    }
                }
            }.flowOn(ioDispatcher)

    override suspend fun clearAllProgress() {
        withContext(ioDispatcher) {
            database.withTransaction {
                // A `quiz_session` ürítése a `CASCADE` idegen kulcs miatt a hozzá tartozó
                // `quiz_attempt` sorokat is viszi (ADR-004) – nincs szükség harmadik,
                // külön `DELETE FROM quiz_attempt` hívásra.
                database.quizSessionDao().deleteAll()
                database.studyReviewDao().deleteAll()
            }
        }
    }
}
