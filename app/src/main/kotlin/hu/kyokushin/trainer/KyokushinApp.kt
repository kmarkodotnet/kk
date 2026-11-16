package hu.kyokushin.trainer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import hu.kyokushin.trainer.navigation.KyokushinNavHost
import hu.kyokushin.trainer.splash.AnimatedIntroScreen

/** Az átugorható intro után a navigáció saját induló útvonala nyílik meg. */
@Composable
fun KyokushinApp(modifier: Modifier = Modifier) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        AnimatedIntroScreen(onFinished = { showSplash = false }, modifier = modifier)
    } else {
        KyokushinNavHost(modifier = modifier)
    }
}
