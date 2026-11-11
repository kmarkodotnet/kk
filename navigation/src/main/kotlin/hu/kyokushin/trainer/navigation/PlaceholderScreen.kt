package hu.kyokushin.trainer.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * Egy továbbvivő művelet a helyőrző képernyőn.
 *
 * @property labelRes a gomb magyar felirata.
 * @property onClick a navigációs művelet.
 */
data class PlaceholderAction(
    @StringRes val labelRes: Int,
    val onClick: () -> Unit,
)

/**
 * Helyőrző képernyő a navigációs vázhoz.
 *
 * A T-206 a navigációt építi meg, nem a képernyőket: amíg a funkciómodulok képernyői
 * elkészülnek (T-207-től), minden cél ezt mutatja. A szöveg nyíltan megmondja, hogy ez
 * még nem a végleges képernyő – nem állítjuk azt, hogy a funkció működik.
 *
 * Az [argument] az útvonalból kiolvasott azonosító (ha az útvonalnak van ilyen):
 * ennek a megjelenítése az egyetlen módja annak, hogy eszköz nélkül is látható legyen,
 * hogy az argumentum-átadás helyes.
 */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    argument: String? = null,
    actions: List<PlaceholderAction> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.nav_placeholder_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (argument != null) {
            Text(
                text = stringResource(R.string.nav_placeholder_argument, argument),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        actions.forEach { action ->
            Button(
                onClick = action.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    // Akadálymentesség: az érintési célpont legalább 48 dp magas.
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(action.labelRes))
            }
        }
    }
}

@Preview(name = "Helyőrző – világos", showBackground = true)
@Composable
private fun PlaceholderScreenLightPreview() {
    KyokushinTrainerTheme(darkTheme = false, dynamicColor = false) {
        Surface {
            PlaceholderScreen(
                titleRes = R.string.nav_title_library_list,
                actions = listOf(
                    PlaceholderAction(R.string.nav_placeholder_open_technique) {},
                ),
            )
        }
    }
}

@Preview(name = "Helyőrző – sötét", showBackground = true)
@Composable
private fun PlaceholderScreenDarkPreview() {
    KyokushinTrainerTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            PlaceholderScreen(
                titleRes = R.string.nav_title_library_detail,
                argument = "demo-technique",
                actions = listOf(
                    PlaceholderAction(R.string.nav_placeholder_open_technique_progress) {},
                ),
            )
        }
    }
}
