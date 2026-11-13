package hu.kyokushin.trainer.core.ui.status

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * Egy technikarekord hitelesítési jelölője (FR-05, FUNCTIONAL_SPEC 2.2).
 *
 * | [status] | Megjelenítés |
 * |---|---|
 * | [VerificationStatus.VERIFIED] | **semmi** – a composable nem bocsát ki UI-t |
 * | [VerificationStatus.AWAITING_REVIEW] | „Ellenőrzésre vár" chip |
 * | [VerificationStatus.INCOMPLETE] | „Hiányos" chip + a hiányzó mezők felsorolása |
 *
 * A hitelesített rekord azért nem kap „Rendben" jelölőt, mert a specifikáció szerint a
 * hitelesség a jelölő **hiánya** (FUNCTIONAL_SPEC 2.2 táblázat). Ha a hitelesített
 * állapotnak is saját jelölője lenne, a felhasználó a sok címke között pont azt nem
 * vette volna észre, amiről szól: hogy ez a rekord nem ellenőrzött.
 *
 * A jelölő a rekord állapotából származik, nem felhasználói beállításból, és nincs
 * elrejtésre szolgáló paramétere (CR-01, ADR-010).
 *
 * @param status a rekord háromállapotú hitelesítési státusza.
 * @param missingFields a hiányzó mezők – kizárólag [VerificationStatus.INCOMPLETE]
 *   esetén jelenik meg. Üres lista mellett a chip felsorolás nélkül áll, mert a „Hiányos"
 *   tény önmagában is igaz; a felsorolás részletezés, nem feltétel.
 * @param modifier a szokásos Compose modifier.
 */
@Composable
fun VerificationBadge(
    status: VerificationStatus,
    missingFields: List<TechniqueField> = emptyList(),
    modifier: Modifier = Modifier,
) {
    if (status == VerificationStatus.VERIFIED) return

    val resources = LocalContext.current.resources
    val label = VerificationBadgeText.label(resources, status) ?: return
    val detail = VerificationBadgeText.missingFieldsDetail(resources, status, missingFields)

    // A chip EGY bejárási egység: a TalkBack a címkét és a részletezést egyszerre
    // olvassa fel, nem két külön fókuszpontként (FUNCTIONAL_SPEC 9.).
    val spoken = listOfNotNull(label, detail).joinToString(separator = ". ")

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        shape = RoundedCornerShape(BADGE_CORNER_RADIUS),
        color = badgeContainerColor(status),
        contentColor = badgeContentColor(status),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            if (detail != null) {
                Text(text = detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** A chip sarokkerekítése. */
private val BADGE_CORNER_RADIUS = 8.dp

/**
 * A jelölő háttérszíne – **csak** téma-tokenből (nincs hardcode-olt `Color`).
 *
 * A „Hiányos" az `errorContainer`-t kapja, mert tartalmi hibát jelez; az „Ellenőrzésre
 * vár" a `surfaceVariant`-ot, mert tájékoztatás, nem hiba (FUNCTIONAL_SPEC 2.1 indoklása).
 */
@Composable
private fun badgeContainerColor(status: VerificationStatus) = when (status) {
    VerificationStatus.INCOMPLETE -> MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

/** A jelölő szövegszíne – a háttérhez tartozó `on…` token, így a kontraszt garantált. */
@Composable
private fun badgeContentColor(status: VerificationStatus) = when (status) {
    VerificationStatus.INCOMPLETE -> MaterialTheme.colorScheme.onErrorContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * A jelölő és a hiányzó mezők szövegének feloldása.
 *
 * Azért `@Composable`-mentes, `Resources`-t fogadó objektum, mert a szövegszabályok így a
 * **tényleges** `strings.xml` fölött, emulátor nélkül tesztelhetők (Robolectric) – ugyanaz
 * a minta, mint az `IllustrationContentDescription`-nál.
 */
object VerificationBadgeText {
    /**
     * A jelölő címkéje, vagy `null`, ha nincs jelölő.
     *
     * [VerificationStatus.VERIFIED] esetén `null` – ez a szabály, nem a hívó jóindulata.
     */
    fun label(resources: Resources, status: VerificationStatus): String? = when (status) {
        VerificationStatus.VERIFIED -> null
        VerificationStatus.AWAITING_REVIEW -> resources.getString(R.string.content_unverified)
        VerificationStatus.INCOMPLETE -> resources.getString(R.string.content_incomplete)
    }

    /**
     * A hiányzó mezők felsorolása, vagy `null`, ha nem jár felsorolás.
     *
     * Felsorolás **kizárólag** [VerificationStatus.INCOMPLETE] mellett van: egy ép, csak
     * még nem lektorált rekordnál nincs hiányzó mező, tehát a hívó tévedését itt nyeljük
     * el, nem a képernyőn.
     */
    fun missingFieldsDetail(
        resources: Resources,
        status: VerificationStatus,
        missingFields: List<TechniqueField>,
    ): String? {
        if (status != VerificationStatus.INCOMPLETE) return null
        if (missingFields.isEmpty()) return null

        val separator = resources.getString(R.string.content_incomplete_missing_fields_separator)
        val names = missingFields.distinct().joinToString(separator) { resources.getString(it.labelRes) }
        return resources.getString(R.string.content_incomplete_missing_fields, names)
    }
}

@Preview(name = "Ellenőrzésre vár – világos", showBackground = true)
@Preview(
    name = "Ellenőrzésre vár – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VerificationBadgeAwaitingReviewPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        VerificationBadge(status = VerificationStatus.AWAITING_REVIEW)
    }
}

@Preview(name = "Hiányos – világos", showBackground = true)
@Preview(
    name = "Hiányos – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VerificationBadgeIncompletePreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        VerificationBadge(
            status = VerificationStatus.INCOMPLETE,
            missingFields = listOf(TechniqueField.ILLUSTRATION, TechniqueField.HUNGARIAN),
        )
    }
}

/**
 * Hitelesített rekord: a komponens nem bocsát ki UI-t. Az előnézetben csak az
 * állványzat-szöveg látszik – ez a bizonyíték arra, hogy nincs jelölő.
 */
@Preview(name = "Hitelesített – világos", showBackground = true)
@Preview(
    name = "Hitelesített – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VerificationBadgeVerifiedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Fejlesztői felirat az előnézethez, soha nem kerül felhasználó elé (NFR-02).
            Text(text = "— nincs jelölő —", style = MaterialTheme.typography.bodySmall)
            VerificationBadge(status = VerificationStatus.VERIFIED)
        }
    }
}
