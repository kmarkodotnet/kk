package hu.kyokushin.trainer.feature.learn.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.feature.learn.HideMode
import hu.kyokushin.trainer.feature.learn.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import hu.kyokushin.trainer.core.ui.R as CoreUiR

/**
 * A tanulás-beállító képernyő állapotkezelése (FR-06, FR-07, FUNCTIONAL_SPEC 4.1, US-09,
 * US-10, G-04 csoport; a kategória-drill-down T-705, FR-39).
 *
 * ## Mit tesz, és mit nem
 *
 * Öt bemenetből (tartalomimport állapota, kijelölt kategóriák, elrejtési mód, a böngészött
 * kategória, a tartalom) állít elő egyetlen [LearnSetupUiState]-et – ugyanaz a mintázat,
 * mint a [hu.kyokushin.trainer.feature.library.list.LibraryListViewModel]-nél (T-207),
 * csak keresés és debounce nélkül: itt nincs szabad szöveges szűrés.
 *
 * A böngészött kategória (T-705) **nem** egy új Navigation Compose route – a
 * kategóriarács és egy adott kategória technikalistája ugyanazon a képernyőn, a
 * [LearnSetupUiState.Content.drillDown] mező ki-/betöltésével vált, pontosan úgy, mint a
 * Tudástár lista keresése/szűrése. Lásd [LearnCategoryDrillDownUiState] KDoc-ja a
 * "miért kategória-szintű marad a kijelölés" tervezési döntésért.
 *
 * ### A kijelölés itt NEM szűrő, hanem kötelező választás
 *
 * A Tudástár és a kvíz kategóriaszűrőjénél az üres kijelölés „minden kategória". Itt
 * nem: a FUNCTIONAL_SPEC 4.1/4. pontja szerint a „Kártyák indítása" gomb letiltott, ha
 * **nincs** kiválasztott kategória – azaz a felhasználónak legalább egyet választania
 * kell, mielőtt a kártyák elindulnak.
 *
 * ### Az importot ez a ViewModel is indítja
 *
 * Minden `topLevel` képernyő (a bottom nav egy fülének kezdőképernyője) maga biztosítja,
 * hogy a tartalom be legyen töltve – nem az `Application`, mert csak így tud a képernyő
 * valódi `Loading` állapotot mutatni (FUNCTIONAL_SPEC 7.1). Az import idempotens
 * (FR-30): ha a Tudástár fül már lefuttatta, ez a hívás
 * [hu.kyokushin.trainer.core.data.content.ImportResult.UpToDate]-tel tér vissza.
 *
 * ### Az „utolsó választás megjegyzése" hatóköre
 *
 * A FUNCTIONAL_SPEC 4.1/5. pontja „a munkamenetek között" őrzi meg a kijelölést és az
 * elrejtési módot. Ehhez tartós (app-újraindítást túlélő) tárolás kellene, de a projekt
 * version catalogjában (`gradle/libs.versions.toml`) nincs DataStore-függőség, és a
 * CLAUDE.md tiltja a verziószám kitalálását és az új, nem tervezett függőség bevezetését
 * (`Nincs korai bővítés`). Ezért a megjegyzés `SavedStateHandle`-ben él: ez túléli a
 * képernyőforgatást és a rendszer által kényszerített folyamat-újraindítást (mentett
 * állapotból), de nem egy teljes, a felhasználó által bezárt app újranyitását. Lásd
 * `docs/CONVENTIONS.md` 1. szakasz.
 *
 * @property repository a tartalom olvasási felülete.
 * @property contentImporter a csomagolt tudástár betöltése.
 * @property defaultDispatcher CPU-kötött háttérmunka dispatchere.
 * @property savedStateHandle a kijelölt kategóriákat és az elrejtési módot őrzi meg.
 */
@HiltViewModel
class LearnSetupViewModel
    @Inject
    constructor(
        private val repository: TechniqueRepository,
        private val contentImporter: ContentImporter,
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

        private val hideModeFlow: Flow<HideMode> =
            savedStateHandle.getStateFlow(KEY_HIDE_MODE, DEFAULT_HIDE_MODE.name)
                .map { raw -> runCatching { HideMode.valueOf(raw) }.getOrDefault(DEFAULT_HIDE_MODE) }

        /**
         * A böngészett kategória azonosítója, vagy `null`, ha a kategóriarácsot látjuk
         * (T-705, FR-39). `""` jelenti a "nincs drill-down" állapotot a `Bundle`-kompatibilis
         * `String` tárolásban – a `getStateFlow` maga nem fogad el `null` alapértéket.
         */
        private val drillDownCategoryFlow: Flow<CategoryId?> =
            savedStateHandle.getStateFlow(KEY_DRILLDOWN_CATEGORY, "")
                .map { raw -> raw.takeIf { it.isNotEmpty() }?.let(CategoryId::of) }

        private val selectionFlow: Flow<SetupSelection> =
            combine(selectedCategoriesFlow, hideModeFlow, drillDownCategoryFlow) { selected, hideMode, drillDown ->
                SetupSelection(selected, hideMode, drillDown)
            }

        /** A képernyő állapota. */
        val uiState: StateFlow<LearnSetupUiState> = combine(
            importPhase,
            selectionFlow,
            repository.observeCategories(),
            repository.observeTechniques(),
            repository.observeContentState(),
        ) { phase, selection, categories, techniques, contentState ->
            buildState(phase, selection, categories, techniques, contentState)
        }
            .flowOn(defaultDispatcher)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = LearnSetupUiState.Loading,
            )

        init {
            startImport()
        }

        /** Egy kategória ki- vagy bekapcsolása (FUNCTIONAL_SPEC 4.1/2., több kategória egyszerre). */
        fun onToggleCategory(categoryId: CategoryId) {
            val current = savedStateHandle.get<List<String>>(KEY_SELECTED_CATEGORIES).orEmpty()
            val next = if (categoryId.value in current) {
                current - categoryId.value
            } else {
                current + categoryId.value
            }
            savedStateHandle[KEY_SELECTED_CATEGORIES] = ArrayList(next)
        }

        /** Az elrejtési mód kiválasztása (FR-07, FUNCTIONAL_SPEC 4.1/3.). */
        fun onSelectHideMode(hideMode: HideMode) {
            savedStateHandle[KEY_HIDE_MODE] = hideMode.name
        }

        /**
         * Egy kategória technikalistájának megnyitása (T-705, FR-39) – a kategóriarács
         * helyett ennek a kategóriának a böngészhető listáját mutatja a UI.
         */
        fun onOpenCategory(categoryId: CategoryId) {
            savedStateHandle[KEY_DRILLDOWN_CATEGORY] = categoryId.value
        }

        /** Vissza a kategóriarácsra (T-705) – a vissza gomb és a rendszer vissza gombja hívja. */
        fun onCloseDrillDown() {
            savedStateHandle[KEY_DRILLDOWN_CATEGORY] = ""
        }

        /** „Újra" az `Error` állapotból: a betöltés újraindul. */
        fun onRetry() {
            startImport()
        }

        private fun startImport() {
            importPhase.value = ImportPhase.RUNNING
            viewModelScope.launch {
                importPhase.value = try {
                    // A visszatérési érték itt nem érdekes, lásd `LibraryListViewModel` KDoc-ja
                    // ugyanerről a mintáról: mindhárom ág rendes működés.
                    contentImporter.importIfNeeded()
                    ImportPhase.DONE
                } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                    ImportPhase.FAILED
                }
            }
        }

        private fun buildState(
            phase: ImportPhase,
            selection: SetupSelection,
            categories: List<Category>,
            techniques: List<Technique>,
            contentState: ContentState,
        ): LearnSetupUiState {
            if (phase == ImportPhase.RUNNING) return LearnSetupUiState.Loading
            if (phase == ImportPhase.FAILED) return LearnSetupUiState.Error(CoreUiR.string.error_load_failed)

            val orderedCategories = categories.sortedWith(
                compareBy({ it.sortOrder }, { it.id.value }),
            )
            // A tanuláshoz nem kell `VERIFIED` vagy `quizEligible` (T-302 „Kész ha"): minden
            // nem visszavont technikát számolunk, a hitelesítési státusztól függetlenül. Az
            // `observeTechniques` már kiszűri a visszavontakat (ADR-004).
            val countByCategory = techniques.groupingBy { it.categoryId }.eachCount()
            val categoryModels = orderedCategories.map { category ->
                LearnCategoryUiModel(
                    id = category.id,
                    nameHu = category.nameHu,
                    displayableCount = countByCategory[category.id] ?: 0,
                    selected = category.id in selection.selectedCategories,
                )
            }

            val selectedDisplayableCount = categoryModels
                .filter { it.selected }
                .sumOf { it.displayableCount }
            val canStart = selection.selectedCategories.isNotEmpty() && selectedDisplayableCount > 0
            val disabledReasonRes = when {
                canStart -> null
                selection.selectedCategories.isEmpty() -> R.string.learn_setup_start_disabled_no_category
                else -> R.string.learn_setup_start_disabled_no_technique
            }

            // A drill-down egy korábban megnyitott, de azóta eltűnt kategóriára is
            // mutathatna (pl. tartalomfrissítés) – ilyenkor csendesen visszaesik a
            // kategóriarácsra, nem omlik össze (ugyanaz az elv, mint a `forRoutePattern`
            // ismeretlen útvonalra adott válasza).
            val drillDownCategory = selection.drillDownCategoryId
                ?.let { id -> orderedCategories.firstOrNull { it.id == id } }
            val drillDown = drillDownCategory?.let { category ->
                LearnCategoryDrillDownUiState(
                    categoryId = category.id,
                    categoryName = category.nameHu,
                    techniques = techniques
                        .filter { it.categoryId == category.id }
                        .sortedBy { it.romaji }
                        .map { LearnTechniqueUiModel(id = it.id, romaji = it.romaji, hungarian = it.hungarian) },
                    selected = category.id in selection.selectedCategories,
                )
            }

            return LearnSetupUiState.Content(
                categories = categoryModels,
                selectedCategories = selection.selectedCategories,
                hideMode = selection.hideMode,
                canStart = canStart,
                disabledReasonRes = disabledReasonRes,
                datasetTrust = contentState.datasetTrust,
                drillDown = drillDown,
            )
        }

        /** A tartalombetöltés szakasza. */
        private enum class ImportPhase { RUNNING, DONE, FAILED }

        /** A kijelölt kategóriák, az elrejtési mód és a böngészett kategória egy csomagban. */
        private data class SetupSelection(
            val selectedCategories: Set<CategoryId>,
            val hideMode: HideMode,
            val drillDownCategoryId: CategoryId?,
        )

        companion object {
            private const val KEY_SELECTED_CATEGORIES = "learn.setup.categories"
            private const val KEY_HIDE_MODE = "learn.setup.hideMode"
            private const val KEY_DRILLDOWN_CATEGORY = "learn.setup.drillDownCategory"

            /**
             * Alapértelmezett elrejtési mód, amíg a felhasználó nem választott: a japán név
             * elrejtése, a FUNCTIONAL_SPEC 4.1/3. pontjában felsorolt első lehetőség.
             */
            private val DEFAULT_HIDE_MODE = HideMode.HIDE_JAPANESE

            /** A konfigurációváltás alatt életben tartja a feliratkozást. */
            private const val STOP_TIMEOUT_MS = 5_000L
        }
    }
