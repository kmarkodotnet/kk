package hu.kyokushin.trainer.feature.library.detail

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.status.TechniqueField
import hu.kyokushin.trainer.feature.library.list.ALL_TECHNIQUES
import hu.kyokushin.trainer.feature.library.list.CATEGORY_DACHI
import hu.kyokushin.trainer.feature.library.list.FakeTechniqueRepository
import hu.kyokushin.trainer.feature.library.list.TECHNIQUE_HEISOKU
import hu.kyokushin.trainer.feature.library.list.importedState
import hu.kyokushin.trainer.feature.library.list.technique
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
 * A Tudástár részletező ViewModel viselkedése – emulátor nélkül (T-209, FR-04, FR-05,
 * US-07, US-08).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket (CONVENTIONS 3.: `@Config(sdk = [34], qualifiers = "hu")`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "hu")
class LibraryDetailViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun libraryDetail_loadsAllPresentFields_FR04() = runTest {
        val viewModel = viewModel(techniqueId = TECHNIQUE_HEISOKU.id)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryDetailUiState.Content
        assertThat(content.id).isEqualTo(TECHNIQUE_HEISOKU.id)
        assertThat(content.romaji).isEqualTo("Heisoku Dachi")
        assertThat(content.hungarian).isEqualTo("Zárt lábú állás")
        // A kategórianév a tartalomból feloldva, nem `strings.xml`-ből (ADR-011).
        assertThat(content.categoryNameHu).isEqualTo(CATEGORY_DACHI.nameHu)
        assertThat(content.categoryId).isEqualTo(CATEGORY_DACHI.id)
        job.cancel()
    }

    @Test
    fun libraryDetail_optionalFieldsAbsent_stayNullOrEmpty_FR04() = runTest {
        // A valódi tartalomban `japaneseScript`, `descriptionHu` és `aliases` jellemzően
        // hiányzik (CONVENTIONS 8.) – a képernyő ekkor nem jelenít meg szakaszt
        // (FUNCTIONAL_SPEC 3.2 „Figyelj rá"), ami a ViewModel oldaláról annyit jelent,
        // hogy ezek a mezők üresek/null maradnak, nem helyettesítődnek placeholder
        // szöveggel.
        val viewModel = viewModel(techniqueId = TECHNIQUE_HEISOKU.id)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryDetailUiState.Content
        assertThat(content.japaneseScript).isNull()
        assertThat(content.descriptionHu).isNull()
        assertThat(content.aliases).isEmpty()
        job.cancel()
    }

    @Test
    fun libraryDetail_incompleteRecord_missingFieldsNameTheIllustration_FR05() = runTest {
        val incomplete = technique(
            id = "KY-0099",
            categoryId = "dachi",
            romaji = "Hiányos Dachi",
            hungarian = "Hiányos állás",
            status = VerificationStatus.INCOMPLETE,
        )
        val repository = FakeTechniqueRepository(techniques = ALL_TECHNIQUES + incomplete)
        val viewModel = viewModel(repository = repository, techniqueId = incomplete.id)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryDetailUiState.Content
        // US-08 3. kritérium: a hiányos rekord jelzi a hiányzó mezőt és a kvízkizárást.
        // A jelenlegi validátor kizárólag a lógó képhivatkozás miatt fokoz le
        // INCOMPLETE-re (lásd `LibraryDetailViewModel` dokumentációja).
        assertThat(content.status).isEqualTo(VerificationStatus.INCOMPLETE)
        assertThat(content.missingFields).containsExactly(TechniqueField.ILLUSTRATION)
        job.cancel()
    }

    @Test
    fun libraryDetail_verifiedRecord_hasNoMissingFields_FR05() = runTest {
        val viewModel = viewModel(techniqueId = TECHNIQUE_HEISOKU.id)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LibraryDetailUiState.Content
        assertThat(content.status).isEqualTo(VerificationStatus.VERIFIED)
        assertThat(content.missingFields).isEmpty()
        job.cancel()
    }

    @Test
    fun libraryDetail_unknownTechniqueId_showsErrorInsteadOfCrashing_US07() = runTest {
        val viewModel = viewModel(techniqueId = TechniqueId.of("KY-DOES-NOT-EXIST"))
        val job = collectState(viewModel)
        advanceUntilIdle()

        val error = viewModel.uiState.value as LibraryDetailUiState.Error
        assertThat(error.messageRes).isEqualTo(
            hu.kyokushin.trainer.feature.library.R.string.library_detail_not_found,
        )
        job.cancel()
    }

    @Test
    fun libraryDetail_repositoryThrows_showsErrorAndRetryRecovers_US07() = runTest {
        var fail = true
        val repository = FailingGetTechniqueRepository(
            delegate = FakeTechniqueRepository(),
            shouldFailProvider = { fail },
        )
        val viewModel = viewModel(repository = repository, techniqueId = TECHNIQUE_HEISOKU.id)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val error = viewModel.uiState.value as LibraryDetailUiState.Error
        // `@StringRes`, nem a kivétel angol üzenete (NFR-02).
        assertThat(error.messageRes).isEqualTo(R.string.error_load_failed)

        fail = false
        viewModel.onRetry()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LibraryDetailUiState.Content::class.java)
        job.cancel()
    }

    @Test
    fun libraryDetail_exposesDatasetTrustForUnreviewedBanner_US08() = runTest {
        val repository = FakeTechniqueRepository(
            contentState = importedState(ALL_TECHNIQUES.size, DatasetTrust.REVIEWED),
        )
        val viewModel = viewModel(repository = repository, techniqueId = TECHNIQUE_HEISOKU.id)
        val job = collectState(viewModel)
        advanceUntilIdle()

        // A szalag bemenete a tartalom bizalmi szintje, nem felhasználói beállítás
        // (ADR-010, AC-08) – ugyanaz a szabály, mint a listánál.
        assertThat(viewModel.uiState.value.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
        job.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun TestScope.viewModel(
        repository: TechniqueRepository = FakeTechniqueRepository(),
        techniqueId: TechniqueId,
    ): LibraryDetailViewModel = LibraryDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf(KEY_TECHNIQUE_ID to techniqueId.value)),
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: LibraryDetailViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    private companion object {
        /** Meg kell egyeznie a `navigation` modul `NavArgs.TECHNIQUE_ID` értékével. */
        const val KEY_TECHNIQUE_ID = "techniqueId"
    }
}

/**
 * [TechniqueRepository] dublőr, amelynek [getTechnique] a hívó kérésére kivételt dob –
 * a `LibraryListFakes.kt` dublőrjei ezt nem teszik, mert a lista ViewModelje sosem hívja
 * ezt a metódust.
 */
private class FailingGetTechniqueRepository(
    private val delegate: TechniqueRepository,
    private val shouldFailProvider: () -> Boolean,
) : TechniqueRepository by delegate {
    override suspend fun getTechnique(id: TechniqueId): Technique? {
        if (shouldFailProvider()) throw IllegalStateException("getTechnique failed (test)")
        return delegate.getTechnique(id)
    }
}
