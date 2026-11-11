package hu.kyokushin.trainer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hu.kyokushin.trainer.navigation.KyokushinNavHost

/**
 * Az alkalmazás gyökér composable-je.
 *
 * Az egész felhasználói felület a `:navigation` modul navigációs vázára épül: felső sáv,
 * bottom navigation négy céllal és a NavHost (FUNCTIONAL_SPEC 1.). Az `app` modul
 * szándékosan nem tud a képernyőkről – az útvonalakat a `navigation` modul dönti el, az
 * egyetlen modul, amely minden funkciómodult ismer (ADR-001, L5).
 *
 * A Fázis 1 helyőrző kezdőképernyőt a T-206 cserélte le erre.
 */
@Composable
fun KyokushinApp(modifier: Modifier = Modifier) {
    KyokushinNavHost(modifier = modifier)
}
