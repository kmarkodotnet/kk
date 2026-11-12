package hu.kyokushin.trainer.feature.quiz.result

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import org.junit.Test

/**
 * A kvízeredmény „Nagy összegzés" döntési logikája – emulátor és Compose-renderelés
 * nélkül (CONVENTIONS 4.1, T-411 „Kész ha").
 */
class QuizResultMessagesTest {

    @Test
    fun quizResultSummary_normalCompletion_returnsScoredWithRoundedPercent_FR20() {
        val state = content(answered = 10, correct = 7, ratio = 0.7, aborted = false, requestedCount = 10)

        val summary = quizResultSummary(state)

        assertThat(summary).isEqualTo(QuizResultSummary.Scored(correct = 7, answered = 10, percent = 70))
    }

    @Test
    fun quizResultSummary_zeroAnswers_returnsNoAnswers_FR27() {
        val state = content(answered = 0, correct = 0, ratio = null, aborted = true, requestedCount = 10)

        assertThat(quizResultSummary(state)).isEqualTo(QuizResultSummary.NoAnswers)
    }

    @Test
    fun quizResultSummary_abortedWithAnswers_returnsAbortedWithRequestedCount_FR19() {
        val state = content(answered = 3, correct = 2, ratio = 2.0 / 3.0, aborted = true, requestedCount = 10)

        assertThat(quizResultSummary(state)).isEqualTo(QuizResultSummary.Aborted(answered = 3, requested = 10))
    }

    /**
     * A két állapot (0 válasz ÉS megszakított) egyszerre is igaz lehet (a felhasználó
     * egyetlen kérdésre sem válaszolva szakítja meg a kvízt) – a „0 válasz" ág nyer, nem
     * a „megszakított" (lásd a [QuizResultSummary] KDoc-ja, FUNCTIONAL_SPEC 5.3/1.
     * felsorolási sorrendje).
     */
    @Test
    fun quizResultSummary_zeroAnswersAndAborted_prefersNoAnswers() {
        val state = content(answered = 0, correct = 0, ratio = null, aborted = true, requestedCount = 10)

        assertThat(quizResultSummary(state)).isEqualTo(QuizResultSummary.NoAnswers)
    }

    @Test
    fun quizResultSummary_perfectScore_roundsToHundredPercent() {
        val state = content(answered = 5, correct = 5, ratio = 1.0, aborted = false, requestedCount = 5)

        assertThat(quizResultSummary(state)).isEqualTo(QuizResultSummary.Scored(correct = 5, answered = 5, percent = 100))
    }

    private fun content(
        answered: Int,
        correct: Int,
        ratio: Double?,
        aborted: Boolean,
        requestedCount: Int,
    ): QuizResultUiState.Content = QuizResultUiState.Content(
        sessionId = SessionId.of("session-1"),
        score = Score(answered = answered, correct = correct, ratio = ratio),
        requestedCount = requestedCount,
        aborted = aborted,
        wrongItems = emptyList(),
        canRetryWrong = false,
        datasetTrust = DatasetTrust.REVIEWED,
    )
}
