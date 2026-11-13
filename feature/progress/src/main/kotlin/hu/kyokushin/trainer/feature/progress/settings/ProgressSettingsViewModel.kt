package hu.kyokushin.trainer.feature.progress.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
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
 * A Beállítások képernyő (`settings`) állapotkezelése (FUNCTIONAL_SPEC 6.3, FR-32, FR-35,
 * NFR-09, US-19, T-507).
 *
 * ## Két, egymástól teljesen független felelősség
 *
 * 1. **Tartalominfó** – a [techniqueRepository]`.observeContentState()` és
 *    `.observeCategories()` olvasása, pontszerűen (nincs írás). Ugyanaz a
 *    [hu.kyokushin.trainer.core.data.repository.ContentState] típus, amit a
 *    `ProgressOverviewViewModel` és a `library/list` is használ (lásd `docs/CONVENTIONS.md`
 *    1. szakasz) – itt a séma-/tartalomverzió és a rekordszám kerül ki belőle, nem csak a
 *    bizalmi szint.
 * 2. **„Előrehaladás törlése"** (FR-35) – a [progressRepository]`.clearAllProgress()` az
 *    **egyetlen** írás ebben a ViewModelben, és **csak** a kétlépcsős megerősítés
 *    (`"Kész ha" 1.`) után hívható. A kétlépcsős állapotot ([ClearProgressStep]) a
 *    [savedStateHandle]-ben tartjuk, ugyanazzal a mintával, mint a `QuizRunViewModel`
 *    `closeConfirmationVisible` mezője (`docs/CONVENTIONS.md`, `KEY_CLOSE_CONFIRMATION`):
 *    forgatás alatt a nyitott párbeszéd nem veszik el.
 *
 * ## Miért nem hat egymásra a két felelősség (T-507 „Kész ha" 2.)
 *
 * A [ProgressRepository.clearAllProgress] dokumentáltan **csak** a felhasználói táblákat
 * (`quiz_session`, `quiz_attempt`, `study_review`) üríti – a tartalomtáblák és a
 * `content_state` érintetlenek maradnak (lásd a repository KDoc-ja). A [uiState] ezt a
 * szerződést a **forrás szintjén** tükrözi: a tartalominfó-mezők a
 * [TechniqueRepository]-ból jönnek, a törlés a [ProgressRepository]-t hívja – a kettő
 * soha nem ugyanaz a hívás, tehát a törlés után a tartalominfó a [uiState]-ben
 * garantáltan változatlan marad, nem csak "véletlenül" az.
 *
 * @property progressRepository a felhasználói adat törlésének forrása (FR-35).
 * @property techniqueRepository a tartalominfó (séma-/tartalomverzió, rekord- és
 *   kategóriaszám, bizalmi szint) forrása (FR-32).
 * @property savedStateHandle a kétlépcsős megerősítés jelenlegi lépését tartja.
 */
@HiltViewModel
class ProgressSettingsViewModel
    @Inject
    constructor(
        private val progressRepository: ProgressRepository,
        private val techniqueRepository: TechniqueRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val clearStepFlow: Flow<ClearProgressStep> =
            savedStateHandle.getStateFlow(KEY_CLEAR_STEP, ClearProgressStep.NONE.name)
                .map { name -> ClearProgressStep.valueOf(name) }

        private val isClearingFlow = MutableStateFlow(false)

        val uiState: StateFlow<ProgressSettingsUiState> = combine(
            techniqueRepository.observeContentState(),
            techniqueRepository.observeCategories(),
            clearStepFlow,
            isClearingFlow.asStateFlow(),
        ) { contentState, categories, clearStep, isClearing ->
            buildState(contentState, categories, clearStep, isClearing)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ProgressSettingsUiState.Loading,
            )

        /** A „Előrehaladás törlése" gomb – megnyitja az első megerősítő párbeszédet. */
        fun onClearProgressClicked() {
            setClearStep(ClearProgressStep.FIRST_WARNING)
        }

        /** Az első párbeszéd „Tovább" gombja – a végső megerősítésre lép. */
        fun onClearFirstWarningConfirmed() {
            setClearStep(ClearProgressStep.FINAL_WARNING)
        }

        /** Bármelyik lépésben a „Mégsem" – visszazár, nem töröl semmit. */
        fun onClearCancelled() {
            setClearStep(ClearProgressStep.NONE)
        }

        /**
         * A végső párbeszéd megerősítő gombja – ez az **egyetlen** hívási út, amely
         * tényleges törlést indít (FR-35). Kétszeri megnyomás ellen a [isClearingFlow] véd:
         * amíg az előző hívás fut, egy újabb nem indít második `clearAllProgress`-t.
         */
        fun onClearConfirmed() {
            if (isClearingFlow.value) return
            viewModelScope.launch {
                isClearingFlow.value = true
                try {
                    progressRepository.clearAllProgress()
                } finally {
                    isClearingFlow.value = false
                    setClearStep(ClearProgressStep.NONE)
                }
            }
        }

        private fun setClearStep(step: ClearProgressStep) {
            savedStateHandle[KEY_CLEAR_STEP] = step.name
        }

        private fun buildState(
            contentState: ContentState,
            categories: List<Category>,
            clearStep: ClearProgressStep,
            isClearing: Boolean,
        ): ProgressSettingsUiState.Content {
            val imported = contentState as? ContentState.Imported
            return ProgressSettingsUiState.Content(
                schemaVersion = imported?.schemaVersion,
                contentVersion = imported?.contentVersion,
                datasetTrust = contentState.datasetTrust,
                recordCount = contentState.activeTechniqueCount,
                categoryCount = categories.size,
                clearStep = clearStep,
                isClearing = isClearing,
            )
        }

        private companion object {
            /** Forgatás alatt is megmarad (lásd az osztály KDoc-ja). */
            const val KEY_CLEAR_STEP = "progress.settings.clearStep"

            /** A konfigurációváltás alatt életben tartja a feliratkozást. */
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
