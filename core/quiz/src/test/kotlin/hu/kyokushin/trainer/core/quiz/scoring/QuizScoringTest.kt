package hu.kyokushin.trainer.core.quiz.scoring

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.QuizAttempt
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import java.time.Instant
import org.junit.Test

/**
 * A [QuizScoring] alaptesztjei (T-113). A teljes TR-04 készlet – 0%, 100%, félbehagyott,
 * dupla válasz, nulla válasz, valódi tartalom – a T-114 tárgya; itt a szerződés azon
 * pontjait fedjük, amelyek nélkül a függvény nem tekinthető késznek.
 */
class QuizScoringTest {

    private val session = SessionId.of("session-1")

    /** Az `answeredAt` fix, injektált időpontból származik – a teszt sem hívja az órát. */
    private val base: Instant = Instant.parse("2026-01-01T10:00:00Z")

    private fun attempt(
        index: Int,
        correct: Boolean,
        sessionId: SessionId = session,
        questionId: QuestionId = QuestionId.of("q-$index"),
        answeredAt: Instant = base.plusSeconds(index.toLong()),
    ): QuizAttempt {
        val techniqueId = TechniqueId.of("tech-$index")
        return QuizAttempt(
            id = 0L,
            sessionId = sessionId,
            questionId = questionId,
            techniqueId = techniqueId,
            questionType = QuestionType.JP_TO_HU,
            selectedTechniqueId = if (correct) techniqueId else TechniqueId.of("tech-other-$index"),
            correct = correct,
            answeredAt = answeredAt,
        )
    }

    @Test
    fun score_noAttempts_hasNoRatioInsteadOfZeroPercent_FR27() {
        val score = QuizScoring.score(emptyList())

        assertThat(score.answered).isEqualTo(0)
        assertThat(score.correct).isEqualTo(0)
        // A „nincs adat" és a 0% két külön állapot: itt az előbbi a helyes (FR-27).
        assertThat(score.ratio).isNull()
    }

    @Test
    fun score_allWrong_isZeroRatioNotMissingData_FR27() {
        val score = QuizScoring.score((1..4).map { attempt(it, correct = false) })

        assertThat(score.answered).isEqualTo(4)
        assertThat(score.correct).isEqualTo(0)
        // A 0% viszont valós adat – nem szabad null-ra esnie.
        assertThat(score.ratio).isEqualTo(0.0)
    }

    @Test
    fun score_allCorrect_isFullRatio_FR20() {
        val score = QuizScoring.score((1..5).map { attempt(it, correct = true) })

        assertThat(score.answered).isEqualTo(5)
        assertThat(score.correct).isEqualTo(5)
        assertThat(score.ratio).isEqualTo(1.0)
    }

    @Test
    fun score_abandonedQuiz_countsOnlyAnsweredQuestions_FR20() {
        // 10 kérdésből 4 megválaszolt, abból 3 helyes: 75%. A hiányzó 6 nem hibás.
        val attempts = listOf(
            attempt(1, correct = true),
            attempt(2, correct = true),
            attempt(3, correct = false),
            attempt(4, correct = true),
        )

        val score = QuizScoring.score(attempts)

        assertThat(score.answered).isEqualTo(4)
        assertThat(score.correct).isEqualTo(3)
        assertThat(score.ratio).isEqualTo(0.75)
    }

    @Test
    fun score_repeatedAnswerForSameQuestion_countsFirstOnly_FR18() {
        val questionId = QuestionId.of("q-1")
        val first = attempt(1, correct = true, questionId = questionId, answeredAt = base)
        val duplicate = attempt(
            index = 2,
            correct = false,
            questionId = questionId,
            answeredAt = base.plusSeconds(1),
        )

        val score = QuizScoring.score(listOf(first, duplicate))

        // Egy kérdés egyszer számít, és az első válasz az érvényes (FR-18).
        assertThat(score.answered).isEqualTo(1)
        assertThat(score.correct).isEqualTo(1)
        assertThat(score.ratio).isEqualTo(1.0)
    }

    @Test
    fun score_sameQuestionIdInDifferentSessions_bothCount_FR21() {
        val questionId = QuestionId.of("q-1")
        val attempts = listOf(
            attempt(1, correct = true, questionId = questionId),
            attempt(
                index = 2,
                correct = false,
                sessionId = SessionId.of("session-2"),
                questionId = questionId,
            ),
        )

        val score = QuizScoring.score(attempts)

        // A kérdésazonosító csak munkameneten belül egyedi: a kulcs a pár, nem a kérdés.
        assertThat(score.answered).isEqualTo(2)
        assertThat(score.correct).isEqualTo(1)
        assertThat(score.ratio).isEqualTo(0.5)
    }

    @Test
    fun score_doesNotRecomputeCorrectnessFromIds_TR04() {
        // A mentett ítélet nyer: a szinonimacsoportos helyességet a kvízmotor dönti el,
        // a statisztika csak összegez (DR-09).
        val techniqueId = TechniqueId.of("tech-1")
        val synonymAnswer = QuizAttempt(
            id = 0L,
            sessionId = session,
            questionId = QuestionId.of("q-1"),
            techniqueId = techniqueId,
            questionType = QuestionType.JP_TO_IMAGE,
            selectedTechniqueId = TechniqueId.of("tech-synonym"),
            correct = true,
            answeredAt = base,
        )

        val score = QuizScoring.score(listOf(synonymAnswer))

        assertThat(score.correct).isEqualTo(1)
        assertThat(score.ratio).isEqualTo(1.0)
    }

    @Test
    fun score_isPureAndOrderIndependent_NFR07() {
        val attempts = (1..6).map { attempt(it, correct = it % 2 == 0) }

        val straight = QuizScoring.score(attempts)
        val reversed = QuizScoring.score(attempts.reversed())
        val again = QuizScoring.score(attempts)

        assertThat(reversed).isEqualTo(straight)
        assertThat(again).isEqualTo(straight)
        assertThat(straight.ratio).isEqualTo(0.5)
    }

    @Test
    fun score_rejectsInconsistentManuallyBuiltScore_FR27() {
        // A típus védi az invariánst: nulla válaszhoz nem gyártható 0.0 arány.
        runCatching { Score(answered = 0, correct = 0, ratio = 0.0) }.let { result ->
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
        runCatching { Score(answered = 2, correct = 3, ratio = 1.0) }.let { result ->
            assertThat(result.isFailure).isTrue()
        }
        runCatching { Score(answered = 2, correct = 1, ratio = null) }.let { result ->
            assertThat(result.isFailure).isTrue()
        }
    }
}
