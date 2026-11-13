package hu.kyokushin.trainer.feature.progress.diagnostics

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationIssue
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import hu.kyokushin.trainer.feature.progress.FakeContentImporter
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
 * A fejlesztői diagnosztika ViewModel viselkedése – emulátor nélkül (FR-32, T-508).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] (a súlyosságszűrőt tárolja) Android
 * `Bundle` fölött validálja a tárolt értékeket (`docs/CONVENTIONS.md` 3. szakasz),
 * ugyanaz a minta, mint a `ProgressSettingsViewModelTest`-nél.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressDiagnosticsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Ebben a processzben nem futott validáció ([hu.kyokushin.trainer.core.data.content.ImportResult.UpToDate]
     * miatt kimaradt az import) – „nincs jelentés", nem hibaállapot, nem üres lista.
     */
    @Test
    fun uiState_noReport_showsNoReportState_FR32() = runTest {
        val viewModel = viewModel(report = null)
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(ProgressDiagnosticsUiState.NoReport)
        job.cancel()
    }

    @Test
    fun uiState_report_exposesGroupsSortedByCode_defaultFilterIsAll_FR32() = runTest {
        val report = ValidationReport(
            issues = listOf(
                ValidationIssue(code = IssueCode.V005, severity = Severity.WARNING, entryIndex = 1),
                ValidationIssue(code = IssueCode.V002, severity = Severity.BLOCKING, entryIndex = 0),
            ),
        )
        val viewModel = viewModel(report = report)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressDiagnosticsUiState.Content
        assertThat(content.severityFilter).isEqualTo(SeverityFilter.ALL)
        assertThat(content.groups.map { it.code }).containsExactly(IssueCode.V002, IssueCode.V005).inOrder()
        job.cancel()
    }

    /** V001, `IssueScope.DATASET`, BLOKKOLÓ → a teljes tudástár elutasítva (`datasetRejected`). */
    @Test
    fun uiState_datasetScopedBlockingIssue_setsDatasetRejectedFlag_FR32() = runTest {
        val report = ValidationReport(issues = listOf(ValidationIssue(code = IssueCode.V001, severity = Severity.BLOCKING)))
        val viewModel = viewModel(report = report)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressDiagnosticsUiState.Content
        assertThat(content.datasetRejected).isTrue()
        job.cancel()
    }

    /** FR-32 „Szűrés súlyosságra": a kiválasztott szűrő forgatás alatt is megmarad. */
    @Test
    fun onSeverityFilterChanged_persistsInSavedStateHandle_andNarrowsTheGroups_FR32() = runTest {
        val report = ValidationReport(
            issues = listOf(
                ValidationIssue(code = IssueCode.V002, severity = Severity.BLOCKING, entryIndex = 0),
                ValidationIssue(code = IssueCode.V005, severity = Severity.WARNING, entryIndex = 1),
            ),
        )
        val savedStateHandle = SavedStateHandle()
        val viewModel = viewModel(report = report, savedStateHandle = savedStateHandle)
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onSeverityFilterChanged(SeverityFilter.WARNING)
        advanceUntilIdle()

        val content = viewModel.uiState.value as ProgressDiagnosticsUiState.Content
        assertThat(content.severityFilter).isEqualTo(SeverityFilter.WARNING)
        assertThat(content.groups.map { it.code }).containsExactly(IssueCode.V005)

        // Forgatás szimulációja: új ViewModel, UGYANAZZAL a SavedStateHandle-lel.
        val recreated = viewModel(report = report, savedStateHandle = savedStateHandle)
        val recreatedJob = collectState(recreated)
        advanceUntilIdle()
        assertThat((recreated.uiState.value as ProgressDiagnosticsUiState.Content).severityFilter)
            .isEqualTo(SeverityFilter.WARNING)

        job.cancel()
        recreatedJob.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun viewModel(
        report: ValidationReport?,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): ProgressDiagnosticsViewModel = ProgressDiagnosticsViewModel(
        contentImporter = FakeContentImporter(report = report),
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: ProgressDiagnosticsViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
}
