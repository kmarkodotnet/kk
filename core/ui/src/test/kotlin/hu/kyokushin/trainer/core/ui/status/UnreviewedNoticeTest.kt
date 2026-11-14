package hu.kyokushin.trainer.core.ui.status

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A lektorálatlanság-szalag (ADR-010, CR-03).
 *
 * A két bizonyítandó állítás:
 * 1. lektorálatlan tartalomnál a szalag szövege megjelenik, és azt mondja, amit az
 *    ADR-010 előír;
 * 2. lektorált tartalomnál maguktól eltűnik a jelölések – kódmódosítás nélkül (AC-08).
 *
 * A szalag elrejthetetlenségét az API alakja bizonyítja: a [UnreviewedNotice.bannerText]
 * egyetlen bemenete a tartalom bizalmi szintje, nincs felhasználói kapcsoló paramétere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "hu")
class UnreviewedNoticeTest {
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<Context>().resources
    }

    @Test
    fun unreviewedDataset_showsTheNonDismissibleBanner_CR03() {
        val text = UnreviewedNotice.bannerText(resources, DatasetTrust.PRACTICE_ON_UNREVIEWED)

        assertThat(text).isEqualTo(
            "Lektorálatlan gyakorlóanyag – a tartalom szakmai ellenőrzésre vár.",
        )
    }

    @Test
    fun reviewedDataset_showsNoBannerWithoutCodeChange_ADR010() {
        val text = UnreviewedNotice.bannerText(resources, DatasetTrust.REVIEWED)

        assertThat(text).isNull()
    }

    @Test
    fun bannerText_dependsOnlyOnDatasetTrust_ADR010() {
        // Ugyanaz a bemenet, ugyanaz a kimenet: nincs olyan rejtett állapot vagy
        // felhasználói beállítás, amitől a szalag eltűnhetne.
        DatasetTrust.entries.forEach { trust ->
            val first = UnreviewedNotice.bannerText(resources, trust)
            val second = UnreviewedNotice.bannerText(resources, trust)

            assertThat(second).isEqualTo(first)
        }
    }
}
