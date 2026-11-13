package hu.kyokushin.trainer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import hu.kyokushin.trainer.navigation.KyokushinNavHost
import hu.kyokushin.trainer.splash.SplashScreen
import kotlinx.coroutines.delay

/**
 * Az indítóképernyő (T-701, FR-36) rögzített időtartama. Ez **nem** egy üzleti
 * időbélyeg (NFR-07 arra vonatkozik), hanem egy UI-átmenet időzítése – nincs rá injektált
 * `Clock`-ra szükség.
 */
private const val SPLASH_DURATION_MILLIS = 1200L

/**
 * Az alkalmazás gyökér composable-je.
 *
 * Induláskor rövid ideig a [SplashScreen] látszik (kék kandzsi logó fehér háttéren),
 * utána a tényleges navigáció. A splash szándékosan **nem** egy konkrét képernyőre (pl.
 * Kezdőoldal, T-703) navigál explicit módon – egyszerűen átadja a vezérlést a
 * `KyokushinNavHost`-nak, ami mindig a saját, aktuális induló útvonalát mutatja. Így a
 * splash független attól, hogy az induló útvonal ma a Tudástár listája, vagy a T-703
 * elkészülte után a Kezdőoldal.
 *
 * Az egész felhasználói felület a `:navigation` modul navigációs vázára épül: felső sáv,
 * bottom navigation négy céllal és a NavHost (FUNCTIONAL_SPEC 1.). Az `app` modul
 * szándékosan nem tud a képernyőkről – az útvonalakat a `navigation` modul dönti el, az
 * egyetlen modul, amely minden funkciómodult ismer (ADR-001, L5).
 *
 * A Fázis 1 helyőrző kezdőképernyőt a T-206 cserélte le erre; a splash-ot a T-701 adta
 * hozzá.
 */
@Composable
fun KyokushinApp(modifier: Modifier = Modifier) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MILLIS)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen(modifier = modifier)
    } else {
        KyokushinNavHost(modifier = modifier)
    }
}
