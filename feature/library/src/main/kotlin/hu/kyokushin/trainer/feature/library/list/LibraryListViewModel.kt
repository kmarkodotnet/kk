package hu.kyokushin.trainer.feature.library.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.data.repository.TechniqueMatch
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.state.EmptyStateReason
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A Tudástár lista képernyő állapotkezelése (FR-01, FR-02, FR-03, FUNCTIONAL_SPEC 3.1).
 *
 * ## Mit tesz, és mit nem
 *
 * Négy bemenetből (tartalomimport állapota, keresőszöveg, kategóriakijelölés, tartalom)
 * állít elő **egyetlen** [LibraryListUiState]-et. Compose típust nem ismer, erőforrást nem
 * old fel, és egyetlen Room típust sem lát: a bemenete a `core/data` publikus repository
 * szerződése, ami domain modellt ad (ADR-001 L3/L6, TR-07).
 *
 * ## Négy döntés, amit érdemes tudni
 *
 * ### 1. A keresés debounce-a itt van, nem a Compose oldalon
 *
 * Minden leütés új SQL `LIKE` lekérdezést indítana (FUNCTIONAL_SPEC 3.1: ~200 ms). A
 * debounce **mentes** az üres keresőszóra: a „×" megnyomása után a teljes lista azonnal
 * visszajön, nem 200 ms késéssel – a törlés visszavonás, nem újabb keresés.
 *
 * A *tárolt* (debounce nélküli) szöveg ettől független: a [onQueryChange] azonnal írja a
 * [SavedStateHandle]-be, hogy a forgatás a félig beírt szót is túlélje (FR-02).
 *
 * ### 2. Egy lekérdezés, nem kettő – a kategóriaszűrés memóriában fut
 *
 * A chipek találatszáma a kategóriakijelölést **figyelmen kívül hagyva** számolt (lásd
 * [CategoryFilterUiModel.matchCount]), tehát a szűrés nélküli találathalmazra minden
 * esetben szükség van. Ebből a halmazból a kijelölt kategóriákra szűkíteni memóriában egy
 * `filter` – egy második, kategóriára szűkített SQL lekérdezés ugyanazt az adatot kérné
 * le másodszor. A `retired` szűréssel ez nem keverhető össze: azt továbbra is a
 * repository végzi SQL-ben, és nem is kerülhet ide (a domain modell már szűrt listát ad).
 *
 * ### 3. A számolás a `@DefaultDispatcher`-en fut
 *
 * A kategóriánkénti összegzés és a kétszintű rendezés CPU-munka, nem I/O. A
 * `Dispatchers.IO` készlete blokkoló lemezműveletekhez van méretezve; a rendezés ott a
 * párhuzamos asseteléréssel akadna (NFR-10). A dispatcher injektált, hogy a teszt
 * virtuális órája vezérelhesse (NFR-07).
 *
 * ### 4. Az importot ez a ViewModel indítja
 *
 * Nem az `Application`: csak így tud a képernyő valódi `Loading` állapotot mutatni, amíg
 * a betöltés tart (FUNCTIONAL_SPEC 7.1). Az import `ImportResult.Rejected` ága **nem**
 * hiba: olyankor a korábbi tartalom érvényben marad (FR-31), és ha nincs korábbi
 * tartalom, az `Empty – nincs tartalom` állapot a helyes olvasat, nem az `Error`. Az
 * `Error` kizárólag kivételre jár.
 *
 * @property repository a tartalom olvasási felülete.
 * @property contentImporter a csomagolt tudástár betöltése.
 * @property defaultDispatcher CPU-kötött háttérmunka dispatchere.
 * @property savedStateHandle a forgatást túlélő keresőszöveg és kategóriakijelölés.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryListViewModel @Inject constructor(
    private val repository: TechniqueRepository,
    private val contentImporter: ContentImporter,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * A tartalombetöltés szakasza – a `Loading` és az `Error` állapot egyetlen forrása.
     *
     * Nem a `ContentState`-ből olvasható ki: a `NotImported` azt mondja meg, hogy *még
     * nem futott* import, de nem azt, hogy *most fut-e*. A kettő különbsége épp a
     * `Loading` és az `Empty – nincs tartalom` közötti különbség.
     */
    private val importPhase = MutableStateFlow(ImportPhase.RUNNING)

    /** A nyers, debounce nélküli keresőszöveg; a keresősáv ezt jeleníti meg. */
    private val queryFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    private val selectedCategoryFlow: Flow<Set<CategoryId>> =
        // ArrayList, nem `emptyList()`: a SavedStateHandle Bundle-be ír, és csak a konkrét,
        // szerializálható gyűjteményt fogadja el – a `Collections.EmptyList` nem az.
        savedStateHandle.getStateFlow(KEY_CATEGORIES, ArrayList<String>())
            .map { raw -> raw.map(CategoryId::of).toSet() }

    /**
     * A keresés eredménye, a keresőszóval **egy csomagban**.
     *
     * Azért egy csomagban, mert a „nincs találat" üzenet idézi a keresőszót: ha a szöveget
     * és a találatokat két külön `Flow`-ból `combine`-olnánk, egy pillanatra az új szöveg
     * a régi találatokkal párosulna, és az üzenet rossz kifejezést idézne.
     */
    private val searchFlow: Flow<SearchSnapshot> = queryFlow
        // Az üres keresőszó késleltetés nélkül jön át: a törlés azonnali visszavonás.
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { query ->
            repository.observeTechniqueMatches(query)
                .map { matches -> SearchSnapshot(query, matches) }
        }

    /** A képernyő állapota. */
    val uiState: StateFlow<LibraryListUiState> = combine(
        importPhase,
        searchFlow,
        selectedCategoryFlow,
        repository.observeCategories(),
        repository.observeContentState(),
    ) { phase, search, selected, categories, contentState ->
        buildState(phase, search, selected, categories, contentState)
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LibraryListUiState.Loading(),
        )

    init {
        startImport()
    }

    /** A keresőszöveg megváltozott. A tárolás azonnali, a keresés debounce-olt. */
    fun onQueryChange(query: String) {
        savedStateHandle[KEY_QUERY] = query
    }

    /** Egy kategóriachip ki- vagy bekapcsolása (FR-02, több kategória egyszerre). */
    fun onToggleCategory(categoryId: CategoryId) {
        val current = savedStateHandle.get<List<String>>(KEY_CATEGORIES).orEmpty()
        val next = if (categoryId.value in current) {
            current - categoryId.value
        } else {
            current + categoryId.value
        }
        // ArrayList, nem a `minus`/`plus` adta tetszőleges List: a SavedStateHandle
        // Bundle-be ír, és csak a konkrét, szerializálható gyűjteményt tudja visszaadni.
        savedStateHandle[KEY_CATEGORIES] = ArrayList(next)
    }

    /**
     * „Szűrők törlése" / „Összes" – a keresőszöveg **és** a kategóriakijelölés is törlődik.
     *
     * A „nincs találat" állapotból ez az egyetlen kiút, és a felhasználó nem tudja, a
     * szöveg vagy a chip szűkítette-e ki a listát – ezért mindkettőt visszaállítja
     * (US-06 4. kritérium).
     */
    fun onClearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_CATEGORIES] = ArrayList<String>()
    }

    /** „Újra" az `Error` állapotból: a betöltés újraindul. */
    fun onRetry() {
        startImport()
    }

    private fun startImport() {
        importPhase.value = ImportPhase.RUNNING
        viewModelScope.launch {
            importPhase.value = try {
                // A visszatérési érték itt nem érdekes: mindhárom ág rendes működés, és a
                // megjeleníthető tartalomról a `ContentState` mond igazat, nem az import
                // mérlege (egy elutasított frissítés után a korábbi kiadás él, FR-31).
                contentImporter.importIfNeeded()
                ImportPhase.DONE
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                ImportPhase.FAILED
            }
        }
    }

    private fun buildState(
        phase: ImportPhase,
        search: SearchSnapshot,
        selected: Set<CategoryId>,
        categories: List<Category>,
        contentState: ContentState,
    ): LibraryListUiState {
        val orderedCategories = categories.sortedWith(
            compareBy({ it.sortOrder }, { it.id.value }),
        )
        val countByCategory = search.matches.groupingBy { it.technique.categoryId }.eachCount()
        val chips = orderedCategories.map { category ->
            CategoryFilterUiModel(
                id = category.id,
                nameHu = category.nameHu,
                matchCount = countByCategory[category.id] ?: 0,
                selected = category.id in selected,
            )
        }

        if (phase == ImportPhase.RUNNING) {
            return LibraryListUiState.Loading(
                query = search.query,
                categories = chips,
                selectedCategories = selected,
                datasetTrust = contentState.datasetTrust,
            )
        }
        if (phase == ImportPhase.FAILED) {
            return LibraryListUiState.Error(
                messageRes = R.string.error_load_failed,
                query = search.query,
                categories = chips,
                selectedCategories = selected,
                datasetTrust = contentState.datasetTrust,
            )
        }

        // Üres kijelölés jelentése: MINDEN kategória – ez a szűrő kijelölésmentes
        // állapota, nem „semmi". Ugyanaz a konvenció, mint a repositoryban.
        val visible = if (selected.isEmpty()) {
            search.matches
        } else {
            search.matches.filter { it.technique.categoryId in selected }
        }

        val emptyReason = when {
            // A „nincs tartalom" a tartalom tényleges állapotából következik, nem abból,
            // hogy a szűrés kiürítette a listát: egy sikeres keresés nélküli üres lista
            // fölött az „A tudástár üres" felirat azt sugallná, hogy elveszett a tananyag.
            !contentState.hasContent -> EmptyStateReason.LibraryNoContent
            visible.isEmpty() -> EmptyStateReason.LibraryNoResult(search.query)
            else -> null
        }
        if (emptyReason != null) {
            return LibraryListUiState.Empty(
                reason = emptyReason,
                query = search.query,
                categories = chips,
                selectedCategories = selected,
                datasetTrust = contentState.datasetTrust,
            )
        }

        val sortOrderById = orderedCategories.associate { it.id to it.sortOrder }
        val nameById = orderedCategories.associate { it.id to it.nameHu }
        val items = visible
            .sortedWith(
                compareBy(
                    // A kategória sorrendje a tartalomból jön; a sorrend nélküli
                    // kategória a lista végére kerül, de nem esik ki.
                    { sortOrderById[it.technique.categoryId] ?: Int.MAX_VALUE },
                    { it.technique.romajiAscii },
                    { it.technique.id.value },
                ),
            )
            .map { match -> match.toUiModel(nameById[match.technique.categoryId].orEmpty()) }

        return LibraryListUiState.Content(
            items = items,
            query = search.query,
            categories = chips,
            selectedCategories = selected,
            datasetTrust = contentState.datasetTrust,
        )
    }

    /** A tartalombetöltés szakasza. */
    private enum class ImportPhase { RUNNING, DONE, FAILED }

    /** Egy keresés eredménye a hozzá tartozó keresőszóval. */
    private data class SearchSnapshot(
        val query: String,
        val matches: List<TechniqueMatch>,
    )

    private companion object {
        const val KEY_QUERY = "library.query"
        const val KEY_CATEGORIES = "library.categories"

        /** FUNCTIONAL_SPEC 3.1: „Debounce ~200 ms". */
        const val SEARCH_DEBOUNCE_MS = 200L

        /** A konfigurációváltás alatt életben tartja a feliratkozást. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun TechniqueMatch.toUiModel(categoryNameHu: String): LibraryTechniqueUiModel =
    LibraryTechniqueUiModel(
        id = technique.id,
        romaji = technique.romaji,
        hungarian = technique.hungarian,
        categoryId = technique.categoryId,
        categoryNameHu = categoryNameHu,
        illustration = technique.illustration,
        status = technique.status,
        matchedAlias = matchedAlias,
    )
