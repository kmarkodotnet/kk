package hu.kyokushin.trainer.splash

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.ui.theme.wcagContrastRatio
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T-701 / FR-36: a splash háttere (fix fehér) és a kandzsi logó (fix `#17317A` kék) a
 * WCAG AA küszöböt (4.5:1) is teljesíti – ugyanazzal a képlettel bizonyítva, mint a
 * `core/ui` `ThemeContrastTest`-je (NFR-04).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SplashContrastTest {
    @Test
    fun splashLogoOnWhiteBackground_meetsWcagAa_FR36() {
        val background = Color.White
        val logo = Color(0xFF17317A)

        val ratio = wcagContrastRatio(logo, background)

        assertThat(ratio).isAtLeast(4.5)
    }
}
