package hu.kyokushin.trainer.feature.library.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.ui.R as CoreUiR
import hu.kyokushin.trainer.core.ui.status.TechniqueField
import hu.kyokushin.trainer.feature.library.R as LibraryR
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A Tudástár részletező képernyő állapotkezelése (FR-04, FR-05, FUNCTIONAL_SPEC 3.2,
 * US-07, US-08).
 *
 * ## A `techniqueId` forrása
 *
 * A [SavedStateHandle]-ben már ott van, amikor ez a ViewModel létrejön: a Navigation
 * Compose minden `NavBackStackEntry`-hez tartozó `SavedStateHandle`-t a route
 * argumentumaiból előre feltölti, a `hiltViewModel()` hívó modultól **függetlenül**
 * (androidx.navigation, 2.4+ óta). Ezért ez a modul nem függ a `navigation` modultól
 * (L5) – a [KEY_TECHNIQUE_ID] kulcs csak **névben** egyezik a
 * `hu.kyokushin.trainer.navigation.NavArgs.TECHNIQUE_ID` értékével, importálás nélkül.
 * Lásd `docs/CONVENTIONS.md` 5. szakasza – ez egy új, a T-209-ben felfedezett konvenció.
 *
 * ## `suspend`, nem `Flow`
 *
 * A [TechniqueRepository.getTechnique] pontszerű lekérdezés (a repository szerződése
 * szerint), nem figyelhető meg folyamként. A részletező csak a lista tételére kattintva
 * érhető el, ahol a tartalom már betöltött állapotban van – nincs itt saját
 * tartalomimport, mint a [hu.kyokushin.trainer.feature.library.list.LibraryListViewModel]-ben.
 *
 * ## A hiányzó mezők levezetése
 *
 * A [Technique] domain modell nem hordoz „mely mezők hiányoznak" listát: ezt a jelenlegi
 * validátor (`DefaultKnowledgeValidator`, `core/quiz`) kizárólag akkor fokozza
 * [VerificationStatus.INCOMPLETE]-re, ha a képhivatkozás lóg (V005, ADR-002) – minden
 * más kötelező mező hiánya a rekordot a leképezés **előtt** elveti, tehát soha nem jut
 * idáig domain objektumként. A [missingFieldsFor] ezért ma mindig legfeljebb egyelemű
 * listát ad; ha a validátor új lefokozási okot kapna, itt kell kiegészíteni (lásd a
 * függvény dokumentációját).
 *
 * @property repository a tartalom olvasási felülete.
 * @property savedStateHandle a navigációs argumentumot hordozza (`techniqueId`).
 */
@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    private val repository: TechniqueRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val techniqueId: TechniqueId = TechniqueId.of(
        requireNotNull(savedStateHandle.get<String>(KEY_TECHNIQUE_ID)) {
            "LibraryDetailViewModel: hiányzó '$KEY_TECHNIQUE_ID' navigációs argumentum."
        },
    )

    /** A legutóbbi lekérdezés kimenete – a `Loading` és az `Error` egyetlen forrása. */
    private val loadResult = MutableStateFlow<LoadResult>(LoadResult.Loading)

    /** A képernyő állapota. */
    val uiState: StateFlow<LibraryDetailUiState> = combine(
        loadResult,
        repository.observeCategories(),
        repository.observeContentState(),
    ) { result, categories, contentState ->
        buildState(result, categories, contentState.datasetTrust)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LibraryDetailUiState.Loading(),
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
                val technique = repository.getTechnique(techniqueId)
                if (technique == null) LoadResult.NotFound else LoadResult.Found(technique)
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                LoadResult.Failed
            }
        }
    }

    private fun buildState(
        result: LoadResult,
        categories: List<Category>,
        datasetTrust: DatasetTrust,
    ): LibraryDetailUiState = when (result) {
        LoadResult.Loading -> LibraryDetailUiState.Loading(datasetTrust = datasetTrust)

        LoadResult.Failed -> LibraryDetailUiState.Error(
            messageRes = CoreUiR.string.error_load_failed,
            datasetTrust = datasetTrust,
        )

        // A technikaazonosító a listáról érkezett, tehát a gyakorlatban mindig létezik;
        // ez az ág a dokumentált kivétel (lásd `TechniqueRepository.getTechnique`): nem
        // omlik össze, hanem a megszokott hibaállapotot mutatja (FUNCTIONAL_SPEC 10.).
        LoadResult.NotFound -> LibraryDetailUiState.Error(
            messageRes = LibraryR.string.library_detail_not_found,
            datasetTrust = datasetTrust,
        )

        is LoadResult.Found -> result.technique.toContentState(categories, datasetTrust)
    }

    private fun Technique.toContentState(
        categories: List<Category>,
        datasetTrust: DatasetTrust,
    ): LibraryDetailUiState.Content {
        val categoryName = categories.firstOrNull { it.id == categoryId }?.nameHu.orEmpty()
        return LibraryDetailUiState.Content(
            id = id,
            romaji = romaji,
            hungarian = hungarian,
            japaneseScript = japaneseScript,
            aliases = aliases,
            descriptionHu = descriptionHu,
            illustration = illustration,
            categoryId = categoryId,
            categoryNameHu = categoryName,
            status = status,
            missingFields = missingFieldsFor(status),
            datasetTrust = datasetTrust,
        )
    }

    /** A legutóbbi `getTechnique` lekérdezés kimenete. */
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

/**
 * A hiányzó mezők levezetése a rekord állapotából (lásd az osztály dokumentációját).
 *
 * [VerificationStatus.INCOMPLETE] kizárólag a lógó képhivatkozás miatt keletkezik a
 * jelenlegi validátorban, tehát a felsorolás ma mindig `[ILLUSTRATION]`.
 */
private fun missingFieldsFor(status: VerificationStatus): List<TechniqueField> =
    if (status == VerificationStatus.INCOMPLETE) listOf(TechniqueField.ILLUSTRATION) else emptyList()
