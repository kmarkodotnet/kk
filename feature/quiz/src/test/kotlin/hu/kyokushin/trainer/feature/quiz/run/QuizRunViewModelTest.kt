package hu.kyokushin.trainer.feature.quiz.run

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizPlan
import hu.kyokushin.trainer.core.quiz.generator.QuizQuestion
import hu.kyokushin.trainer.core.quiz.generator.QuizRunStateCodec
import hu.kyokushin.trainer.feature.quiz.FakeQuizRunProgressRepository
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
 * A futó kvíz állapotgépe – emulátor nélkül (T-405, T-406, T-410; FR-16, FR-18, FR-19,
 * G-07 csoport).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket (CONVENTIONS 3.). A bemenetet (sessionId/seed/terv) kézzel összerakott
 * `SavedStateHandle`-lel adjuk, ugyanúgy, mint a `LearnCardsViewModelTest`/
 * `QuizSetupViewModelTest` – a tényleges navigációs bekötés (`QuizRunViewModel` KDoc-ja)
 * egy későbbi feladat tárgya.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuizRunViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --------------------------------------------------------------------- betöltés / FR-16

    @Test
    fun quizRun_withValidPlan_showsFirstQuestion_FR16() = runTest {
        val plan = threeQuestionPlan()
        val viewModel = viewModel(savedStateHandle = handleFor(plan))
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizRunUiState.Content
        assertThat(content.question).isEqualTo(plan.questions[0])
        assertThat(content.position).isEqualTo(0)
        assertThat(content.total).isEqualTo(3)
        assertThat(content.answeredCount).isEqualTo(0)
        assertThat(content.selectedTechniqueId).isNull()
        assertThat(content.isLastQuestion).isFalse()
        assertThat(content.closeConfirmationVisible).isFalse()
        job.cancel()
    }

    @Test
    fun quizRun_missingInput_showsEmpty_NFR06() = runTest {
        // Nincs bemenet a SavedStateHandle-ben – ma még nem létezik hívó (lásd a
        // ViewModel KDoc-ja), de a ViewModel nem omlik össze, és nem ragad `Loading`-ban.
        val viewModel = viewModel(savedStateHandle = SavedStateHandle())
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(QuizRunUiState.Empty)
        job.cancel()
    }

    // ---------------------------------------------------------- forgatás túlélése / T-405 Kész ha

    @Test
    fun quizRun_afterRecreateWithSameSavedStateHandle_sameQuestionSameOptionOrder_FR19() = runTest {
        val plan = threeQuestionPlan()
        val handle = handleFor(plan)

        val first = viewModel(savedStateHandle = handle)
        val firstJob = collectState(first)
        advanceUntilIdle()
        first.onNext()
        advanceUntilIdle()
        val movedContent = first.uiState.value as QuizRunUiState.Content
        assertThat(movedContent.position).isEqualTo(1)
        firstJob.cancel()

        // Forgatás / mentett állapotból visszaállított folyamat: ugyanaz a handle.
        val restored = viewModel(savedStateHandle = handle)
        val restoredJob = collectState(restored)
        advanceUntilIdle()
        val restoredContent = restored.uiState.value as QuizRunUiState.Content

        assertThat(restoredContent.position).isEqualTo(movedContent.position)
        assertThat(restoredContent.question).isEqualTo(movedContent.question)
        // Az opciósorrend is a mentett terv része – nem újragenerált (ADR-006/007).
        assertThat(restoredContent.question.optionTechniqueIds)
            .isEqualTo(movedContent.question.optionTechniqueIds)
        restoredJob.cancel()
    }

    @Test
    fun quizRun_answersSurviveRecreateWithSameSavedStateHandle_FR19() = runTest {
        val plan = threeQuestionPlan()
        val handle = handleFor(plan)
        val repository = FakeQuizRunProgressRepository()

        val first = viewModel(savedStateHandle = handle, progressRepository = repository)
        val firstJob = collectState(first)
        advanceUntilIdle()
        val firstQuestion = (first.uiState.value as QuizRunUiState.Content).question
        first.onAnswer(firstQuestion.id, firstQuestion.correctTechniqueId)
        advanceUntilIdle()
        firstJob.cancel()

        val restored = viewModel(savedStateHandle = handle, progressRepository = repository)
        val restoredJob = collectState(restored)
        advanceUntilIdle()
        val restoredContent = restored.uiState.value as QuizRunUiState.Content

        assertThat(restoredContent.selectedTechniqueId).isEqualTo(firstQuestion.correctTechniqueId)
        assertThat(restoredContent.answeredCount).isEqualTo(1)
        restoredJob.cancel()
    }

    // ----------------------------------------------------------------------- válasz / T-406

    @Test
    fun quizRun_onAnswer_correctness_isIdentifierBased_notTextual_FR18() = runTest {
        val plan = threeQuestionPlan()
        val repository = FakeQuizRunProgressRepository()
        val viewModel = viewModel(savedStateHandle = handleFor(plan), progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()
        val question = (viewModel.uiState.value as QuizRunUiState.Content).question

        viewModel.onAnswer(question.id, question.correctTechniqueId)
        advanceUntilIdle()

        val attempt = repository.allAttempts.single()
        assertThat(attempt.selectedTechniqueId).isEqualTo(question.correctTechniqueId)
        assertThat(attempt.correct).isTrue()
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).selectedTechniqueId)
            .isEqualTo(question.correctTechniqueId)
        job.cancel()
    }

    @Test
    fun quizRun_onAnswer_wrongOption_recordsIncorrectById_FR18() = runTest {
        val plan = threeQuestionPlan()
        val repository = FakeQuizRunProgressRepository()
        val viewModel = viewModel(savedStateHandle = handleFor(plan), progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()
        val question = (viewModel.uiState.value as QuizRunUiState.Content).question
        val wrongOption = question.distractorTechniqueIds.first()

        viewModel.onAnswer(question.id, wrongOption)
        advanceUntilIdle()

        val attempt = repository.allAttempts.single()
        assertThat(attempt.selectedTechniqueId).isEqualTo(wrongOption)
        assertThat(attempt.correct).isFalse()
        job.cancel()
    }

    /**
     * T-406 „Kész ha" 1.: gyors dupla koppintásra egyetlen [hu.kyokushin.trainer.core.data.repository.NewAttempt]
     * keletkezik, és a KORÁBBAN rögzített válasz marad érvényben – ezt itt a ViewModel
     * saját, `answers` map-alapú ellenőrzése biztosítja (az első védelmi vonal, lásd a
     * ViewModel KDoc-ja): `UnconfinedTestDispatcher`-rel a második `onAnswer` hívásig az
     * első már lefutott, tehát a második már a map-ellenőrzésen akad el.
     */
    @Test
    fun quizRun_onAnswer_rapidDoubleTapOnSameQuestion_recordsOnlyFirstAttempt_FR18() = runTest {
        val plan = threeQuestionPlan()
        val repository = FakeQuizRunProgressRepository()
        val viewModel = viewModel(savedStateHandle = handleFor(plan), progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()
        val question = (viewModel.uiState.value as QuizRunUiState.Content).question
        val firstChoice = question.correctTechniqueId
        val secondChoice = question.distractorTechniqueIds.first()

        viewModel.onAnswer(question.id, firstChoice)
        viewModel.onAnswer(question.id, secondChoice)
        advanceUntilIdle()

        assertThat(repository.allAttempts).hasSize(1)
        assertThat(repository.allAttempts.single().selectedTechniqueId).isEqualTo(firstChoice)
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).selectedTechniqueId)
            .isEqualTo(firstChoice)
        job.cancel()
    }

    /**
     * T-406 „Teendők" 2.: a `recordAttempt` `false` válasza (a DB-szintű, második
     * védelmi vonal elutasítása) nem módosítja a ViewModel állapotát.
     */
    @Test
    fun quizRun_onAnswer_whenRepositoryRejects_doesNotChangeState_FR18() = runTest {
        val plan = threeQuestionPlan()
        val repository = FakeQuizRunProgressRepository(forceRecordResult = false)
        val viewModel = viewModel(savedStateHandle = handleFor(plan), progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()
        val question = (viewModel.uiState.value as QuizRunUiState.Content).question

        viewModel.onAnswer(question.id, question.correctTechniqueId)
        advanceUntilIdle()

        // A kísérlet megtörtént (a repository tudja róla), de az elutasítás miatt a
        // kérdés a ViewModel szerint továbbra is megválaszolatlan.
        assertThat(repository.allAttempts).hasSize(1)
        val content = viewModel.uiState.value as QuizRunUiState.Content
        assertThat(content.selectedTechniqueId).isNull()
        assertThat(content.answeredCount).isEqualTo(0)
        job.cancel()
    }

    @Test
    fun quizRun_onAnswer_unknownQuestionId_isNoOp() = runTest {
        val plan = threeQuestionPlan()
        val repository = FakeQuizRunProgressRepository()
        val viewModel = viewModel(savedStateHandle = handleFor(plan), progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onAnswer(QuestionId.of("Q-nonexistent"), plan.questions[0].correctTechniqueId)
        advanceUntilIdle()

        assertThat(repository.allAttempts).isEmpty()
        job.cancel()
    }

    // ------------------------------------------------------------- előrehaladás / T-410

    @Test
    fun quizRun_onNext_advancesPosition_FR16() = runTest {
        val plan = threeQuestionPlan()
        val viewModel = viewModel(savedStateHandle = handleFor(plan))
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onNext()
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as QuizRunUiState.Content).position).isEqualTo(1)
        job.cancel()
    }

    /** T-410 „Teendők": „a kérdések között nincs visszalépés" – és az utolsón túl sem megy. */
    @Test
    fun quizRun_onNext_atLastQuestion_isNoOp() = runTest {
        val plan = threeQuestionPlan()
        val viewModel = viewModel(savedStateHandle = handleFor(plan))
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onNext()
        viewModel.onNext()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).isLastQuestion).isTrue()

        viewModel.onNext()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).position).isEqualTo(2)
        job.cancel()
    }

    // --------------------------------------------------------------- megszakítás / T-410

    @Test
    fun quizRun_onRequestClose_showsConfirmation_FUNCTIONAL_SPEC_5_2() = runTest {
        val viewModel = viewModel(savedStateHandle = handleFor(threeQuestionPlan()))
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onRequestClose()
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as QuizRunUiState.Content).closeConfirmationVisible).isTrue()
        job.cancel()
    }

    @Test
    fun quizRun_onCancelClose_hidesConfirmationAndStaysOnQuiz() = runTest {
        val viewModel = viewModel(savedStateHandle = handleFor(threeQuestionPlan()))
        val job = collectState(viewModel)
        advanceUntilIdle()
        viewModel.onRequestClose()
        advanceUntilIdle()

        viewModel.onCancelClose()
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as QuizRunUiState.Content).closeConfirmationVisible).isFalse()
        job.cancel()
    }

    /**
     * FR-19: megszakításkor a válaszok megmaradnak, és a ViewModel nem hív
     * `finishSession`-t (lásd `FakeQuizRunProgressRepository.finishSession`, amely
     * kivételt dob, ha mégis meghívnák).
     */
    @Test
    fun quizRun_onConfirmClose_keepsRecordedAnswersAndEmitsCloseEvent_FR19() = runTest {
        val plan = threeQuestionPlan()
        val repository = FakeQuizRunProgressRepository()
        val viewModel = viewModel(savedStateHandle = handleFor(plan), progressRepository = repository)
        val job = collectState(viewModel)
        advanceUntilIdle()
        val question = (viewModel.uiState.value as QuizRunUiState.Content).question
        viewModel.onAnswer(question.id, question.correctTechniqueId)
        advanceUntilIdle()

        val events = mutableListOf<Unit>()
        val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.closeEvent.collect { events += it }
        }

        viewModel.onRequestClose()
        viewModel.onConfirmClose()
        advanceUntilIdle()

        assertThat(events).hasSize(1)
        assertThat(repository.allAttempts).hasSize(1)
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).answeredCount).isEqualTo(1)
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).closeConfirmationVisible).isFalse()
        job.cancel()
        eventJob.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun threeQuestionPlan(): QuizPlan = QuizPlan(
        questions = listOf(
            QuizQuestion(
                id = QuestionId.of("Q-1"),
                type = QuestionType.JP_TO_HU,
                correctTechniqueId = TechniqueId.of("KY-0001"),
                optionTechniqueIds = listOf("KY-0001", "KY-0002", "KY-0003", "KY-0004").map(TechniqueId::of),
            ),
            QuizQuestion(
                id = QuestionId.of("Q-2"),
                type = QuestionType.HU_TO_JP,
                correctTechniqueId = TechniqueId.of("KY-0003"),
                optionTechniqueIds = listOf("KY-0003", "KY-0001", "KY-0004", "KY-0002").map(TechniqueId::of),
            ),
            QuizQuestion(
                id = QuestionId.of("Q-3"),
                type = QuestionType.JP_TO_HU,
                correctTechniqueId = TechniqueId.of("KY-0005"),
                optionTechniqueIds = listOf("KY-0005", "KY-0001", "KY-0002", "KY-0003").map(TechniqueId::of),
            ),
        ),
    )

    private fun handleFor(
        plan: QuizPlan,
        sessionId: SessionId = SessionId.of("session-1"),
        seed: Long = 42L,
    ): SavedStateHandle = SavedStateHandle(
        mapOf(
            QuizRunViewModel.KEY_SESSION_ID to sessionId.value,
            QuizRunViewModel.KEY_SEED to seed,
            QuizRunViewModel.KEY_PLAN to QuizRunStateCodec.encodePlan(plan),
        ),
    )

    private fun TestScope.viewModel(
        progressRepository: FakeQuizRunProgressRepository = FakeQuizRunProgressRepository(),
        savedStateHandle: SavedStateHandle,
    ): QuizRunViewModel = QuizRunViewModel(
        progressRepository = progressRepository,
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: QuizRunViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
}
