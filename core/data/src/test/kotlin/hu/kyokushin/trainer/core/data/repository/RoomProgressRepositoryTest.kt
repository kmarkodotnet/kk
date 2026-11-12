package hu.kyokushin.trainer.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.db.CategoryEntity
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.TechniqueEntity
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A [ProgressRepository] tényleges viselkedése in-memory Room példányon
 * (FR-10, FR-18, FR-19, FR-28, FR-35, NFR-07, NFR-09, ADR-005, ADR-007, TR-04).
 *
 * A DAO-szintű kényszereket (`UNIQUE(sessionId, questionId)`, `CASCADE`, enum/időbélyeg
 * tárolás) a [hu.kyokushin.trainer.core.data.db.KyokushinDatabaseTest] már bizonyítja –
 * ez a készlet a *repository* hozzáadott viselkedését méri: az injektált `Clock`-ból vett
 * időbélyegeket, a `SessionId` előállítását, a tranzakciós atomicitást és a `Boolean`
 * visszatérést.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomProgressRepositoryTest {

    private lateinit var database: KyokushinDatabase
    private lateinit var repository: ProgressRepository

    private val fixedInstant = Instant.ofEpochMilli(1_762_000_000_000L)
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    private val techniqueId = TechniqueId.of("KY-0001")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KyokushinDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Lásd [hu.kyokushin.trainer.core.data.repository.RoomTechniqueRepositoryTest] indokát. */
    private fun repositoryTest(body: suspend TestScope.() -> Unit) = runTest {
        repository = RoomProgressRepository(
            database = database,
            clock = clock,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        body()
    }

    // --- ADR-007 / NFR-07: a menetindítás injektált forrásokból dolgozik --------

    @Test
    fun startSession_usesInjectedClockAndSeedNotSystemCalls_NFR07() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 10, seed = 42L)

        val stored = database.quizSessionDao().getById(sessionId)
        assertThat(stored).isNotNull()
        assertThat(stored!!.startedAt).isEqualTo(fixedInstant)
        assertThat(stored.requestedCount).isEqualTo(10)
        assertThat(stored.seed).isEqualTo(42L)
        assertThat(stored.answeredCount).isEqualTo(0)
        assertThat(stored.finishedAt).isNull()
    }

    @Test
    fun startSession_differentSeedsProduceDifferentSessionIds_ADR007() = repositoryTest {
        val first = repository.startSession(requestedCount = 10, seed = 1L)
        val second = repository.startSession(requestedCount = 10, seed = 2L)

        assertThat(first).isNotEqualTo(second)
        assertThat(database.quizSessionDao().count()).isEqualTo(2)
    }

    // --- FR-18 / ADR-005: egyszeri válasz a repository szerződésében ------------

    @Test
    fun recordAttempt_firstAnswerIsRecordedAndIncrementsAnsweredCount_FR18() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 10, seed = 1L)

        val recorded = repository.recordAttempt(
            NewAttempt(
                sessionId = sessionId,
                questionId = QuestionId.of("q-1"),
                techniqueId = techniqueId,
                questionType = QuestionType.JP_TO_HU,
                selectedTechniqueId = techniqueId,
                correct = true,
            ),
        )

        assertThat(recorded).isTrue()
        val stored = database.quizAttemptDao().getBySession(sessionId).single()
        assertThat(stored.correct).isTrue()
        // Az `answeredAt` az injektált `Clock`-ból jön, nem a hívó adja át (NFR-07).
        assertThat(stored.answeredAt).isEqualTo(fixedInstant)
        assertThat(database.quizSessionDao().getById(sessionId)!!.answeredCount).isEqualTo(1)
    }

    @Test
    fun recordAttempt_secondAnswerToSameQuestionReturnsFalseAndDoesNotIncrement_TR04() =
        repositoryTest {
            val sessionId = repository.startSession(requestedCount = 10, seed = 1L)
            val attempt = NewAttempt(
                sessionId = sessionId,
                questionId = QuestionId.of("q-1"),
                techniqueId = techniqueId,
                questionType = QuestionType.JP_TO_HU,
                selectedTechniqueId = techniqueId,
                correct = true,
            )

            val first = repository.recordAttempt(attempt)
            // Gyors dupla koppintás: ugyanaz a kérdés, immár eltérő (hibás) válasz.
            val second = repository.recordAttempt(attempt.copy(correct = false))

            assertThat(first).isTrue()
            assertThat(second).isFalse()
            assertThat(database.quizAttemptDao().count()).isEqualTo(1)
            // Az ELSŐ válasz ítélete maradt meg, és a számláló nem nőtt duplán.
            assertThat(database.quizAttemptDao().getBySession(sessionId).single().correct).isTrue()
            assertThat(database.quizSessionDao().getById(sessionId)!!.answeredCount).isEqualTo(1)
        }

    // --- FR-19: a megszakított menet `finishedAt`-ja null marad -----------------

    @Test
    fun finishSession_unfinishedSessionKeepsNullFinishedAtUntilClosed_FR19() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 10, seed = 1L)

        assertThat(database.quizSessionDao().getById(sessionId)!!.finishedAt).isNull()

        repository.finishSession(sessionId)

        assertThat(database.quizSessionDao().getById(sessionId)!!.finishedAt).isEqualTo(fixedInstant)
    }

    // --- FR-10 / FR-28: önértékelés külön a kvízstatisztikától -------------------

    @Test
    fun recordStudyReview_isStoredWithInjectedTimestamp_FR10() = repositoryTest {
        repository.recordStudyReview(techniqueId, SelfRating.KNEW)

        val stored = database.studyReviewDao().getForTechnique(techniqueId).single()
        assertThat(stored.selfRating).isEqualTo(SelfRating.KNEW)
        assertThat(stored.reviewedAt).isEqualTo(fixedInstant)
    }

    @Test
    fun observeStudyReviewSummary_countsEachRatingSeparatelyAndNeverTouchesQuizData_FR28() =
        repositoryTest {
            repository.recordStudyReview(techniqueId, SelfRating.KNEW)
            repository.recordStudyReview(techniqueId, SelfRating.KNEW)
            repository.recordStudyReview(techniqueId, SelfRating.NEEDS_PRACTICE)

            val summary = repository.observeStudyReviewSummary().first()

            assertThat(summary.knownCount).isEqualTo(2)
            assertThat(summary.needsPracticeCount).isEqualTo(1)
            assertThat(summary.totalCount).isEqualTo(3)
            // Az önértékelés nem ír a kvízmunkamenet-táblákba.
            assertThat(database.quizSessionDao().count()).isEqualTo(0)
            assertThat(database.quizAttemptDao().count()).isEqualTo(0)
        }

    // --- FR-35 / NFR-09: a törlés csak a felhasználói táblákat üríti ------------

    @Test
    fun clearAllProgress_removesUserDataButKeepsContentUntouched_FR35() = repositoryTest {
        seedContent()
        val sessionId = repository.startSession(requestedCount = 10, seed = 1L)
        repository.recordAttempt(
            NewAttempt(
                sessionId = sessionId,
                questionId = QuestionId.of("q-1"),
                techniqueId = techniqueId,
                questionType = QuestionType.JP_TO_HU,
                selectedTechniqueId = techniqueId,
                correct = true,
            ),
        )
        repository.recordStudyReview(techniqueId, SelfRating.NEEDS_PRACTICE)

        repository.clearAllProgress()

        assertThat(database.quizSessionDao().count()).isEqualTo(0)
        assertThat(database.quizAttemptDao().count()).isEqualTo(0)
        assertThat(database.studyReviewDao().count()).isEqualTo(0)
        // A tartalom és a `content_state` érintetlen marad (FR-35, NFR-09).
        assertThat(database.techniqueDao().count()).isEqualTo(1)
        assertThat(database.categoryDao().count()).isEqualTo(1)
    }

    // --- NFR-03 / TR-07 / ADR-001 L4: a publikus API nem lát Room típust --------

    @Test
    fun progressRepository_publicApiExposesNoRoomOrDatabaseTypes_NFR03() {
        val leaked = ProgressRepository::class.java.methods.flatMap { method ->
            val types = buildList {
                add(method.genericReturnType)
                addAll(method.genericParameterTypes)
            }
            types.flatMap { collectTypeNames(it) }
        }.filter { name ->
            name.contains(".core.data.db.") ||
                name.startsWith("androidx.room") ||
                name.startsWith("androidx.sqlite") ||
                name.endsWith("Entity") ||
                name.endsWith("Dao")
        }

        assertThat(leaked).isEmpty()
    }

    /** Egy generikus típus minden összetevőjének neve – `Flow<StudyReviewSummary>` is. */
    private fun collectTypeNames(type: Type): List<String> = when (type) {
        is ParameterizedType ->
            listOf((type.rawType as? Class<*>)?.name.orEmpty()) +
                type.actualTypeArguments.flatMap { collectTypeNames(it) }

        is Class<*> -> listOf(type.name)
        else -> emptyList()
    }

    // --- fixtúra ------------------------------------------------------------------

    private suspend fun seedContent() {
        database.categoryDao().upsertAll(
            listOf(CategoryEntity(id = CategoryId.of("tsuki"), nameHu = "Ütések", sortOrder = 0)),
        )
        database.techniqueDao().upsertAll(
            listOf(
                TechniqueEntity(
                    id = techniqueId,
                    categoryId = CategoryId.of("tsuki"),
                    romaji = "Seiken Chudan Tsuki",
                    romajiAscii = "Seiken Chudan Tsuki",
                    hungarian = "Ököl ütés",
                    status = VerificationStatus.AWAITING_REVIEW,
                    quizExcluded = false,
                    retired = false,
                    datasetVersion = "2026.11.01",
                    searchRomaji = "seiken chudan tsuki",
                    searchHungarian = "okol utes",
                ),
            ),
        )
    }
}
