package hu.kyokushin.trainer.feature.progress.diagnostics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A fejlesztői diagnosztika képernyő állapotkezelése (FUNCTIONAL_SPEC 6.3, FR-32, T-508).
 *
 * ## Egyetlen, pontszerű olvasás, nem egy megfigyelhető `Flow`
 *
 * A [ContentImporter.lastValidationReport] `suspend fun`, nem `Flow` – a jelentés az
 * import egyetlen futásának **pillanatfelvétele**, nem egy folyamatosan változó adat
 * (lásd a szerződés KDoc-ja: „a részletes észrevételek szándékosan nem perzisztálódnak").
 * A [reportLoadState] ezért egy [MutableStateFlow], amit az [init] blokk egyszer tölt fel
 * – ugyanaz a mintázat, mint a `ProgressSettingsViewModel.isClearingFlow`, csak itt a
 * kezdő `Loading` állapotot egy valódi, egyszeri `suspend` hívás zárja le, nem egy
 * felhasználói akció.
 *
 * ## A súlyosságszűrő `SavedStateHandle`-ben él
 *
 * Forgatás alatt is megmarad, ugyanazzal a mintával, mint a `ProgressSettingsViewModel`
 * `clearStep`-je (`docs/CONVENTIONS.md` 4.1 szakasz).
 *
 * @property contentImporter az **ebben a processzben** lefutott validáció forrása.
 * @property savedStateHandle a kiválasztott súlyosságszűrőt őrzi meg.
 */
@HiltViewModel
class ProgressDiagnosticsViewModel
    @Inject
    constructor(
        private val contentImporter: ContentImporter,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val reportLoadState = MutableStateFlow<ReportLoadState>(ReportLoadState.Loading)

        init {
            viewModelScope.launch {
                reportLoadState.value = ReportLoadState.Loaded(contentImporter.lastValidationReport())
            }
        }

        private val severityFilterFlow: Flow<SeverityFilter> =
            savedStateHandle.getStateFlow(KEY_SEVERITY_FILTER, SeverityFilter.ALL.name)
                .map { name -> runCatching { SeverityFilter.valueOf(name) }.getOrDefault(SeverityFilter.ALL) }

        val uiState: StateFlow<ProgressDiagnosticsUiState> = combine(
            reportLoadState.asStateFlow(),
            severityFilterFlow,
        ) { loadState, filter ->
            buildState(loadState, filter)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ProgressDiagnosticsUiState.Loading,
            )

        /** A súlyosságszűrő chip-sora hívja (FR-32 „Szűrés súlyosságra"). */
        fun onSeverityFilterChanged(filter: SeverityFilter) {
            savedStateHandle[KEY_SEVERITY_FILTER] = filter.name
        }

        private fun buildState(loadState: ReportLoadState, filter: SeverityFilter): ProgressDiagnosticsUiState =
            when (loadState) {
                ReportLoadState.Loading -> ProgressDiagnosticsUiState.Loading
                is ReportLoadState.Loaded -> loadState.report.toUiState(filter)
            }

        private fun ValidationReport?.toUiState(filter: SeverityFilter): ProgressDiagnosticsUiState {
            val report = this ?: return ProgressDiagnosticsUiState.NoReport
            return ProgressDiagnosticsUiState.Content(
                severityFilter = filter,
                datasetRejected = report.datasetRejected,
                acceptedCount = report.accepted.size,
                rejectedCount = report.rejected.size,
                groups = ProgressDiagnosticsGrouping.buildGroups(report, filter),
            )
        }

        /** A pontszerű [ContentImporter.lastValidationReport] hívás állapota ebben a VM-ben. */
        private sealed interface ReportLoadState {
            data object Loading : ReportLoadState

            data class Loaded(val report: ValidationReport?) : ReportLoadState
        }

        private companion object {
            /** Forgatás alatt is megmarad (lásd az osztály KDoc-ja). */
            const val KEY_SEVERITY_FILTER = "progress.diagnostics.severityFilter"

            /** A konfigurációváltás alatt életben tartja a feliratkozást. */
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
