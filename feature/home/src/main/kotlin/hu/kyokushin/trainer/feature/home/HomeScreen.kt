package hu.kyokushin.trainer.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.core.ui.brand.KyoKanjiMark
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * A Kezdőoldal (Főoldal) belépési pontja a navigációból (FR-37).
 *
 * Nincs ViewModel: a képernyőnek szándékosan **nincs** megfigyelendő állapota – a
 * kandzsi logón kívül nincs más tartalom (a négy funkció az alsó navigációból érhető
 * el, nem a Kezdőoldal törzséből, lásd a T-703 task „Teendők" listáját). Forgatás után
 * ugyanezt mutatja, mert nincs mit elveszteni.
 */
@Composable
fun HomeRoute(modifier: Modifier = Modifier) {
    HomeScreen(modifier = modifier)
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            KyoKanjiMark()
        }
    }
}

@Preview(name = "Kezdőoldal – világos", showBackground = true)
@Preview(
    name = "Kezdőoldal – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        HomeScreen()
    }
}
