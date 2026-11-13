package hu.kyokushin.trainer.feature.progress.diagnostics

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationIssue
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import org.junit.Test

/**
 * A jelentés súlyosság szerinti szűrése és kódonkénti csoportosítása (FR-32,
 * „kódonkénti bontás", „Szűrés súlyosságra", T-508) – `@Composable`-mentes, sima
 * JUnittal bizonyítható döntési logika, ugyanaz a minta, mint a
 * `QuizOptionFeedbackResolver`/`ProgressNeedsPracticeAccessibility` (`docs/CONVENTIONS.md`
 * 4. szakasz).
 */
class ProgressDiagnosticsGroupingTest {
    private val v002 = ValidationIssue(code = IssueCode.V002, severity = Severity.BLOCKING, entryIndex = 1)
    private val v005Blocking = ValidationIssue(code = IssueCode.V005, severity = Severity.BLOCKING, entryIndex = 2)
    private val v005Warning = ValidationIssue(code = IssueCode.V005, severity = Severity.WARNING, entryIndex = 3)
    private val v006 = ValidationIssue(code = IssueCode.V006, severity = Severity.WARNING, entryIndex = 4)

    @Test
    fun buildGroups_all_groupsByCodeSortedByNameRegardlessOfReportOrder_FR32() {
        val report = ValidationReport(issues = listOf(v006, v002, v005Blocking, v005Warning))

        val groups = ProgressDiagnosticsGrouping.buildGroups(report, SeverityFilter.ALL)

        assertThat(groups.map { it.code }).containsExactly(IssueCode.V002, IssueCode.V005, IssueCode.V006).inOrder()
        assertThat(groups.first { it.code == IssueCode.V005 }.issues).containsExactly(v005Blocking, v005Warning)
    }

    /**
     * A V005 két észrevétele eltérő, ESETI súlyosságú (lásd [ValidationIssue.severity]
     * KDoc-ja) – a szűrés az ESETI súlyosságot nézi, nem a kód alapértelmezését.
     */
    @Test
    fun buildGroups_blockingFilter_keepsOnlyActuallyBlockingIssuesEvenWithinTheSameCode_FR32() {
        val report = ValidationReport(issues = listOf(v002, v005Blocking, v005Warning, v006))

        val groups = ProgressDiagnosticsGrouping.buildGroups(report, SeverityFilter.BLOCKING)

        assertThat(groups.flatMap { it.issues }).containsExactly(v002, v005Blocking)
    }

    @Test
    fun buildGroups_warningFilter_keepsOnlyActuallyWarningIssues_FR32() {
        val report = ValidationReport(issues = listOf(v005Blocking, v005Warning))

        val groups = ProgressDiagnosticsGrouping.buildGroups(report, SeverityFilter.WARNING)

        assertThat(groups).hasSize(1)
        assertThat(groups.single().issues).containsExactly(v005Warning)
    }

    @Test
    fun buildGroups_noIssueMatchesTheFilter_returnsEmptyList_FR32() {
        val report = ValidationReport(issues = listOf(v006))

        val groups = ProgressDiagnosticsGrouping.buildGroups(report, SeverityFilter.BLOCKING)

        assertThat(groups).isEmpty()
    }
}
