package hu.kyokushin.trainer.feature.progress.overview

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.quiz.scoring.Score
import org.junit.Test

/**
 * [progressScoreDisplay] – `@Composable`-mentes döntési logika (T-503, FR-27).
 *
 * Ugyanaz a minta, mint a `feature/quiz` `QuizResultMessagesTest`-je: sima JUnit,
 * Robolectric/Compose nélkül (CONVENTIONS 4.1).
 */
class ProgressMessagesTest {

    @Test
    fun progressScoreDisplay_zeroAnswered_returnsNoData_FR27() {
        val display = progressScoreDisplay(Score(answered = 0, correct = 0, ratio = null))

        assertThat(display).isEqualTo(ProgressScoreDisplay.NoData)
    }

    @Test
    fun progressScoreDisplay_withAnswers_roundsRatioToPercent() {
        val display = progressScoreDisplay(Score(answered = 3, correct = 2, ratio = 2.0 / 3.0))

        assertThat(display).isEqualTo(ProgressScoreDisplay.Scored(correct = 2, answered = 3, percent = 67))
    }

    @Test
    fun progressScoreDisplay_perfectScore_returnsHundredPercent() {
        val display = progressScoreDisplay(Score(answered = 5, correct = 5, ratio = 1.0))

        assertThat(display).isEqualTo(ProgressScoreDisplay.Scored(correct = 5, answered = 5, percent = 100))
    }

    @Test
    fun progressScoreDisplay_zeroCorrect_returnsZeroPercent_notNoData() {
        val display = progressScoreDisplay(Score(answered = 4, correct = 0, ratio = 0.0))

        assertThat(display).isEqualTo(ProgressScoreDisplay.Scored(correct = 0, answered = 4, percent = 0))
    }
}
