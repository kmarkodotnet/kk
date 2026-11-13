package hu.kyokushin.trainer.feature.progress.settings

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.repository.CategoryProgress
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.data.repository.OverallProgress
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.feature.progress.CATEGORY_DACHI
import hu.kyokushin.trainer.feature.progress.CATEGORY_TSUKI
import hu.kyokushin.trainer.feature.progress.FakeProgressSettingsRepository
import hu.kyokushin.trainer.feature.progress.FakeProgressTechniqueRepository
import hu.kyokushin.trainer.feature.progress.FakeResettableProgressRepository
import hu.kyokushin.trainer.feature.progress.overview.ProgressOverviewUiState
import hu.kyokushin.trainer.feature.progress.overview.ProgressOverviewViewModel
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

/**
 * A Beállítások ViewModel viselkedése – emulátor nélkül (T-507, T-509, FR-32, FR-35,
 * NFR-09, US-19).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] (a kétlépcsős megerősítés lépését
 * tárolja) Android `Bundle` fölött validálja a tárolt értékeket (`docs/CONVENTIONS.md`
 * 3. szakasz), ugyanaz a minta, mint a `ProgressTechniqueViewModelTest`-nél.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressSettingsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** FR-32/US-19: a betöltött tartalom séma-/tartalomverziója, állapota és számai. */
    @Test
    fun progressSettings_imported_showsSchemaContentVersionRecordAndCategoryCounts_FR32() = runTest {
        val viewModel = viewModel(
            techniqueRepository = FakeProgressTechniqueRepository(
                categories = listOf(CATEGORY_DACHI, CATEGORY_TSUKI),
                contentState = progressImportedState(activeTechniqueCount = 161, datasetTrust = DatasetTrust.REVIEWED),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressSettingsUiState.Content
        assertThat(content.schemaVersion).isEqualTo("1.0.0")
        assertThat(content.contentVersion).isEqualTo("test-1")
        assertThat(content.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
        assertThat(content.recordCount).isEqualTo(161)
        assertThat(content.categoryCount).isEqualTo(2)
        job.cancel()
    }

    /**
     * Az első indítás rendes állapota ([ContentState.NotImported]) – „nincs adat", nem
     * hibaállapot: a séma-/tartalomverzió `null`, a számok `0`.
     */
    @Test
    fun progressSettings_notImported_showsNullVersionsAndZeroCounts() = runTest {
        val viewModel = viewModel(
            techniqueRepository = FakeProgressTechniqueRepository(
                categories = emptyList(),
                contentState = ContentState.NotImported,
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressSettingsUiState.Content
        assertThat(content.schemaVersion).isNull()
        assertThat(content.contentVersion).isNull()
        assertThat(content.recordCount).isEqualTo(0)
        assertThat(content.categoryCount).isEqualTo(0)
        job.cancel()
    }

    /**
     * FR-35 „Kész ha" 1.: a törlés csak a **két** megerősítő lépés után indul – az első
     * lépés után (`FIRST_WARNING` → `FINAL_WARNING`) a repository még nem híva.
     */
    @Test
    fun progressSettings_clearProgress_requiresTwoConfirmationSteps_FR35() = runTest {
        val repository = FakeProgressSettingsRepository()
        val viewModel = viewModel(progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as ProgressSettingsUiState.Content).clearStep)
            .isEqualTo(ClearProgressStep.NONE)

        viewModel.onClearProgressClicked()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as ProgressSettingsUiState.Content).clearStep)
            .isEqualTo(ClearProgressStep.FIRST_WARNING)
        assertThat(repository.clearCallCount).isEqualTo(0)

        viewModel.onClearFirstWarningConfirmed()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as ProgressSettingsUiState.Content).clearStep)
            .isEqualTo(ClearProgressStep.FINAL_WARNING)
        assertThat(repository.clearCallCount).isEqualTo(0)

        viewModel.onClearConfirmed()
        advanceUntilIdle()
        assertThat(repository.clearCallCount).isEqualTo(1)
        assertThat((viewModel.uiState.value as ProgressSettingsUiState.Content).clearStep)
            .isEqualTo(ClearProgressStep.NONE)
        job.cancel()
    }

    /** NFR-09: „Mégsem" bármelyik lépésben visszazár, és sosem hív törlést. */
    @Test
    fun progressSettings_cancelAtEitherStep_neverCallsClearAllProgress_NFR09() = runTest {
        val repository = FakeProgressSettingsRepository()
        val viewModel = viewModel(progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onClearProgressClicked()
        viewModel.onClearCancelled()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as ProgressSettingsUiState.Content).clearStep)
            .isEqualTo(ClearProgressStep.NONE)

        viewModel.onClearProgressClicked()
        viewModel.onClearFirstWarningConfirmed()
        viewModel.onClearCancelled()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as ProgressSettingsUiState.Content).clearStep)
            .isEqualTo(ClearProgressStep.NONE)
        assertThat(repository.clearCallCount).isEqualTo(0)
        job.cancel()
    }

    /**
     * FR-35 „Kész ha" 2.: a törlés **nem** érinti a tudástárat – a tartalominfó
     * (séma-/tartalomverzió, rekord- és kategóriaszám) a megerősített törlés előtt és
     * után is ugyanaz, mert két egymástól független forrásból jön (lásd a ViewModel
     * KDoc-ja).
     */
    @Test
    fun progressSettings_clearProgress_leavesContentInfoUnchanged_FR35() = runTest {
        val viewModel = viewModel(
            progressRepository = FakeProgressSettingsRepository(),
            techniqueRepository = FakeProgressTechniqueRepository(
                categories = listOf(CATEGORY_DACHI, CATEGORY_TSUKI),
                contentState = progressImportedState(activeTechniqueCount = 42),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val before = viewModel.uiState.value as ProgressSettingsUiState.Content

        viewModel.onClearProgressClicked()
        viewModel.onClearFirstWarningConfirmed()
        viewModel.onClearConfirmed()
        advanceUntilIdle()

        val after = viewModel.uiState.value as ProgressSettingsUiState.Content
        assertThat(after.schemaVersion).isEqualTo(before.schemaVersion)
        assertThat(after.contentVersion).isEqualTo(before.contentVersion)
        assertThat(after.recordCount).isEqualTo(before.recordCount)
        assertThat(after.categoryCount).isEqualTo(before.categoryCount)
        job.cancel()
    }

    /**
     * T-509 „Teendők" 3.: a Beállításokon megerősített törlés az áttekintő
     * statisztikáját is nullázza – ugyanazt a [hu.kyokushin.trainer.core.data.repository.ProgressRepository]
     * példányt figyeli a `ProgressOverviewViewModel` és a `ProgressSettingsViewModel` is
     * (lásd [FakeResettableProgressRepository] KDoc-ja).
     */
    @Test
    fun progressSettings_clearProgress_zeroesOverviewStatistics_leavesContentInfoUnchanged() = runTest {
        val sharedProgressRepository = FakeResettableProgressRepository(
            overallProgress = OverallProgress(score = Score(answered = 10, correct = 7, ratio = 0.7)),
            categoryProgress = listOf(
                CategoryProgress(CATEGORY_DACHI.id, Score(answered = 4, correct = 3, ratio = 0.75)),
            ),
        )
        val techniqueRepository = FakeProgressTechniqueRepository(
            categories = listOf(CATEGORY_DACHI),
            contentState = progressImportedState(activeTechniqueCount = 20),
        )

        val overviewViewModel = ProgressOverviewViewModel(
            progressRepository = sharedProgressRepository,
            techniqueRepository = techniqueRepository,
        )
        val settingsViewModel = viewModel(
            progressRepository = sharedProgressRepository,
            techniqueRepository = techniqueRepository,
        )

        val overviewJob = launch(UnconfinedTestDispatcher(testScheduler)) { overviewViewModel.uiState.collect() }
        val settingsJob = collectState(settingsViewModel)
        advanceUntilIdle()

        val overviewBefore = overviewViewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(overviewBefore.overallScore.answered).isEqualTo(10)
        val settingsBefore = settingsViewModel.uiState.value as ProgressSettingsUiState.Content
        assertThat(settingsBefore.recordCount).isEqualTo(20)

        settingsViewModel.onClearProgressClicked()
        settingsViewModel.onClearFirstWarningConfirmed()
        settingsViewModel.onClearConfirmed()
        advanceUntilIdle()

        // A statisztika nullázódik – az áttekintő ugyanazt a repository-t figyeli.
        val overviewAfter = overviewViewModel.uiState.value as ProgressOverviewUiState.Content
        assertThat(overviewAfter.overallScore.answered).isEqualTo(0)
        assertThat(overviewAfter.overallScore.ratio).isNull()
        assertThat(overviewAfter.categories.first().score.ratio).isNull()

        // A tartalom (technikák/kategóriák száma) érintetlen marad.
        val settingsAfter = settingsViewModel.uiState.value as ProgressSettingsUiState.Content
        assertThat(settingsAfter.recordCount).isEqualTo(20)
        assertThat(settingsAfter.categoryCount).isEqualTo(1)

        overviewJob.cancel()
        settingsJob.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun viewModel(
        progressRepository: ProgressRepository = FakeProgressSettingsRepository(),
        techniqueRepository: FakeProgressTechniqueRepository = FakeProgressTechniqueRepository(),
    ): ProgressSettingsViewModel = ProgressSettingsViewModel(
        progressRepository = progressRepository,
        techniqueRepository = techniqueRepository,
        savedStateHandle = SavedStateHandle(),
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: ProgressSettingsViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
}
