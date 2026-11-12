package hu.kyokushin.trainer.feature.quiz.setup

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.quiz.generator.Shortfall
import hu.kyokushin.trainer.core.quiz.generator.ShortfallReason
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A kvízbeállító képernyő apró szövegdöntései a **tényleges** `strings.xml` fölött
 * (FUNCTIONAL_SPEC 5.1, 8., FR-11, FR-12, FR-22, T-404).
 *
 * Ugyanaz a minta, mint a `LibraryListItemAccessibilityTest`/`VerificationBadgeTextTest`:
 * Robolectric, hogy a tényleges, szó szerinti FUNCTIONAL_SPEC-szöveg (nem egy kitalált
 * helyettesítő) kerüljön bizonyításra, emulátor és Compose-renderelés nélkül.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuizSetupMessagesTest {

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<Context>().resources
    }

    private fun content(
        expectedQuestionCount: Int,
        shortfall: Shortfall?,
    ): QuizSetupUiState.Content = QuizSetupUiState.Content(
        categories = emptyList(),
        selectedCategories = emptySet(),
        requestedCount = shortfall?.requestedCount ?: expectedQuestionCount,
        questionTypes = emptyList(),
        expectedQuestionCount = expectedQuestionCount,
        shortfall = shortfall,
        canStart = expectedQuestionCount > 0,
        notEnoughDataMessageRes = null,
        datasetTrust = DatasetTrust.REVIEWED,
    )

    private fun resolve(message: QuizSetupMessage): String =
        resources.getString(message.textRes, *message.args.toTypedArray())

    @Test
    fun quizReadinessSummary_withoutShortfall_announcesExpectedCount_FR11() {
        val message = quizReadinessSummary(content(expectedQuestionCount = 12, shortfall = null))

        assertThat(resolve(message)).isEqualTo("A beállításokkal 12 kérdés állítható össze.")
    }

    @Test
    fun quizReadinessSummary_withShortfall_announcesRequestedAndActualCount_FR22() {
        val shortfall = Shortfall(
            requestedCount = 20,
            actualCount = 12,
            reason = ShortfallReason.NOT_ENOUGH_CANDIDATES,
        )
        val message = quizReadinessSummary(content(expectedQuestionCount = 12, shortfall = shortfall))

        assertThat(resolve(message))
            .isEqualTo("A kért 20 helyett 12 kérdés lesz, mert nincs több megfelelő technika.")
    }

    @Test
    fun quizReadinessSummary_zeroQuestions_stillAnnouncesTheCount_FR22() {
        // 0 kérdés esetén NEM shortfall jön (az csak akkor létezik, ha a kért kérdésszám
        // kisebb a kértnél, de pozitív maradt volna) – lásd `QuizSetupUiState.Content`
        // KDoc-ja és `QuizReadiness.init`. A letiltott gomb alatti javaslatot a
        // `notEnoughDataMessageRes` adja, ez a sor ettől független, mindig látszik.
        val message = quizReadinessSummary(content(expectedQuestionCount = 0, shortfall = null))

        assertThat(resolve(message)).isEqualTo("A beállításokkal 0 kérdés állítható össze.")
    }

    @Test
    fun questionTypeLabelRes_returnsTheFunctionalSpecWordingForEachType_FR12() {
        assertThat(resources.getString(questionTypeLabelRes(QuestionType.JP_TO_HU)))
            .isEqualTo("Japán név → magyar jelentés")
        assertThat(resources.getString(questionTypeLabelRes(QuestionType.HU_TO_JP)))
            .isEqualTo("Magyar név → japán név")
        assertThat(resources.getString(questionTypeLabelRes(QuestionType.IMAGE_TO_JP)))
            .isEqualTo("Ábra → japán név")
        assertThat(resources.getString(questionTypeLabelRes(QuestionType.JP_TO_IMAGE)))
            .isEqualTo("Japán név → ábra")
    }
}
