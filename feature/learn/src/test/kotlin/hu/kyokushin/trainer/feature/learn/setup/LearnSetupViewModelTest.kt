package hu.kyokushin.trainer.feature.learn.setup

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.content.ImportResult
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.feature.learn.ALL_LEARN_CATEGORIES
import hu.kyokushin.trainer.feature.learn.ALL_LEARN_TECHNIQUES
import hu.kyokushin.trainer.feature.learn.FakeLearnContentImporter
import hu.kyokushin.trainer.feature.learn.FakeLearnTechniqueRepository
import hu.kyokushin.trainer.feature.learn.GatedLearnContentImporter
import hu.kyokushin.trainer.feature.learn.learnImportedState
import hu.kyokushin.trainer.feature.learn.learnTechnique
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
import hu.kyokushin.trainer.feature.learn.R as LearnR

/**
 * A tanulás-beállító ViewModel viselkedése – emulátor nélkül (T-302, FR-06, FR-07).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket (CONVENTIONS 3.). `@Config(sdk = [34])` – a `compileSdk = 37`-hez nincs
 * `android-all` csomag.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearnSetupViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --------------------------------------------------------------- betöltés / FR-06

    @Test
    fun learnSetup_whileImportRunning_showsLoading_FR06() = runTest {
        val gate = Job()
        val viewModel = viewModel(blockingImport = gate)

        val job = collectState(viewModel)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(LearnSetupUiState.Loading)

        gate.complete()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LearnSetupUiState.Content::class.java)
        job.cancel()
    }

    @Test
    fun learnSetup_importThrows_showsErrorAndRetryRecovers_FR06() = runTest {
        var fail = true
        val viewModel = viewModel(
            importer = FakeLearnContentImporter {
                if (fail) throw IllegalStateException("import failed") else ImportResult.UpToDate("test-1")
            },
        )

        val job = collectState(viewModel)
        advanceUntilIdle()

        val error = viewModel.uiState.value as LearnSetupUiState.Error
        // `@StringRes`, nem a kivétel angol üzenete (NFR-02).
        assertThat(error.messageRes).isEqualTo(R.string.error_load_failed)

        fail = false
        viewModel.onRetry()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(LearnSetupUiState.Content::class.java)
        job.cancel()
    }

    @Test
    fun learnSetup_categoryNamesAndCountsComeFromContent_FR06() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.categories.map { it.nameHu }).containsExactly("Állások", "Ütések").inOrder()
        // „Állások": KY-0001 + KY-0002 = 2 technika, a hitelesítési státusztól függetlenül
        // (a Fudo Dachi `AWAITING_REVIEW`, de a tanuláshoz ez nem szűrő, T-302 „Kész ha").
        assertThat(content.categories.first { it.nameHu == "Állások" }.displayableCount).isEqualTo(2)
        assertThat(content.categories.first { it.nameHu == "Ütések" }.displayableCount).isEqualTo(1)
        job.cancel()
    }

    @Test
    fun learnSetup_retiredTechniqueIsNotCounted_FR06() = runTest {
        val viewModel = viewModel(
            repository = FakeLearnTechniqueRepository(
                techniques = ALL_LEARN_TECHNIQUES + learnTechnique(
                    id = "KY-0099",
                    categoryId = "dachi",
                    romaji = "Retired Dachi",
                    hungarian = "Visszavont állás",
                    retired = true,
                ),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.categories.first { it.nameHu == "Állások" }.displayableCount).isEqualTo(2)
        job.cancel()
    }

    @Test
    fun learnSetup_exposesDatasetTrustForUnreviewedBanner_FR06() = runTest {
        val viewModel = viewModel(
            repository = FakeLearnTechniqueRepository(
                contentState = learnImportedState(ALL_LEARN_TECHNIQUES.size, DatasetTrust.REVIEWED),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
        job.cancel()
    }

    // ------------------------------------------------------- kijelölés és indíthatóság / FR-06

    @Test
    fun learnSetup_noCategorySelected_cannotStartWithReason_FR06() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.selectedCategories).isEmpty()
        assertThat(content.canStart).isFalse()
        assertThat(content.disabledReasonRes)
            .isEqualTo(LearnR.string.learn_setup_start_disabled_no_category)
        job.cancel()
    }

    @Test
    fun learnSetup_selectingCategoryWithTechniquesEnablesStart_FR06() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onToggleCategory(CategoryId.of("dachi"))
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.selectedCategories).containsExactly(CategoryId.of("dachi"))
        assertThat(content.canStart).isTrue()
        assertThat(content.disabledReasonRes).isNull()
        job.cancel()
    }

    @Test
    fun learnSetup_toggleCategoryTwiceClearsSelection_FR06() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onToggleCategory(CategoryId.of("dachi"))
        viewModel.onToggleCategory(CategoryId.of("dachi"))
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.selectedCategories).isEmpty()
        assertThat(content.canStart).isFalse()
        job.cancel()
    }

    @Test
    fun learnSetup_multipleCategoriesSelectedTogether_FR06() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onToggleCategory(CategoryId.of("dachi"))
        viewModel.onToggleCategory(CategoryId.of("tsuki"))
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.selectedCategories)
            .containsExactly(CategoryId.of("dachi"), CategoryId.of("tsuki"))
        assertThat(content.canStart).isTrue()
        job.cancel()
    }

    @Test
    fun learnSetup_categoryWithoutDisplayableTechniques_cannotStartWithOtherReason_FR06() = runTest {
        val emptyCategoryId = CategoryId.of("keri")
        val viewModel = viewModel(
            repository = FakeLearnTechniqueRepository(
                categories = ALL_LEARN_CATEGORIES +
                    Category(id = emptyCategoryId, nameHu = "Rúgások", sortOrder = 3),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onToggleCategory(emptyCategoryId)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.canStart).isFalse()
        assertThat(content.disabledReasonRes)
            .isEqualTo(LearnR.string.learn_setup_start_disabled_no_technique)
        job.cancel()
    }

    // ------------------------------------------------- SavedStateHandle megőrzés / FR-06
    //
    // Az elrejtési mód (FR-07) tesztjei T-706 óta a `LearnCardsViewModelTest`-ben élnek –
    // a `learn/setup` már nem dönt erről, lásd `LearnSetupUiState` osztály KDoc-ja.

    @Test
    fun learnSetup_savedStateHandle_restoresSelectionAfterRecreate_FR06() = runTest {
        val handle = SavedStateHandle()
        val first = viewModel(savedStateHandle = handle)
        val firstJob = collectState(first)
        advanceUntilIdle()

        first.onToggleCategory(CategoryId.of("tsuki"))
        advanceUntilIdle()
        firstJob.cancel()

        // Ugyanaz a `SavedStateHandle`, mint amit a forgatás után vagy a mentett
        // állapotból helyreállított folyamatban a rendszer visszaad.
        val restored = viewModel(savedStateHandle = handle)
        val restoredJob = collectState(restored)
        advanceUntilIdle()

        val content = restored.uiState.value as LearnSetupUiState.Content
        assertThat(content.selectedCategories).containsExactly(CategoryId.of("tsuki"))
        restoredJob.cancel()
    }

    // ------------------------------------------------------- kategória-drilldown / FR-39

    @Test
    fun learnSetup_openingCategory_showsItsTechniquesInDrillDown_FR39() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onOpenCategory(CategoryId.of("dachi"))
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        val drillDown = checkNotNull(content.drillDown)
        assertThat(drillDown.categoryId).isEqualTo(CategoryId.of("dachi"))
        assertThat(drillDown.categoryName).isEqualTo("Állások")
        // Romaji szerint rendezve (ABC sorrend), nem a betöltési sorrendben.
        assertThat(drillDown.techniques.map { it.romaji })
            .containsExactly("Fudo Dachi", "Heisoku Dachi").inOrder()
        job.cancel()
    }

    @Test
    fun learnSetup_drillDown_reflectsWhetherTheCategoryIsSelected_FR39() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onOpenCategory(CategoryId.of("dachi"))
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnSetupUiState.Content).drillDown?.selected).isFalse()

        viewModel.onToggleCategory(CategoryId.of("dachi"))
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnSetupUiState.Content).drillDown?.selected).isTrue()
        job.cancel()
    }

    @Test
    fun learnSetup_closingDrillDown_returnsToTheGrid_FR39() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onOpenCategory(CategoryId.of("dachi"))
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnSetupUiState.Content).drillDown).isNotNull()

        viewModel.onCloseDrillDown()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnSetupUiState.Content).drillDown).isNull()
        job.cancel()
    }

    @Test
    fun learnSetup_drillDown_survivesSavedStateHandleRestore_FR39() = runTest {
        val handle = SavedStateHandle()
        val first = viewModel(savedStateHandle = handle)
        val firstJob = collectState(first)
        advanceUntilIdle()

        first.onOpenCategory(CategoryId.of("tsuki"))
        advanceUntilIdle()
        firstJob.cancel()

        val restored = viewModel(savedStateHandle = handle)
        val restoredJob = collectState(restored)
        advanceUntilIdle()

        val content = restored.uiState.value as LearnSetupUiState.Content
        assertThat(content.drillDown?.categoryId).isEqualTo(CategoryId.of("tsuki"))
        restoredJob.cancel()
    }

    @Test
    fun learnSetup_noCategoryOpened_drillDownIsNull_FR39() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnSetupUiState.Content
        assertThat(content.drillDown).isNull()
        job.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun TestScope.viewModel(
        repository: FakeLearnTechniqueRepository = FakeLearnTechniqueRepository(),
        importer: FakeLearnContentImporter = FakeLearnContentImporter(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        blockingImport: Job? = null,
    ): LearnSetupViewModel = LearnSetupViewModel(
        repository = repository,
        contentImporter = if (blockingImport == null) importer else GatedLearnContentImporter(blockingImport),
        defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: LearnSetupViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
}
