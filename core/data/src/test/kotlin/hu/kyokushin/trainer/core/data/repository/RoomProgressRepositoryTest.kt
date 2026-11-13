package hu.kyokushin.trainer.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.db.CategoryEntity
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.QuizAttemptEntity
import hu.kyokushin.trainer.core.data.db.TechniqueEntity
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
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
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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
    private val secondTechniqueId = TechniqueId.of("KY-0002")
    private val thirdTechniqueId = TechniqueId.of("KY-0003")
    private val tsukiCategoryId = CategoryId.of("tsuki")
    private val geriCategoryId = CategoryId.of("geri")

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

    // --- FR-20 / FR-21: a menet teljes eredménye visszaolvasható ----------------

    @Test
    fun getSessionResult_returnsSessionAndAttemptsInAnsweredOrder_FR20() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 3, seed = 7L)
        val secondTechnique = TechniqueId.of("KY-0002")
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
        repository.recordAttempt(
            NewAttempt(
                sessionId = sessionId,
                questionId = QuestionId.of("q-2"),
                techniqueId = secondTechnique,
                questionType = QuestionType.HU_TO_JP,
                selectedTechniqueId = techniqueId,
                correct = false,
            ),
        )

        val result = repository.getSessionResult(sessionId)

        assertThat(result).isNotNull()
        assertThat(result!!.session.id).isEqualTo(sessionId)
        assertThat(result.session.requestedCount).isEqualTo(3)
        assertThat(result.session.isFinished).isFalse()
        assertThat(result.attempts.map { it.questionId.value })
            .containsExactly("q-1", "q-2")
            .inOrder()
        assertThat(result.attempts.last().correct).isFalse()
        assertThat(result.attempts.last().selectedTechniqueId).isEqualTo(techniqueId)
    }

    @Test
    fun getSessionResult_finishedSessionReportsIsFinishedTrue_FR20() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 1, seed = 1L)
        repository.finishSession(sessionId)

        val result = repository.getSessionResult(sessionId)

        assertThat(result!!.session.isFinished).isTrue()
        assertThat(result.attempts).isEmpty()
    }

    @Test
    fun getSessionResult_unknownSessionIdReturnsNull_NFR06() = repositoryTest {
        val result = repository.getSessionResult(SessionId.of("session-does-not-exist"))

        assertThat(result).isNull()
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

    // --- FR-23 / FR-27 / DR-09: összesített statisztika, aggregált SQL-ből ------

    @Test
    fun observeOverallProgress_noAttemptsYieldsNullRatioNotZero_FR27() = repositoryTest {
        val progress = repository.observeOverallProgress().first()

        assertThat(progress.score.answered).isEqualTo(0)
        assertThat(progress.score.correct).isEqualTo(0)
        assertThat(progress.score.ratio).isNull()
    }

    @Test
    fun observeOverallProgress_sumsAnsweredAndCorrectAcrossSessions_FR23() = repositoryTest {
        seedCategoriesAndTechniques()
        val firstSession = repository.startSession(requestedCount = 5, seed = 1L)
        val secondSession = repository.startSession(requestedCount = 5, seed = 2L)
        insertAttempt(firstSession, "q-1", techniqueId, QuestionType.JP_TO_HU, correct = true, fixedInstant)
        insertAttempt(firstSession, "q-2", secondTechniqueId, QuestionType.JP_TO_HU, correct = false, fixedInstant)
        insertAttempt(secondSession, "q-1", techniqueId, QuestionType.HU_TO_JP, correct = true, fixedInstant)

        val progress = repository.observeOverallProgress().first()

        assertThat(progress.score.answered).isEqualTo(3)
        assertThat(progress.score.correct).isEqualTo(2)
        assertThat(progress.score.ratio).isWithin(1e-9).of(2.0 / 3.0)
    }

    // --- FR-24 / DR-09: kategóriánkénti bontás, „nincs adat" a próbálkozásmentes --
    // --- kategórián --------------------------------------------------------------

    @Test
    fun observeCategoryProgress_includesEveryCategoryEvenWithoutAttempts_FR24() = repositoryTest {
        seedCategoriesAndTechniques()
        val sessionId = repository.startSession(requestedCount = 5, seed = 1L)
        // Csak a `tsuki` kategória technikáira érkezik próbálkozás – a `geri`
        // kategóriának (thirdTechniqueId) egyre sem.
        insertAttempt(sessionId, "q-1", techniqueId, QuestionType.JP_TO_HU, correct = true, fixedInstant)

        val categories = repository.observeCategoryProgress().first()

        assertThat(categories.map { it.categoryId }).containsExactly(tsukiCategoryId, geriCategoryId).inOrder()
        val geri = categories.first { it.categoryId == geriCategoryId }
        assertThat(geri.score.answered).isEqualTo(0)
        assertThat(geri.score.ratio).isNull()
    }

    @Test
    fun observeCategoryProgress_groupsAttemptsFromAllTechniquesInTheSameCategory_FR24() =
        repositoryTest {
            seedCategoriesAndTechniques()
            val sessionId = repository.startSession(requestedCount = 5, seed = 1L)
            // `techniqueId` és `secondTechniqueId` mindketten a `tsuki` kategóriában vannak.
            insertAttempt(sessionId, "q-1", techniqueId, QuestionType.JP_TO_HU, correct = true, fixedInstant)
            insertAttempt(sessionId, "q-2", secondTechniqueId, QuestionType.JP_TO_HU, correct = false, fixedInstant)

            val tsuki = repository.observeCategoryProgress().first().first { it.categoryId == tsukiCategoryId }

            assertThat(tsuki.score.answered).isEqualTo(2)
            assertThat(tsuki.score.correct).isEqualTo(1)
            assertThat(tsuki.score.ratio).isWithin(1e-9).of(0.5)
        }

    // --- FR-25: technikánkénti statisztika, kérdéstípus-bontással ----------------

    @Test
    fun observeTechniqueProgress_neverPracticedTechniqueHasNullLastPracticedAndEmptyBreakdown_FR25() =
        repositoryTest {
            val progress = repository.observeTechniqueProgress(techniqueId).first()

            assertThat(progress.techniqueId).isEqualTo(techniqueId)
            assertThat(progress.score.answered).isEqualTo(0)
            assertThat(progress.score.ratio).isNull()
            assertThat(progress.lastPracticedAt).isNull()
            assertThat(progress.byQuestionType).isEmpty()
        }

    @Test
    fun observeTechniqueProgress_tracksLastPracticedAtAndBreaksDownByQuestionType_FR25() =
        repositoryTest {
            val sessionId = repository.startSession(requestedCount = 5, seed = 1L)
            val earlier = fixedInstant.minusSeconds(60)
            insertAttempt(sessionId, "q-1", techniqueId, QuestionType.JP_TO_HU, correct = true, earlier)
            insertAttempt(sessionId, "q-2", techniqueId, QuestionType.HU_TO_JP, correct = false, fixedInstant)
            // Másik technika próbálkozása nem szivároghat bele.
            insertAttempt(sessionId, "q-3", secondTechniqueId, QuestionType.JP_TO_HU, correct = true, fixedInstant)

            val progress = repository.observeTechniqueProgress(techniqueId).first()

            assertThat(progress.score.answered).isEqualTo(2)
            assertThat(progress.score.correct).isEqualTo(1)
            assertThat(progress.lastPracticedAt).isEqualTo(fixedInstant)
            assertThat(progress.byQuestionType.keys).containsExactly(QuestionType.JP_TO_HU, QuestionType.HU_TO_JP)
            assertThat(progress.byQuestionType[QuestionType.JP_TO_HU]!!.correct).isEqualTo(1)
            assertThat(progress.byQuestionType[QuestionType.HU_TO_JP]!!.correct).isEqualTo(0)
        }

    // --- FR-26: „gyakorlást igénylő" lista: sikerarány, majd régiség, limit ------

    @Test
    fun observeNeedsPractice_excludesTechniquesWithoutAnyAttempt_FR26() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 5, seed = 1L)
        insertAttempt(sessionId, "q-1", techniqueId, QuestionType.JP_TO_HU, correct = true, fixedInstant)

        val needsPractice = repository.observeNeedsPractice(limit = 10).first()

        assertThat(needsPractice.map { it.techniqueId }).containsExactly(techniqueId)
    }

    @Test
    fun observeNeedsPractice_ordersByAscendingRatioThenByOldestLastPractice_FR26() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 5, seed = 1L)
        // `techniqueId`: 100% (1/1) – jó sikerarány, kerüljön hátra.
        insertAttempt(sessionId, "q-1", techniqueId, QuestionType.JP_TO_HU, correct = true, fixedInstant)
        // `secondTechniqueId`: 0% (0/1), régebben gyakorolva – a legrosszabb, elöl kell lennie.
        insertAttempt(
            sessionId,
            "q-2",
            secondTechniqueId,
            QuestionType.JP_TO_HU,
            correct = false,
            fixedInstant.minusSeconds(120),
        )
        // `thirdTechniqueId`: 0% (0/1), de frissebben gyakorolva – azonos arány esetén
        // a régebbi (`secondTechniqueId`) előzi meg (FUNCTIONAL_SPEC 6.1/4.).
        insertAttempt(sessionId, "q-3", thirdTechniqueId, QuestionType.JP_TO_HU, correct = false, fixedInstant)

        val needsPractice = repository.observeNeedsPractice(limit = 10).first()

        assertThat(needsPractice.map { it.techniqueId })
            .containsExactly(secondTechniqueId, thirdTechniqueId, techniqueId)
            .inOrder()
        assertThat(needsPractice.first().score.ratio).isWithin(1e-9).of(0.0)
    }

    @Test
    fun observeNeedsPractice_respectsLimit_FR26() = repositoryTest {
        val sessionId = repository.startSession(requestedCount = 5, seed = 1L)
        insertAttempt(sessionId, "q-1", techniqueId, QuestionType.JP_TO_HU, correct = false, fixedInstant)
        insertAttempt(sessionId, "q-2", secondTechniqueId, QuestionType.JP_TO_HU, correct = false, fixedInstant)
        insertAttempt(sessionId, "q-3", thirdTechniqueId, QuestionType.JP_TO_HU, correct = false, fixedInstant)

        val needsPractice = repository.observeNeedsPractice(limit = 2).first()

        assertThat(needsPractice).hasSize(2)
    }

    // --- FR-28 / T-506: önértékelési előzmény technikánként, legfrissebb elöl ---

    @Test
    fun observeStudyReviewHistory_mostRecentFirstAndOnlyForTheRequestedTechnique_FR28() =
        repositoryTest {
            repository.recordStudyReview(techniqueId, SelfRating.NEEDS_PRACTICE)
            repository.recordStudyReview(techniqueId, SelfRating.KNEW)
            repository.recordStudyReview(secondTechniqueId, SelfRating.KNEW)

            val history = repository.observeStudyReviewHistory(techniqueId).first()

            assertThat(history.map { it.selfRating })
                .containsExactly(SelfRating.KNEW, SelfRating.NEEDS_PRACTICE)
                .inOrder()
            assertThat(history.map { it.techniqueId }).containsExactly(techniqueId, techniqueId)
        }

    @Test
    fun observeStudyReviewHistory_noReviewsYieldsEmptyListNotNull_FR28() = repositoryTest {
        val history = repository.observeStudyReviewHistory(techniqueId).first()

        assertThat(history).isEmpty()
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

    /**
     * Két kategória, három technika – a [tsukiCategoryId] a [techniqueId]-t és a
     * [secondTechniqueId]-t tartalmazza, a [geriCategoryId] a [thirdTechniqueId]-t. A
     * T-501 kategóriabontás-tesztjeihez kell: a `tsuki` kategóriának **két** technikából
     * kell összegeznie (FR-24, "GROUP BY categoryId, próbálkozásszámmal").
     */
    private suspend fun seedCategoriesAndTechniques() {
        database.categoryDao().upsertAll(
            listOf(
                CategoryEntity(id = tsukiCategoryId, nameHu = "Ütések", sortOrder = 0),
                CategoryEntity(id = geriCategoryId, nameHu = "Rúgások", sortOrder = 1),
            ),
        )
        database.techniqueDao().upsertAll(
            listOf(
                technique(techniqueId, tsukiCategoryId),
                technique(secondTechniqueId, tsukiCategoryId),
                technique(thirdTechniqueId, geriCategoryId),
            ),
        )
    }

    private fun technique(id: TechniqueId, categoryId: CategoryId) = TechniqueEntity(
        id = id,
        categoryId = categoryId,
        romaji = id.value,
        romajiAscii = id.value,
        hungarian = id.value,
        status = VerificationStatus.AWAITING_REVIEW,
        quizExcluded = false,
        retired = false,
        datasetVersion = "2026.11.01",
        searchRomaji = id.value.lowercase(),
        searchHungarian = id.value.lowercase(),
    )

    /**
     * Egy próbálkozás **közvetlen** beszúrása a DAO-n, a [repository]`.recordAttempt`
     * megkerülésével – a T-501 tesztjeinek kézzel kell állítania az `answeredAt`
     * időpontot (a régiség szerinti rendezés bizonyításához, FR-26), ami a
     * `ProgressRepository.recordAttempt` szerződése szerint mindig az injektált
     * [clock]-ból jönne, nem a hívótól (NFR-07) – itt viszont a **teszt**, nem az
     * üzleti kód állítja be közvetlenül, ugyanazzal a mintával, mint a
     * [seedContent]/[seedCategoriesAndTechniques] a tartalomtáblákra.
     */
    private suspend fun insertAttempt(
        sessionId: SessionId,
        questionId: String,
        techniqueId: TechniqueId,
        questionType: QuestionType,
        correct: Boolean,
        answeredAt: Instant,
    ) {
        database.quizAttemptDao().insertIgnoringDuplicate(
            QuizAttemptEntity(
                sessionId = sessionId,
                questionId = QuestionId.of(questionId),
                techniqueId = techniqueId,
                questionType = questionType,
                selectedTechniqueId = techniqueId,
                correct = correct,
                answeredAt = answeredAt,
            ),
        )
    }
}
