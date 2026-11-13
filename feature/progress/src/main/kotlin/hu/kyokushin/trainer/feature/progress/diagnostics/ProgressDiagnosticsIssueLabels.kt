package hu.kyokushin.trainer.feature.progress.diagnostics

import android.content.res.Resources
import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationIssue
import hu.kyokushin.trainer.feature.progress.R

/**
 * Egy [ValidationIssue] „érintett rekord" leírásának és súlyosság-feliratának
 * összeállítása, `@Composable`-mentes formában (FR-32, T-508) – ugyanaz a minta, mint a
 * `ProgressNeedsPracticeAccessibility` (lásd `docs/CONVENTIONS.md` 4. szakasz): a tényleges
 * `strings.xml` fölött, emulátor nélkül (Robolectric) tesztelhető.
 */
object ProgressDiagnosticsIssueLabels {

    /**
     * Az érintett rekord leírása, vagy `null`, ha az észrevétel a **teljes** tudástárra
     * vonatkozik, nem egyetlen rekordra ([ValidationIssue.entryIndex] `null` –
     * [hu.kyokushin.trainer.core.quiz.validation.IssueScope.DATASET]).
     *
     * [ValidationIssue.entryId] külön is hiányozhat (ha éppen az `id` mező volt hibás,
     * V002) – ilyenkor az „ismeretlen azonosító" szöveg jár, az [ValidationIssue.entryIndex]
     * (1-alapúra emelve, hogy a fejlesztő a fájl sorszámára gondoljon) mellett.
     */
    fun recordLabel(resources: Resources, issue: ValidationIssue): String? {
        val entryIndex = issue.entryIndex ?: return null
        val position = entryIndex + 1
        return if (issue.entryId != null) {
            resources.getString(R.string.progress_diagnostics_record_known, issue.entryId, position)
        } else {
            resources.getString(R.string.progress_diagnostics_record_unknown_id, position)
        }
    }

    @StringRes
    fun severityLabelRes(severity: Severity): Int = when (severity) {
        Severity.BLOCKING -> R.string.progress_diagnostics_severity_blocking
        Severity.WARNING -> R.string.progress_diagnostics_severity_warning
    }

    @StringRes
    fun filterLabelRes(filter: SeverityFilter): Int = when (filter) {
        SeverityFilter.ALL -> R.string.progress_diagnostics_filter_all
        SeverityFilter.BLOCKING -> R.string.progress_diagnostics_filter_blocking
        SeverityFilter.WARNING -> R.string.progress_diagnostics_filter_warning
    }
}
