package hu.kyokushin.trainer.feature.progress.technique

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.TechniqueProgress
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.StudyReview
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.localizedName
import hu.kyokushin.trainer.core.ui.locale.currentLanguageTag
import hu.kyokushin.trainer.feature.progress.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import hu.kyokushin.trainer.core.ui.R as CoreUiR

/**
 * A technika-részletező (`progress/technique/{techniqueId}`) állapotkezelése
 * (FUNCTIONAL_SPEC 6.2, FR-25, FR-28, DR-07, US-18, T-506).
 *
 * ## A `techniqueId` forrása, és miért `suspend` a technika-lekérdezés
 *
 * Ugyanaz a két minta, mint a `feature/library` `LibraryDetailViewModel`-nél
 * (`docs/CONVENTIONS.md` 5. szakasz): a [SavedStateHandle] a Navigation Compose route
 * argumentumaiból előre feltöltve érkezik, a `navigation` modulra hivatkozás nélkül
 * (ADR-001 L5) – a [KEY_TECHNIQUE_ID] csak **névben** egyezik a `navigation` modul
 * `NavArgs.TECHNIQUE_ID` értékével. A [TechniqueRepository.getTechnique] pontszerű
 * `suspend` lekérdezés, nem `Flow` – a részletező csak egy korábbi listáról nyitható meg,
 * ahol a tartalom már betöltött állapotban van.
 *
 * ## `getTechnique` szándékosan visszaadja a visszavont technikát is (DR-07)
 *
 * A [TechniqueRepository] KDoc-ja szerint ez a metódus az **egyetlen** hely, ahol a
 * `retired` sor is visszajön – pontosan ennek a képernyőnek a kedvéért: egy visszavont
 * technikára korábban kvízeredmény vagy önértékelés született, és ennek továbbra is meg
 * kell jelennie, a visszavonás jelzése mellett (FUNCTIONAL_SPEC 6.2 utolsó pontja). A
 * [TechniqueRepository.getTechnique]`== null` ezzel szemben azt jelenti, hogy a tudástár
 * **soha** nem is tartalmazta ezt az azonosítót – ez a [ProgressTechniqueUiState.Error]
 * `NotFound` ága, nem a [ProgressTechniqueUiState.Content.retired] zászló.
 *
 * ## A kvízstatisztika és az önértékelési előzmény két külön forrás (FR-28)
 *
 * A [ProgressRepository.observeTechniqueProgress] és a
 * [ProgressRepository.observeStudyReviewHistory] egymástól teljesen független `Flow` –
 * a [ProgressTechniqueUiState.Content] ezért két külön mezőt ad ki, nem egy közös típust
 * (ugyanaz a döntés, mint az áttekintő képernyőn, T-505).
 *
 * @property progressRepository a technikánkénti kvízstatisztika és az önértékelési
 *   előzmény forrása.
 * @property techniqueRepository a technika azonosító adatainak és a lektorálatlanság-
 *   szalag (ADR-010) bemenete.
 * @property savedStateHandle a navigációs argumentumot hordozza (`techniqueId`).
 */
@HiltViewModel
class ProgressTechniqueViewModel
    @Inject
    constructor(
        private val progressRepository: ProgressRepository,
        private val techniqueRepository: TechniqueRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val techniqueId: TechniqueId = TechniqueId.of(
            requireNotNull(savedStateHandle.get<String>(KEY_TECHNIQUE_ID)) {
                "ProgressTechniqueViewModel: hiányzó '$KEY_TECHNIQUE_ID' navigációs argumentum."
            },
        )

        /** A legutóbbi [TechniqueRepository.getTechnique] lekérdezés kimenete. */
        private val loadResult = MutableStateFlow<LoadResult>(LoadResult.Loading)

        val uiState: StateFlow<ProgressTechniqueUiState> = combine(
            loadResult,
            progressRepository.observeTechniqueProgress(techniqueId),
            progressRepository.observeStudyReviewHistory(techniqueId),
            techniqueRepository.observeContentState(),
        ) { result, techniqueProgress, selfRatingHistory, contentState ->
            buildState(result, techniqueProgress, selfRatingHistory, contentState.datasetTrust)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ProgressTechniqueUiState.Loading(),
            )

        init {
            load()
        }

        /** „Újra" az `Error` állapotból. */
        fun onRetry() {
            load()
        }

        private fun load() {
            loadResult.value = LoadResult.Loading
            viewModelScope.launch {
                loadResult.value = try {
                    val technique = techniqueRepository.getTechnique(techniqueId)
                    if (technique == null) LoadResult.NotFound else LoadResult.Found(technique)
                } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                    LoadResult.Failed
                }
            }
        }

        private fun buildState(
            result: LoadResult,
            techniqueProgress: TechniqueProgress,
            selfRatingHistory: List<StudyReview>,
            datasetTrust: DatasetTrust,
        ): ProgressTechniqueUiState = when (result) {
            LoadResult.Loading -> ProgressTechniqueUiState.Loading(datasetTrust = datasetTrust)

            LoadResult.Failed -> ProgressTechniqueUiState.Error(
                messageRes = CoreUiR.string.error_load_failed,
                datasetTrust = datasetTrust,
            )

            // A technikaazonosító egy korábbi listáról (áttekintő vagy Tudástár) érkezett,
            // tehát a gyakorlatban mindig létezik; ez a dokumentált kivétel (lásd az osztály
            // KDoc-ja): nem omlik össze, hanem a megszokott hibaállapotot mutatja.
            LoadResult.NotFound -> ProgressTechniqueUiState.Error(
                messageRes = R.string.progress_technique_not_found,
                datasetTrust = datasetTrust,
            )

            is LoadResult.Found -> result.technique.toContentState(techniqueProgress, selfRatingHistory, datasetTrust)
        }

        private fun Technique.toContentState(
            techniqueProgress: TechniqueProgress,
            selfRatingHistory: List<StudyReview>,
            datasetTrust: DatasetTrust,
        ): ProgressTechniqueUiState.Content = ProgressTechniqueUiState.Content(
            id = id,
            romaji = romaji,
            hungarian = localizedName(currentLanguageTag()),
            illustration = illustration,
            retired = retired,
            score = techniqueProgress.score,
            lastPracticedAt = techniqueProgress.lastPracticedAt,
            byQuestionType = techniqueProgress.byQuestionType,
            selfRatingHistory = selfRatingHistory,
            datasetTrust = datasetTrust,
        )

        /** A legutóbbi [TechniqueRepository.getTechnique] lekérdezés kimenete. */
        private sealed interface LoadResult {
            data object Loading : LoadResult

            data object Failed : LoadResult

            data object NotFound : LoadResult

            data class Found(val technique: Technique) : LoadResult
        }

        private companion object {
            /**
             * Meg kell egyeznie a `navigation` modul `NavArgs.TECHNIQUE_ID` értékével –
             * importálás nélkül, lásd az osztály dokumentációját.
             */
            const val KEY_TECHNIQUE_ID = "techniqueId"

            /** A konfigurációváltás alatt életben tartja a feliratkozást. */
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
