package hu.kyokushin.trainer.feature.library.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.core.ui.illustration.TechniqueIllustration
import hu.kyokushin.trainer.core.ui.state.EmptyState
import hu.kyokushin.trainer.core.ui.state.EmptyStateReason
import hu.kyokushin.trainer.core.ui.state.ErrorState
import hu.kyokushin.trainer.core.ui.status.VerificationBadge
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.library.BuildConfig
import hu.kyokushin.trainer.feature.library.R

/**
 * A Tudástár lista képernyő route-szintű bekötése (FR-01…FR-05, FUNCTIONAL_SPEC 3.1).
 *
 * Ez az egyetlen composable a modulban, amely [LibraryListViewModel]-t lát: innentől
 * lefelé minden összetevő [LibraryListUiState]-et és lambda eseményeket kap, nem
 * ViewModelt (L5 – a feature modul nem navigál magától, lambdát ad kifelé).
 *
 * ### A „Diagnosztika" gomb debug kapuja (FR-32, T-210)
 *
 * A teljes validációs jelentés képernyője a `feature/progress` modulban épül fel
 * (T-508) – ez a route csak annyit garantál, hogy a gomb **kizárólag debug buildben**
 * jelenik meg. A [LibraryListScreen] ezt abból tudja, hogy release buildben `null`
 * érkezik az `onDiagnostics` paraméterben: az [EmptyState] komponens pedig
 * dokumentáltan nem rajzol gombot akció nélkül, még akkor sem, ha az oknak lenne
 * címkéje.
 *
 * @param onOpenTechnique navigáció a technika részletező képernyőjére (T-209).
 * @param modifier a szokásos Compose modifier.
 * @param viewModel a Hilt által adott ViewModel; teszt vagy előnézet nem ezt a
 *   konstruktort hívja, hanem közvetlenül a stateless [LibraryListScreen]-t.
 */
@Composable
fun LibraryListRoute(
    onOpenTechnique: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryListScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onToggleCategory = viewModel::onToggleCategory,
        onClearFilters = viewModel::onClearFilters,
        onRetry = viewModel::onRetry,
        onItemClick = onOpenTechnique,
        // A teljes diagnosztika képernyő (T-508) még nem létezik; addig a debug build
        // csak a gomb LÁTHATÓSÁGÁT garantálja (T-210 Teendői), a célja még nincs kijelölve.
        onDiagnostics = if (BuildConfig.DEBUG) ({}) else null,
        modifier = modifier,
    )
}

/**
 * A Tudástár lista képernyő – stateless (FUNCTIONAL_SPEC 3.1, 2.4, 10.).
 *
 * A keresősáv és a kategóriachip-sor a [LibraryListUiState] mind a négy ágán látszik
 * (lásd [LibraryListUiState] osztály-dokumentációja): csak a lista helye vált
 * `Loading`/`Empty`/`Content`/`Error` között.
 *
 * @param onDiagnostics a „Diagnosztika" akció az „A tudástár üres" állapotban, vagy
 *   `null`, ha ezúttal nem jár gomb (release build, FR-32).
 */
@Composable
fun LibraryListScreen(
    uiState: LibraryListUiState,
    onQueryChange: (String) -> Unit,
    onToggleCategory: (CategoryId) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
    onDiagnostics: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Nem elutasítható, nem a UI állapotától függő sáv (ADR-010) – minden ágon látszik.
        UnreviewedBanner(trust = uiState.datasetTrust)

        LibrarySearchField(
            query = uiState.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        CategoryFilterRow(
            categories = uiState.categories,
            onToggleCategory = onToggleCategory,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (uiState) {
                is LibraryListUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                is LibraryListUiState.Empty -> EmptyState(
                    reason = uiState.reason,
                    onAction = libraryEmptyStateAction(uiState.reason, onClearFilters, onDiagnostics),
                    modifier = Modifier.align(Alignment.Center),
                )

                is LibraryListUiState.Error -> ErrorState(
                    onRetry = onRetry,
                    messageRes = uiState.messageRes,
                    modifier = Modifier.align(Alignment.Center),
                )

                is LibraryListUiState.Content -> LibraryTechniqueList(
                    items = uiState.items,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

/**
 * Melyik akció tartozik az üres állapothoz (FUNCTIONAL_SPEC 3.1 táblázata).
 *
 * `internal`, nem `private`: a leképezés önmagában tesztelhető a Compose fa nélkül.
 */
internal fun libraryEmptyStateAction(
    reason: EmptyStateReason,
    onClearFilters: () -> Unit,
    onDiagnostics: (() -> Unit)?,
): (() -> Unit)? = when (reason) {
    is EmptyStateReason.LibraryNoResult -> onClearFilters
    EmptyStateReason.LibraryNoContent -> onDiagnostics
    else -> null
}

/** Keresősáv törlés ikonnal (FUNCTIONAL_SPEC 3.1, FR-03). */
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(text = stringResource(R.string.library_search_hint)) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                // Az IconButton Material 3 alapértelmezett érintési célpontja már 48 dp.
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.library_search_clear),
                    )
                }
            }
        },
    )
}

/**
 * Vízszintesen görgethető kategóriachip-sor, „Összes" chippel (FUNCTIONAL_SPEC 3.1, FR-02).
 *
 * Az „Összes" chip nem egy külön ViewModel-akció: a jelenleg kijelölt kategóriákat
 * egyenként kapcsolja ki [onToggleCategory]-n keresztül, amíg a kijelölés ki nem ürül –
 * ugyanaz a végállapot, mintha a felhasználó egyenként koppintott volna le minden chipet.
 */
@Composable
private fun CategoryFilterRow(
    categories: List<CategoryFilterUiModel>,
    onToggleCategory: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSelected = categories.none { it.selected }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "__all__") {
            FilterChip(
                selected = allSelected,
                onClick = {
                    categories.filter { it.selected }.forEach { onToggleCategory(it.id) }
                },
                label = { Text(text = stringResource(R.string.library_filter_all)) },
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
            )
        }
        items(categories, key = { it.id.value }) { category ->
            FilterChip(
                selected = category.selected,
                onClick = { onToggleCategory(category.id) },
                label = {
                    Text(
                        text = stringResource(
                            R.string.library_filter_count,
                            category.nameHu,
                            category.matchCount,
                        ),
                    )
                },
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
            )
        }
    }
}

/** A találati lista (FUNCTIONAL_SPEC 3.1: kategória `sortOrder`, azon belül romaji ábécé). */
@Composable
private fun LibraryTechniqueList(
    items: List<LibraryTechniqueUiModel>,
    onItemClick: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A lista MÁR rendezett (ViewModel felelőssége) – itt nincs újrarendezés.
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items, key = { it.id.value }) { item ->
            LibraryListItem(item = item, onClick = onItemClick)
            HorizontalDivider()
        }
    }
}

/**
 * Egy listatétel (FUNCTIONAL_SPEC 3.1, 9.).
 *
 * A teljes sor **egy** TalkBack bejárási egység: a belső illusztráció, a két szövegsor
 * és a hitelesítési chip saját leírásai nem hangzanak el külön-külön, mert
 * [clearAndSetSemantics] törli az alábontott (gyermek) szemantikát, és helyette a
 * [LibraryListItemAccessibility.describe] állítja elő a teljes, egységes leírást.
 */
@Composable
private fun LibraryListItem(
    item: LibraryTechniqueUiModel,
    onClick: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val description = LibraryListItemAccessibility.describe(resources, item)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // US-05 1. kritérium / globális DoD: a teljes sor kattintható, ≥ 64 dp magas.
            .heightIn(min = LIST_ITEM_MIN_HEIGHT)
            .clickable(role = Role.Button) { onClick(item.id) }
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // A hely 56 dp szélesen fenntartva akkor is, ha a technika nem hivatkozik
        // illusztrációra: a `null` eset szándékosan NEM helyőrző a listán (T-204), de a
        // sorok egymáshoz igazítása miatt a hely megmarad.
        Box(modifier = Modifier.width(LIST_ILLUSTRATION_SIZE)) {
            TechniqueIllustration(
                illustration = item.illustration,
                label = IllustrationLabel.Technique(romaji = item.romaji, hungarian = item.hungarian),
                awaitingReview = item.status != VerificationStatus.VERIFIED,
                maxWidth = LIST_ILLUSTRATION_SIZE,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.romaji,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.hungarian,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.matchedAlias != null) {
                Text(
                    text = stringResource(R.string.library_matched_alias, item.matchedAlias),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        VerificationBadge(status = item.status)
    }
}

/** A listatétel bal oldali illusztrációjának mérete (FUNCTIONAL_SPEC 3.1: „56×56 dp"). */
private val LIST_ILLUSTRATION_SIZE: Dp = 56.dp

/** A listatétel minimális magassága (FUNCTIONAL_SPEC 3.1, globális DoD). */
private val LIST_ITEM_MIN_HEIGHT: Dp = 64.dp

/** A szűrőchipek minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – mind a négy állapot, világos és sötét témában (T-208, T-210).
// ---------------------------------------------------------------------------------------

private val PREVIEW_CATEGORIES = listOf(
    CategoryFilterUiModel(id = CategoryId.of("dachi"), nameHu = "Állások", matchCount = 2, selected = false),
    CategoryFilterUiModel(id = CategoryId.of("tsuki"), nameHu = "Ütések", matchCount = 1, selected = true),
)

private val PREVIEW_ITEMS = listOf(
    LibraryTechniqueUiModel(
        id = TechniqueId.of("KY-0001"),
        romaji = "Heisoku Dachi",
        hungarian = "Zárt lábú állás",
        categoryId = CategoryId.of("dachi"),
        categoryNameHu = "Állások",
        illustration = IllustrationRef.Placeholder,
        status = VerificationStatus.VERIFIED,
        matchedAlias = null,
    ),
    LibraryTechniqueUiModel(
        id = TechniqueId.of("KY-0003"),
        romaji = "Seiken Chudan Tsuki",
        hungarian = "Középszintű ütés",
        categoryId = CategoryId.of("tsuki"),
        categoryNameHu = "Ütések",
        illustration = null,
        status = VerificationStatus.AWAITING_REVIEW,
        matchedAlias = "chudan zuki",
    ),
)

@Preview(name = "Lista – világos", showBackground = true)
@Preview(
    name = "Lista – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryListScreenContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryListScreen(
            uiState = LibraryListUiState.Content(
                items = PREVIEW_ITEMS,
                query = "",
                categories = PREVIEW_CATEGORIES,
                selectedCategories = setOf(CategoryId.of("tsuki")),
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
            ),
            onQueryChange = {},
            onToggleCategory = {},
            onClearFilters = {},
            onRetry = {},
            onItemClick = {},
        )
    }
}

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(
    name = "Betöltés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryListScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryListScreen(
            uiState = LibraryListUiState.Loading(),
            onQueryChange = {},
            onToggleCategory = {},
            onClearFilters = {},
            onRetry = {},
            onItemClick = {},
        )
    }
}

@Preview(name = "Üres – nincs tartalom (debug) – világos", showBackground = true)
@Preview(
    name = "Üres – nincs tartalom (debug) – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryListScreenEmptyNoContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryListScreen(
            uiState = LibraryListUiState.Empty(
                reason = EmptyStateReason.LibraryNoContent,
                query = "",
                categories = emptyList(),
                selectedCategories = emptySet(),
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
            ),
            onQueryChange = {},
            onToggleCategory = {},
            onClearFilters = {},
            onRetry = {},
            onItemClick = {},
            // Az előnézet a debug ágat mutatja: a gomb csak akkor jelenik meg, ha a
            // hívó ad hozzá műveletet (lásd `LibraryListRoute` dokumentációja).
            onDiagnostics = {},
        )
    }
}

@Preview(name = "Üres – nincs találat – világos", showBackground = true)
@Preview(
    name = "Üres – nincs találat – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryListScreenEmptyNoResultPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryListScreen(
            uiState = LibraryListUiState.Empty(
                reason = EmptyStateReason.LibraryNoResult(query = "mawashi"),
                query = "mawashi",
                categories = PREVIEW_CATEGORIES,
                selectedCategories = emptySet(),
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
            ),
            onQueryChange = {},
            onToggleCategory = {},
            onClearFilters = {},
            onRetry = {},
            onItemClick = {},
        )
    }
}

@Preview(name = "Hiba – világos", showBackground = true)
@Preview(
    name = "Hiba – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryListScreenErrorPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryListScreen(
            uiState = LibraryListUiState.Error(
                messageRes = hu.kyokushin.trainer.core.ui.R.string.error_load_failed,
            ),
            onQueryChange = {},
            onToggleCategory = {},
            onClearFilters = {},
            onRetry = {},
            onItemClick = {},
        )
    }
}
