package hu.kyokushin.trainer.feature.quiz.run

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.feature.quiz.R
import org.junit.Test

/**
 * A kvízopció ikon/szöveg/`stateDescription` leképezése – emulátor és Robolectric nélkül,
 * sima JUnit teszttel (T-413, NFR-04, FUNCTIONAL_SPEC 5.2/4., FUNCTIONAL_SPEC 9.).
 *
 * Ez a teszt a T-413 „Opciók `selected` szemantikával; válasz után `stateDescription`"
 * teendőjének bizonyítéka: a döntés ([quizOptionIcon]/[quizOptionFeedbackTextRes]/
 * [quizOptionStateDescriptionRes]) a Composable rétegtől független, `internal` függvény
 * (lásd a `QuizRunScreen.kt` KDoc-ját), ezért itt, a tényleges `strings.xml` feloldása
 * nélkül is bizonyítható, hogy válasz ELŐTT és egy nem kiválasztott, helytelen opciónál
 * semmilyen helyesség-állapot nem jelenik meg a TalkBack felé.
 */
class QuizOptionAccessibilityMappingTest {
    @Test
    fun unanswered_hasNoIconTextOrStateDescription_NFR04() {
        // Válasz előtt egyetlen opció sem árulhat el semmit a helyességről.
        assertThat(quizOptionIcon(QuizOptionFeedback.UNANSWERED)).isNull()
        assertThat(quizOptionFeedbackTextRes(QuizOptionFeedback.UNANSWERED)).isNull()
        assertThat(quizOptionStateDescriptionRes(QuizOptionFeedback.UNANSWERED)).isNull()
    }

    @Test
    fun neutral_afterAnswer_hasNoIconTextOrStateDescription_NFR04() {
        // Egy nem kiválasztott, helytelen opció válasz UTÁN is néma marad – nem jelez
        // semmit, hogy a felhasználó ne tudja kizárásos alapon kitalálni a helyes választ.
        assertThat(quizOptionIcon(QuizOptionFeedback.NEUTRAL)).isNull()
        assertThat(quizOptionFeedbackTextRes(QuizOptionFeedback.NEUTRAL)).isNull()
        assertThat(quizOptionStateDescriptionRes(QuizOptionFeedback.NEUTRAL)).isNull()
    }

    @Test
    fun selectedCorrect_hasCheckIconAndCorrectStateDescription_FR17() {
        assertThat(quizOptionIcon(QuizOptionFeedback.SELECTED_CORRECT)).isEqualTo(Icons.Filled.Check)
        assertThat(quizOptionFeedbackTextRes(QuizOptionFeedback.SELECTED_CORRECT))
            .isEqualTo(R.string.quiz_run_answer_correct)
        assertThat(quizOptionStateDescriptionRes(QuizOptionFeedback.SELECTED_CORRECT))
            .isEqualTo(R.string.quiz_run_option_state_correct)
    }

    /**
     * NFR-04: a kiemelt, de nem kiválasztott helyes opció UGYANAZT a pipa+szöveg+
     * `stateDescription` jelzést kapja, mint a tényleg kiválasztott helyes opció – a
     * kiemelés nem lehet csak szín (lásd `QuizOptionFeedbackResolverTest` is).
     */
    @Test
    fun correctUnselected_hasSameSignalAsSelectedCorrect_NFR04() {
        assertThat(quizOptionIcon(QuizOptionFeedback.CORRECT_UNSELECTED))
            .isEqualTo(quizOptionIcon(QuizOptionFeedback.SELECTED_CORRECT))
        assertThat(quizOptionFeedbackTextRes(QuizOptionFeedback.CORRECT_UNSELECTED))
            .isEqualTo(quizOptionFeedbackTextRes(QuizOptionFeedback.SELECTED_CORRECT))
        assertThat(quizOptionStateDescriptionRes(QuizOptionFeedback.CORRECT_UNSELECTED))
            .isEqualTo(quizOptionStateDescriptionRes(QuizOptionFeedback.SELECTED_CORRECT))
    }

    @Test
    fun selectedIncorrect_hasCloseIconAndIncorrectStateDescription_FR17() {
        assertThat(quizOptionIcon(QuizOptionFeedback.SELECTED_INCORRECT)).isEqualTo(Icons.Filled.Close)
        assertThat(quizOptionFeedbackTextRes(QuizOptionFeedback.SELECTED_INCORRECT))
            .isEqualTo(R.string.quiz_run_answer_wrong)
        assertThat(quizOptionStateDescriptionRes(QuizOptionFeedback.SELECTED_INCORRECT))
            .isEqualTo(R.string.quiz_run_option_state_incorrect)
    }

    /**
     * A látható felirat (`quiz_run_answer_correct`/`wrong`) és a TalkBack
     * `stateDescription` (`quiz_run_option_state_correct`/`incorrect`) két önálló
     * `strings.xml` bejegyzés (lásd `docs/CONVENTIONS.md` 4.1) – ez a teszt azt
     * bizonyítja, hogy a leképezés valóban két KÜLÖNBÖZŐ erőforrást használ, nem
     * ugyanazt a kulcsot kettőzi.
     */
    @Test
    fun feedbackTextAndStateDescription_areDistinctStringResources_NFR04() {
        assertThat(quizOptionFeedbackTextRes(QuizOptionFeedback.SELECTED_CORRECT))
            .isNotEqualTo(quizOptionStateDescriptionRes(QuizOptionFeedback.SELECTED_CORRECT))
        assertThat(quizOptionFeedbackTextRes(QuizOptionFeedback.SELECTED_INCORRECT))
            .isNotEqualTo(quizOptionStateDescriptionRes(QuizOptionFeedback.SELECTED_INCORRECT))
    }
}
