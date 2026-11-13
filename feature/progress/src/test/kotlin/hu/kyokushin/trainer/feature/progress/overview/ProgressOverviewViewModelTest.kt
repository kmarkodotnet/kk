package hu.kyokushin.trainer.feature.progress.overview

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.repository.CategoryProgress
import hu.kyokushin.trainer.core.data.repository.OverallProgress
import hu.kyokushin.trainer.core.data.repository.StudyReviewSummary
import hu.kyokushin.trainer.core.data.repository.TechniqueProgress
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.feature.progress.CATEGORY_DACHI
import hu.kyokushin.trainer.feature.progress.CATEGORY_TSUKI
import hu.kyokushin.trainer.feature.progress.FakeProgressOverviewRepository
import hu.kyokushin.trainer.feature.progress.FakeProgressTechniqueRepository
import hu.kyokushin.trainer.feature.progress.progressImportedState
import java.time.Instant
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

/**
 * A Haladás áttekintő ViewModel viselkedése – emulátor nélkül (T-503, FR-23, FR-24,
 * FR-27, US-17).
 *
 * Nincs `SavedStateHandle` (a `progress/overview` route nem vesz át argumentumot), ezért
 * – ellentétben a `feature/quiz`/`feature/learn` ViewModel tesztjeivel – nincs szükség
 * Robolectricre (CONVENTIONS 3.): sima JUnittal futtatható, `kotlinx-coroutines-test`-tel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressOverviewViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun progressOverview_noAttempts_showsNullRatioOverallScore_FR27() = runTest {
        val viewModel = viewModel(
            progressRepository = FakeProgressOverviewRepository(
                overallProgress = OverallProgress(score = Score(0, 0, null)),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(content.overallScore.answered).isEqualTo(0)
        assertThat(content.overallScore.ratio).isNull()
        job.cancel()
    }

    @Test
    fun progressOverview_withAttempts_showsOverallScore_FR23() = runTest {
        val viewModel = viewModel(
            progressRepository = FakeProgressOverviewRepository(
                overallProgress = OverallProgress(score = Score(answered = 10, correct = 7, ratio = 0.7)),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(content.overallScore.answered).isEqualTo(10)
        assertThat(content.overallScore.correct).isEqualTo(7)
        assertThat(content.overallScore.ratio).isEqualTo(0.7)
        job.cancel()
    }

    /**
     * FR-24/FR-27: minden kategóriának van sora, a próbálkozás nélküli kategória
     * `ratio`-ja `null`, nem `0.0` – a nevet a `TechniqueRepository.observeCategories()`
     * adja, a pontszámot a `ProgressRepository.observeCategoryProgress()`.
     */
    @Test
    fun progressOverview_categoryBreakdown_pairsNameWithScoreByCategoryId_FR24_FR27() = runTest {
        val viewModel = viewModel(
            progressRepository = FakeProgressOverviewRepository(
                categoryProgress = listOf(
                    CategoryProgress(CATEGORY_DACHI.id, Score(answered = 4, correct = 3, ratio = 0.75)),
                    CategoryProgress(CATEGORY_TSUKI.id, Score(answered = 0, correct = 0, ratio = null)),
                ),
            ),
            techniqueRepository = FakeProgressTechniqueRepository(
                categories = listOf(CATEGORY_DACHI, CATEGORY_TSUKI),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(content.categories).hasSize(2)
        val dachi = content.categories.first { it.categoryId == CATEGORY_DACHI.id }
        assertThat(dachi.nameHu).isEqualTo("Állások")
        assertThat(dachi.score.ratio).isEqualTo(0.75)
        val tsuki = content.categories.first { it.categoryId == CATEGORY_TSUKI.id }
        assertThat(tsuki.nameHu).isEqualTo("Ütések")
        assertThat(tsuki.score.answered).isEqualTo(0)
        assertThat(tsuki.score.ratio).isNull()
        job.cancel()
    }

    /**
     * T-501 szerződése szerint minden kategóriára pontosan egy sor jár – de ha ez
     * mégsem teljesülne, a hiányzó kategória „nincs adat"-ot kap, nem kivételt vagy
     * hamis 0%-ot (védelmi háló, lásd a ViewModel KDoc-ja).
     */
    @Test
    fun progressOverview_categoryMissingFromProgressRows_defaultsToNullRatio() = runTest {
        val viewModel = viewModel(
            progressRepository = FakeProgressOverviewRepository(categoryProgress = emptyList()),
            techniqueRepository = FakeProgressTechniqueRepository(
                categories = listOf(CATEGORY_DACHI),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        val dachi = content.categories.single()
        assertThat(dachi.score.answered).isEqualTo(0)
        assertThat(dachi.score.ratio).isNull()
        job.cancel()
    }

    /**
     * FR-26/T-504: a „Gyakorlást igénylő" lista a technikák nevét/ábráját a
     * `TechniqueRepository.observeTechniques()`-ből oldja fel, azonosító szerint.
     */
    @Test
    fun progressOverview_needsPractice_resolvesNameAndIllustrationByTechniqueId_FR26() = runTest {
        val technique = Technique(
            id = TechniqueId.of("KY-0002"),
            categoryId = CATEGORY_DACHI.id,
            romaji = "mae geri",
            romajiAscii = "mae geri",
            hungarian = "egyenes előre rúgás",
            illustration = IllustrationRef.Placeholder,
            datasetVersion = "test-1",
        )
        val viewModel = viewModel(
            progressRepository = FakeProgressOverviewRepository(
                needsPractice = listOf(
                    TechniqueProgress(
                        techniqueId = technique.id,
                        score = Score(answered = 4, correct = 1, ratio = 0.25),
                        lastPracticedAt = Instant.parse("2026-11-01T08:00:00Z"),
                        byQuestionType = emptyMap(),
                    ),
                ),
            ),
            techniqueRepository = FakeProgressTechniqueRepository(techniques = listOf(technique)),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        val item = content.needsPractice.single()
        assertThat(item.techniqueId).isEqualTo(technique.id)
        assertThat(item.romaji).isEqualTo("mae geri")
        assertThat(item.hungarian).isEqualTo("egyenes előre rúgás")
        assertThat(item.score.ratio).isEqualTo(0.25)
        job.cancel()
    }

    /**
     * T-504 „Figyelj rá": a sosem gyakorolt technika nem „gyenge pont" – ha a
     * `ProgressRepository` szerződésesen üres listát ad (lásd `observeNeedsPractice`
     * KDoc-ja), a képernyő is üres listát kap, nem kivételt.
     */
    @Test
    fun progressOverview_needsPractice_emptyWhenNoAttemptsYet_FR26() = runTest {
        val viewModel = viewModel(progressRepository = FakeProgressOverviewRepository(needsPractice = emptyList()))
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(content.needsPractice).isEmpty()
        job.cancel()
    }

    /**
     * A visszavont (az aktív `observeTechniques()`-ben már nem szereplő) technika korábbi
     * próbálkozása kimarad a listából (lásd a ViewModel KDoc-ja) – nem jelenik meg hibásan,
     * üres névvel.
     */
    @Test
    fun progressOverview_needsPractice_excludesTechniqueMissingFromActiveTechniques_DR07() = runTest {
        val viewModel = viewModel(
            progressRepository = FakeProgressOverviewRepository(
                needsPractice = listOf(
                    TechniqueProgress(
                        techniqueId = TechniqueId.of("KY-0099"),
                        score = Score(answered = 2, correct = 0, ratio = 0.0),
                        lastPracticedAt = Instant.parse("2026-11-01T08:00:00Z"),
                        byQuestionType = emptyMap(),
                    ),
                ),
            ),
            techniqueRepository = FakeProgressTechniqueRepository(techniques = emptyList()),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(content.needsPractice).isEmpty()
        job.cancel()
    }

    /** FR-28/T-505: az önértékelés-összegzés a kvízstatisztikától függetlenül érkezik. */
    @Test
    fun progressOverview_studyReviewSummary_isIndependentFromQuizScore_FR28() = runTest {
        val viewModel = viewModel(
            progressRepository = FakeProgressOverviewRepository(
                studyReviewSummary = StudyReviewSummary(knownCount = 3, needsPracticeCount = 2),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(content.studyReviewSummary.knownCount).isEqualTo(3)
        assertThat(content.studyReviewSummary.needsPracticeCount).isEqualTo(2)
        job.cancel()
    }

    @Test
    fun progressOverview_datasetTrust_reflectsContentState() = runTest {
        val viewModel = viewModel(
            techniqueRepository = FakeProgressTechniqueRepository(
                contentState = progressImportedState(datasetTrust = DatasetTrust.REVIEWED),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(content.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
        job.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun viewModel(
        progressRepository: FakeProgressOverviewRepository = FakeProgressOverviewRepository(),
        techniqueRepository: FakeProgressTechniqueRepository = FakeProgressTechniqueRepository(),
    ): ProgressOverviewViewModel = ProgressOverviewViewModel(
        progressRepository = progressRepository,
        techniqueRepository = techniqueRepository,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: ProgressOverviewViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
}
