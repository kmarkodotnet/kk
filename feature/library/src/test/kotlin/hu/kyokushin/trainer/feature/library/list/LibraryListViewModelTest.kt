package hu.kyokushin.trainer.feature.library.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.content.ImportResult
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.state.EmptyStateReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Tudástár lista ViewModel viselkedése – emulátor nélkül (T-211, FR-01…FR-03).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket, és az üres állapot üzenetét a tényleges `strings.xml`-ből oldjuk fel.
 * `@Config(sdk = [34])` – a `compileSdk = 37`-hez nincs `android-all` csomag
 * (CONVENTIONS 3.).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryListViewModelTest {

    private val resources get() =
        ApplicationProvider.getApplicationContext<Context>().resources

    @Before
    fun setUp() {
        // A `viewModelScope` a `Dispatchers.Main`-en fut; a teszt virtuális órája csak így
        // vezérli az importot és a debounce-ot.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------- betöltés / FR-01

    @Test
    fun libraryList_whileImportRunning_showsLoading_FR01() = runTest {
        // Az import addig nem fejeződik be, amíg a teszt nem engedi: ez a `Loading`
        // állapot egyetlen honest módja (FUNCTIONAL_SPEC 7.1).
        val gate = Job()
        val viewModel = viewModel(blockingImport = gate)

        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(LibraryListUiState.Loading::class.java)

        gate.complete()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LibraryListUiState.Content::class.java)
        job.cancel()
    }

    @Test
    fun libraryList_ordersByCategorySortOrderThenRomaji_FR01() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryListUiState.Content
        // „Állások" (sortOrder 1) előbb, azon belül romaji ábécérendben, majd „Ütések".
        assertThat(content.items.map { it.romaji })
            .containsExactly("Fudo Dachi", "Heisoku Dachi", "Seiken Chudan Tsuki")
            .inOrder()
        assertThat(content.items.first().categoryNameHu).isEqualTo("Állások")
        job.cancel()
    }

    @Test
    fun libraryList_categoryNamesComeFromContent_FR01() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        // A kategórianév tartalmi adat (ADR-011), nem `strings.xml` kulcs.
        assertThat(viewModel.uiState.value.categories.map { it.nameHu })
            .containsExactly("Állások", "Ütések")
            .inOrder()
        job.cancel()
    }

    @Test
    fun libraryList_importThrows_showsErrorAndRetryRecovers_FR01() = runTest {
        var fail = true
        val viewModel = viewModel(
            importer = FakeContentImporter {
                if (fail) throw IllegalStateException("import failed") else ImportResult.UpToDate("test-1")
            },
        )

        val job = collectState(viewModel)
        advanceUntilIdle()

        val error = viewModel.uiState.value as LibraryListUiState.Error
        // `@StringRes`, nem a kivétel angol üzenete (NFR-02).
        assertThat(error.messageRes).isEqualTo(R.string.error_load_failed)

        fail = false
        viewModel.onRetry()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LibraryListUiState.Content::class.java)
        job.cancel()
    }

    // ------------------------------------------------------- kategóriaszűrés / FR-02

    @Test
    fun libraryList_multipleCategories_filterTogetherAndCountsIgnoreSelection_FR02() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onToggleCategory(CategoryId.of("dachi"))
        advanceUntilIdle()

        val oneCategory = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(oneCategory.items.map { it.romaji })
            .containsExactly("Fudo Dachi", "Heisoku Dachi")
        // A nem kijelölt chip találatszáma NEM nulla: a szám a kijelölést figyelmen
        // kívül hagyva számolt, különben épp arra nem válaszolna, amire használják.
        assertThat(oneCategory.categories.single { it.id == CategoryId.of("tsuki") }.matchCount)
            .isEqualTo(1)
        assertThat(oneCategory.selectedCategories).containsExactly(CategoryId.of("dachi"))

        // Több kategória egyszerre (FR-02).
        viewModel.onToggleCategory(CategoryId.of("tsuki"))
        advanceUntilIdle()
        val bothCategories = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(bothCategories.items).hasSize(3)

        // Újra megnyomva kikapcsol.
        viewModel.onToggleCategory(CategoryId.of("dachi"))
        advanceUntilIdle()
        val onlyTsuki = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(onlyTsuki.items.map { it.romaji }).containsExactly("Seiken Chudan Tsuki")
        job.cancel()
    }

    @Test
    fun libraryList_savedStateHandle_restoresQueryAndCategories_FR02() = runTest {
        // Ugyanaz a SavedStateHandle, mint amit a forgatás után a rendszer visszaad.
        val handle = SavedStateHandle()
        val first = viewModel(savedStateHandle = handle)
        val firstJob = collectState(first)
        advanceUntilIdle()

        first.onQueryChange("dachi")
        first.onToggleCategory(CategoryId.of("dachi"))
        advanceUntilIdle()
        assertThat(first.uiState.value.query).isEqualTo("dachi")
        firstJob.cancel()

        val restored = viewModel(savedStateHandle = handle)
        val restoredJob = collectState(restored)
        advanceUntilIdle()

        val state = restored.uiState.value as LibraryListUiState.Content
        assertThat(state.query).isEqualTo("dachi")
        assertThat(state.selectedCategories).containsExactly(CategoryId.of("dachi"))
        assertThat(state.items.map { it.romaji }).containsExactly("Fudo Dachi", "Heisoku Dachi")
        restoredJob.cancel()
    }

    @Test
    fun libraryList_clearFilters_restoresFullList_FR02() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onQueryChange("dachi")
        viewModel.onToggleCategory(CategoryId.of("tsuki"))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LibraryListUiState.Empty::class.java)

        viewModel.onClearFilters()
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(content.items).hasSize(3)
        assertThat(content.query).isEmpty()
        assertThat(content.selectedCategories).isEmpty()
        job.cancel()
    }

    // ------------------------------------------------------------------ keresés / FR-03

    @Test
    fun libraryList_searchIgnoresAccents_FR03() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        // US-06 1. kritérium: „zart labu" megtalálja a „Zárt lábú állás"-t.
        viewModel.onQueryChange("zart labu")
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(content.items.map { it.romaji }).containsExactly("Heisoku Dachi")
        job.cancel()
    }

    @Test
    fun libraryList_searchIgnoresCaseAndMatchesSubstring_FR03() = runTest {
        val repository = FakeTechniqueRepository()
        val viewModel = viewModel(repository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()

        // US-06 2. kritérium: „DACHI" nagybetűvel, rész-szóként.
        viewModel.onQueryChange("DACHI")
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(content.items.map { it.romaji })
            .containsExactly("Fudo Dachi", "Heisoku Dachi")
        // A normalizálás a repository (SQL) dolga: a ViewModel a NYERS szöveget adja át.
        assertThat(repository.observedQueries).contains("DACHI")
        job.cancel()
    }

    @Test
    fun libraryList_aliasMatch_reportsMatchedAlias_FR03() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onQueryChange("chudan zuki")
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryListUiState.Content
        // US-06 3. kritérium: az „Egyezés: {alias}" sor forrása.
        assertThat(content.items.single().matchedAlias).isEqualTo("chudan zuki")
        job.cancel()
    }

    @Test
    fun libraryList_nameMatch_hasNoMatchedAlias_FR03() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onQueryChange("Seiken")
        advanceUntilIdle()

        // Saját névből jött a találat: nincs mit kiírni.
        val content = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(content.items.single().matchedAlias).isNull()
        job.cancel()
    }

    @Test
    fun libraryList_typingIsDebounced_FR03() = runTest {
        val repository = FakeTechniqueRepository()
        val viewModel = viewModel(repository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()
        val beforeTyping = repository.observedQueries.size

        // Négy leütés 200 ms-on belül: csak az utolsó ér le a repositoryig.
        listOf("d", "da", "dac", "dach").forEach { prefix ->
            viewModel.onQueryChange(prefix)
            advanceTimeBy(50)
        }
        advanceUntilIdle()

        val issued = repository.observedQueries.drop(beforeTyping)
        assertThat(issued).containsExactly("dach")
        job.cancel()
    }

    @Test
    fun libraryList_clearingQueryIsNotDebounced_FR03() = runTest {
        val repository = FakeTechniqueRepository()
        val viewModel = viewModel(repository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onQueryChange("dachi")
        advanceUntilIdle()
        val beforeClear = repository.observedQueries.size

        // A „×" megnyomása visszavonás, nem újabb keresés: nincs 200 ms várakozás.
        viewModel.onQueryChange("")
        advanceTimeBy(1)

        assertThat(repository.observedQueries.drop(beforeClear)).containsExactly("")
        advanceUntilIdle()
        job.cancel()
    }

    // ------------------------------------------------- üres állapot oka / FR-01, FR-03

    @Test
    fun libraryList_withoutContent_emptyReasonIsNoContent_FR01() = runTest {
        val viewModel = viewModel(
            repository = FakeTechniqueRepository(
                categories = emptyList(),
                techniques = emptyList(),
                contentState = ContentState.NotImported,
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val empty = viewModel.uiState.value as LibraryListUiState.Empty
        assertThat(empty.reason).isEqualTo(EmptyStateReason.LibraryNoContent)
        assertThat(empty.reason.message(resources))
            .isEqualTo("A tudástár üres. A tartalom betöltése nem sikerült.")
        job.cancel()
    }

    @Test
    fun libraryList_withContentButNoMatch_emptyReasonCarriesQuery_FR03() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onQueryChange("tornado")
        advanceUntilIdle()

        val empty = viewModel.uiState.value as LibraryListUiState.Empty
        // US-06 4. kritérium: a konkrét kifejezés benne van az üzenetben.
        assertThat(empty.reason).isEqualTo(EmptyStateReason.LibraryNoResult("tornado"))
        assertThat(empty.reason.message(resources)).contains("tornado")
        // A keresősáv és a chipek az üres állapotban is megvannak – ez a kiút.
        assertThat(empty.query).isEqualTo("tornado")
        assertThat(empty.categories).hasSize(2)
        job.cancel()
    }

    @Test
    fun libraryList_retiredTechniqueStaysOutOfList_FR01() = runTest {
        val viewModel = viewModel(
            repository = FakeTechniqueRepository(
                techniques = ALL_TECHNIQUES + technique(
                    id = "KY-0099",
                    categoryId = "dachi",
                    romaji = "Retired Dachi",
                    hungarian = "Visszavont állás",
                    retired = true,
                ),
                contentState = importedState(ALL_TECHNIQUES.size),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryListUiState.Content
        assertThat(content.items.map { it.romaji }).doesNotContain("Retired Dachi")
        job.cancel()
    }

    @Test
    fun libraryList_exposesDatasetTrustForUnreviewedBanner_FR01() = runTest {
        val reviewed = viewModel(
            repository = FakeTechniqueRepository(
                contentState = importedState(ALL_TECHNIQUES.size, DatasetTrust.REVIEWED),
            ),
        )
        val job = collectState(reviewed)
        advanceUntilIdle()

        // A szalag bemenete a tartalom bizalmi szintje, nem felhasználói beállítás
        // (ADR-010, AC-08).
        assertThat(reviewed.uiState.value.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
        job.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun TestScope.viewModel(
        repository: FakeTechniqueRepository = FakeTechniqueRepository(),
        importer: FakeContentImporter = FakeContentImporter(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        blockingImport: Job? = null,
    ): LibraryListViewModel = LibraryListViewModel(
        repository = repository,
        contentImporter = if (blockingImport == null) {
            importer
        } else {
            GatedContentImporter(blockingImport)
        },
        // A `runTest` saját `testScheduler`-ét kell átadni, különben a `combine` + `flowOn`
        // lánc „Detected use of different schedulers" hibával elhasal (CONVENTIONS 3.).
        defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: LibraryListViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
}
