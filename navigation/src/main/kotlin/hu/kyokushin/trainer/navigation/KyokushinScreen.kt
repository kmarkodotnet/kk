package hu.kyokushin.trainer.navigation

import androidx.annotation.StringRes
import hu.kyokushin.trainer.feature.learn.cards.LearnCardsViewModel

/**
 * Az alkalmazás navigációs szakaszai – a bottom navigation négy célja (FUNCTIONAL_SPEC 1.).
 *
 * A `settings` képernyőnek nincs önálló füle: a Haladás szakaszból nyílik, ezért a
 * [NavSection.PROGRESS] szakaszhoz tartozik, és megnyitva is a Haladás fül marad kijelölve.
 */
enum class NavSection {
    LIBRARY,
    LEARN,
    QUIZ,
    PROGRESS,
}

/**
 * Az alkalmazás összes navigációs célja, útvonalminta szerint (FUNCTIONAL_SPEC 1.).
 *
 * Ez a felsorolás az útvonalak egyetlen hiteles forrása: a [KyokushinNavHost] ebből
 * építi a gráfot, a felső sáv címét és a bottom navigation kijelölését is innen veszi.
 * Így nem lehet olyan képernyő, amelynek útvonala van, de a fül kijelölése vagy a
 * bottom nav látszása nincs eldöntve.
 *
 * Tiszta Kotlin adat (az erőforrás-azonosító csak `Int`), ezért Android nélkül,
 * sima JVM unit tesztben ellenőrizhető.
 *
 * @property routePattern a Navigation Compose útvonalminta, pontosan ahogy a gráfban szerepel.
 * @property titleRes a felső sáv magyar címe (NFR-02: minden szöveg erőforrásból).
 * @property section melyik bottom nav fülhöz tartozik a képernyő.
 * @property topLevel igaz, ha a képernyő egy fül kezdőképernyője (nincs vissza nyíl).
 * @property showsBottomBar hamis, ha a bottom nav el van rejtve ezen a képernyőn.
 */
enum class KyokushinScreen(
    val routePattern: String,
    @StringRes val titleRes: Int,
    val section: NavSection,
    val topLevel: Boolean,
    val showsBottomBar: Boolean,
) {
    LIBRARY_LIST(
        routePattern = "library/list",
        titleRes = R.string.nav_title_library_list,
        section = NavSection.LIBRARY,
        topLevel = true,
        showsBottomBar = true,
    ),
    LIBRARY_DETAIL(
        routePattern = "library/detail/{${NavArgs.TECHNIQUE_ID}}",
        titleRes = R.string.nav_title_library_detail,
        section = NavSection.LIBRARY,
        topLevel = false,
        showsBottomBar = true,
    ),
    LEARN_SETUP(
        routePattern = "learn/setup",
        titleRes = R.string.nav_title_learn_setup,
        section = NavSection.LEARN,
        topLevel = true,
        showsBottomBar = true,
    ),

    /**
     * A `categoryIds`/`hideMode` opcionális lekérdezőparaméter neve **szándékosan**
     * egyezik a [LearnCardsViewModel.KEY_CATEGORY_IDS]/[LearnCardsViewModel.KEY_HIDE_MODE]
     * kulccsal (T-303 KDoc-ja, T-304): a Navigation Compose a route argumentumokat
     * automatikusan a cél `NavBackStackEntry` `SavedStateHandle`-jébe teszi, tehát a
     * ViewModel ugyanazzal a kulccsal olvassa vissza, amivel ide kerültek – lásd
     * [KyokushinRoutes.learnCards].
     */
    LEARN_CARDS(
        routePattern = "learn/cards?${LearnCardsViewModel.KEY_CATEGORY_IDS}={${LearnCardsViewModel.KEY_CATEGORY_IDS}}" +
            "&${LearnCardsViewModel.KEY_HIDE_MODE}={${LearnCardsViewModel.KEY_HIDE_MODE}}",
        titleRes = R.string.nav_title_learn_cards,
        section = NavSection.LEARN,
        topLevel = false,
        showsBottomBar = true,
    ),
    QUIZ_SETUP(
        routePattern = "quiz/setup",
        titleRes = R.string.nav_title_quiz_setup,
        section = NavSection.QUIZ,
        topLevel = true,
        showsBottomBar = true,
    ),

    /**
     * Futó kvíz. A bottom nav itt **el van rejtve**, hogy a kvíz ne legyen véletlenül
     * elhagyható (FUNCTIONAL_SPEC 1.). A kilépés explicit megerősítéssel történik – azt a
     * T-410 építi meg.
     */
    QUIZ_RUN(
        routePattern = "quiz/run/{${NavArgs.SESSION_ID}}",
        titleRes = R.string.nav_title_quiz_run,
        section = NavSection.QUIZ,
        topLevel = false,
        showsBottomBar = false,
    ),

    /** Kvízeredmény. A bottom nav itt is rejtve marad (FUNCTIONAL_SPEC 1.). */
    QUIZ_RESULT(
        routePattern = "quiz/result/{${NavArgs.SESSION_ID}}",
        titleRes = R.string.nav_title_quiz_result,
        section = NavSection.QUIZ,
        topLevel = false,
        showsBottomBar = false,
    ),
    PROGRESS_OVERVIEW(
        routePattern = "progress/overview",
        titleRes = R.string.nav_title_progress_overview,
        section = NavSection.PROGRESS,
        topLevel = true,
        showsBottomBar = true,
    ),
    PROGRESS_TECHNIQUE(
        routePattern = "progress/technique/{${NavArgs.TECHNIQUE_ID}}",
        titleRes = R.string.nav_title_progress_technique,
        section = NavSection.PROGRESS,
        topLevel = false,
        showsBottomBar = true,
    ),
    DIAGNOSTICS(
        routePattern = "progress/diagnostics",
        titleRes = R.string.nav_title_diagnostics,
        section = NavSection.PROGRESS,
        topLevel = false,
        showsBottomBar = true,
    ),
    SETTINGS(
        routePattern = "settings",
        titleRes = R.string.nav_title_settings,
        section = NavSection.PROGRESS,
        topLevel = false,
        showsBottomBar = true,
    ),
    ;

    companion object {
        /**
         * A megadott útvonalmintához tartozó képernyő, vagy `null`, ha nincs ilyen.
         *
         * A Navigation Compose a `NavDestination.route`-ban a **mintát** adja vissza
         * (a behelyettesített argumentumok nélkül), ezért pontos egyezést keresünk.
         */
        fun forRoutePattern(routePattern: String?): KyokushinScreen? =
            entries.firstOrNull { it.routePattern == routePattern }

        /** Az alkalmazás indulási útvonala: a Tudástár listája (FUNCTIONAL_SPEC 1.). */
        val startRoute: String get() = LIBRARY_LIST.routePattern
    }
}

/** Az útvonal-argumentumok nevei. Egy helyen, hogy a minta és a kiolvasás ne csúszhasson el. */
object NavArgs {
    const val TECHNIQUE_ID: String = "techniqueId"
    const val SESSION_ID: String = "sessionId"
}

/**
 * Az argumentumot hordozó útvonalak felépítése.
 *
 * A Navigation Compose argumentumai szövegesek, ezért a hívó a tipizált azonosító
 * `value` mezőjét adja át; a visszaolvasáskor a cél modul állítja vissza a típust
 * (`TechniqueId.of(...)`). Az útvonal nem domain határ, hanem szövegminta.
 */
object KyokushinRoutes {
    fun libraryDetail(techniqueId: String): String = "library/detail/$techniqueId"

    /**
     * A `learn/setup` (T-302) kijelölése a `learn/cards` (T-304) bemenete.
     *
     * @param categoryIds a [LearnCardsViewModel.CATEGORY_ID_SEPARATOR]-rel elválasztott
     *   kategória-azonosítók, ugyanabban az alakban, ahogy a `LearnCardsViewModel` olvassa.
     * @param hideMode a `HideMode` enum neve (`HideMode.name`).
     */
    fun learnCards(categoryIds: String, hideMode: String): String =
        "learn/cards?${LearnCardsViewModel.KEY_CATEGORY_IDS}=$categoryIds" +
            "&${LearnCardsViewModel.KEY_HIDE_MODE}=$hideMode"

    fun quizRun(sessionId: String): String = "quiz/run/$sessionId"

    fun quizResult(sessionId: String): String = "quiz/result/$sessionId"

    fun progressTechnique(techniqueId: String): String = "progress/technique/$techniqueId"
}
