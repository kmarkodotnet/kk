package hu.kyokushin.trainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.collectAsState
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
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.quiz.generator.QuizRunStateCodec
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.home.HomeRoute
import hu.kyokushin.trainer.feature.learn.cards.LearnCardsRoute
import hu.kyokushin.trainer.feature.learn.cards.LearnCardsViewModel
import hu.kyokushin.trainer.feature.learn.setup.LearnSetupRoute
import hu.kyokushin.trainer.feature.library.detail.LibraryDetailRoute
import hu.kyokushin.trainer.feature.library.list.LibraryListRoute
import hu.kyokushin.trainer.feature.library.list.LibraryListViewModel
import hu.kyokushin.trainer.feature.progress.diagnostics.ProgressDiagnosticsRoute
import hu.kyokushin.trainer.feature.progress.overview.ProgressOverviewRoute
import hu.kyokushin.trainer.feature.progress.settings.ProgressSettingsRoute
import hu.kyokushin.trainer.feature.progress.technique.ProgressTechniqueRoute
import hu.kyokushin.trainer.feature.quiz.result.QuizResultRoute
import hu.kyokushin.trainer.feature.quiz.run.QuizRunRoute
import hu.kyokushin.trainer.feature.quiz.run.QuizRunViewModel
import hu.kyokushin.trainer.feature.quiz.setup.QuizSetupRoute
import hu.kyokushin.trainer.feature.quiz.setup.QuizStartResult

private const val CATEGORY_PRESET = "quiz.categoryPreset"

/** A terv a cél back stack bejegyzésében menthető szöveg, sosem URL-paraméter. */
private fun NavHostController.startQuiz(start: QuizStartResult, replaceResult: Boolean = false) {
    val route = KyokushinRoutes.quizRun(start.sessionId.value)
    navigate(route) {
        if (replaceResult) popUpTo(KyokushinScreen.QUIZ_RESULT.routePattern) { inclusive = true }
    }
    getBackStackEntry(route).savedStateHandle.apply {
        set(QuizRunViewModel.KEY_PLAN, QuizRunStateCodec.encodePlan(start.plan))
        set(QuizRunViewModel.KEY_SEED, start.seed)
    }
}

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
                            currentScreen?.titleRes ?: R.string.nav_title_home,
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
        BottomNavDestination.HOME -> Icons.Filled.Home
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

/** A működő funkcióképernyők összekötése (T-611). */
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
        composable(KyokushinScreen.HOME.routePattern) {
            HomeRoute()
        }

        composable(KyokushinScreen.LIBRARY_LIST.routePattern) {
            LibraryListRoute(
                onDiagnostics = { navController.navigate(KyokushinScreen.DIAGNOSTICS.routePattern) },
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
                // A kijelölt kategóriák a `learn/cards` route lekérdezőparaméterként
                // mennek tovább – a Navigation Compose ezt automatikusan a cél
                // `SavedStateHandle`-jébe teszi, ugyanazzal a névvel, amivel a
                // `LearnCardsViewModel` olvassa (T-303/T-304, lásd
                // `KyokushinScreen.LEARN_CARDS` KDoc-ja). Az elrejtési irányt T-706 óta a
                // kártyaképernyő felső fülváltója dönti el, nem ez a route.
                onStartCards = { categoryIds ->
                    navController.navigate(
                        KyokushinRoutes.learnCards(
                            categoryIds = categoryIds.joinToString(
                                LearnCardsViewModel.CATEGORY_ID_SEPARATOR,
                            ) { it.value },
                        ),
                    )
                },
            )
        }

        composable(
            route = KyokushinScreen.LEARN_CARDS.routePattern,
            arguments = listOf(
                navArgument(LearnCardsViewModel.KEY_CATEGORY_IDS) { defaultValue = "" },
            ),
        ) {
            LearnCardsRoute(
                // A „×" bezárás és a záróképernyő „Vissza a beállításokhoz" gombja is
                // ide vezet: egyszerű visszalépés a `learn/setup`-ra.
                onExit = { navController.popBackStack() },
                onStartQuiz = { categories ->
                    navController.navigate(KyokushinScreen.QUIZ_SETUP.routePattern)
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        CATEGORY_PRESET,
                        ArrayList(categories.map { it.value }),
                    )
                },
            )
        }

        composable(KyokushinScreen.QUIZ_SETUP.routePattern) { entry ->
            val preset by entry.savedStateHandle
                .getStateFlow<ArrayList<String>?>(CATEGORY_PRESET, null).collectAsState()
            QuizSetupRoute(
                categoryPreset = preset,
                onPresetConsumed = { entry.savedStateHandle[CATEGORY_PRESET] = null },
                onStartQuiz = { navController.startQuiz(it) },
            )
        }

        composable(KyokushinScreen.QUIZ_RUN.routePattern) { entry ->
            val plan = entry.savedStateHandle.get<String>(QuizRunViewModel.KEY_PLAN)
            val seed = entry.savedStateHandle.get<Long>(QuizRunViewModel.KEY_SEED)
            val sessionId = requireNotNull(entry.arguments?.getString(NavArgs.SESSION_ID))
            val start = if (plan != null && seed != null) {
                runCatching {
                    QuizStartResult(SessionId.of(sessionId), QuizRunStateCodec.decodePlan(plan), seed)
                }.getOrNull()
            } else {
                null
            }
            QuizRunRoute(
                initialStart = start,
                onOpenTechnique = { navController.navigate(KyokushinRoutes.libraryDetail(it.value)) },
                onExit = { navController.popBackStack() },
                onFinish = {
                    navController.navigate(KyokushinRoutes.quizResult(it.value)) {
                        popUpTo(KyokushinScreen.QUIZ_RUN.routePattern) { inclusive = true }
                    }
                },
            )
        }

        composable(KyokushinScreen.QUIZ_RESULT.routePattern) {
            QuizResultRoute(
                onOpenTechnique = { navController.navigate(KyokushinRoutes.libraryDetail(it.value)) },
                onRetryWrong = { navController.startQuiz(it, replaceResult = true) },
                onNewQuiz = { navController.popBackStack() },
                onBackToProgress = {
                    navController.popBackStack()
                    navController.navigateToBottomNavDestination(BottomNavDestination.PROGRESS)
                },
            )
        }

        composable(KyokushinScreen.PROGRESS_OVERVIEW.routePattern) {
            // A „Gyakorlást igénylő" lista (T-504) tételére koppintva a technika-
            // részletezőre navigál – ugyanaz a minta, mint a Tudástár listájánál
            // (`LIBRARY_LIST` ág fent).
            ProgressOverviewRoute(
                onStartQuiz = {
                    navController.navigate(KyokushinScreen.QUIZ_SETUP.routePattern)
                },
                onOpenTechnique = { techniqueId ->
                    navController.navigate(KyokushinRoutes.progressTechnique(techniqueId.value))
                },
                onOpenSettings = {
                    navController.navigate(KyokushinScreen.SETTINGS.routePattern)
                },
            )
        }

        composable(KyokushinScreen.PROGRESS_TECHNIQUE.routePattern) {
            // A `techniqueId` argumentumot a ViewModel saját `SavedStateHandle`-je olvassa
            // ki közvetlenül a NavBackStackEntry-ből (lásd `ProgressTechniqueViewModel`
            // dokumentációja) – itt nincs rá szükség.
            ProgressTechniqueRoute(
                onOpenLibraryDetail = { techniqueId ->
                    navController.navigate(KyokushinRoutes.libraryDetail(techniqueId.value))
                },
            )
        }

        if (BuildConfig.DEBUG) {
            composable(KyokushinScreen.DIAGNOSTICS.routePattern) {
                ProgressDiagnosticsRoute()
            }
        }

        composable(KyokushinScreen.SETTINGS.routePattern) {
            ProgressSettingsRoute(
                onOpenDiagnostics = { navController.navigate(KyokushinScreen.DIAGNOSTICS.routePattern) },
            )
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
