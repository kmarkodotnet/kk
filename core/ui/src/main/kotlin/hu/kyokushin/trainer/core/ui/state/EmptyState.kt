package hu.kyokushin.trainer.core.ui.state

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * **Miért** üres a képernyő – és ebből következik, mit írunk ki és mit lehet tenni
 * (NFR-06, FUNCTIONAL_SPEC 2.4 és 8.).
 *
 * ### Miért ok, és nem egy `message: String` paraméter?
 *
 * A „nincs tartalom" és a „nincs találat" ugyanúgy egy üres lista, de a felhasználó
 * számára két teljesen más helyzet: az egyik betöltési hiba, amin ő nem tud segíteni, a
 * másik a saját keresésének a következménye, amit egy koppintással visszavonhat. Ha a
 * komponens kész szöveget kapna, a két eset összecsúszása csendes hiba lenne: egy
 * „A tudástár üres" felirat a keresés fölött azt sugallná, hogy elveszett a tananyag.
 *
 * Az ok magával hozza a szöveget **és** a javasolt akció címkéjét is, így a két döntés
 * nem szakadhat el egymástól, és a magyar szöveg nem szökik ki a `strings.xml`-ből
 * (NFR-02).
 */
sealed interface EmptyStateReason {

    /** A megjelenített magyar üzenet feloldása. */
    fun message(resources: Resources): String

    /**
     * A javasolt akció címkéje, vagy `null`, ha ehhez az okhoz nem tartozik akció.
     *
     * A gomb akkor jelenik meg, ha ez nem `null` **és** a hívó adott hozzá műveletet:
     * a cselekvés lehetőségét az ok írja le, a végrehajtást a képernyő tudja.
     */
    @get:StringRes
    val actionLabelRes: Int?

    /**
     * A tudástár üres, mert a tartalom betöltése nem sikerült
     * (FUNCTIONAL_SPEC 10.: nem támogatott `schemaVersion` vagy parse hiba).
     *
     * A javasolt akció a fejlesztői diagnosztika (FR-32) – debug buildben a teljes
     * validációs jelentés. Release buildben a hívó egyszerűen nem ad műveletet, és a
     * gomb nem jelenik meg.
     */
    data object LibraryNoContent : EmptyStateReason {
        override fun message(resources: Resources): String =
            resources.getString(R.string.empty_library_no_content)

        override val actionLabelRes: Int = R.string.empty_action_diagnostics
    }

    /**
     * Van tartalom, de a keresés vagy a szűrők nem adtak találatot.
     *
     * @property query a keresőszöveg, ahogy a felhasználó beírta – az üzenet idézi, hogy
     *   látható legyen, mire nem volt találat.
     */
    data class LibraryNoResult(val query: String) : EmptyStateReason {
        override fun message(resources: Resources): String =
            resources.getString(R.string.empty_library_no_result, query)

        override val actionLabelRes: Int = R.string.empty_action_clear_filters
    }

    /**
     * A haladás képernyő még nem tud mit mutatni, mert nem volt kvíz.
     *
     * Nincs akciógombja: az üzenet maga hívja cselekvésre („Indíts egy kvízt!"), és a
     * kvíz indítása az alsó navigáción egy koppintás – egy második út ugyanoda csak
     * zajt adna.
     */
    data object ProgressNoData : EmptyStateReason {
        override fun message(resources: Resources): String =
            resources.getString(R.string.empty_progress_no_data)

        override val actionLabelRes: Int? = null
    }

    /**
     * A kiválasztott beállításokkal nem állítható össze kvíz (FUNCTIONAL_SPEC 5.1).
     *
     * Nincs akciógombja: a teendőt – több kategória, több kérdéstípus vagy kevesebb
     * kérdés – maga a beállító képernyő vezérlői adják, amelyek ott vannak az üzenet
     * mellett.
     */
    data object QuizNotEnoughData : EmptyStateReason {
        override fun message(resources: Resources): String =
            resources.getString(R.string.empty_quiz_not_enough_data)

        override val actionLabelRes: Int? = null
    }
}

/**
 * Üres állapot: konkrét, magyar, cselekvésre hívó üzenet – soha nem csak egy üres lista
 * (NFR-06, FUNCTIONAL_SPEC 2.4).
 *
 * @param reason miért üres a képernyő; ez dönti el a szöveget és a javasolt akciót.
 * @param onAction a javasolt akció végrehajtása. `null` esetén nincs gomb – akkor is, ha
 *   az okhoz tartozna akciócímke (például release buildben a diagnosztika).
 * @param modifier a szokásos Compose modifier.
 */
@Composable
fun EmptyState(
    reason: EmptyStateReason,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val actionLabel = reason.actionLabelRes?.let { resources.getString(it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = reason.message(resources),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (actionLabel != null && onAction != null) {
            OutlinedButton(
                onClick = onAction,
                // A Material gomb alapértelmezett magassága 40 dp; a 48 dp-s érintési
                // célpont követelmény ennél nagyobb (FUNCTIONAL_SPEC 9.).
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Preview(name = "Nincs tartalom – világos", showBackground = true)
@Preview(
    name = "Nincs tartalom – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EmptyStateNoContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        EmptyState(reason = EmptyStateReason.LibraryNoContent, onAction = {})
    }
}

@Preview(name = "Nincs találat – világos", showBackground = true)
@Preview(
    name = "Nincs találat – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EmptyStateNoResultPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        EmptyState(reason = EmptyStateReason.LibraryNoResult(query = "mawashi"), onAction = {})
    }
}

/** Akció nélküli ok: az üzenet maga hívja cselekvésre, gomb nincs. */
@Preview(name = "Nincs haladási adat – világos", showBackground = true)
@Preview(
    name = "Nincs haladási adat – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EmptyStateProgressNoDataPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        EmptyState(reason = EmptyStateReason.ProgressNoData)
    }
}
