package hu.kyokushin.trainer.navigation

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.feature.learn.cards.LearnCardsViewModel
import org.junit.Test

/**
 * A navigációs váz szabályai (NFR-03, TR-06, FUNCTIONAL_SPEC 1.).
 *
 * Miért sima JVM teszt, és nem Compose UI teszt? A [KyokushinScreen] és a
 * [BottomNavDestination] szándékosan Android-mentes adat: az útvonalak, a fülhöz
 * tartozás és a bottom nav látszása eldönthető a Compose fa kirenderelése nélkül.
 * A tényleges koppintás-bejárás emulátort igényel – az a T-606 tárgya.
 */
class NavigationGraphTest {
    @Test
    fun navGraph_declaresExactlyTheSpecifiedRoutes_NFR03() {
        val routes = KyokushinScreen.entries.map { it.routePattern }

        assertThat(routes).containsExactly(
            "home",
            "library/list",
            "library/detail/{techniqueId}",
            "learn/setup",
            "learn/cards?categoryIds={categoryIds}",
            "quiz/setup",
            "quiz/run/{sessionId}",
            "quiz/result/{sessionId}",
            "progress/overview",
            "progress/technique/{techniqueId}",
            "settings",
            "progress/diagnostics",
        )
    }

    @Test
    fun navGraph_hasNoDuplicateRoutePattern_NFR03() {
        val routes = KyokushinScreen.entries.map { it.routePattern }

        assertThat(routes).hasSize(routes.toSet().size)
    }

    @Test
    fun navGraph_startsOnTheHomeScreen_FR37() {
        // T-703: a Kezdőoldal váltotta fel a Tudástár listáját induló útvonalként.
        assertThat(KyokushinScreen.startRoute).isEqualTo(KyokushinScreen.HOME.routePattern)
        assertThat(KyokushinScreen.HOME.topLevel).isTrue()
    }

    @Test
    fun bottomNav_hasTheFiveSpecifiedDestinationsInOrder_NFR03_FR37_FR38() {
        val destinations = BottomNavDestination.entries

        // T-703: Főoldal a sor bal szélén. T-704: a Tudástár a sor végére került,
        // minden más fül eggyel balra csúszott.
        assertThat(destinations.map { it.route }).containsExactly(
            "home",
            "learn/setup",
            "quiz/setup",
            "progress/overview",
            "library/list",
        ).inOrder()
    }

    @Test
    fun bottomNav_everyDestinationStartsOnATopLevelScreen_NFR03() {
        BottomNavDestination.entries.forEach { destination ->
            assertThat(destination.screen.topLevel).isTrue()
        }
    }

    @Test
    fun bottomNav_everyScreenBelongsToExactlyOneTab_NFR03() {
        KyokushinScreen.entries.forEach { screen ->
            val selected = BottomNavDestination.entries.filter { it.isSelected(screen) }
            assertThat(selected).hasSize(1)
        }
    }

    @Test
    fun bottomNav_keepsTheOwningTabSelectedOnDeeperScreens_NFR03() {
        assertThat(BottomNavDestination.selectedFor(KyokushinScreen.LIBRARY_DETAIL))
            .isEqualTo(BottomNavDestination.LIBRARY)
        assertThat(BottomNavDestination.selectedFor(KyokushinScreen.LEARN_CARDS))
            .isEqualTo(BottomNavDestination.LEARN)
        assertThat(BottomNavDestination.selectedFor(KyokushinScreen.PROGRESS_TECHNIQUE))
            .isEqualTo(BottomNavDestination.PROGRESS)
    }

    @Test
    fun bottomNav_settingsKeepsTheProgressTabSelected_NFR03() {
        // A `settings` a Haladás szakaszból nyílik, nincs önálló füle (FUNCTIONAL_SPEC 1.).
        assertThat(BottomNavDestination.selectedFor(KyokushinScreen.SETTINGS))
            .isEqualTo(BottomNavDestination.PROGRESS)
    }

    @Test
    fun bottomBar_isHiddenWhileAQuizIsRunning_TR06() {
        assertThat(isBottomBarVisible("quiz/run/{sessionId}")).isFalse()
        assertThat(KyokushinScreen.QUIZ_RUN.showsBottomBar).isFalse()
    }

    @Test
    fun bottomBar_isHiddenOnTheQuizResult_TR06() {
        assertThat(isBottomBarVisible("quiz/result/{sessionId}")).isFalse()
        assertThat(KyokushinScreen.QUIZ_RESULT.showsBottomBar).isFalse()
    }

    @Test
    fun bottomBar_isVisibleOnEveryScreenOutsideTheRunningQuiz_TR06() {
        val hidden = KyokushinScreen.entries.filterNot { it.showsBottomBar }

        assertThat(hidden).containsExactly(
            KyokushinScreen.QUIZ_RUN,
            KyokushinScreen.QUIZ_RESULT,
        )
    }

    @Test
    fun bottomBar_isVisibleOnAnUnknownRoute_TR06() {
        // Ismeretlen útvonal ne zárja csapdába a felhasználót azzal, hogy elveszi a navigációt.
        assertThat(isBottomBarVisible(null)).isTrue()
        assertThat(isBottomBarVisible("valami/ismeretlen")).isTrue()
    }

    @Test
    fun routeLookup_matchesThePatternNotTheFilledRoute_NFR03() {
        // A Navigation Compose a mintát adja vissza a `NavDestination.route`-ban.
        assertThat(KyokushinScreen.forRoutePattern("quiz/run/{sessionId}"))
            .isEqualTo(KyokushinScreen.QUIZ_RUN)
        assertThat(KyokushinScreen.forRoutePattern("nincs/ilyen")).isNull()
    }

    @Test
    fun routeBuilders_fillTheArgumentIntoTheDeclaredPattern_NFR03() {
        assertThat(KyokushinRoutes.libraryDetail("mae-geri"))
            .isEqualTo(
                KyokushinScreen.LIBRARY_DETAIL.routePattern
                    .replace("{" + NavArgs.TECHNIQUE_ID + "}", "mae-geri"),
            )
        assertThat(KyokushinRoutes.progressTechnique("mae-geri"))
            .isEqualTo(
                KyokushinScreen.PROGRESS_TECHNIQUE.routePattern
                    .replace("{" + NavArgs.TECHNIQUE_ID + "}", "mae-geri"),
            )
        assertThat(KyokushinRoutes.quizRun("s-1"))
            .isEqualTo(
                KyokushinScreen.QUIZ_RUN.routePattern
                    .replace("{" + NavArgs.SESSION_ID + "}", "s-1"),
            )
        assertThat(KyokushinRoutes.quizResult("s-1"))
            .isEqualTo(
                KyokushinScreen.QUIZ_RESULT.routePattern
                    .replace("{" + NavArgs.SESSION_ID + "}", "s-1"),
            )
    }

    @Test
    fun argumentCarryingRoutes_useTheSharedArgumentNames_NFR03() {
        val withArguments = KyokushinScreen.entries.filter { it.routePattern.contains("{") }

        // LIBRARY_DETAIL, QUIZ_RUN, QUIZ_RESULT, PROGRESS_TECHNIQUE (path argumentum,
        // `NavArgs`-ból) + LEARN_CARDS (lekérdezőparaméter, `LearnCardsViewModel`
        // kulcsaiból, lásd a következő tesztet).
        assertThat(withArguments).hasSize(5)
        withArguments.filterNot { it == KyokushinScreen.LEARN_CARDS }.forEach { screen ->
            val usesKnownArgument =
                screen.routePattern.contains("{" + NavArgs.TECHNIQUE_ID + "}") ||
                    screen.routePattern.contains("{" + NavArgs.SESSION_ID + "}")
            assertThat(usesKnownArgument).isTrue()
        }
    }

    /**
     * A `learn/cards` lekérdezőparamétere a [LearnCardsViewModel] kulcsát használja,
     * nem a `NavArgs`-t: a Navigation Compose a route argumentumokat automatikusan a cél
     * `SavedStateHandle`-jébe teszi, tehát a ViewModel ugyanazzal a névvel olvassa vissza,
     * amivel ide kerültek (T-303 KDoc-ja, T-304). **Nincs `hideMode` argumentum** (T-706) –
     * az elrejtési irány a kártyaképernyő felső fülváltójával élőben váltható.
     */
    @Test
    fun learnCards_routeArgumentNames_matchTheViewModelKeys_NFR03() {
        assertThat(KyokushinScreen.LEARN_CARDS.routePattern)
            .contains("{${LearnCardsViewModel.KEY_CATEGORY_IDS}}")
    }

    @Test
    fun routeBuilders_learnCards_fillsCategoryIds_NFR03() {
        val built = KyokushinRoutes.learnCards(categoryIds = "dachi,tsuki")

        assertThat(built).isEqualTo(
            KyokushinScreen.LEARN_CARDS.routePattern
                .replace("{${LearnCardsViewModel.KEY_CATEGORY_IDS}}", "dachi,tsuki"),
        )
    }
}
