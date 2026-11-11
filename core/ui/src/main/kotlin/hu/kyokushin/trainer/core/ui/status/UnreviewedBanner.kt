package hu.kyokushin.trainer.core.ui.status

import android.content.res.Resources
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * A lektorálatlanság-szalag: minden képernyő tetején megjelenő, **nem elutasítható**
 * tájékoztatás (ADR-010, CR-03, FUNCTIONAL_SPEC 2.1).
 *
 * ### Amit ez a komponens szándékosan NEM tud
 *
 * Nincs `onDismiss`, nincs `dismissible`, nincs `visible` és nincs bezárógombja.
 * Ez nem feledékenység, hanem az ADR-010 döntésének kódba zárása: „a jelölés nem
 * elrejthető és nem kikapcsolható a felhasználó által". Egy elrejthető figyelmeztetés
 * elrejtett kockázat – a felhasználó az első koppintás után egy látszólag hiteles
 * tananyagot gyakorolna.
 *
 * ### Mitől függ, hogy látszik-e
 *
 * Kizárólag a [trust] értékétől, ami a manifest szerkesztői állapotából származik, nem
 * felhasználói beállításból. [DatasetTrust.REVIEWED] mellett a composable **semmit nem
 * bocsát ki**: így lektorált tartalom érkezésekor a jelölések maguktól eltűnnek,
 * kódmódosítás nélkül (AC-08).
 *
 * @param trust a betöltött tudástár bizalmi szintje.
 * @param modifier a szokásos Compose modifier.
 */
@Composable
fun UnreviewedBanner(
    trust: DatasetTrust,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val message = UnreviewedNotice.bannerText(resources, trust) ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // A szalag a képernyő tetején jelenik meg, és a tartalomváltáskor is igaz
            // marad: `Polite` élő régió, hogy a TalkBack kimondja, de ne szakítsa meg a
            // felhasználó éppen folyó műveletét (FUNCTIONAL_SPEC 9.).
            .semantics { liveRegion = LiveRegionMode.Polite },
        // Alacsony kontrasztú, de olvasható tájékoztató sáv: a `surfaceVariant` és a
        // hozzá tartozó `on…` token páros adja a WCAG AA kontrasztot mindkét témában.
        // Nem `errorContainer`: ez nem hibaállapot, hanem tájékoztatás.
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * A lektorálatlanságra figyelmeztető szövegek feloldása.
 *
 * `@Composable`-mentes és `Resources`-t fogad, hogy az ADR-010 szabály – „lektorált
 * tartalomnál nincs jelzés, lektorálatlannál van" – emulátor nélkül, a tényleges
 * `strings.xml` fölött tesztelhető legyen.
 */
object UnreviewedNotice {

    /**
     * A szalag szövege, vagy `null`, ha nincs szalag.
     *
     * A függvénynek nincs és nem is lehet olyan paramétere, amivel a hívó elnyomhatná a
     * szöveget: egyetlen bemenete a tartalom bizalmi szintje.
     */
    fun bannerText(resources: Resources, trust: DatasetTrust): String? = when (trust) {
        DatasetTrust.REVIEWED -> null
        DatasetTrust.PRACTICE_ON_UNREVIEWED -> resources.getString(R.string.content_unreviewed_banner)
    }
}

@Preview(name = "Lektorálatlan – világos", showBackground = true)
@Preview(
    name = "Lektorálatlan – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun UnreviewedBannerPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        UnreviewedBanner(trust = DatasetTrust.PRACTICE_ON_UNREVIEWED)
    }
}

/**
 * Lektorált tartalom: a szalag nem jelenik meg. Az előnézet üres sávja a bizonyíték arra,
 * hogy a jelzés kódmódosítás nélkül eltűnik (AC-08).
 */
@Preview(name = "Lektorált – világos", showBackground = true)
@Preview(
    name = "Lektorált – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun UnreviewedBannerReviewedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        UnreviewedBanner(trust = DatasetTrust.REVIEWED)
    }
}
