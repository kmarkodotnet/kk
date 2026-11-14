package hu.kyokushin.trainer.feature.library.list

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A listatétel egyetlen bejárási egységként felolvasott leírása
 * (FR-05, NFR-04, FUNCTIONAL_SPEC 9.: „`{romaji}`, `{magyar}`, `{kategória}`, `{státusz}`").
 *
 * A teszt a **tényleges** `strings.xml` fölött fut (Robolectric,
 * `isIncludeAndroidResources = true`), ugyanazzal a mintával, mint a
 * `VerificationBadgeText` és az `IllustrationContentDescription` tesztjei a `core/ui`-ban.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "hu")
class LibraryListAccessibilityTest {
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<Context>().resources
    }

    private fun item(
        romaji: String = "Heisoku Dachi",
        hungarian: String = "Zárt lábú állás",
        categoryNameHu: String = "Állások",
        status: VerificationStatus = VerificationStatus.VERIFIED,
        matchedAlias: String? = null,
    ): LibraryTechniqueUiModel = LibraryTechniqueUiModel(
        id = TechniqueId.of("KY-0001"),
        romaji = romaji,
        hungarian = hungarian,
        categoryId = CategoryId.of("dachi"),
        categoryNameHu = categoryNameHu,
        illustration = null,
        status = status,
        matchedAlias = matchedAlias,
    )

    @Test
    fun verifiedRecord_omitsTheStatusPart_FR05() {
        val description = LibraryListItemAccessibility.describe(resources, item())

        // VERIFIED rekordnak nincs kimondott státusza – ugyanaz a döntés, mint a
        // VerificationBadge látható chipjénél (a hitelesség a jelölő hiánya).
        assertThat(description).isEqualTo("Heisoku Dachi, Zárt lábú állás, Állások")
    }

    @Test
    fun awaitingReview_appendsTheSpokenStatus_FR05() {
        val description = LibraryListItemAccessibility.describe(
            resources,
            item(status = VerificationStatus.AWAITING_REVIEW),
        )

        assertThat(description)
            .isEqualTo("Heisoku Dachi, Zárt lábú állás, Állások, Ellenőrzésre vár")
    }

    @Test
    fun incomplete_appendsTheSpokenStatus_FR05() {
        val description = LibraryListItemAccessibility.describe(
            resources,
            item(status = VerificationStatus.INCOMPLETE),
        )

        assertThat(description)
            .isEqualTo("Heisoku Dachi, Zárt lábú állás, Állások, Hiányos")
    }

    @Test
    fun aliasMatch_appendsTheMatchedAliasAsASeparateSentence_FR03() {
        val description = LibraryListItemAccessibility.describe(
            resources,
            item(
                romaji = "Seiken Chudan Tsuki",
                hungarian = "Középszintű ütés",
                categoryNameHu = "Ütések",
                status = VerificationStatus.AWAITING_REVIEW,
                matchedAlias = "chudan zuki",
            ),
        )

        assertThat(description).isEqualTo(
            "Seiken Chudan Tsuki, Középszintű ütés, Ütések, Ellenőrzésre vár. Egyezés: chudan zuki",
        )
    }

    @Test
    fun nameMatch_hasNoAliasSentence_FR03() {
        val description = LibraryListItemAccessibility.describe(resources, item(matchedAlias = null))

        assertThat(description).doesNotContain("Egyezés")
    }
}
