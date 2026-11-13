package hu.kyokushin.trainer.feature.quiz.result

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.repository.QuizSessionResult
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.QuizAttempt
import hu.kyokushin.trainer.core.model.QuizSession
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.DefaultQuizGenerator
import hu.kyokushin.trainer.core.quiz.generator.QuizGenerator
import hu.kyokushin.trainer.feature.quiz.FakeQuizResultProgressRepository
import hu.kyokushin.trainer.feature.quiz.FakeQuizTechniqueRepository
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_CHUDAN
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_FUDO
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_HEISOKU
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_JODAN
import hu.kyokushin.trainer.feature.quiz.TECHNIQUE_MUSUBI
import hu.kyokushin.trainer.feature.quiz.quizImportedState
import hu.kyokushin.trainer.feature.quiz.setup.QuizStartResult
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
import java.time.Instant
import kotlin.random.Random

/**
 * A kvízeredmény ViewModel viselkedése – emulátor nélkül (T-411, T-412, T-415, FR-20,
 * FR-21, FR-27, G-09 csoport).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket (CONVENTIONS 3.). A kvízmotor ([DefaultQuizGenerator]) **valódi**,
 * nem dublőrözött – ugyanaz a döntés, mint a `QuizSetupViewModelTest`-nél.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuizResultViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --------------------------------------------------------------------- betöltés / FR-20

    @Test
    fun quizResult_withFinishedSession_showsScoreAndWrongItems_FR20() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = finishedSession(sessionId, requestedCount = 5, answeredCount = 5),
            attempts = listOf(
                attempt(1, "q-1", TECHNIQUE_HEISOKU.id, QuestionType.JP_TO_HU, TECHNIQUE_FUDO.id, correct = false),
                attempt(2, "q-2", TECHNIQUE_FUDO.id, QuestionType.JP_TO_HU, TECHNIQUE_HEISOKU.id, correct = false),
                attempt(3, "q-3", TECHNIQUE_MUSUBI.id, QuestionType.HU_TO_JP, TECHNIQUE_CHUDAN.id, correct = false),
                attempt(4, "q-4", TECHNIQUE_CHUDAN.id, QuestionType.HU_TO_JP, TECHNIQUE_MUSUBI.id, correct = false),
                attempt(5, "q-5", TECHNIQUE_JODAN.id, QuestionType.JP_TO_HU, TECHNIQUE_JODAN.id, correct = true),
            ),
        )
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result)),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizResultUiState.Content
        assertThat(content.score.answered).isEqualTo(5)
        assertThat(content.score.correct).isEqualTo(1)
        assertThat(content.aborted).isFalse()
        assertThat(content.wrongItems).hasSize(4)
        assertThat(content.wrongItems[0].correctRomaji).isEqualTo(TECHNIQUE_HEISOKU.romaji)
        assertThat(content.wrongItems[0].selectedRomaji).isEqualTo(TECHNIQUE_FUDO.romaji)
        assertThat(content.canRetryWrong).isTrue()
        job.cancel()
    }

    @Test
    fun quizResult_unknownSessionId_showsNotFound_NFR06() = runTest {
        val viewModel = viewModel(savedStateHandle = handleFor(SessionId.of("missing")))
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(QuizResultUiState.NotFound)
        job.cancel()
    }

    @Test
    fun quizResult_missingSessionIdInput_showsNotFound_NFR06() = runTest {
        val viewModel = viewModel(savedStateHandle = SavedStateHandle())
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(QuizResultUiState.NotFound)
        job.cancel()
    }

    @Test
    fun quizResult_abortedSessionWithNoAnswers_showsAbortedAndNullRatio_FR19_FR27() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = unfinishedSession(sessionId, requestedCount = 10, answeredCount = 0),
            attempts = emptyList(),
        )
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result)),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizResultUiState.Content
        assertThat(content.aborted).isTrue()
        assertThat(content.score.answered).isEqualTo(0)
        assertThat(content.score.ratio).isNull()
        assertThat(content.canRetryWrong).isFalse()
        job.cancel()
    }

    /**
     * T-414 "Teendők" 3./TR-04 „félbehagyott teszt": megszakításkor a `finishedAt`
     * `null` marad (US-15 AC-2), de az ADDIG rögzített válaszok megmaradnak és SZÁMÍTANAK
     * – a pontszám a kért 10 helyett a tényleg megválaszolt 3-ból számol, nem a
     * hiányzó 7-et bünteti 0%-ként (ellentétben
     * [quizResult_abortedSessionWithNoAnswers_showsAbortedAndNullRatio_FR19_FR27]-vel,
     * amely a ZÉRÓ válasz esetét fedi).
     */
    @Test
    fun quizResult_abortedSessionWithPartialAnswers_scoresOnlyTheAnsweredQuestions_FR19() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = unfinishedSession(sessionId, requestedCount = 10, answeredCount = 3),
            attempts = listOf(
                attempt(1, "q-1", TECHNIQUE_HEISOKU.id, QuestionType.JP_TO_HU, TECHNIQUE_HEISOKU.id, correct = true),
                attempt(2, "q-2", TECHNIQUE_FUDO.id, QuestionType.JP_TO_HU, TECHNIQUE_FUDO.id, correct = true),
                attempt(3, "q-3", TECHNIQUE_MUSUBI.id, QuestionType.HU_TO_JP, TECHNIQUE_CHUDAN.id, correct = false),
            ),
        )
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result)),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizResultUiState.Content
        assertThat(content.aborted).isTrue()
        // A nevező a 3 megválaszolt, NEM a kért 10 – a hátralévő 7 nem rontja az arányt.
        assertThat(content.score.answered).isEqualTo(3)
        assertThat(content.score.correct).isEqualTo(2)
        assertThat(content.score.ratio).isEqualTo(2.0 / 3.0)
        assertThat(content.requestedCount).isEqualTo(10)
        assertThat(content.canRetryWrong).isTrue()
        job.cancel()
    }

    /**
     * T-415: a jelzés a `datasetTrust`-ból jön, nem felhasználói beállításból – a
     * [QuizResultUiState.Content.datasetTrust] a tartalom bizalmi szintjét tükrözi.
     */
    @Test
    fun quizResult_datasetTrust_reflectsContentState_T415() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = finishedSession(sessionId, requestedCount = 1, answeredCount = 1),
            attempts = listOf(
                attempt(1, "q-1", TECHNIQUE_HEISOKU.id, QuestionType.JP_TO_HU, TECHNIQUE_HEISOKU.id, correct = true),
            ),
        )
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result)),
            techniqueRepository = FakeQuizTechniqueRepository(
                contentState = quizImportedState(5, datasetTrust = DatasetTrust.REVIEWED),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizResultUiState.Content
        assertThat(content.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
        job.cancel()
    }

    /**
     * NFR-06 védelmi háló: ha egy hibás próbálkozás technikája nem oldható fel a
     * jelenlegi tudástárból, az a tétel kimarad a listából, de a "Hibák újragyakorlása"
     * gomb a NYERS próbálkozásból dönt, nem a feloldott listából (lásd
     * `QuizResultUiState.Content.canRetryWrong` KDoc-ja).
     */
    @Test
    fun quizResult_unresolvableTechnique_omittedFromWrongItemsButCanRetryStaysTrue_NFR06() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = finishedSession(sessionId, requestedCount = 1, answeredCount = 1),
            attempts = listOf(
                attempt(1, "q-1", TECHNIQUE_JODAN.id, QuestionType.JP_TO_HU, TECHNIQUE_HEISOKU.id, correct = false),
            ),
        )
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result)),
            // TECHNIQUE_JODAN szándékosan hiányzik: a helyes technika nem oldható fel.
            techniqueRepository = FakeQuizTechniqueRepository(
                techniques = listOf(TECHNIQUE_HEISOKU, TECHNIQUE_FUDO, TECHNIQUE_MUSUBI, TECHNIQUE_CHUDAN),
            ),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as QuizResultUiState.Content
        assertThat(content.wrongItems).isEmpty()
        assertThat(content.canRetryWrong).isTrue()
        job.cancel()
    }

    // --------------------------------------------------------- hibák újragyakorlása / T-412

    @Test
    fun quizResult_onRetryWrong_withFourDistinctWrongTechniques_startsNewSessionWithNewSeed_FR21() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = finishedSession(sessionId, requestedCount = 5, answeredCount = 5),
            attempts = listOf(
                attempt(1, "q-1", TECHNIQUE_HEISOKU.id, QuestionType.JP_TO_HU, TECHNIQUE_FUDO.id, correct = false),
                attempt(2, "q-2", TECHNIQUE_FUDO.id, QuestionType.JP_TO_HU, TECHNIQUE_HEISOKU.id, correct = false),
                attempt(3, "q-3", TECHNIQUE_MUSUBI.id, QuestionType.HU_TO_JP, TECHNIQUE_CHUDAN.id, correct = false),
                attempt(4, "q-4", TECHNIQUE_CHUDAN.id, QuestionType.HU_TO_JP, TECHNIQUE_MUSUBI.id, correct = false),
                attempt(5, "q-5", TECHNIQUE_JODAN.id, QuestionType.JP_TO_HU, TECHNIQUE_JODAN.id, correct = true),
            ),
        )
        val progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result))
        val expectedSeed = Random(SEED).nextLong()
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = progressRepository,
            random = Random(SEED),
        )
        val job = collectState(viewModel)
        val events = mutableListOf<QuizStartResult>()
        val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.retryEvent.collect { events += it }
        }
        advanceUntilIdle()

        viewModel.onRetryWrong()
        advanceUntilIdle()

        assertThat(progressRepository.startSessionCalls).containsExactly(4 to expectedSeed)
        assertThat(events).hasSize(1)
        val retried = events.single()
        assertThat(retried.seed).isEqualTo(expectedSeed)
        assertThat(retried.plan.questions).isNotEmpty()
        // US-16 AC-1: ÚJ munkamenet, saját azonosítóval – nem a megtekintett menet
        // folytatása.
        assertThat(retried.sessionId).isNotEqualTo(sessionId)
        // Kizárólag a hibás technikák (US-16 AC-1: "ugyanazokra a technikákra") –
        // soha nem a helyes (ötödik, JODAN) technikára.
        retried.plan.questions.forEach { question ->
            assertThat(question.correctTechniqueId).isNotEqualTo(TECHNIQUE_JODAN.id)
        }
        // US-16 AC-2: a régi munkamenet eredménye változatlan – nincs kettős könyvelés.
        assertThat(progressRepository.getSessionResult(sessionId)).isEqualTo(result)
        job.cancel()
        eventJob.cancel()
    }

    /**
     * A motor dokumentált, nem-optimista szabálya (legalább 4 technika kell egy
     * négyopciós kérdéshez, lásd `docs/CONVENTIONS.md` 1. szakasz): 3-nál kevesebb
     * EGYEDI hibás technikával nem indul új, üres munkamenet.
     */
    @Test
    fun quizResult_onRetryWrong_withFewerThanFourDistinctWrongTechniques_doesNotStartSession() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = finishedSession(sessionId, requestedCount = 2, answeredCount = 2),
            attempts = listOf(
                attempt(1, "q-1", TECHNIQUE_HEISOKU.id, QuestionType.JP_TO_HU, TECHNIQUE_FUDO.id, correct = false),
                attempt(2, "q-2", TECHNIQUE_FUDO.id, QuestionType.JP_TO_HU, TECHNIQUE_HEISOKU.id, correct = false),
            ),
        )
        val progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result))
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = progressRepository,
        )
        val job = collectState(viewModel)
        val events = mutableListOf<QuizStartResult>()
        val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.retryEvent.collect { events += it }
        }
        advanceUntilIdle()

        viewModel.onRetryWrong()
        advanceUntilIdle()

        assertThat(progressRepository.startSessionCalls).isEmpty()
        assertThat(events).isEmpty()
        job.cancel()
        eventJob.cancel()
    }

    @Test
    fun quizResult_onRetryWrong_noWrongAttempts_isNoOp() = runTest {
        val sessionId = SessionId.of("session-1")
        val result = QuizSessionResult(
            session = finishedSession(sessionId, requestedCount = 1, answeredCount = 1),
            attempts = listOf(
                attempt(1, "q-1", TECHNIQUE_HEISOKU.id, QuestionType.JP_TO_HU, TECHNIQUE_HEISOKU.id, correct = true),
            ),
        )
        val progressRepository = FakeQuizResultProgressRepository(mapOf(sessionId to result))
        val viewModel = viewModel(
            savedStateHandle = handleFor(sessionId),
            progressRepository = progressRepository,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onRetryWrong()
        advanceUntilIdle()

        assertThat(progressRepository.startSessionCalls).isEmpty()
        job.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private val fixedInstant: Instant = Instant.parse("2026-11-12T08:00:00Z")

    private fun finishedSession(id: SessionId, requestedCount: Int, answeredCount: Int): QuizSession = QuizSession(
        id = id,
        startedAt = fixedInstant,
        finishedAt = fixedInstant.plusSeconds(60),
        requestedCount = requestedCount,
        answeredCount = answeredCount,
        seed = 1L,
    )

    private fun unfinishedSession(id: SessionId, requestedCount: Int, answeredCount: Int): QuizSession = QuizSession(
        id = id,
        startedAt = fixedInstant,
        finishedAt = null,
        requestedCount = requestedCount,
        answeredCount = answeredCount,
        seed = 1L,
    )

    private fun attempt(
        id: Long,
        questionId: String,
        techniqueId: TechniqueId,
        questionType: QuestionType,
        selectedTechniqueId: TechniqueId,
        correct: Boolean,
    ): QuizAttempt = QuizAttempt(
        id = id,
        sessionId = SessionId.of("session-1"),
        questionId = QuestionId.of(questionId),
        techniqueId = techniqueId,
        questionType = questionType,
        selectedTechniqueId = selectedTechniqueId,
        correct = correct,
        answeredAt = fixedInstant,
    )

    private fun handleFor(sessionId: SessionId): SavedStateHandle = SavedStateHandle(
        mapOf(QuizResultViewModel.KEY_SESSION_ID to sessionId.value),
    )

    private fun TestScope.viewModel(
        progressRepository: FakeQuizResultProgressRepository = FakeQuizResultProgressRepository(),
        techniqueRepository: FakeQuizTechniqueRepository = FakeQuizTechniqueRepository(),
        quizGenerator: QuizGenerator = DefaultQuizGenerator,
        random: Random = Random(SEED),
        savedStateHandle: SavedStateHandle,
    ): QuizResultViewModel = QuizResultViewModel(
        progressRepository = progressRepository,
        techniqueRepository = techniqueRepository,
        quizGenerator = quizGenerator,
        random = random,
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: QuizResultViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    private companion object {
        const val SEED = 99L
    }
}
