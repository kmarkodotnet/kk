package hu.kyokushin.trainer.feature.quiz.setup

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.content.ImportResult
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.generator.DefaultQuizGenerator
import hu.kyokushin.trainer.core.quiz.generator.QuizGenerator
import hu.kyokushin.trainer.core.ui.R as CoreUiR
import hu.kyokushin.trainer.feature.quiz.ALL_QUIZ_CATEGORIES
import hu.kyokushin.trainer.feature.quiz.ALL_QUIZ_TECHNIQUES
import hu.kyokushin.trainer.feature.quiz.CATEGORY_DACHI
import hu.kyokushin.trainer.feature.quiz.FakeQuizContentImporter
import hu.kyokushin.trainer.feature.quiz.FakeQuizProgressRepository
import hu.kyokushin.trainer.feature.quiz.FakeQuizTechniqueRepository
import hu.kyokushin.trainer.feature.quiz.GatedQuizContentImporter
import hu.kyokushin.trainer.feature.quiz.R
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_CHUDAN
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_FUDO
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_HEISOKU
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_JODAN
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_MUSUBI
import hu.kyokushin.trainer.feature.quiz.quizImportedState
import hu.kyokushin.trainer.feature.quiz.quizTechnique
import hu.kyokushin.trainer.feature.quiz.quizUsableIllustration
import kotlin.random.Random
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
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A kvízbeállító ViewModel viselkedése – emulátor nélkül (T-403, FR-11, FR-12, FR-13,
 * FR-22, TR-03).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket (CONVENTIONS 3.). A kvízmotor ([DefaultQuizGenerator]) **valódi**,
 * nem dublőrözött: a `dryRun`/`generate` egyezés (TR-03, T-111) csak így bizonyítható
 * hitelesen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuizSetupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --------------------------------------------------------------- betöltés / FR-11

    @Test
    fun quizSetup_whileImportRunning_showsLoading_FR11() = runTest {
        val gate = Job()
        val viewModel = viewModel(blockingImport = gate)

        val job = collectState(viewModel)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(QuizSetupUiState.Loading)

        gate.complete()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(QuizSetupUiState.Content::class.java)
        job.cancel()
    }

    @Test
    fun quizSetup_importThrows_showsErrorAndRetryRecovers_FR11() = runTest {
        var fail = true
        val viewModel = viewModel(
            importer = FakeQuizContentImporter {
                if (fail) throw IllegalStateException("import failed") else ImportResult.UpToDate("test-1")
            },
        )

        val job = collectState(viewModel)
        advanceUntilIdle()

        val error = viewModel.uiState.value as QuizSetupUiState.Error
        assertThat(error.messageRes).isEqualTo(CoreUiR.string.error_load_failed)

        fail = false
        viewModel.onRetry()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(QuizSetupUiState.Content::class.java)
        job.cancel()
    }

    // --------------------------------------------------------- alapértelmezett állapot / FR-11, FR-12

    @Test
    fun quizSetup_defaultState_emptySelectionAllTypesTenQuestions_FR11() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        // Üres kijelölés = minden kategória (core/quiz QuizRequest KDoc, FR-11).
        assertThat(content.selectedCategories).isEmpty()
        assertThat(content.requestedCount).isEqualTo(10)
        assertThat(content.questionTypes.filter { it.selected }.map { it.type })
            .containsExactlyElementsIn(QuestionType.entries)
        job.cancel()
    }

    @Test
    fun quizSetup_categoryCounts_quizEligibleCountExcludesIncomplete_totalCountDoesNot_FR11() = runTest {
        val incomplete = quizTechnique(
            id = "KY-0099",
            categoryId = "dachi",
            romaji = "Incomplete Dachi",
            hungarian = "Hiányos állás",
            status = VerificationStatus.INCOMPLETE,
        )
        val viewModel = viewModel(
            repository = FakeQuizTechniqueRepository(techniques = ALL_QUIZ_TECHNIQUES + incomplete),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        val dachi = content.categories.first { it.id == CATEGORY_DACHI.id }
        // Heisoku + Fudo + Musubi + Incomplete = 4 technika, de az utóbbi INCOMPLETE,
        // ezért nem kvízképes (ADR-010).
        assertThat(dachi.totalCount).isEqualTo(4)
        assertThat(dachi.quizEligibleCount).isEqualTo(3)
        job.cancel()
    }

    @Test
    fun quizSetup_retiredTechniqueIsNotCounted_FR11() = runTest {
        val viewModel = viewModel(
            repository = FakeQuizTechniqueRepository(
                techniques = ALL_QUIZ_TECHNIQUES + quizTechnique(
                    id = "KY-0098",
                    categoryId = "dachi",
                    romaji = "Retired Dachi",
                    hungarian = "Visszavont állás",
                    retired = true,
                ),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        assertThat(content.categories.first { it.id == CATEGORY_DACHI.id }.totalCount).isEqualTo(3)
        job.cancel()
    }

    // --------------------------------------------------------- élő készenlét / FR-13, FR-22, TR-03

    @Test
    fun quizSetup_noImages_disablesBothImageTypesWithReason_FR13() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        val imageTypes = content.questionTypes.filter {
            it.type == QuestionType.IMAGE_TO_JP || it.type == QuestionType.JP_TO_IMAGE
        }
        assertThat(imageTypes).hasSize(2)
        imageTypes.forEach { typeModel ->
            assertThat(typeModel.enabled).isFalse()
            assertThat(typeModel.disabledReasonRes).isEqualTo(R.string.quiz_setup_type_disabled_images)
            assertThat(typeModel.disabledReasonArg).isEqualTo(0)
        }
        val textTypes = content.questionTypes.filter {
            it.type == QuestionType.JP_TO_HU || it.type == QuestionType.HU_TO_JP
        }
        textTypes.forEach { typeModel ->
            assertThat(typeModel.enabled).isTrue()
            assertThat(typeModel.disabledReasonRes).isNull()
        }
        job.cancel()
    }

    @Test
    fun quizSetup_enoughDistinctImages_enablesImageTypes_FR13() = runTest {
        val illustrated = listOf(
            TECHNIQUE_HEISOKU.copy(illustration = quizUsableIllustration("KY-0001")),
            TECHNIQUE_FUDO.copy(illustration = quizUsableIllustration("KY-0002")),
            TECHNIQUE_MUSUBI.copy(illustration = quizUsableIllustration("KY-0003")),
            TECHNIQUE_CHUDAN.copy(illustration = quizUsableIllustration("KY-0004")),
            TECHNIQUE_JODAN.copy(illustration = quizUsableIllustration("KY-0005")),
        )
        val viewModel = viewModel(repository = FakeQuizTechniqueRepository(techniques = illustrated))
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        val jpToImage = content.questionTypes.first { it.type == QuestionType.JP_TO_IMAGE }
        assertThat(jpToImage.enabled).isTrue()
        assertThat(jpToImage.disabledReasonRes).isNull()
        job.cancel()
    }

    @Test
    fun quizSetup_expectedQuestionCount_isNotEmptyWithDefaultContent_FR22() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        // 5 technika, A/B típus mindkettőnél helyes válasz lehet, de a motor technikánként
        // dedupol: 5 különböző helyes technika -> min(10, 5) = 5.
        assertThat(content.expectedQuestionCount).isEqualTo(5)
        assertThat(content.canStart).isTrue()
        assertThat(content.shortfall).isNotNull()
        assertThat(content.shortfall?.actualCount).isEqualTo(5)
        job.cancel()
    }

    @Test
    fun quizSetup_toggleCategory_narrowsLiveRequest_FR11() = runTest {
        // Egy négyopciós kérdéshez (helyes + 3 distraktor) legalább 4 egymástól
        // különböző, kvízképes technika kell egy típuson belül (QuizQuestion.OPTION_COUNT,
        // DistractorSelector). Az "állások" kategória az alap öttechnikás fixtúrában csak
        // 3 technikát tartalmaz (Heisoku, Fudo, Musubi) – ennyivel egyetlen kérdés sem áll
        // össze (ez a motor dokumentált, nem-optimista viselkedése, T-403 "Figyelj rá").
        // A szűrés tényleges hatásának bizonyításához ezért egy negyedik, kvízképes
        // "állások" technikát is felveszünk a jelöltkészletbe.
        val extraDachi = quizTechnique(
            id = "KY-0097",
            categoryId = "dachi",
            romaji = "Sanchin Dachi",
            hungarian = "Sanchin állás",
        )
        val viewModel = viewModel(
            repository = FakeQuizTechniqueRepository(techniques = ALL_QUIZ_TECHNIQUES + extraDachi),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val fullCount = (viewModel.uiState.value as QuizSetupUiState.Content).expectedQuestionCount

        viewModel.onToggleCategory(CATEGORY_DACHI.id)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        assertThat(content.selectedCategories).containsExactly(CATEGORY_DACHI.id)
        // Most már 4 technika van az "állások" kategóriában – pontosan annyi, amennyi
        // egyetlen (A)/(B) típusú kérdéshez minimálisan kell, tehát a dryRun valódi,
        // nullától eltérő, a teljes (6 technikás) készlettől kisebb értéket ad.
        assertThat(content.expectedQuestionCount).isEqualTo(4)
        assertThat(content.expectedQuestionCount).isLessThan(fullCount)
        job.cancel()
    }

    @Test
    fun quizSetup_allTypesDisabled_cannotStartWithNotEnoughDataMessage_FR22() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        QuestionType.entries.forEach { viewModel.onToggleType(it) }
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        assertThat(content.questionTypes.none { it.selected }).isTrue()
        assertThat(content.expectedQuestionCount).isEqualTo(0)
        assertThat(content.canStart).isFalse()
        // FUNCTIONAL_SPEC 8.: `quiz.notEnoughData` – egyetlen, szó szerinti üzenet, nem
        // három kitalált javaslat-sztring (CONVENTIONS 4.).
        assertThat(content.notEnoughDataMessageRes)
            .isEqualTo(R.string.quiz_setup_message_not_enough_data)
        job.cancel()
    }

    @Test
    fun quizSetup_canStart_hasNoNotEnoughDataMessage_FR22() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        assertThat(content.canStart).isTrue()
        assertThat(content.notEnoughDataMessageRes).isNull()
        job.cancel()
    }

    // --------------------------------------------------------------- kérdésszám / FR-11

    @Test
    fun quizSetup_selectRequestedCount_onlyAllowsFiveTenTwenty_FR11() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onSelectRequestedCount(20)
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as QuizSetupUiState.Content).requestedCount).isEqualTo(20)

        viewModel.onSelectRequestedCount(5)
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as QuizSetupUiState.Content).requestedCount).isEqualTo(5)

        assertThrows(IllegalArgumentException::class.java) {
            viewModel.onSelectRequestedCount(7)
        }
        job.cancel()
    }

    // --------------------------------------------------------------- kérdéstípus / FR-12

    @Test
    fun quizSetup_toggleType_updatesSelection_FR12() = runTest {
        val viewModel = viewModel()
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onToggleType(QuestionType.HU_TO_JP)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizSetupUiState.Content
        assertThat(content.questionTypes.first { it.type == QuestionType.HU_TO_JP }.selected).isFalse()
        assertThat(content.questionTypes.first { it.type == QuestionType.JP_TO_HU }.selected).isTrue()
        job.cancel()
    }

    // ------------------------------------------------- SavedStateHandle megőrzés / FR-11, FR-12

    @Test
    fun quizSetup_savedStateHandle_restoresSelectionAfterRecreate_FR11() = runTest {
        val handle = SavedStateHandle()
        val first = viewModel(savedStateHandle = handle)
        val firstJob = collectState(first)
        advanceUntilIdle()

        first.onToggleCategory(CATEGORY_DACHI.id)
        first.onSelectRequestedCount(20)
        first.onToggleType(QuestionType.JP_TO_IMAGE)
        advanceUntilIdle()
        firstJob.cancel()

        val restored = viewModel(savedStateHandle = handle)
        val restoredJob = collectState(restored)
        advanceUntilIdle()

        val content = restored.uiState.value as QuizSetupUiState.Content
        assertThat(content.selectedCategories).containsExactly(CATEGORY_DACHI.id)
        assertThat(content.requestedCount).isEqualTo(20)
        assertThat(content.questionTypes.first { it.type == QuestionType.JP_TO_IMAGE }.selected).isFalse()
        restoredJob.cancel()
    }

    // ----------------------------------------------------------------- indítás / FR-19, TR-03

    @Test
    fun quizSetup_onStartQuiz_drawsSeedFromInjectedRandom_startsSessionAndEmitsPlan_FR19() = runTest {
        val expectedSeed = Random(SEED).nextLong()
        val progressRepository = FakeQuizProgressRepository()
        val viewModel = viewModel(
            progressRepository = progressRepository,
            random = Random(SEED),
        )
        val stateJob = collectState(viewModel)
        val events = mutableListOf<QuizStartResult>()
        val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.startEvent.collect { events += it }
        }
        advanceUntilIdle()

        val before = viewModel.uiState.value as QuizSetupUiState.Content
        viewModel.onStartQuiz()
        advanceUntilIdle()

        assertThat(progressRepository.startSessionCalls).containsExactly(10 to expectedSeed)
        assertThat(events).hasSize(1)
        val result = events.single()
        assertThat(result.sessionId).isEqualTo(SessionId.of("session-1"))
        // T-403 "Kész ha" 2.: a kiírt kérdésszám megegyezik a tényleges kvíz kérdésszámával.
        assertThat(result.plan.questionCount).isEqualTo(before.expectedQuestionCount)
        stateJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun quizSetup_onStartQuiz_noopWhenCannotStart_FR22() = runTest {
        val progressRepository = FakeQuizProgressRepository()
        val viewModel = viewModel(progressRepository = progressRepository)
        val stateJob = collectState(viewModel)
        val events = mutableListOf<QuizStartResult>()
        val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.startEvent.collect { events += it }
        }
        advanceUntilIdle()

        QuestionType.entries.forEach { viewModel.onToggleType(it) }
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as QuizSetupUiState.Content).canStart).isFalse()

        viewModel.onStartQuiz()
        advanceUntilIdle()

        assertThat(progressRepository.startSessionCalls).isEmpty()
        assertThat(events).isEmpty()
        stateJob.cancel()
        eventJob.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun TestScope.viewModel(
        repository: FakeQuizTechniqueRepository = FakeQuizTechniqueRepository(),
        importer: FakeQuizContentImporter = FakeQuizContentImporter(),
        progressRepository: FakeQuizProgressRepository = FakeQuizProgressRepository(),
        quizGenerator: QuizGenerator = DefaultQuizGenerator,
        random: Random = Random(SEED),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        blockingImport: Job? = null,
    ): QuizSetupViewModel = QuizSetupViewModel(
        repository = repository,
        contentImporter = if (blockingImport == null) importer else GatedQuizContentImporter(blockingImport),
        progressRepository = progressRepository,
        quizGenerator = quizGenerator,
        random = random,
        defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: QuizSetupViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    private companion object {
        const val SEED = 42L
    }
}
