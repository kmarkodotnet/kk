package hu.kyokushin.trainer.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A FUNCTIONAL_SPEC 9. / NFR-04 „WCAG AA kontraszt világos és sötét témában" teendőjének
 * (T-604) tényleges, kiszámolt bizonyítéka – nem csak dokumentációs állítás.
 *
 * A WCAG-kontraszt a [LightScheme]/[DarkScheme] színpárjainak matematikai függvénye.
 * Az alapértelmezett téma ezeket a palettákat használja; az eszköztesztek külön az
 * akadálymentes navigációt és a nagy betűméretes elrendezést ellenőrzik.
 *
 * A 4.5:1 küszöb a WCAG 2.1 AA „normál szöveg" szintje. Minden pár, amit a komponensek a
 * projektben ténylegesen szöveg/ikon-előtér + háttér kombinációként használnak (lásd
 * `QuizRunScreen.quizOptionContainerColor`/`quizOptionContentColor`, `UnreviewedBanner`,
 * `VerificationBadge`, `ProgressCategoryRow`, stb. – mind `ColorScheme` token-párok, sosem
 * hardcode-olt szín), itt egyenként ellenőrzött.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeContrastTest {
    private val minNormalTextRatio = 4.5

    @Test
    fun lightScheme_primaryPairs_meetWcagAa_NFR04() {
        assertAllPairsMeetAa(LightScheme)
    }

    @Test
    fun darkScheme_primaryPairs_meetWcagAa_NFR04() {
        assertAllPairsMeetAa(DarkScheme)
    }

    /**
     * Minden `on<Role>`/`<role>` token-pár, amit a komponensek valójában előtér/háttér
     * színként összepárosítanak (lásd az osztály KDoc-ja).
     */
    private fun assertAllPairsMeetAa(scheme: ColorScheme) {
        val pairs = listOf(
            "onPrimary / primary" to (scheme.onPrimary to scheme.primary),
            "onPrimaryContainer / primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "onSecondary / secondary" to (scheme.onSecondary to scheme.secondary),
            "onSecondaryContainer / secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "onError / error" to (scheme.onError to scheme.error),
            "onErrorContainer / errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
            "onSurface / surface" to (scheme.onSurface to scheme.surface),
            "onSurfaceVariant / surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            // A hibaszín (`error`) a felszínen (`surface`) jár a tartalmi integritás /
            // kvízkizárás szövegéhez (lásd `LibraryDetailScreen.TechniqueStatusSection`).
            "error / surface" to (scheme.error to scheme.surface),
        )

        pairs.forEach { (label, pair) ->
            val (foreground, background) = pair
            val ratio = wcagContrastRatio(foreground, background)
            assertWithMessage("$label kontrasztaránya ($ratio)")
                .that(ratio)
                .isAtLeast(minNormalTextRatio)
        }
    }

    @Test
    fun wcagContrastRatio_blackOnWhite_isMaximal_NFR04() {
        // Önellenőrzés: fekete/fehér a WCAG képlet szerint pontosan 21:1 (két tizedesre).
        val ratio = wcagContrastRatio(Color.Black, Color.White)

        assertThat(ratio).isWithin(0.01).of(21.0)
    }

    @Test
    fun wcagContrastRatio_sameColor_isMinimal_NFR04() {
        // Önellenőrzés: azonos szín önmagával 1:1.
        val ratio = wcagContrastRatio(Color(0xFF336699), Color(0xFF336699))

        assertThat(ratio).isWithin(0.01).of(1.0)
    }
}
