package hu.kyokushin.trainer.feature.progress.technique

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.repository.TechniqueProgress
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.StudyReview
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.feature.progress.CATEGORY_DACHI
import hu.kyokushin.trainer.feature.progress.FakeProgressTechniqueDetailRepository
import hu.kyokushin.trainer.feature.progress.FakeProgressTechniqueRepository
import hu.kyokushin.trainer.feature.progress.progressImportedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import hu.kyokushin.trainer.core.ui.R as CoreUiR

/**
 * A technika-részletező ViewModel viselkedése – emulátor nélkül (T-506, FR-25, FR-28,
 * DR-07, US-18).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket (CONVENTIONS 3.: `@Config(sdk = [34])`), ugyanaz a minta, mint a
 * `feature/library` `LibraryDetailViewModelTest`-jénél.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressTechniqueViewModelTest {
    private val activeTechnique = Technique(
        id = TechniqueId.of("KY-0002"),
        categoryId = CATEGORY_DACHI.id,
        romaji = "mae geri",
        romajiAscii = "mae geri",
        hungarian = "egyenes előre rúgás",
        datasetVersion = "test-1",
    )

    private val retiredTechnique = activeTechnique.copy(retired = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun progressTechnique_loadsIdentityAndScore_FR25() = runTest {
        val viewModel = viewModel(
            technique = activeTechnique,
            techniqueProgress = TechniqueProgress(
                techniqueId = activeTechnique.id,
                score = Score(answered = 10, correct = 7, ratio = 0.7),
                lastPracticedAt = Instant.parse("2026-11-10T08:00:00Z"),
                byQuestionType = mapOf(QuestionType.JP_TO_HU to Score(answered = 10, correct = 7, ratio = 0.7)),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressTechniqueUiState.Content
        assertThat(content.id).isEqualTo(activeTechnique.id)
        assertThat(content.romaji).isEqualTo("mae geri")
        assertThat(content.hungarian).isEqualTo("egyenes előre rúgás")
        assertThat(content.retired).isFalse()
        assertThat(content.score.answered).isEqualTo(10)
        assertThat(content.score.ratio).isEqualTo(0.7)
        assertThat(content.lastPracticedAt).isEqualTo(Instant.parse("2026-11-10T08:00:00Z"))
        assertThat(content.byQuestionType).containsKey(QuestionType.JP_TO_HU)
        job.cancel()
    }

    /** FR-27: nulla megválaszolt kérdés – „nincs adat" ágra kell futnia, nem hibára. */
    @Test
    fun progressTechnique_neverPracticed_scoreHasNullRatio_FR27() = runTest {
        val viewModel = viewModel(
            technique = activeTechnique,
            techniqueProgress = TechniqueProgress(
                techniqueId = activeTechnique.id,
                score = Score(answered = 0, correct = 0, ratio = null),
                lastPracticedAt = null,
                byQuestionType = emptyMap(),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressTechniqueUiState.Content
        assertThat(content.score.answered).isEqualTo(0)
        assertThat(content.score.ratio).isNull()
        assertThat(content.lastPracticedAt).isNull()
        job.cancel()
    }

    /** DR-07/T-506 „Kész ha": a visszavont technika statisztikája elérhető marad. */
    @Test
    fun progressTechnique_retiredTechnique_statisticsStayVisible_DR07() = runTest {
        val viewModel = viewModel(
            technique = retiredTechnique,
            techniqueProgress = TechniqueProgress(
                techniqueId = retiredTechnique.id,
                score = Score(answered = 3, correct = 2, ratio = 2.0 / 3.0),
                lastPracticedAt = Instant.parse("2026-09-01T08:00:00Z"),
                byQuestionType = emptyMap(),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressTechniqueUiState.Content
        assertThat(content.retired).isTrue()
        assertThat(content.score.answered).isEqualTo(3)
        job.cancel()
    }

    /** FR-28: az önértékelési előzmény a kvízstatisztikától függetlenül érkezik. */
    @Test
    fun progressTechnique_selfRatingHistory_isIndependentFromQuizScore_FR28() = runTest {
        val history = listOf(
            StudyReview(
                id = 2,
                techniqueId = activeTechnique.id,
                selfRating = SelfRating.KNEW,
                reviewedAt = Instant.parse("2026-11-09T08:00:00Z"),
            ),
            StudyReview(
                id = 1,
                techniqueId = activeTechnique.id,
                selfRating = SelfRating.NEEDS_PRACTICE,
                reviewedAt = Instant.parse("2026-10-20T08:00:00Z"),
            ),
        )
        val viewModel = viewModel(
            technique = activeTechnique,
            techniqueProgress = TechniqueProgress(
                techniqueId = activeTechnique.id,
                score = Score(answered = 0, correct = 0, ratio = null),
                lastPracticedAt = null,
                byQuestionType = emptyMap(),
            ),
            studyReviewHistory = history,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressTechniqueUiState.Content
        assertThat(content.selfRatingHistory).hasSize(2)
        assertThat(content.selfRatingHistory.first().selfRating).isEqualTo(SelfRating.KNEW)
        // A nulla próbálkozású kvízstatisztika érintetlen marad az önértékeléstől.
        assertThat(content.score.answered).isEqualTo(0)
        job.cancel()
    }

    @Test
    fun progressTechnique_unknownTechniqueId_showsErrorInsteadOfCrashing_NFR06() = runTest {
        val viewModel = viewModel(
            techniqueId = TechniqueId.of("KY-DOES-NOT-EXIST"),
            technique = null,
            techniqueProgress = TechniqueProgress(
                techniqueId = TechniqueId.of("KY-DOES-NOT-EXIST"),
                score = Score(answered = 0, correct = 0, ratio = null),
                lastPracticedAt = null,
                byQuestionType = emptyMap(),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val error = viewModel.uiState.value as ProgressTechniqueUiState.Error
        assertThat(error.messageRes).isEqualTo(
            hu.kyokushin.trainer.feature.progress.R.string.progress_technique_not_found,
        )
        job.cancel()
    }

    @Test
    fun progressTechnique_repositoryThrows_showsErrorAndRetryRecovers() = runTest {
        var fail = true
        val delegate = FakeProgressTechniqueRepository(techniqueById = mapOf(activeTechnique.id to activeTechnique))
        val repository = FailingGetTechniqueRepository(delegate = delegate, shouldFailProvider = { fail })
        val viewModel = ProgressTechniqueViewModel(
            progressRepository = FakeProgressTechniqueDetailRepository(
                techniqueProgress = TechniqueProgress(
                    techniqueId = activeTechnique.id,
                    score = Score(answered = 0, correct = 0, ratio = null),
                    lastPracticedAt = null,
                    byQuestionType = emptyMap(),
                ),
            ),
            techniqueRepository = repository,
            savedStateHandle = SavedStateHandle(mapOf(KEY_TECHNIQUE_ID to activeTechnique.id.value)),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val error = viewModel.uiState.value as ProgressTechniqueUiState.Error
        // `@StringRes`, nem a kivétel angol üzenete (NFR-02).
        assertThat(error.messageRes).isEqualTo(CoreUiR.string.error_load_failed)

        fail = false
        viewModel.onRetry()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(ProgressTechniqueUiState.Content::class.java)
        job.cancel()
    }

    @Test
    fun progressTechnique_exposesDatasetTrustForUnreviewedBanner() = runTest {
        val viewModel = viewModel(
            technique = activeTechnique,
            techniqueProgress = TechniqueProgress(
                techniqueId = activeTechnique.id,
                score = Score(answered = 0, correct = 0, ratio = null),
                lastPracticedAt = null,
                byQuestionType = emptyMap(),
            ),
            contentDatasetTrust = DatasetTrust.REVIEWED,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
        job.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun TestScope.viewModel(
        technique: Technique?,
        techniqueProgress: TechniqueProgress,
        studyReviewHistory: List<StudyReview> = emptyList(),
        techniqueId: TechniqueId = technique?.id ?: techniqueProgress.techniqueId,
        contentDatasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    ): ProgressTechniqueViewModel = ProgressTechniqueViewModel(
        progressRepository = FakeProgressTechniqueDetailRepository(
            techniqueProgress = techniqueProgress,
            studyReviewHistory = studyReviewHistory,
        ),
        techniqueRepository = FakeProgressTechniqueRepository(
            techniqueById = technique?.let { mapOf(it.id to it) } ?: emptyMap(),
            contentState = progressImportedState(datasetTrust = contentDatasetTrust),
        ),
        savedStateHandle = SavedStateHandle(mapOf(KEY_TECHNIQUE_ID to techniqueId.value)),
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: ProgressTechniqueViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    private companion object {
        /** Meg kell egyeznie a `navigation` modul `NavArgs.TECHNIQUE_ID` értékével. */
        const val KEY_TECHNIQUE_ID = "techniqueId"
    }
}

/**
 * [TechniqueRepository] dublőr, amelynek [getTechnique] a hívó kérésére kivételt dob –
 * ugyanaz a minta, mint a `feature/library` `FailingGetTechniqueRepository`-ja.
 */
private class FailingGetTechniqueRepository(
    private val delegate: TechniqueRepository,
    private val shouldFailProvider: () -> Boolean,
) : TechniqueRepository by delegate {
    override suspend fun getTechnique(id: TechniqueId): Technique? {
        if (shouldFailProvider()) throw IllegalStateException("getTechnique failed (test)")
        return delegate.getTechnique(id)
    }
}
