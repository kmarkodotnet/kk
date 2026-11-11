package hu.kyokushin.trainer.core.ui.illustration

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Az illusztrációk akadálymentes leírásai (NFR-04, FUNCTIONAL_SPEC 2.3 és 9.).
 *
 * A teszt a **tényleges** `core/ui/src/main/res/values/strings.xml` fölött fut
 * (Robolectric, `isIncludeAndroidResources = true`), nem a formátumstringek másolatán:
 * így egy magyar szöveg átírása is kibukik, ha elrontja a szabályt – például ha valaki
 * a kvízopció leírásába technikanév-helyet tesz.
 */
@RunWith(RobolectricTestRunner::class)
// A projekt Robolectric-konvenciója: a telepített SDK-k közül a 34-es képe van
// letöltve (ugyanaz, mint a `core/data` és az `app` tesztjeiben).
@Config(sdk = [34])
class IllustrationContentDescriptionTest {

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun assetDescription_inLibrary_namesTheTechnique_NFR04() {
        val description = IllustrationContentDescription.forAsset(
            resources = resources,
            label = IllustrationLabel.Technique(romaji = "kiba dachi", hungarian = "lovagló állás"),
        )

        // A FUNCTIONAL_SPEC 9. pontja a Tudástár részletben MEGKÖVETELI a nevet.
        assertThat(description).isNotNull()
        assertThat(description).contains("kiba dachi")
        assertThat(description).contains("lovagló állás")
        assertThat(description).contains("illusztrációja")
    }

    @Test
    fun assetDescription_forQuizOption_omitsTechniqueName_NFR04() {
        val technique = "kiba dachi"
        val hungarian = "lovagló állás"

        val description = IllustrationContentDescription.forAsset(
            resources = resources,
            label = IllustrationLabel.QuizOption(position = 4),
        )

        // Ez a teszt a T-204 legfontosabb pontja: a kvízopció leírása sem a romaji, sem
        // a magyar nevet nem mondhatja ki, különben a TalkBack elárulná a megoldást.
        assertThat(description).isNotNull()
        assertThat(description).doesNotContain(technique)
        assertThat(description).doesNotContain(hungarian)
        assertThat(description).contains("4.")
        assertThat(description).contains("válaszlehetőség")
    }

    @Test
    fun placeholderDescription_forQuizOption_omitsTechniqueName_NFR04() {
        val description = IllustrationContentDescription.forPlaceholder(
            resources = resources,
            label = IllustrationLabel.QuizOption(position = 2),
        )

        assertThat(description).isEqualTo(
            IllustrationContentDescription.forAsset(
                resources = resources,
                label = IllustrationLabel.QuizOption(position = 2),
            ),
        )
        assertThat(description).doesNotContain("kiba dachi")
    }

    @Test
    fun placeholderDescription_inLibrary_statesTheMissingIllustration_NFR04() {
        val description = IllustrationContentDescription.forPlaceholder(
            resources = resources,
            label = IllustrationLabel.Technique(romaji = "gedan barai", hungarian = "lefelé hárítás"),
        )

        assertThat(description).isEqualTo("Ehhez a technikához még nincs ábra")
    }

    @Test
    fun decorativeLabel_hasNoDescription_NFR04() {
        assertThat(
            IllustrationContentDescription.forAsset(resources, IllustrationLabel.Decorative),
        ).isNull()
        assertThat(
            IllustrationContentDescription.forPlaceholder(resources, IllustrationLabel.Decorative),
        ).isNull()
    }

    @Test
    fun quizOptionLabel_rejectsNonPositivePosition_NFR04() {
        // A nulla vagy negatív sorszámból értelmezhetetlen leírás lenne („0. válaszlehetőség").
        runCatching { IllustrationLabel.QuizOption(position = 0) }
            .also { assertThat(it.isFailure).isTrue() }
    }
}
