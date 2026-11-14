package hu.kyokushin.trainer.feature.learn.setup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.ui.category.categoryIcon
import hu.kyokushin.trainer.core.ui.state.ErrorState
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.learn.R

/**
 * A tanulás-beállító képernyő route-szintű bekötése (FR-06, FUNCTIONAL_SPEC 4.1; a
 * kategória-drill-down T-705, FR-39). Az elrejtési irány (FR-07) T-706 óta **nem**
 * ennek a képernyőnek a bemenete – lásd [onStartCards] dokumentációja.
 *
 * Ez az egyetlen composable a `setup` csomagban, amely [LearnSetupViewModel]-t lát: innen
 * lefelé minden összetevő [LearnSetupUiState]-et és lambda eseményeket kap, nem ViewModelt
 * (L5 – a feature modul nem navigál magától, lambdát ad kifelé).
 *
 * @param onStartCards a „Kártyák indítása" gomb művelete: a kijelölt kategóriák a
 *   kártyaállapot (T-303, `LearnCardsViewModel`) bemenete lesznek. Az elrejtési irányt a
 *   kártyaképernyő felső fülváltója dönti el (T-706), nem ez a hívás.
 * @param modifier a szokásos Compose modifier.
 * @param viewModel a Hilt által adott ViewModel; teszt vagy előnézet nem ezt a
 *   konstruktort hívja, hanem közvetlenül a stateless [LearnSetupScreen]-t.
 */
@Composable
fun LearnSetupRoute(
    onStartCards: (Set<CategoryId>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LearnSetupScreen(
        uiState = uiState,
        onToggleCategory = viewModel::onToggleCategory,
        onOpenCategory = viewModel::onOpenCategory,
        onCloseDrillDown = viewModel::onCloseDrillDown,
        onRetry = viewModel::onRetry,
        onStartCards = onStartCards,
        modifier = modifier,
    )
}

/** A tanulás-beállító képernyő – stateless (FUNCTIONAL_SPEC 4.1, T-705). */
@Composable
fun LearnSetupScreen(
    uiState: LearnSetupUiState,
    onToggleCategory: (CategoryId) -> Unit,
    onOpenCategory: (CategoryId) -> Unit,
    onCloseDrillDown: () -> Unit,
    onRetry: () -> Unit,
    onStartCards: (Set<CategoryId>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            LearnSetupUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            is LearnSetupUiState.Error -> ErrorState(
                onRetry = onRetry,
                messageRes = uiState.messageRes,
                modifier = Modifier.align(Alignment.Center),
            )

            is LearnSetupUiState.Content -> LearnSetupContent(
                state = uiState,
                onToggleCategory = onToggleCategory,
                onOpenCategory = onOpenCategory,
                onCloseDrillDown = onCloseDrillDown,
                onStartCards = { onStartCards(uiState.selectedCategories) },
            )
        }
    }
}

/**
 * A tényleges beállító tartalom: vagy a kategóriarács + indítógomb, vagy – ha
 * [LearnSetupUiState.Content.drillDown] nem `null` – egy kategória technikalistája
 * (T-705). A kettő ugyanazon a képernyőn vált, nem külön route.
 */
@Composable
private fun LearnSetupContent(
    state: LearnSetupUiState.Content,
    onToggleCategory: (CategoryId) -> Unit,
    onOpenCategory: (CategoryId) -> Unit,
    onCloseDrillDown: () -> Unit,
    onStartCards: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drillDown = state.drillDown
    // A rendszer vissza gombja a drill-downból a rácsra lép, nem elhagyja a képernyőt.
    BackHandler(enabled = drillDown != null, onBack = onCloseDrillDown)

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Nem elutasítható, a UI állapotától független sáv (ADR-010) – mindkét
        // al-állapotban (rács és drill-down) is látszik.
        UnreviewedBanner(trust = state.datasetTrust)

        if (drillDown != null) {
            CategoryDrillDownContent(
                drillDown = drillDown,
                onToggleSelected = { onToggleCategory(drillDown.categoryId) },
                onBack = onCloseDrillDown,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.learn_setup_categories_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            CategoryIconGrid(categories = state.categories, onOpenCategory = onOpenCategory)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // FUNCTIONAL_SPEC 4.1/4.: „A letiltás mellett magyarázó szöveg".
            if (state.disabledReasonRes != null) {
                Text(
                    text = stringResource(state.disabledReasonRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = onStartCards,
                enabled = state.canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(R.string.learn_setup_start))
            }
        }
    }
}

/**
 * Kategóriakártyák ikonnal, checkbox helyett (T-705, FR-39) – kettesével tördelve, a
 * `docs/CONVENTIONS.md` "kis, fix elemszámú rács" mintáját követve (9 kategória, nincs
 * lapozás, nem indokolt `LazyVerticalGrid`-et bevezetni egy már `verticalScroll`-ozott
 * szülőbe ágyazva).
 */
@Composable
private fun CategoryIconGrid(
    categories: List<LearnCategoryUiModel>,
    onOpenCategory: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onOpenCategory(category.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Egy kategória kártyája: ikon + név + darabszám, és egy kiválasztottság-jelző (T-705).
 *
 * **Koppintás = a technikalista megnyitása, NEM kijelölés** – a tényleges ki-/
 * bekapcsolás a drill-down fejlécéből történik ([CategoryDrillDownContent]). A kártya
 * csak **mutatja** a kijelölt állapotot (háttérszín **és** egy sarokban megjelenő pipa
 * ikon – NFR-04: nem csak színnel jelezve).
 *
 * **Az ikon szándékosan dekoratív (`contentDescription = null`)**, eltérve a T-705
 * Teendőinek szó szerinti "contentDescription minden kategória-ikonra, a kategória
 * magyar nevével" pontjától: a kártya a `Surface(onClick = …)` miatt egyetlen,
 * összevont (`mergeDescendants`) TalkBack-egység, és a látható `category.nameHu`
 * szöveg már ugyanazon az egységen belül kimondásra kerül – egy azonos szövegű
 * `contentDescription` az ikonon **duplán** mondaná be a nevet, ami pont az az
 * akadálymentességi hiba, amit a projekt a bottom nav ikonjainál (lásd
 * `KyokushinNavHost.KyokushinBottomBar`) már dokumentáltan elkerül. A kijelölt állapotot
 * ehelyett `stateDescription` jelzi.
 */
@Composable
private fun CategoryCard(
    category: LearnCategoryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedDescription = stringResource(R.string.learn_setup_category_a11y_selected)
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .semantics {
                if (category.selected) stateDescription = selectedDescription
            },
        shape = MaterialTheme.shapes.medium,
        color = if (category.selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = categoryIcon(category.id),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = category.nameHu,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.learn_setup_category_count, category.displayableCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (category.selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    // Dekoratív: a `stateDescription` (fent) már bejelenti a kijelölt
                    // állapotot, ugyanúgy, mint a kategórianév mellett.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp),
                )
            }
        }
    }
}

/**
 * Egy kategória technikalistája, böngészhető formában (T-705, FR-39) – a kategóriarács
 * helyén jelenik meg, amíg a felhasználó vissza nem lép.
 */
@Composable
private fun CategoryDrillDownContent(
    drillDown: LearnCategoryDrillDownUiState,
    onToggleSelected: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .heightIn(min = MIN_TOUCH_TARGET)
                    .widthIn(min = MIN_TOUCH_TARGET),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.learn_setup_drilldown_back),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = drillDown.categoryName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))

        if (drillDown.techniques.isEmpty()) {
            Text(
                text = stringResource(R.string.learn_setup_drilldown_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            drillDown.techniques.forEach { technique ->
                TechniqueRow(technique = technique)
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.padding(top = 16.dp))

        Button(
            onClick = onToggleSelected,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Text(
                text = stringResource(
                    if (drillDown.selected) {
                        R.string.learn_setup_drilldown_selected
                    } else {
                        R.string.learn_setup_drilldown_select
                    },
                ),
            )
        }
    }
}

/** Egy böngészhető technikasor a drill-downban: romaji kiemelve, alatta a magyar jelentés. */
@Composable
private fun TechniqueRow(technique: LearnTechniqueUiModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = technique.romaji,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = technique.hungarian,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A gomb minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában, a fő állapotokra (T-302, T-705).
// ---------------------------------------------------------------------------------------

private val PREVIEW_CATEGORIES = listOf(
    LearnCategoryUiModel(id = CategoryId.of("dachi"), nameHu = "Állások", displayableCount = 12, selected = true),
    LearnCategoryUiModel(id = CategoryId.of("tsuki"), nameHu = "Ütések", displayableCount = 8, selected = false),
    LearnCategoryUiModel(id = CategoryId.of("keri"), nameHu = "Rúgások", displayableCount = 5, selected = false),
)

private val PREVIEW_DRILLDOWN_TECHNIQUES = listOf(
    LearnTechniqueUiModel(id = TechniqueId.of("KY-0001"), romaji = "Heisoku Dachi", hungarian = "Zárt lábú állás"),
    LearnTechniqueUiModel(id = TechniqueId.of("KY-0002"), romaji = "Fudo Dachi", hungarian = "Természetes állás"),
)

private val NO_OP_START_CARDS: (Set<CategoryId>) -> Unit = {}

@Preview(name = "Kategóriarács – világos", showBackground = true)
@Preview(
    name = "Kategóriarács – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Content(
                categories = PREVIEW_CATEGORIES,
                selectedCategories = setOf(CategoryId.of("dachi")),
                canStart = true,
                disabledReasonRes = null,
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
            ),
            onToggleCategory = {},
            onOpenCategory = {},
            onCloseDrillDown = {},
            onRetry = {},
            onStartCards = NO_OP_START_CARDS,
        )
    }
}

@Preview(name = "Kategória-drilldown – kijelölve – világos", showBackground = true)
@Preview(
    name = "Kategória-drilldown – kijelölve – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenDrillDownSelectedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Content(
                categories = PREVIEW_CATEGORIES,
                selectedCategories = setOf(CategoryId.of("dachi")),
                canStart = true,
                disabledReasonRes = null,
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
                drillDown = LearnCategoryDrillDownUiState(
                    categoryId = CategoryId.of("dachi"),
                    categoryName = "Állások",
                    techniques = PREVIEW_DRILLDOWN_TECHNIQUES,
                    selected = true,
                ),
            ),
            onToggleCategory = {},
            onOpenCategory = {},
            onCloseDrillDown = {},
            onRetry = {},
            onStartCards = NO_OP_START_CARDS,
        )
    }
}

@Preview(name = "Kategória-drilldown – üres kategória – világos", showBackground = true)
@Preview(
    name = "Kategória-drilldown – üres kategória – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenDrillDownEmptyPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Content(
                categories = PREVIEW_CATEGORIES,
                selectedCategories = emptySet(),
                canStart = false,
                disabledReasonRes = R.string.learn_setup_start_disabled_no_category,
                datasetTrust = DatasetTrust.REVIEWED,
                drillDown = LearnCategoryDrillDownUiState(
                    categoryId = CategoryId.of("keri"),
                    categoryName = "Rúgások",
                    techniques = emptyList(),
                    selected = false,
                ),
            ),
            onToggleCategory = {},
            onOpenCategory = {},
            onCloseDrillDown = {},
            onRetry = {},
            onStartCards = NO_OP_START_CARDS,
        )
    }
}

@Preview(name = "Tartalom – nincs kategória kijelölve – világos", showBackground = true)
@Preview(
    name = "Tartalom – nincs kategória kijelölve – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenDisabledPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Content(
                categories = PREVIEW_CATEGORIES.map { it.copy(selected = false) },
                selectedCategories = emptySet(),
                canStart = false,
                disabledReasonRes = R.string.learn_setup_start_disabled_no_category,
                datasetTrust = DatasetTrust.REVIEWED,
            ),
            onToggleCategory = {},
            onOpenCategory = {},
            onCloseDrillDown = {},
            onRetry = {},
            onStartCards = NO_OP_START_CARDS,
        )
    }
}

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(
    name = "Betöltés – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Loading,
            onToggleCategory = {},
            onOpenCategory = {},
            onCloseDrillDown = {},
            onRetry = {},
            onStartCards = NO_OP_START_CARDS,
        )
    }
}

@Preview(name = "Hiba – világos", showBackground = true)
@Preview(
    name = "Hiba – sötét",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenErrorPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Error(
                messageRes = hu.kyokushin.trainer.core.ui.R.string.error_load_failed,
            ),
            onToggleCategory = {},
            onOpenCategory = {},
            onCloseDrillDown = {},
            onRetry = {},
            onStartCards = NO_OP_START_CARDS,
        )
    }
}
