package hu.kyokushin.trainer.navigation

import androidx.annotation.StringRes

/**
 * A bottom navigation négy célja, megjelenési sorrendben (FUNCTIONAL_SPEC 1.).
 *
 * Minden fülhöz egy kezdőképernyő tartozik; a fül akkor kijelölt, ha az éppen látható
 * képernyő ugyanabba a [NavSection] szakaszba esik. Így a Tudástár fül a részletező
 * képernyőn is kijelölve marad, a `settings` pedig a Haladás fülhöz tartozik.
 *
 * Az ikon szándékosan **nem** az enum része: az `ImageVector` Compose-típus, az enum pedig
 * Android nélkül, sima JVM tesztben is betölthető marad (a hozzárendelés a
 * [KyokushinNavHost] `bottomNavIcon` függvényében van).
 *
 * @property screen a fül kezdőképernyője.
 * @property labelRes a fül magyar felirata (NFR-02).
 */
enum class BottomNavDestination(
    val screen: KyokushinScreen,
    @StringRes val labelRes: Int,
) {
    LIBRARY(KyokushinScreen.LIBRARY_LIST, R.string.nav_library),
    LEARN(KyokushinScreen.LEARN_SETUP, R.string.nav_learn),
    QUIZ(KyokushinScreen.QUIZ_SETUP, R.string.nav_quiz),
    PROGRESS(KyokushinScreen.PROGRESS_OVERVIEW, R.string.nav_progress),
    ;

    /** A fül útvonala – ide navigál a megnyomása. */
    val route: String get() = screen.routePattern

    /** Igaz, ha a [current] képernyő ehhez a fülhöz tartozik. */
    fun isSelected(current: KyokushinScreen?): Boolean = current?.section == screen.section

    companion object {
        /** Az a fül, amely a [current] képernyőn kijelölt, vagy `null`, ha egyik sem. */
        fun selectedFor(current: KyokushinScreen?): BottomNavDestination? =
            entries.firstOrNull { it.isSelected(current) }
    }
}

/**
 * Látszik-e a bottom navigation a megadott útvonalon?
 *
 * A futó kvíz és a kvízeredmény képernyőn **nem** (FUNCTIONAL_SPEC 1.): a kvíz nem
 * hagyható el véletlen koppintással, a kilépés explicit megerősítést kér (T-410).
 *
 * Ismeretlen útvonalra `true` a válasz: egy el nem ismert útvonal ne zárja csapdába a
 * felhasználót azzal, hogy elveszi tőle a navigációt.
 */
fun isBottomBarVisible(routePattern: String?): Boolean =
    KyokushinScreen.forRoutePattern(routePattern)?.showsBottomBar ?: true
