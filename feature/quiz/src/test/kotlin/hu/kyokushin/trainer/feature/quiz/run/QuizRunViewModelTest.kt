package hu.kyokushin.trainer.feature.quiz.run

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizPlan
import hu.kyokushin.trainer.core.quiz.generator.QuizQuestion
import hu.kyokushin.trainer.core.quiz.generator.QuizRunStateCodec
import hu.kyokushin.trainer.core.quiz.generator.Shortfall
import hu.kyokushin.trainer.core.quiz.generator.ShortfallReason
import hu.kyokushin.trainer.feature.quiz.FakeQuizRunProgressRepository
import hu.kyokushin.trainer.feature.quiz.FakeQuizTechniqueRepository
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_CHUDAN
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_FUDO
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_HEISOKU
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_MUSUBI
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

    /**
     * T-407 „Kész ha": a kérdés-UI a `TechniqueRepository`-ból feloldott nevekkel kap
     * opciót, **ugyanabban a sorrendben**, mint a [QuizQuestion.optionTechniqueIds]
     * (a seedelt keverés sorrendje, ADR-007 – nem rendezi át).
     */
    @Test
    fun quizRun_content_resolvesOptionDisplayFromTechniqueRepository_FR16() = runTest {
        val plan = threeQuestionPlan()
        val viewModel = viewModel(savedStateHandle = handleFor(plan))
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizRunUiState.Content
        assertThat(content.options.map { it.techniqueId })
            .isEqualTo(content.question.optionTechniqueIds)
        assertThat(content.options.map { it.romaji })
            .isEqualTo(listOf(TECHNIQUE_HEISOKU, TECHNIQUE_FUDO, TECHNIQUE_MUSUBI, TECHNIQUE_CHUDAN).map { it.romaji })
        assertThat(content.options.map { it.hungarian })
            .isEqualTo(
                listOf(TECHNIQUE_HEISOKU, TECHNIQUE_FUDO, TECHNIQUE_MUSUBI, TECHNIQUE_CHUDAN).map {
                    it.hungarian
                },
            )
        job.cancel()
    }

    /**
     * NFR-06 védelmi háló: ha egy opció technikája nem található a tudástárban (pl. egy
     * régi mentett állapot egy közben eltávolított technikára hivatkozna), a ViewModel
     * nem omlik össze és nem ad hiányos (4-nél kevesebb opciós) állapotot – [QuizRunUiState.Empty]-t ad.
     */
    @Test
    fun quizRun_content_missingOptionTechnique_showsEmpty_NFR06() = runTest {
        val plan = threeQuestionPlan()
        // Csak az első kérdés helyes technikáját adja vissza – a többi opció feloldatlan marad.
        val incompleteTechniqueRepository = FakeQuizTechniqueRepository(
            techniques = listOf(TECHNIQUE_HEISOKU),
        )
        val viewModel = viewModel(
            savedStateHandle = handleFor(plan),
            techniqueRepository = incompleteTechniqueRepository,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(QuizRunUiState.Empty)
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

    /**
     * T-414 "Teendők" 4.: egy nulla kérdéses terv (teljes shortfall – minden
     * engedélyezett típus kimaradt, FR-22) a `quiz/run`-ba a `quiz/setup` "Kész ha"-ja
     * szerint rendesen sosem kerülne, de ez a teszt – a [quizRun_missingInput_showsEmpty_NFR06]-tól
     * eltérően – egy ÉRVÉNYES sessionId/seed mellett, egy valódi (üres kérdéslistájú)
     * [QuizPlan]-nal bizonyítja a [QuizRunUiState.Empty] védelmi hálót: a `buildUiState`
     * a `questions` ürességét vizsgálja, nem a bemenet puszta jelenlétét.
     */
    @Test
    fun quizRun_zeroQuestionShortfallPlan_showsEmpty_FR22() = runTest {
        val plan = QuizPlan(
            questions = emptyList(),
            shortfall = Shortfall(
                requestedCount = 10,
                actualCount = 0,
                reason = ShortfallReason.ALL_TYPES_SKIPPED,
            ),
        )
        val viewModel = viewModel(savedStateHandle = handleFor(plan))
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(QuizRunUiState.Empty)
        job.cancel()
    }

    /**
     * T-414 "Teendők" 4.: nem teljes shortfall esetén (a kért 5 helyett csak 2 kérdés
     * állt össze) a `quiz/run` a TÉNYLEGES kérdésszámot mutatja [QuizRunUiState.Content.total]-ként,
     * nem a kértet – a [QuizRunState.shortfall] a tervezéskori adat, a lejátszást nem
     * módosítja (lásd a `QuizRunState` KDoc-ja).
     */
    @Test
    fun quizRun_content_withShortfallPlan_showsActualQuestionCount_FR22() = runTest {
        val fullPlan = threeQuestionPlan()
        val shortfallPlan = fullPlan.copy(
            questions = fullPlan.questions.take(2),
            shortfall = Shortfall(
                requestedCount = 5,
                actualCount = 2,
                reason = ShortfallReason.NOT_ENOUGH_CANDIDATES,
            ),
        )
        val viewModel = viewModel(savedStateHandle = handleFor(shortfallPlan))
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizRunUiState.Content
        assertThat(content.total).isEqualTo(2)
        assertThat(content.question).isEqualTo(shortfallPlan.questions[0])
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

    /**
     * T-410 „Teendők": „a kérdések között nincs visszalépés" – és az utolsón túl sem megy
     * POZÍCIÓBAN (a harmadik [onNext] hívás T-411 óta lezárja a menetet és kiadja a
     * [finishEvent]-et, lásd `quizRun_onNext_atLastQuestion_finishesSessionAndEmitsFinishEvent_FR20`
     * – ez a teszt csak a pozíciót vizsgálja).
     */
    @Test
    fun quizRun_onNext_atLastQuestion_positionDoesNotAdvanceFurther() = runTest {
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
     * `finishSession`-t – ezt itt a [FakeQuizRunProgressRepository.finishSessionCalls]
     * üres volta bizonyítja (T-411: a fake immár rögzíti, nem dobja el a hívást).
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
        assertThat(repository.finishSessionCalls).isEmpty()
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).answeredCount).isEqualTo(1)
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).closeConfirmationVisible).isFalse()
        job.cancel()
        eventJob.cancel()
    }

    // --------------------------------------------------------- normál befejezés / T-411

    /**
     * T-411 (G-09): az utolsó kérdésen a „Következő"/„Eredmény" gomb ([onNext]) a menetet
     * lezárja ([ProgressRepository.finishSession]) és a [finishEvent]-en keresztül jelzi a
     * hívónak, hogy navigáljon az eredményképernyőre – a pozíció **nem** lép tovább
     * (nincs hova, FUNCTIONAL_SPEC 5.2/4.).
     */
    @Test
    fun quizRun_onNext_atLastQuestion_finishesSessionAndEmitsFinishEvent_FR20() = runTest {
        val plan = threeQuestionPlan()
        val sessionId = SessionId.of("session-1")
        val repository = FakeQuizRunProgressRepository()
        val viewModel = viewModel(
            savedStateHandle = handleFor(plan, sessionId = sessionId),
            progressRepository = repository,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val events = mutableListOf<SessionId>()
        val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.finishEvent.collect { events += it }
        }

        viewModel.onNext()
        viewModel.onNext()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).isLastQuestion).isTrue()

        viewModel.onNext()
        advanceUntilIdle()

        assertThat(repository.finishSessionCalls).containsExactly(sessionId)
        assertThat(events).containsExactly(sessionId)
        // A pozíció nem lépett túl az utolsó kérdésen.
        assertThat((viewModel.uiState.value as QuizRunUiState.Content).position).isEqualTo(2)
        job.cancel()
        eventJob.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    @Test
    fun navigationInput_initializesOnceAndRestoresAnswer_FR19() = runTest {
        val plan = threeQuestionPlan()
        val handle = SavedStateHandle(mapOf(QuizRunViewModel.KEY_SESSION_ID to "session-1"))
        val start = hu.kyokushin.trainer.feature.quiz.setup.QuizStartResult(SessionId.of("session-1"), plan, 42L)
        val vm = viewModel(savedStateHandle = handle)
        val job = collectState(vm)
        vm.initialize(start)
        advanceUntilIdle()
        vm.onAnswer(plan.questions.first().id, plan.questions.first().optionTechniqueIds.first())
        advanceUntilIdle()
        vm.initialize(start)
        advanceUntilIdle()
        val answered = vm.uiState.value as QuizRunUiState.Content
        assertThat(answered.answeredCount).isEqualTo(1)
        val restored = viewModel(
            savedStateHandle = SavedStateHandle(
                handle.keys().associateWith {
                    handle.get<Any>(it)
                },
            ),
        )
        val restoredJob = collectState(restored)
        restored.initialize(start)
        advanceUntilIdle()
        assertThat((restored.uiState.value as QuizRunUiState.Content).selectedTechniqueId)
            .isEqualTo(answered.selectedTechniqueId)
        job.cancel()
        restoredJob.cancel()
    }

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
        techniqueRepository: TechniqueRepository = FakeQuizTechniqueRepository(),
        savedStateHandle: SavedStateHandle,
    ): QuizRunViewModel = QuizRunViewModel(
        progressRepository = progressRepository,
        techniqueRepository = techniqueRepository,
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: QuizRunViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
}
