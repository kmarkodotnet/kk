package hu.kyokushin.trainer.feature.quiz.run

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.TechniqueId
import org.junit.Test

/**
 * A [QuizOptionFeedbackResolver] tiszta döntési logikája – emulátor és Robolectric
 * nélkül, sima JUnit teszttel (T-409, FR-17, NFR-04).
 */
class QuizOptionFeedbackResolverTest {
    private val correct = TechniqueId.of("KY-0001")
    private val wrongA = TechniqueId.of("KY-0002")
    private val wrongB = TechniqueId.of("KY-0003")

    @Test
    fun resolve_beforeAnswer_everyOptionIsUnanswered_FR17() {
        assertThat(QuizOptionFeedbackResolver.resolve(correct, correct, selectedTechniqueId = null))
            .isEqualTo(QuizOptionFeedback.UNANSWERED)
        assertThat(QuizOptionFeedbackResolver.resolve(wrongA, correct, selectedTechniqueId = null))
            .isEqualTo(QuizOptionFeedback.UNANSWERED)
    }

    @Test
    fun resolve_selectedTheCorrectOption_isSelectedCorrect_FR17() {
        assertThat(QuizOptionFeedbackResolver.resolve(correct, correct, selectedTechniqueId = correct))
            .isEqualTo(QuizOptionFeedback.SELECTED_CORRECT)
    }

    @Test
    fun resolve_selectedAWrongOption_thatOptionIsSelectedIncorrect_FR17() {
        assertThat(QuizOptionFeedbackResolver.resolve(wrongA, correct, selectedTechniqueId = wrongA))
            .isEqualTo(QuizOptionFeedback.SELECTED_INCORRECT)
    }

    /**
     * NFR-04: a helyes opció akkor is kiemelt jelzést kap, ha a felhasználó egy másik
     * (helytelen) opciót választott – "a helyes opció mindig kiemelve".
     */
    @Test
    fun resolve_wrongSelected_correctOptionIsStillHighlighted_NFR04() {
        assertThat(QuizOptionFeedbackResolver.resolve(correct, correct, selectedTechniqueId = wrongA))
            .isEqualTo(QuizOptionFeedback.CORRECT_UNSELECTED)
    }

    @Test
    fun resolve_wrongSelected_otherUnselectedWrongOptionsAreNeutral_FR17() {
        assertThat(QuizOptionFeedbackResolver.resolve(wrongB, correct, selectedTechniqueId = wrongA))
            .isEqualTo(QuizOptionFeedback.NEUTRAL)
    }
}
