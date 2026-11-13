package hu.kyokushin.trainer.feature.progress.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationIssue
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.progress.R

/**
 * A fejlesztői diagnosztika route-szintű bekötése (FUNCTIONAL_SPEC 6.3, FR-32, T-508).
 *
 * Ugyanaz a minta, mint a többi `feature/progress` route-nál: ez az egyetlen composable,
 * amely [ProgressDiagnosticsViewModel]-t lát (L5). A `settings` képernyő (T-507) debug
 * buildben ide navigál (`onOpenDiagnostics`) – a tényleges navigációs bekötés a
 * `navigation` modul tárgya, ugyanazzal a nyitott mintával, mint a `quiz/setup`/
 * `quiz/run`/`settings` útvonalaknál (lásd `docs/CONVENTIONS.md` 5. szakasz).
 */
@Composable
fun ProgressDiagnosticsRoute(
    modifier: Modifier = Modifier,
    viewModel: ProgressDiagnosticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProgressDiagnosticsScreen(
        uiState = uiState,
        onSeverityFilterChanged = viewModel::onSeverityFilterChanged,
        modifier = modifier,
    )
}

/** A fejlesztői diagnosztika képernyő – stateless (FUNCTIONAL_SPEC 6.3). */
@Composable
fun ProgressDiagnosticsScreen(
    uiState: ProgressDiagnosticsUiState,
    onSeverityFilterChanged: (SeverityFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            ProgressDiagnosticsUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            ProgressDiagnosticsUiState.NoReport -> ProgressDiagnosticsNoReport(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )

            is ProgressDiagnosticsUiState.Content -> ProgressDiagnosticsContent(
                state = uiState,
                onSeverityFilterChanged = onSeverityFilterChanged,
            )
        }
    }
}

@Composable
private fun ProgressDiagnosticsNoReport(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.progress_diagnostics_no_report_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.progress_diagnostics_no_report_message),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ProgressDiagnosticsContent(
    state: ProgressDiagnosticsUiState.Content,
    onSeverityFilterChanged: (SeverityFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.progress_diagnostics_summary, state.acceptedCount, state.rejectedCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.datasetRejected) {
                Text(
                    text = stringResource(R.string.progress_diagnostics_dataset_rejected_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        ProgressDiagnosticsFilterRow(
            selected = state.severityFilter,
            onSeverityFilterChanged = onSeverityFilterChanged,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.groups.isEmpty()) {
                Text(
                    text = stringResource(R.string.progress_diagnostics_empty_after_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.groups, key = { it.code.name }) { group ->
                        ProgressDiagnosticsGroupCard(group = group)
                    }
                }
            }
        }
    }
}

/** A súlyosságszűrő chip-sora (FR-32 „Szűrés súlyosságra"). */
@Composable
private fun ProgressDiagnosticsFilterRow(
    selected: SeverityFilter,
    onSeverityFilterChanged: (SeverityFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SeverityFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSeverityFilterChanged(filter) },
                label = { Text(text = stringResource(ProgressDiagnosticsIssueLabels.filterLabelRes(filter))) },
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
            )
        }
    }
}

/** Egy [IssueCode] csoport kártyája: cím + az összes érintett rekord felsorolva. */
@Composable
private fun ProgressDiagnosticsGroupCard(group: ProgressDiagnosticsGroup, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.progress_diagnostics_group_title, group.code.name, group.issues.size),
                style = MaterialTheme.typography.titleMedium,
            )
            group.issues.forEach { issue ->
                ProgressDiagnosticsIssueRow(issue = issue)
            }
        }
    }
}

@Composable
private fun ProgressDiagnosticsIssueRow(issue: ValidationIssue, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(ProgressDiagnosticsIssueLabels.severityLabelRes(issue.severity)),
            style = MaterialTheme.typography.labelMedium,
        )
        ProgressDiagnosticsIssueLabels.recordLabel(resources, issue)?.let { recordLabel ->
            Text(text = recordLabel, style = MaterialTheme.typography.bodySmall)
        }
        if (issue.detail.isNotBlank()) {
            // A `detail` már magyar, fejlesztői diagnosztikaszöveg (lásd `ValidationIssue`
            // KDoc-ja) – szándékosan NEM `strings.xml`-ből jön, mert nem végfelhasználói
            // UI-felirat, hanem a validátor saját, dokumentáltan Hungarian kimenete.
            Text(text = issue.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A gombok/chipek minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában; betöltés, nincs jelentés, tartalom (szűréssel) és
// a teljes tudástár-elutasítás állapota.
// ---------------------------------------------------------------------------------------

private val PREVIEW_ISSUES = listOf(
    ValidationIssue(
        code = IssueCode.V002,
        severity = Severity.BLOCKING,
        entryId = null,
        entryIndex = 3,
        field = "hungarian",
        detail = "Kötelező mező hiányzik vagy üres: magyar jelentés.",
    ),
    ValidationIssue(
        code = IssueCode.V005,
        severity = Severity.BLOCKING,
        entryId = "KY-0044",
        entryIndex = 11,
        field = "illustration",
        detail = "Ismeretlen képhivatkozás: „KY-0044.jpg\" nem szerepel a manifest nyilvántartásában.",
    ),
    ValidationIssue(
        code = IssueCode.V005,
        severity = Severity.WARNING,
        entryId = "KY-0012",
        entryIndex = 5,
        field = "illustration",
        detail = "A képfájl nincs a csomagban; a rekord helyőrző képet kap.",
    ),
    ValidationIssue(
        code = IssueCode.V006,
        severity = Severity.WARNING,
        entryId = "KY-0020",
        entryIndex = 7,
        field = "aliases",
        detail = "Normalizálva azonos névvel, kölcsönös distraktor-kizárási csoportba került.",
    ),
)

private val PREVIEW_CONTENT = ProgressDiagnosticsUiState.Content(
    severityFilter = SeverityFilter.ALL,
    datasetRejected = false,
    acceptedCount = 158,
    rejectedCount = 3,
    groups = ProgressDiagnosticsGrouping.buildGroups(
        ValidationReport(issues = PREVIEW_ISSUES),
        SeverityFilter.ALL,
    ),
)

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(name = "Betöltés – sötét", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProgressDiagnosticsScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressDiagnosticsScreen(uiState = ProgressDiagnosticsUiState.Loading, onSeverityFilterChanged = {})
    }
}

@Preview(name = "Nincs jelentés – világos", showBackground = true)
@Preview(
    name = "Nincs jelentés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressDiagnosticsScreenNoReportPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressDiagnosticsScreen(uiState = ProgressDiagnosticsUiState.NoReport, onSeverityFilterChanged = {})
    }
}

@Preview(name = "Jelentés – világos", showBackground = true)
@Preview(name = "Jelentés – sötét", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProgressDiagnosticsScreenContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressDiagnosticsScreen(uiState = PREVIEW_CONTENT, onSeverityFilterChanged = {})
    }
}

@Preview(name = "Elutasított tudástár – világos", showBackground = true)
@Preview(
    name = "Elutasított tudástár – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressDiagnosticsScreenDatasetRejectedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressDiagnosticsScreen(
            uiState = PREVIEW_CONTENT.copy(datasetRejected = true, acceptedCount = 0, rejectedCount = 0),
            onSeverityFilterChanged = {},
        )
    }
}
