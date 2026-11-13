package hu.kyokushin.trainer.splash

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.R
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * Induláskor, a tényleges navigáció (`KyokushinNavHost`) betöltése előtt megjelenő, rövid
 * indítóképernyő: kék kandzsi logó fehér háttéren (FR-36).
 *
 * A háttér szándékosan **fix fehér** (`Color.White`), nem `MaterialTheme.colorScheme`
 * token – ez egy dokumentált kivétel a `docs/CONVENTIONS.md` 4. szakaszának "ne hardcode-olj
 * színt" szabálya alól: a splash célja egy márkajelzés, aminek a megjelenése a feladat
 * (T-701) szerint sötét rendszertémában sem változhat.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_kyo_kanji),
                contentDescription = stringResource(id = R.string.splash_content_description),
                modifier = Modifier
                    .width(160.dp)
                    .semantics { heading() },
            )
        }
    }
}

@Preview(name = "Indítóképernyő – világos", showBackground = true)
@Preview(
    name = "Indítóképernyő – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SplashScreenPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        SplashScreen()
    }
}
