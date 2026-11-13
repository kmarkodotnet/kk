package hu.kyokushin.trainer.feature.progress.overview

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.repository.CategoryProgress
import hu.kyokushin.trainer.core.data.repository.OverallProgress
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.feature.progress.CATEGORY_DACHI
import hu.kyokushin.trainer.feature.progress.CATEGORY_TSUKI
import hu.kyokushin.trainer.feature.progress.FakeProgressOverviewRepository
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
