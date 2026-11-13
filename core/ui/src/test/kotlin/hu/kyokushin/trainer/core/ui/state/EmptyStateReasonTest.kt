package hu.kyokushin.trainer.core.ui.state

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.ui.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Az üres állapot okai: más ok, más üzenet, más akció (NFR-06, FUNCTIONAL_SPEC 2.4 és 8.).
 *
 * A legfontosabb állítás az, hogy a „nincs tartalom" és a „nincs találat" NEM ugyanaz:
 * ha valaki egyetlen közös szövegre egyszerűsítené őket, ez a teszt bukik.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmptyStateReasonTest {
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<Context>().resources
    }

    @Test
    fun noContent_andNoResult_differInMessageAndAction_NFR06() {
        val noContent = EmptyStateReason.LibraryNoContent
        val noResult = EmptyStateReason.LibraryNoResult(query = "mawashi")

        assertThat(noResult.message(resources)).isNotEqualTo(noContent.message(resources))
        assertThat(noResult.actionLabelRes).isNotEqualTo(noContent.actionLabelRes)
    }

    @Test
    fun libraryNoContent_reportsLoadFailureAndOffersDiagnostics_FR32() {
        val reason = EmptyStateReason.LibraryNoContent

        assertThat(reason.message(resources))
            .isEqualTo("A tudástár üres. A tartalom betöltése nem sikerült.")
        assertThat(reason.actionLabelRes).isEqualTo(R.string.empty_action_diagnostics)
        assertThat(resources.getString(R.string.empty_action_diagnostics)).isEqualTo("Diagnosztika")
    }

    @Test
    fun libraryNoResult_quotesTheQueryAndOffersFilterReset_NFR06() {
        val reason = EmptyStateReason.LibraryNoResult(query = "mawashi")

        // A beírt keresőszöveg látszik: a felhasználó tudja, mire nem volt találat.
        assertThat(reason.message(resources)).contains("mawashi")
        assertThat(reason.message(resources)).isEqualTo("Nincs találat a(z) „mawashi” keresésre.")
        assertThat(reason.actionLabelRes).isEqualTo(R.string.empty_action_clear_filters)
        assertThat(resources.getString(R.string.empty_action_clear_filters)).isEqualTo("Szűrők törlése")
    }

    @Test
    fun progressNoData_callsToActionInTheMessageAndHasNoButton_NFR06() {
        val reason = EmptyStateReason.ProgressNoData

        assertThat(reason.message(resources)).isEqualTo("Még nincs adat. Indíts egy kvízt!")
        assertThat(reason.actionLabelRes).isNull()
    }

    @Test
    fun quizNotEnoughData_explainsWhatToChange_NFR06() {
        val reason = EmptyStateReason.QuizNotEnoughData

        assertThat(reason.message(resources)).isEqualTo(
            "A kiválasztott beállításokkal nem állítható össze kvíz. " +
                "Válassz több kategóriát, engedélyezz több kérdéstípust, vagy csökkentsd a kérdésszámot.",
        )
        assertThat(reason.actionLabelRes).isNull()
    }

    @Test
    fun everyReason_producesANonEmptyHungarianMessage_NFR06() {
        val reasons = listOf(
            EmptyStateReason.LibraryNoContent,
            EmptyStateReason.LibraryNoResult(query = "x"),
            EmptyStateReason.ProgressNoData,
            EmptyStateReason.QuizNotEnoughData,
        )

        reasons.forEach { reason ->
            assertThat(reason.message(resources)).isNotEmpty()
        }
    }

    @Test
    fun errorState_offersRetryInHungarian_NFR06() {
        assertThat(resources.getString(R.string.error_action_retry)).isEqualTo("Újra")
        assertThat(resources.getString(R.string.error_load_failed)).isNotEmpty()
    }
}
