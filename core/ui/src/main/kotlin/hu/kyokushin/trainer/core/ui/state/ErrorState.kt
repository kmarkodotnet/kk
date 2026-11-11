package hu.kyokushin.trainer.core.ui.state

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * Hibaállapot: magyar hibaüzenet és egy „Újra" művelet (NFR-06, FUNCTIONAL_SPEC 2.4).
 *
 * ### Miért `@StringRes`, és nem `String`?
 *
 * Egy `message: String` paraméter mellett a hívó – jellemzően egy ViewModel – magyar
 * szöveget írna a kódba, vagy ami rosszabb, egy kivétel `message`-ét adná át, és a
 * felhasználó egy angol stacktrace-töredéket látna. Erőforrás-azonosítóval a magyar
 * szöveg szerkezetileg a `strings.xml`-ben marad (NFR-02), és a hibaok megnevezése
 * fordítható.
 *
 * A művelet **nem** opcionális: hibaállapotban mindig kell kiút, különben a képernyő
 * zsákutca. Ha nincs mit újrapróbálni, akkor nem hiba, hanem üres állapot – arra az
 * [EmptyState] való.
 *
 * @param onRetry az újrapróbálkozás.
 * @param messageRes a hibaüzenet erőforrása; alapértelmezésben a betöltés meghiúsulása.
 * @param modifier a szokásos Compose modifier.
 */
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    @StringRes messageRes: Int = R.string.error_load_failed,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            // A hiba színe téma-tokenből jön; a hibaállapot nem CSAK színnel jelenik
            // meg, hanem szöveggel is, ezért színvakság mellett sem veszik el.
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onRetry,
            // A Material gomb alapértelmezett magassága 40 dp; a 48 dp-s érintési
            // célpont követelmény ennél nagyobb (FUNCTIONAL_SPEC 9.).
            modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Text(text = stringResource(R.string.error_action_retry))
        }
    }
}

@Preview(name = "Hiba – világos", showBackground = true)
@Preview(
    name = "Hiba – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ErrorStatePreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ErrorState(onRetry = {})
    }
}
