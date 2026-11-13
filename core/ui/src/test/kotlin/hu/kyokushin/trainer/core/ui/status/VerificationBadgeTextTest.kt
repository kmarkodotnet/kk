package hu.kyokushin.trainer.core.ui.status

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A hitelesítési jelölő szövegei (FR-05, FUNCTIONAL_SPEC 2.2).
 *
 * A teszt a **tényleges** `core/ui/src/main/res/values/strings.xml` fölött fut
 * (Robolectric, `isIncludeAndroidResources = true`): ha valaki átírja a magyar szöveget
 * vagy a VERIFIED ágra is jelölőt tesz, az itt bukik ki.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerificationBadgeTextTest {
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<Context>().resources
    }

    @Test
    fun verifiedRecord_hasNoBadgeAtAll_FR05() {
        // A FUNCTIONAL_SPEC 2.2 táblázata szerint a hitelesség a jelölő HIÁNYA.
        assertThat(VerificationBadgeText.label(resources, VerificationStatus.VERIFIED)).isNull()
    }

    @Test
    fun awaitingReview_isLabelledAwaitingReview_FR05() {
        val label = VerificationBadgeText.label(resources, VerificationStatus.AWAITING_REVIEW)

        assertThat(label).isEqualTo("Ellenőrzésre vár")
    }

    @Test
    fun incomplete_isLabelledIncomplete_FR05() {
        val label = VerificationBadgeText.label(resources, VerificationStatus.INCOMPLETE)

        assertThat(label).isEqualTo("Hiányos")
    }

    @Test
    fun incomplete_listsTheMissingFieldsByName_FR05() {
        val detail = VerificationBadgeText.missingFieldsDetail(
            resources = resources,
            status = VerificationStatus.INCOMPLETE,
            missingFields = listOf(TechniqueField.ILLUSTRATION, TechniqueField.HUNGARIAN),
        )

        // A mezőnevek magyarul, a `strings.xml`-ből – nem a hívó kódjából (NFR-02).
        assertThat(detail).isEqualTo("Hiányzó mezők: illusztráció, magyar jelentés")
    }

    @Test
    fun incomplete_withoutMissingFieldList_showsLabelWithoutDetail_FR05() {
        val detail = VerificationBadgeText.missingFieldsDetail(
            resources = resources,
            status = VerificationStatus.INCOMPLETE,
            missingFields = emptyList(),
        )

        // A „Hiányos" tény üres felsorolás mellett is igaz; a felsorolás részletezés.
        assertThat(detail).isNull()
        assertThat(VerificationBadgeText.label(resources, VerificationStatus.INCOMPLETE)).isNotNull()
    }

    @Test
    fun awaitingReview_neverListsMissingFields_FR05() {
        val detail = VerificationBadgeText.missingFieldsDetail(
            resources = resources,
            status = VerificationStatus.AWAITING_REVIEW,
            missingFields = listOf(TechniqueField.ILLUSTRATION),
        )

        // Egy ép, csak nem lektorált rekordnak nincs hiányzó mezője: a hívó tévedését
        // itt nyeljük el, nem a képernyőn.
        assertThat(detail).isNull()
    }

    @Test
    fun missingFields_areDeduplicated_FR05() {
        val detail = VerificationBadgeText.missingFieldsDetail(
            resources = resources,
            status = VerificationStatus.INCOMPLETE,
            missingFields = listOf(TechniqueField.ILLUSTRATION, TechniqueField.ILLUSTRATION),
        )

        assertThat(detail).isEqualTo("Hiányzó mezők: illusztráció")
    }

    @Test
    fun everyTechniqueField_hasAHungarianLabel_NFR02() {
        TechniqueField.entries.forEach { field ->
            assertThat(resources.getString(field.labelRes)).isNotEmpty()
        }
    }
}
