package hu.kyokushin.trainer.feature.quiz.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateFilter
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import hu.kyokushin.trainer.core.quiz.generator.QuizGenerator
import hu.kyokushin.trainer.core.quiz.generator.QuizRequest
import hu.kyokushin.trainer.core.quiz.generator.SkipReason
import hu.kyokushin.trainer.core.quiz.validation.QuizEligibility
import hu.kyokushin.trainer.core.ui.R as CoreUiR
import hu.kyokushin.trainer.feature.quiz.R
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A kvízbeállító képernyő állapotkezelése (FR-11, FR-12, FR-13, FR-22, FUNCTIONAL_SPEC
 * 5.1, US-12, T-403).
 *
 * ## Mit tesz, és mit nem
 *
 * Ugyanaz a mintázat, mint a [hu.kyokushin.trainer.feature.learn.setup.LearnSetupViewModel]-nél:
 * a tartalomimport állapotát, a kijelölést és a tartalmat egyetlen [QuizSetupUiState]-be
 * állítja össze. A különbség, hogy itt a kijelölésből **minden változtatásra** egy valódi
 * `QuizGenerator.dryRun()` fut – ez a kvízmotor egyetlen igazságforrása a „ennyi kérdés
 * áll össze" ígéretre (T-403 „Figyelj rá": a dryRun-t tilos optimistán kerekíteni).
 *
 * ## Egy jelöltkészlet-lekérdezés, nem kategóriánként egy
 *
 * A `TechniqueRepository.getQuizCandidates(categories)` **suspend**, tehát minden
 * jelölőnégyzet-kattintásra újrahívni felesleges adatbázis-kört jelentene. Ehelyett a
 * [candidateSetFlow] egyetlen, **minden kategóriára** szóló (`getQuizCandidates()` üres
 * halmazzal) jelöltkészletet tart életben, és a kategóriaszűrést a [QuizRequest.categoryFilter]
 * végzi el memóriában, a `dryRun()` minden hívásakor – ugyanaz a döntés, mint a
 * `feature/library`-ban a kategóriaszűrésnél (lásd `docs/CONVENTIONS.md` 4.1 „Egy
 * lekérdezés, nem kettő"). A [candidateSetFlow] a [TechniqueRepository.observeTechniques]
 * változására frissül újra (tartalomimport után), nem minden kijelölésváltásra.
 *
 * ## Miért van egy második, `Eagerly` megosztású belső állapot a nyilvános [uiState] mellett
 *
 * A [onStartQuiz] a tényleges `generate()`-hez a teljes [QuizCandidateSet]-et és a
 * bizalmi szintet igényli, de ezeket **nem** tesszük ki a nyilvános [QuizSetupUiState]-be
 * (az a UI-nak szól, nem hordozhat akkora, meg nem jelenítendő domain objektumot). A
 * [requestInputs] ezért egy külön, `SharingStarted.Eagerly`-vel életben tartott belső
 * állapot, amiből [onStartQuiz] szinkronon leolvashatja a legfrissebb bemenetet, attól
 * függetlenül, hogy a [uiState]-et éppen figyeli-e valaki.
 *
 * ## A típusonkénti letiltás oka TARTALMI, nem a felhasználó saját kapcsolóállása (FR-13)
 *
 * A [QuizQuestionTypeUiModel.enabled] eldöntéséhez **nem** a felhasználó aktuális
 * kijelölésével futtatott `dryRun()`-t használjuk: ha a felhasználó már kikapcsolt egy
 * típust, a motor `SkipReason.TYPE_DISABLED`-et adna, ami nem a FR-13 szerinti
 * tartalmi indoklás. Ezért a [buildState] egy **próba**-kérést épít, amelyben minden
 * típus engedélyezett, és ennek `QuizCandidateFilter.skipReasons()` eredményét nézi –
 * így a letiltás és az indoklás kizárólag attól függ, hogy a kiválasztott kategóriákban
 * **összeállna-e** ebből a típusból kérdés, a felhasználó saját jelölőnégyzet-állásától
 * függetlenül. A `QuizCandidateFilter` ezért közvetlenül látható ebből a modulból is –
 * ezt a T-402 lezárása kifejezetten diagnosztikai célra tartotta meg
 * (`docs/done/T-402-core-data-kvizjeloltek-lekerdezese.md`).
 *
 * @property repository a tudástár olvasási felülete.
 * @property contentImporter a csomagolt tudástár betöltése.
 * @property progressRepository a kvízmenet nyitása ([onStartQuiz]).
 * @property quizGenerator a kvízmotor; `dryRun()` minden kijelölésváltásra, `generate()`
 *   csak indításkor.
 * @property random az alkalmazás egyetlen véletlenforrása, kizárólag a kvízmenet seedjének
 *   sorsolására (NFR-07, ADR-007) – ugyanaz a kötés, mint a `DefaultQuizGenerator`-nál.
 * @property defaultDispatcher CPU-kötött háttérmunka dispatchere (a `dryRun` és az
 *   összegzés).
 * @property savedStateHandle a kijelölt kategóriákat, a kért kérdésszámot és a kijelölt
 *   kérdéstípusokat őrzi meg.
 */
@HiltViewModel
class QuizSetupViewModel @Inject constructor(
    private val repository: TechniqueRepository,
    private val contentImporter: ContentImporter,
    private val progressRepository: ProgressRepository,
    private val quizGenerator: QuizGenerator,
    private val random: Random,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** A tartalombetöltés szakasza – a `Loading` és az `Error` állapot egyetlen forrása. */
    private val importPhase = MutableStateFlow(ImportPhase.RUNNING)

    private val selectedCategoriesFlow: Flow<Set<CategoryId>> =
        // ArrayList, nem `emptyList()`: a SavedStateHandle Bundle-be ír, és csak a konkrét,
        // szerializálható gyűjteményt fogadja el (CONVENTIONS 4.1).
        savedStateHandle.getStateFlow(KEY_SELECTED_CATEGORIES, ArrayList<String>())
            .map { raw -> raw.map(CategoryId::of).toSet() }

    private val requestedCountFlow: Flow<Int> =
        savedStateHandle.getStateFlow(KEY_REQUESTED_COUNT, DEFAULT_REQUESTED_COUNT)

    private val selectedTypesFlow: Flow<Set<QuestionType>> =
        savedStateHandle.getStateFlow(KEY_SELECTED_TYPES, ArrayList(DEFAULT_SELECTED_TYPES.map { it.name }))
            .map { raw ->
                raw.mapNotNull { name -> runCatching { QuestionType.valueOf(name) }.getOrNull() }.toSet()
            }

    /**
     * A teljes (minden kategóriára szóló) jelöltkészlet, a tartalom változására frissítve.
     * Lásd az osztály KDoc-ja: a kategóriaszűrés innentől memóriában fut.
     */
    private val candidateSetFlow: Flow<QuizCandidateSet> =
        repository.observeTechniques().map { repository.getQuizCandidates() }

    /** Lásd az osztály KDoc-ja: a [onStartQuiz] szinkron bemenete. */
    private val requestInputs: StateFlow<RequestInputs?> = combine(
        selectedCategoriesFlow,
        requestedCountFlow,
        selectedTypesFlow,
        candidateSetFlow,
        repository.observeContentState(),
    ) { categories, count, types, candidateSet, contentState ->
        RequestInputs(
            selectedCategories = categories,
            requestedCount = count,
            selectedTypes = types,
            candidateSet = candidateSet,
            datasetTrust = contentState.datasetTrust,
        )
    }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _startEvent = Channel<QuizStartResult>(Channel.BUFFERED)

    /** Egyszeri esemény: a „Kvíz indítása" gomb eredménye (ARCHITECTURE 6.1). */
    val startEvent: Flow<QuizStartResult> = _startEvent.receiveAsFlow()

    /** A képernyő állapota. */
    val uiState: StateFlow<QuizSetupUiState> = combine(
        importPhase,
        requestInputs,
        repository.observeCategories(),
        repository.observeTechniques(),
    ) { phase, inputs, categories, techniques ->
        buildState(phase, inputs, categories, techniques)
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = QuizSetupUiState.Loading,
        )

    init {
        startImport()
    }

    /** Egy kategória ki- vagy bekapcsolása (FUNCTIONAL_SPEC 5.1/2., több kategória egyszerre). */
    fun onToggleCategory(categoryId: CategoryId) {
        val current = savedStateHandle.get<List<String>>(KEY_SELECTED_CATEGORIES).orEmpty()
        val next = if (categoryId.value in current) {
            current - categoryId.value
        } else {
            current + categoryId.value
        }
        savedStateHandle[KEY_SELECTED_CATEGORIES] = ArrayList(next)
    }

    /** A kért kérdésszám kiválasztása (FUNCTIONAL_SPEC 5.1/3.: 5 / 10 / 20). */
    fun onSelectRequestedCount(count: Int) {
        require(count in REQUESTED_COUNT_OPTIONS) {
            "A kért kérdésszám csak 5, 10 vagy 20 lehet (FR-11), kapott: $count."
        }
        savedStateHandle[KEY_REQUESTED_COUNT] = count
    }

    /**
     * Egy kérdéstípus ki- vagy bekapcsolása (FUNCTIONAL_SPEC 5.1/4.).
     *
     * Nem ellenőrzi, hogy a típus [QuizQuestionTypeUiModel.enabled]-e: a UI a letiltott
     * jelölőnégyzetet nem teszi interakcióra késszé, ez csak másodlagos védelem, ugyanúgy,
     * mint a kvíz egyszeri válasza (ADR-005). Egy tartalmilag nem elérhető típus
     * bekapcsolása a `dryRun()`-t nem téveszti meg: az a típus egyszerűen kimarad a
     * tervből, ugyanazzal a tartalmi okkal.
     */
    fun onToggleType(type: QuestionType) {
        val current = savedStateHandle.get<List<String>>(KEY_SELECTED_TYPES).orEmpty()
        val next = if (type.name in current) {
            current - type.name
        } else {
            current + type.name
        }
        savedStateHandle[KEY_SELECTED_TYPES] = ArrayList(next)
    }

    /** „Újra" az `Error` állapotból: a betöltés újraindul. */
    fun onRetry() {
        startImport()
    }

    /**
     * „Kvíz indítása" (FUNCTIONAL_SPEC 5.1/6., T-403 Teendők 4.): seed sorsolása az
     * injektált RNG-ből, [hu.kyokushin.trainer.core.model.QuizSession] nyitása, majd a
     * [hu.kyokushin.trainer.core.quiz.generator.QuizPlan] generálása ugyanazzal a seeddel.
     *
     * No-op, ha a képernyő még nem áll készen, vagy a jelenlegi beállításokkal egyetlen
     * kérdés sem állítható össze ([QuizSetupUiState.Content.canStart] hamis) – ez csak
     * másodlagos védelem, a UI a „Kvíz indítása" gombot ilyenkor letiltja.
     */
    fun onStartQuiz() {
        val content = uiState.value as? QuizSetupUiState.Content ?: return
        if (!content.canStart) return
        val inputs = requestInputs.value ?: return
        viewModelScope.launch {
            val seed = random.nextLong()
            val sessionId = progressRepository.startSession(inputs.requestedCount, seed)
            val request = QuizRequest(
                candidates = inputs.candidateSet,
                enabledTypes = inputs.selectedTypes,
                categoryFilter = inputs.selectedCategories,
                requestedCount = inputs.requestedCount,
                seed = seed,
                datasetTrust = inputs.datasetTrust,
            )
            val plan = quizGenerator.generate(request)
            _startEvent.send(QuizStartResult(sessionId, plan, seed))
        }
    }

    private fun startImport() {
        importPhase.value = ImportPhase.RUNNING
        viewModelScope.launch {
            importPhase.value = try {
                contentImporter.importIfNeeded()
                ImportPhase.DONE
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                ImportPhase.FAILED
            }
        }
    }

    private fun buildState(
        phase: ImportPhase,
        inputs: RequestInputs?,
        categories: List<Category>,
        techniques: List<Technique>,
    ): QuizSetupUiState {
        if (phase == ImportPhase.FAILED) return QuizSetupUiState.Error(CoreUiR.string.error_load_failed)
        // `inputs == null`: a jelöltkészlet első suspend lekérdezése még nem tért vissza –
        // a felhasználó szempontjából ez is "még töltünk" (Loading).
        if (phase == ImportPhase.RUNNING || inputs == null) return QuizSetupUiState.Loading

        val datasetTrust = inputs.datasetTrust

        val orderedCategories = categories.sortedWith(compareBy({ it.sortOrder }, { it.id.value }))
        val techniquesByCategory = techniques.groupBy { it.categoryId }
        val categoryModels = orderedCategories.map { category ->
            val inCategory = techniquesByCategory[category.id].orEmpty()
            QuizCategoryUiModel(
                id = category.id,
                nameHu = category.nameHu,
                quizEligibleCount = inCategory.count {
                    QuizEligibility.eligibleTypes(it, datasetTrust).isNotEmpty()
                },
                totalCount = inCategory.size,
                selected = category.id in inputs.selectedCategories,
            )
        }

        val liveRequest = QuizRequest(
            candidates = inputs.candidateSet,
            enabledTypes = inputs.selectedTypes,
            categoryFilter = inputs.selectedCategories,
            requestedCount = inputs.requestedCount,
            seed = DRY_RUN_SEED,
            datasetTrust = datasetTrust,
        )
        val readiness = quizGenerator.dryRun(liveRequest)

        // Próba: MINDEN típus engedélyezve, hogy a letiltás oka tartalmi legyen, ne a
        // felhasználó saját kapcsolóállása (FR-13) – lásd az osztály KDoc-ja.
        val probeRequest = liveRequest.copy(enabledTypes = QuestionType.entries.toSet())
        val probeSkipReasons = QuizCandidateFilter.skipReasons(probeRequest)
        val typeModels = QuestionType.entries.map { type ->
            val skipReason = probeSkipReasons[type]
            val disabledReasonRes = when (skipReason) {
                SkipReason.NOT_ENOUGH_IMAGES -> R.string.quiz_setup_type_disabled_images
                SkipReason.NOT_ENOUGH_CANDIDATES -> R.string.quiz_setup_type_disabled_candidates
                SkipReason.TYPE_DISABLED, null -> null
            }
            QuizQuestionTypeUiModel(
                type = type,
                selected = type in inputs.selectedTypes,
                enabled = skipReason == null,
                disabledReasonRes = disabledReasonRes,
                disabledReasonArg = if (disabledReasonRes != null) {
                    QuizCandidateFilter.correctAnswerPool(probeRequest, type).size
                } else {
                    0
                },
            )
        }

        return QuizSetupUiState.Content(
            categories = categoryModels,
            selectedCategories = inputs.selectedCategories,
            requestedCount = inputs.requestedCount,
            questionTypes = typeModels,
            expectedQuestionCount = readiness.expectedQuestionCount,
            shortfall = readiness.expectedShortfall,
            canStart = readiness.canStart,
            notEnoughDataMessageRes = if (readiness.canStart) {
                null
            } else {
                R.string.quiz_setup_message_not_enough_data
            },
            datasetTrust = datasetTrust,
        )
    }

    /** A tartalombetöltés szakasza. */
    private enum class ImportPhase { RUNNING, DONE, FAILED }

    /**
     * A [onStartQuiz]-hoz és a [buildState]-hez szükséges, a `SavedStateHandle`-ből és a
     * tartalomból származtatott bemenet, egy csomagban.
     */
    private data class RequestInputs(
        val selectedCategories: Set<CategoryId>,
        val requestedCount: Int,
        val selectedTypes: Set<QuestionType>,
        val candidateSet: QuizCandidateSet,
        val datasetTrust: DatasetTrust,
    )

    companion object {
        private const val KEY_SELECTED_CATEGORIES = "quiz.setup.categories"
        private const val KEY_REQUESTED_COUNT = "quiz.setup.requestedCount"
        private const val KEY_SELECTED_TYPES = "quiz.setup.types"

        /** A kért kérdésszám lehetséges értékei (FUNCTIONAL_SPEC 5.1/3.: „5 / 10 / 20"). */
        val REQUESTED_COUNT_OPTIONS: List<Int> = listOf(5, 10, 20)

        /**
         * Alapértelmezett kért kérdésszám, amíg a felhasználó nem választott. A
         * FUNCTIONAL_SPEC nem mond ki explicit alapértéket; a három lehetőség közepe
         * dokumentált feltételezés.
         */
        private const val DEFAULT_REQUESTED_COUNT = 10

        /**
         * Alapértelmezetten minden kérdéstípus kijelölve (FR-12): a felhasználó explicit
         * döntéssel kapcsol ki egy típust, nem fordítva. A FUNCTIONAL_SPEC nem mond ki
         * explicit alapértéket; ez is dokumentált feltételezés, ugyanúgy, mint a
         * [DEFAULT_REQUESTED_COUNT].
         */
        private val DEFAULT_SELECTED_TYPES: Set<QuestionType> = QuestionType.entries.toSet()

        /**
         * A `dryRun()` seedje – a motor dokumentált garanciája szerint a `dryRun` nem
         * használ `Random`-ot (T-111 „Teendők": „ne generáljon teljes tervet... nincs
         * `Random`, nincs keverés"), tehát ez az érték soha nem kerül felhasználásra. A
         * valódi indításkor ([onStartQuiz]) az injektált RNG-ből sorsolt, friss seed megy
         * a `generate()`-be – ez **nem** ugyanaz az érték.
         */
        private const val DRY_RUN_SEED = 0L

        /** A konfigurációváltás alatt életben tartja a feliratkozást. */
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
