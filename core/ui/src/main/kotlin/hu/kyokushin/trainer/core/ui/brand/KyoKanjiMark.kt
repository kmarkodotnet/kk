package hu.kyokushin.trainer.core.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme

/**
 * A márka-kandzsi: a felhasználó saját gi-fotójából (`asserts/kyo.png`) vektorizált,
 * kék (`#17317A`) kandzsi logó (FR-36, FR-37).
 *
 * **Miért itt, a `core/ui`-ban?** Az indítóképernyő (`app`, T-701) és a Kezdőoldal
 * (`feature/home`, T-703) is ugyanazt a képet mutatja. Az `app` modulra egy
 * funkciómodul **nem** függhet (ADR-001, L5 fordított iránya tiltott) – a közös
 * elem ezért a megosztott `core/ui`-ban él, nem az egyik hívóban duplikálva.
 *
 * A komponens nem állít be méretet vagy hátteret: a drawable saját, 160dp széles
 * belső méretét használja, hacsak a hívó a `modifier`-en keresztül másként nem dönt –
 * így a splash (fix fehér háttér) és a Kezdőoldal (a normál téma háttere) is szabadon
 * eldöntheti a keretet, a logó maga theme-független.
 */
@Composable
fun KyoKanjiMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_kyo_kanji),
        contentDescription = stringResource(id = R.string.kyo_kanji_content_description),
        modifier = modifier.semantics { heading() },
    )
}

@Preview(name = "Kandzsi – világos", showBackground = true)
@Preview(
    name = "Kandzsi – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun KyoKanjiMarkPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        KyoKanjiMark()
    }
}
