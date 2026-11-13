package hu.kyokushin.trainer.feature.learn.cards

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.feature.learn.captionedIllustration
import hu.kyokushin.trainer.feature.learn.plainIllustration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A tanulókártya tiszta döntési szabályai – emulátor/Compose-renderelés nélkül
 * (T-304, T-305, FR-07, FR-08, NFR-04).
 *
 * `@Config(sdk = [34])`: a [LearnCardStateDescription] a **tényleges** `strings.xml`
 * fölött tesztelt (Robolectric), ugyanazzal a mintával, mint a
 * `VerificationBadgeText`/`IllustrationContentDescription` a `core/ui`-ban.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearnCardPoliciesTest {
    // --------------------------------------------------------------------- T-305

    @Test
    fun imagePolicy_captionedAssetNotYetRevealed_mustHide_T305() {
        assertThat(
            LearnCardImagePolicy.shouldHideUntilRevealed(
                illustration = captionedIllustration(),
                revealed = false,
            ),
        ).isTrue()
    }

    @Test
    fun imagePolicy_captionedAssetRevealed_noLongerHidden_T305() {
        assertThat(
            LearnCardImagePolicy.shouldHideUntilRevealed(
                illustration = captionedIllustration(),
                revealed = true,
            ),
        ).isFalse()
    }

    @Test
    fun imagePolicy_plainAsset_neverHidden_T305() {
        assertThat(
            LearnCardImagePolicy.shouldHideUntilRevealed(illustration = plainIllustration(), revealed = false),
        ).isFalse()
        assertThat(
            LearnCardImagePolicy.shouldHideUntilRevealed(illustration = plainIllustration(), revealed = true),
        ).isFalse()
    }

    @Test
    fun imagePolicy_placeholderOrMissingIllustration_neverHidden_T305() {
        assertThat(
            LearnCardImagePolicy.shouldHideUntilRevealed(IllustrationRef.Placeholder, revealed = false),
        ).isFalse()
        assertThat(LearnCardImagePolicy.shouldHideUntilRevealed(null, revealed = false)).isFalse()
    }

    // ------------------------------------------------------------------- FR-07/NFR-04

    @Test
    fun illustrationLabelPolicy_beforeReveal_isDecorative_notTheTechniqueName_FR07() {
        val label = LearnCardIllustrationLabelPolicy.labelFor(
            romaji = "Kiba Dachi",
            hungarian = "Lovagló állás",
            revealed = false,
        )

        // A kvízopcióhoz hasonlóan: a leírás nem a technika nevét adja, hogy a TalkBack
        // ne árulja el a rejtett nevet felfedés előtt.
        assertThat(label).isEqualTo(IllustrationLabel.Decorative)
    }

    @Test
    fun illustrationLabelPolicy_afterReveal_namesTheTechnique_FR07() {
        val label = LearnCardIllustrationLabelPolicy.labelFor(
            romaji = "Kiba Dachi",
            hungarian = "Lovagló állás",
            revealed = true,
        )

        assertThat(label).isEqualTo(IllustrationLabel.Technique(romaji = "Kiba Dachi", hungarian = "Lovagló állás"))
    }

    // ------------------------------------------------------------------------ T-304

    @Test
    fun stateDescription_beforeReveal_isHidden_T304() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources

        assertThat(LearnCardStateDescription.forRevealed(resources, revealed = false)).isEqualTo("elrejtve")
    }

    @Test
    fun stateDescription_afterReveal_isRevealed_T304() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources

        assertThat(LearnCardStateDescription.forRevealed(resources, revealed = true)).isEqualTo("felfedve")
    }
}
