package hu.kyokushin.trainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.learn.HideMode
import hu.kyokushin.trainer.feature.learn.cards.LearnCardsRoute
import hu.kyokushin.trainer.feature.learn.cards.LearnCardsViewModel
import hu.kyokushin.trainer.feature.learn.setup.LearnSetupRoute
import hu.kyokushin.trainer.feature.library.detail.LibraryDetailRoute
import hu.kyokushin.trainer.feature.library.list.LibraryListRoute
import hu.kyokushin.trainer.feature.library.list.LibraryListViewModel
import hu.kyokushin.trainer.feature.progress.overview.ProgressOverviewRoute

/**
 * Helyőrző azonosítók a navigációs váz bejárásához.
 *
 * Nem tartalom: egyik sem a tudástárból jön, és egyik sem állít semmit egy technikáról.
 * A funkciómodulok képernyői (T-207-től) a tényleges azonosítókat adják át.
 */
private const val PLACEHOLDER_SESSION_ID = "placeholder-session"

/**
 * Az alkalmazás navigációs váza: felső sáv, bottom navigation és a NavHost
 * (FUNCTIONAL_SPEC 1.).
 *
 * **Miért itt, a `navigation` modulban?** Ez az egyetlen modul, amely minden
 * funkciómodult ismer (ADR-001, L5). A funkciómodulok nem ismerik egymást, és nem
 * ismerik a NavHostot sem: a képernyők eseményeket adnak kifelé (lambdák), az útvonalat
 * ez a modul dönti el.
 *
 * **Állapotmegőrzés fülváltáskor:** a fülre navigálás `saveState`/`restoreState`
 * párossal történik, `launchSingleTop` mellett. Így a Tudástár keresőszövege és
 * listapozíciója megmarad, miközben a visszalépés a gráf kezdőképernyőjére visz.
 *
 * **A futó kvíz nem hagyható el véletlenül:** a `quiz/run` és `quiz/result` képernyőn a
 * bottom navigation nincs összeállítva (lásd [isBottomBarVisible]), és a felső sávban
 * sincs vissza nyíl. A megszakítás explicit megerősítést kérő útja a T-410 tárgya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KyokushinNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentScreen = KyokushinScreen.forRoutePattern(currentRoute)
    val bottomBarVisible = isBottomBarVisible(currentRoute)

    // A vissza nyíl csak akkor jelenik meg, ha a képernyő nem fülkezdő ÉS a bottom nav is
    // látszik: a futó kvízből nem vezet ki egy koppintásos út (FUNCTIONAL_SPEC 1., T-410).
    val backArrowVisible = currentScreen != null && !currentScreen.topLevel && bottomBarVisible

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            currentScreen?.titleRes ?: R.string.nav_title_library_list,
                        ),
                    )
                },
                navigationIcon = {
                    if (backArrowVisible) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.nav_a11y_back),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (bottomBarVisible) {
                KyokushinBottomBar(
                    currentScreen = currentScreen,
                    onDestinationClick = { destination ->
                        navController.navigateToBottomNavDestination(destination)
                    },
                )
            }
        },
    ) { innerPadding ->
        KyokushinNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * A bottom navigation sávja: négy cél, magyar felirattal és ikonnal (FUNCTIONAL_SPEC 1.).
 *
 * A `NavigationBarItem` érintési célpontja Material 3-ban legalább 48 dp magas, és a
 * felirat mindig látszik (`alwaysShowLabel`), hogy az ikon jelentése ne csak alakból
 * derüljön ki.
 */
@Composable
private fun KyokushinBottomBar(
    currentScreen: KyokushinScreen?,
    onDestinationClick: (BottomNavDestination) -> Unit,
) {
    NavigationBar {
        BottomNavDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = destination.isSelected(currentScreen),
                onClick = { onDestinationClick(destination) },
                icon = {
                    Icon(
                        imageVector = bottomNavIcon(destination),
                        // A feliratot a TalkBack a `label`-ből olvassa fel; az ikon
                        // ugyanazt mondaná, ezért dekoratív.
                        contentDescription = null,
                    )
                },
                label = { Text(text = label) },
                alwaysShowLabel = true,
            )
        }
    }
}

/** A fülhöz tartozó ikon. Compose-típus, ezért nem az enumban él. */
private fun bottomNavIcon(destination: BottomNavDestination): ImageVector =
    when (destination) {
        BottomNavDestination.LIBRARY -> Icons.AutoMirrored.Filled.MenuBook
        BottomNavDestination.LEARN -> Icons.Filled.School
        BottomNavDestination.QUIZ -> Icons.Filled.Quiz
        BottomNavDestination.PROGRESS -> Icons.Filled.BarChart
    }

/**
 * Fülre váltás: a fül állapota megmarad, és nem épül végtelen visszalépési lánc.
 *
 * - `popUpTo(startDestination) { saveState = true }` – a korábbi fül állapota elmentve,
 * - `restoreState = true` – a fülre visszatérve az elmentett állapot áll vissza,
 * - `launchSingleTop = true` – ugyanarra a fülre koppintva nem kerül új példány a stackre.
 */
private fun NavController.navigateToBottomNavDestination(destination: BottomNavDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * A navigációs gráf: a FUNCTIONAL_SPEC 1. pontjának tíz útvonala.
 *
 * A funkciómodulok képernyői sorban cserélik le az eredeti [PlaceholderScreen]-t;
 * az útvonalak, az argumentumnevek és a bottom nav viselkedése nem változik. A
 * `library/list` (T-208: [LibraryListRoute]), a `library/detail` (T-209:
 * [LibraryDetailRoute]), a `learn/setup` (T-302: [LearnSetupRoute]), a `learn/cards`
 * (T-304: [LearnCardsRoute]) és a `progress/overview` (T-503: [ProgressOverviewRoute])
 * útvonal már a valódi képernyőt mutatja, a többi cél egyelőre helyőrző – a `quiz/setup`,
 * `quiz/run` és `quiz/result` elkészült képernyőjének a bekötése szándékosan nyitva
 * maradt (lásd `docs/CONVENTIONS.md` 5. szakasz).
 */
@Composable
private fun KyokushinNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = KyokushinScreen.startRoute,
        modifier = modifier,
    ) {
        composable(KyokushinScreen.LIBRARY_LIST.routePattern) {
            LibraryListRoute(
                onOpenTechnique = { techniqueId ->
                    navController.navigate(KyokushinRoutes.libraryDetail(techniqueId.value))
                },
            )
        }

        composable(KyokushinScreen.LIBRARY_DETAIL.routePattern) {
            // A `techniqueId` argumentumot a ViewModel saját `SavedStateHandle`-je olvassa
            // ki közvetlenül a NavBackStackEntry-ből (lásd `LibraryDetailViewModel`
            // dokumentációja) – itt nincs rá szükség.
            LibraryDetailRoute(
                // A kategóriachipre koppintva vissza a listára, arra a kategóriára
                // szűrve (US-07/T-209 Kész ha 2. pont). A Navigation Compose „eredmény
                // visszaadása" mintája: a célként visszakapott SavedStateHandle-be írunk,
                // mielőtt visszalépünk – ugyanaz a SavedStateHandle, amit a lista
                // ViewModelje a kategóriakijelöléshez használ.
                onOpenCategory = { categoryId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(LibraryListViewModel.KEY_CATEGORIES, arrayListOf(categoryId.value))
                    navController.popBackStack()
                },
            )
        }

        composable(KyokushinScreen.LEARN_SETUP.routePattern) {
            LearnSetupRoute(
                // A kijelölt kategóriák és az elrejtési mód a `learn/cards` route
                // lekérdezőparamétereként mennek tovább – a Navigation Compose ezeket
                // automatikusan a cél `SavedStateHandle`-jébe teszi, ugyanazzal a névvel,
                // amivel a `LearnCardsViewModel` olvassa (T-303/T-304, lásd
                // `KyokushinScreen.LEARN_CARDS` KDoc-ja).
                onStartCards = { categoryIds, hideMode ->
                    navController.navigate(
                        KyokushinRoutes.learnCards(
                            categoryIds = categoryIds.joinToString(
                                LearnCardsViewModel.CATEGORY_ID_SEPARATOR,
                            ) { it.value },
                            hideMode = hideMode.name,
                        ),
                    )
                },
            )
        }

        composable(
            route = KyokushinScreen.LEARN_CARDS.routePattern,
            arguments = listOf(
                navArgument(LearnCardsViewModel.KEY_CATEGORY_IDS) { defaultValue = "" },
                navArgument(LearnCardsViewModel.KEY_HIDE_MODE) { defaultValue = HideMode.HIDE_JAPANESE.name },
            ),
        ) {
            LearnCardsRoute(
                // A „×" bezárás és a záróképernyő „Vissza a beállításokhoz" gombja is
                // ide vezet: egyszerű visszalépés a `learn/setup`-ra.
                onExit = { navController.popBackStack() },
                // A `quiz/setup` (T-403/T-404) még nem létezik, tehát a kategóriák
                // tényleges átvétele is csak akkor köthető be, amikor az a képernyő
                // elkészül (ugyanaz a döntés, mint a T-302/T-303-nál, lásd
                // `docs/CONVENTIONS.md` 4.1 szakasza). Itt csak a navigáció történik meg.
                onStartQuiz = { _ ->
                    navController.navigate(KyokushinScreen.QUIZ_SETUP.routePattern)
                },
            )
        }

        composable(KyokushinScreen.QUIZ_SETUP.routePattern) {
            PlaceholderScreen(
                titleRes = KyokushinScreen.QUIZ_SETUP.titleRes,
                actions = listOf(
                    PlaceholderAction(R.string.nav_placeholder_start_quiz) {
                        navController.navigate(KyokushinRoutes.quizRun(PLACEHOLDER_SESSION_ID))
                    },
                ),
            )
        }

        composable(KyokushinScreen.QUIZ_RUN.routePattern) { entry ->
            val sessionId = entry.arguments?.getString(NavArgs.SESSION_ID)
                ?: PLACEHOLDER_SESSION_ID
            PlaceholderScreen(
                titleRes = KyokushinScreen.QUIZ_RUN.titleRes,
                argument = sessionId,
                actions = listOf(
                    PlaceholderAction(R.string.nav_placeholder_finish_quiz) {
                        // A futó kvíz képernyőjét az eredmény VÁLTJA FEL: a befejezett
                        // kvízhez visszalépni nem lehet (FUNCTIONAL_SPEC 1.).
                        navController.navigate(KyokushinRoutes.quizResult(sessionId)) {
                            popUpTo(KyokushinScreen.QUIZ_RUN.routePattern) { inclusive = true }
                        }
                    },
                ),
            )
        }

        composable(KyokushinScreen.QUIZ_RESULT.routePattern) { entry ->
            PlaceholderScreen(
                titleRes = KyokushinScreen.QUIZ_RESULT.titleRes,
                argument = entry.arguments?.getString(NavArgs.SESSION_ID),
                actions = listOf(
                    PlaceholderAction(R.string.nav_placeholder_back_to_quiz_setup) {
                        navController.navigate(KyokushinScreen.QUIZ_SETUP.routePattern) {
                            popUpTo(KyokushinScreen.QUIZ_SETUP.routePattern) { inclusive = true }
                        }
                    },
                ),
            )
        }

        composable(KyokushinScreen.PROGRESS_OVERVIEW.routePattern) {
            // A T-503 a kereten (összesítő kártya + kategóriánkénti bontás) túl még nem
            // adja a „Gyakorlást igénylő" listát (T-504) és az önértékelési blokkot
            // (T-505) – ezért innen még nem nyílik technika-részletező (lásd
            // `docs/CONVENTIONS.md` 5. szakasz).
            ProgressOverviewRoute(
                onStartQuiz = {
                    navController.navigate(KyokushinScreen.QUIZ_SETUP.routePattern)
                },
                onOpenSettings = {
                    navController.navigate(KyokushinScreen.SETTINGS.routePattern)
                },
            )
        }

        composable(KyokushinScreen.PROGRESS_TECHNIQUE.routePattern) { entry ->
            PlaceholderScreen(
                titleRes = KyokushinScreen.PROGRESS_TECHNIQUE.titleRes,
                argument = entry.arguments?.getString(NavArgs.TECHNIQUE_ID),
            )
        }

        composable(KyokushinScreen.SETTINGS.routePattern) {
            PlaceholderScreen(titleRes = KyokushinScreen.SETTINGS.titleRes)
        }
    }
}

@Preview(name = "Bottom nav – világos")
@Composable
private fun KyokushinBottomBarLightPreview() {
    KyokushinTrainerTheme(darkTheme = false, dynamicColor = false) {
        KyokushinBottomBar(
            currentScreen = KyokushinScreen.LIBRARY_LIST,
            onDestinationClick = {},
        )
    }
}

@Preview(name = "Bottom nav – sötét")
@Composable
private fun KyokushinBottomBarDarkPreview() {
    KyokushinTrainerTheme(darkTheme = true, dynamicColor = false) {
        KyokushinBottomBar(
            // A Haladás fül a `settings` képernyőn is kijelölt marad.
            currentScreen = KyokushinScreen.SETTINGS,
            onDestinationClick = {},
        )
    }
}
