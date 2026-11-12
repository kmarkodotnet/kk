package hu.kyokushin.trainer.feature.library.list

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.ui.state.EmptyStateReason
import org.junit.Test

/**
 * Melyik akció tartozik az üres állapothoz a Tudástár listáján
 * (FUNCTIONAL_SPEC 3.1 táblázata, FR-32, T-210).
 *
 * A leképezés a Compose fától függetlenül tesztelhető, mert [libraryEmptyStateAction]
 * sima függvény: bemenete az ok és a két lehetséges művelet, kimenete pedig az, amelyik
 * érvényes marad.
 */
class LibraryEmptyStateActionTest {

    @Test
    fun noResult_offersClearFilters_notDiagnostics() {
        var clearCalled = false
        var diagnosticsCalled = false

        val action = libraryEmptyStateAction(
            reason = EmptyStateReason.LibraryNoResult(query = "mawashi"),
            onClearFilters = { clearCalled = true },
            onDiagnostics = { diagnosticsCalled = true },
        )
        action?.invoke()

        assertThat(clearCalled).isTrue()
        assertThat(diagnosticsCalled).isFalse()
    }

    @Test
    fun noContent_inDebugBuild_offersDiagnostics_FR32() {
        var diagnosticsCalled = false

        val action = libraryEmptyStateAction(
            reason = EmptyStateReason.LibraryNoContent,
            onClearFilters = {},
            onDiagnostics = { diagnosticsCalled = true },
        )
        action?.invoke()

        assertThat(diagnosticsCalled).isTrue()
    }

    @Test
    fun noContent_inReleaseBuild_hasNoAction_FR32() {
        // Release buildben a hívó `null`-t ad: az EmptyState emiatt nem rajzol gombot,
        // még akkor sem, ha az oknak lenne címkéje (lásd EmptyState dokumentációja).
        val action = libraryEmptyStateAction(
            reason = EmptyStateReason.LibraryNoContent,
            onClearFilters = {},
            onDiagnostics = null,
        )

        assertThat(action).isNull()
    }

    @Test
    fun otherReasons_haveNoActionOnTheLibraryScreen() {
        val action = libraryEmptyStateAction(
            reason = EmptyStateReason.ProgressNoData,
            onClearFilters = { error("nem hívható") },
            onDiagnostics = { error("nem hívható") },
        )

        assertThat(action).isNull()
    }
}
