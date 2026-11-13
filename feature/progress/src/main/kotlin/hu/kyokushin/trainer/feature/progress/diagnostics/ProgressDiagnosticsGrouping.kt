package hu.kyokushin.trainer.feature.progress.diagnostics

import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport

/**
 * A validációs jelentés szűrése és kódonkénti csoportosítása (FR-32, T-508) –
 * `@Composable`-mentes, sima JUnittal tesztelhető döntési logika, ugyanaz a minta, mint a
 * `QuizOptionFeedbackResolver`/`ProgressNeedsPracticeAccessibility` (lásd
 * `docs/CONVENTIONS.md` 4. szakasz „Compose döntési logika tesztelése emulátor nélkül").
 *
 * A csoportok **kódnév szerint rendezve** érkeznek (`V001` < `V002` < … < `V010`), hogy a
 * képernyő sorrendje determinisztikus legyen, ne a jelentésben előforduló (rekordsorrend
 * szerinti) véletlenszerű sorrend.
 */
object ProgressDiagnosticsGrouping {

    fun buildGroups(report: ValidationReport, filter: SeverityFilter): List<ProgressDiagnosticsGroup> =
        report.issues
            .filter { matches(filter, it.severity) }
            .groupBy { it.code }
            .toSortedMap(compareBy { it.name })
            .map { (code, issues) -> ProgressDiagnosticsGroup(code = code, issues = issues) }

    private fun matches(filter: SeverityFilter, severity: Severity): Boolean = when (filter) {
        SeverityFilter.ALL -> true
        SeverityFilter.BLOCKING -> severity == Severity.BLOCKING
        SeverityFilter.WARNING -> severity == Severity.WARNING
    }
}
