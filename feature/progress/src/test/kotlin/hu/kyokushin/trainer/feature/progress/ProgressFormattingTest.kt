package hu.kyokushin.trainer.feature.progress

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

/**
 * [formatLastPracticed] – `@Composable`-mentes dátumformázás (FR-25, FR-26, T-504, T-506).
 *
 * Sima JUnit, Robolectric/Compose nélkül (CONVENTIONS 4.1): a függvény csak `java.time`-ot
 * használ, nincs benne `Resources`-függés.
 */
class ProgressFormattingTest {

    @Test
    fun formatLastPracticed_usesTheGivenZoneNotTheSystemDefault_FR25() {
        val instant = Instant.parse("2026-11-01T23:30:00Z")

        val formatted = formatLastPracticed(instant, zone = ZoneOffset.UTC)

        assertThat(formatted).isEqualTo("2026.11.01.")
    }

    @Test
    fun formatLastPracticed_differentZonesCanYieldDifferentCalendarDays_FR25() {
        val instant = Instant.parse("2026-11-01T23:30:00Z")

        val formattedFarEast = formatLastPracticed(instant, zone = ZoneOffset.ofHours(2))

        // 23:30 UTC + 2 óra = másnap 01:30 – ugyanaz az `Instant`, más naptári nap,
        // mert a formázás a megadott időzóna szerint történik, nem UTC-ben rögzítve.
        assertThat(formattedFarEast).isEqualTo("2026.11.02.")
    }
}
