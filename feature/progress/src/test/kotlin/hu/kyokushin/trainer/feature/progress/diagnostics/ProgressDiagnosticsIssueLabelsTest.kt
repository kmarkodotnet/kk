package hu.kyokushin.trainer.feature.progress.diagnostics

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationIssue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Az érintett rekord leírása és a súlyosság-/szűrőfeliratok (FR-32, T-508).
 *
 * A teszt a **tényleges** `strings.xml` fölött fut (Robolectric,
 * `isIncludeAndroidResources = true`) – ugyanaz a minta, mint a
 * `ProgressNeedsPracticeAccessibilityTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "hu")
class ProgressDiagnosticsIssueLabelsTest {
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<Context>().resources
    }

    @Test
    fun recordLabel_knownEntryId_includesIdAndOneBasedEntryPosition_FR32() {
        val issue = ValidationIssue(
            code = IssueCode.V005,
            severity = Severity.WARNING,
            entryId = "KY-0044",
            entryIndex = 11,
        )

        val label = ProgressDiagnosticsIssueLabels.recordLabel(resources, issue)

        assertThat(label).isEqualTo("Érintett rekord: KY-0044 (12. bejegyzés)")
    }

    /** V002: éppen az `id` mező hiányzik – „ismeretlen azonosító", nem kivétel vagy `null`. */
    @Test
    fun recordLabel_missingEntryId_describesUnknownIdWithOneBasedEntryPosition_FR32() {
        val issue = ValidationIssue(
            code = IssueCode.V002,
            severity = Severity.BLOCKING,
            entryId = null,
            entryIndex = 3,
        )

        val label = ProgressDiagnosticsIssueLabels.recordLabel(resources, issue)

        assertThat(label).isEqualTo("Érintett rekord: ismeretlen azonosító (4. bejegyzés)")
    }

    /** V001/IssueScope.DATASET: a teljes tudástárra vonatkozik, nincs „érintett rekord" sor. */
    @Test
    fun recordLabel_datasetScopedIssueWithoutEntryIndex_returnsNull_FR32() {
        val issue = ValidationIssue(code = IssueCode.V001, severity = Severity.BLOCKING, entryIndex = null)

        val label = ProgressDiagnosticsIssueLabels.recordLabel(resources, issue)

        assertThat(label).isNull()
    }

    @Test
    fun severityLabelRes_mapsBlockingAndWarningToDistinctResources() {
        val blockingRes = ProgressDiagnosticsIssueLabels.severityLabelRes(Severity.BLOCKING)
        val warningRes = ProgressDiagnosticsIssueLabels.severityLabelRes(Severity.WARNING)

        assertThat(blockingRes).isNotEqualTo(warningRes)
        assertThat(resources.getString(blockingRes)).isEqualTo("Blokkoló")
        assertThat(resources.getString(warningRes)).isEqualTo("Figyelmeztetés")
    }

    @Test
    fun filterLabelRes_mapsAllThreeFiltersToDistinctResources_FR32() {
        val resIds = SeverityFilter.entries.map { ProgressDiagnosticsIssueLabels.filterLabelRes(it) }

        assertThat(resIds.toSet()).hasSize(SeverityFilter.entries.size)
    }
}
