package hu.kyokushin.trainer.feature.learn.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.ui.state.ErrorState
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.learn.HideMode
import hu.kyokushin.trainer.feature.learn.R

/**
 * A tanulás-beállító képernyő route-szintű bekötése (FR-06, FR-07, FUNCTIONAL_SPEC 4.1).
 *
 * Ez az egyetlen composable a `setup` csomagban, amely [LearnSetupViewModel]-t lát: innen
 * lefelé minden összetevő [LearnSetupUiState]-et és lambda eseményeket kap, nem ViewModelt
 * (L5 – a feature modul nem navigál magától, lambdát ad kifelé).
 *
 * @param onStartCards a „Kártyák indítása" gomb művelete: a kijelölt kategóriák és az
 *   elrejtési mód a kártyaállapot (T-303, `LearnCardsViewModel`) bemenete lesz.
 * @param modifier a szokásos Compose modifier.
 * @param viewModel a Hilt által adott ViewModel; teszt vagy előnézet nem ezt a
 *   konstruktort hívja, hanem közvetlenül a stateless [LearnSetupScreen]-t.
 */
@Composable
fun LearnSetupRoute(
    onStartCards: (Set<CategoryId>, HideMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LearnSetupScreen(
        uiState = uiState,
        onToggleCategory = viewModel::onToggleCategory,
        onSelectHideMode = viewModel::onSelectHideMode,
        onRetry = viewModel::onRetry,
        onStartCards = onStartCards,
        modifier = modifier,
    )
}

/** A tanulás-beállító képernyő – stateless (FUNCTIONAL_SPEC 4.1). */
@Composable
fun LearnSetupScreen(
    uiState: LearnSetupUiState,
    onToggleCategory: (CategoryId) -> Unit,
    onSelectHideMode: (HideMode) -> Unit,
    onRetry: () -> Unit,
    onStartCards: (Set<CategoryId>, HideMode) -> Unit,
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
                onSelectHideMode = onSelectHideMode,
                onStartCards = { onStartCards(uiState.selectedCategories, uiState.hideMode) },
            )
        }
    }
}

/** A tényleges beállító tartalom: kategórialista, elrejtésválasztó, indítógomb. */
@Composable
private fun LearnSetupContent(
    state: LearnSetupUiState.Content,
    onToggleCategory: (CategoryId) -> Unit,
    onSelectHideMode: (HideMode) -> Unit,
    onStartCards: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Nem elutasítható, a UI állapotától független sáv (ADR-010).
        UnreviewedBanner(trust = state.datasetTrust)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.learn_setup_categories_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            CategoryCheckboxList(categories = state.categories, onToggleCategory = onToggleCategory)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.learn_setup_hide_mode_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            HideModeSelector(hideMode = state.hideMode, onSelectHideMode = onSelectHideMode)
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
 * Többszörös választású kategórialista, jelölőnégyzettel (FUNCTIONAL_SPEC 4.1/2.).
 *
 * `Modifier.toggleable` adja a jelölőnégyzet szemantikáját (szerep + kijelölt/nem
 * kijelölt állapot) a teljes sorra, nem csak a [Checkbox]-ra – a TalkBack a sor teljes
 * szövegét (kategórianév + darabszám) egy bejárási egységként mondja ki.
 */
@Composable
private fun CategoryCheckboxList(
    categories: List<LearnCategoryUiModel>,
    onToggleCategory: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_TOUCH_TARGET)
                    .toggleable(
                        value = category.selected,
                        role = Role.Checkbox,
                        onValueChange = { onToggleCategory(category.id) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = category.selected, onCheckedChange = null)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = category.nameHu, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(
                            R.string.learn_setup_category_count,
                            category.displayableCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Két kölcsönösen kizáró lehetőség rádiógombbal (FR-07, FUNCTIONAL_SPEC 4.1/3.). */
@Composable
private fun HideModeSelector(
    hideMode: HideMode,
    onSelectHideMode: (HideMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HideModeOption(
            label = stringResource(R.string.learn_setup_hide_japanese),
            selected = hideMode == HideMode.HIDE_JAPANESE,
            onClick = { onSelectHideMode(HideMode.HIDE_JAPANESE) },
        )
        HideModeOption(
            label = stringResource(R.string.learn_setup_hide_hungarian),
            selected = hideMode == HideMode.HIDE_HUNGARIAN,
            onClick = { onSelectHideMode(HideMode.HIDE_HUNGARIAN) },
        )
    }
}

@Composable
private fun HideModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A gomb minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában, a három állapotra (T-302).
// ---------------------------------------------------------------------------------------

private val PREVIEW_CATEGORIES = listOf(
    LearnCategoryUiModel(id = CategoryId.of("dachi"), nameHu = "Állások", displayableCount = 12, selected = true),
    LearnCategoryUiModel(id = CategoryId.of("tsuki"), nameHu = "Ütések", displayableCount = 8, selected = false),
)

@Preview(name = "Tartalom – indítható – világos", showBackground = true)
@Preview(
    name = "Tartalom – indítható – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Content(
                categories = PREVIEW_CATEGORIES,
                selectedCategories = setOf(CategoryId.of("dachi")),
                hideMode = HideMode.HIDE_JAPANESE,
                canStart = true,
                disabledReasonRes = null,
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
            ),
            onToggleCategory = {},
            onSelectHideMode = {},
            onRetry = {},
            onStartCards = { _, _ -> },
        )
    }
}

@Preview(name = "Tartalom – nincs kategória kijelölve – világos", showBackground = true)
@Preview(
    name = "Tartalom – nincs kategória kijelölve – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnSetupScreenDisabledPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Content(
                categories = PREVIEW_CATEGORIES.map { it.copy(selected = false) },
                selectedCategories = emptySet(),
                hideMode = HideMode.HIDE_HUNGARIAN,
                canStart = false,
                disabledReasonRes = R.string.learn_setup_start_disabled_no_category,
                datasetTrust = DatasetTrust.REVIEWED,
            ),
            onToggleCategory = {},
            onSelectHideMode = {},
            onRetry = {},
            onStartCards = { _, _ -> },
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
private fun LearnSetupScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Loading,
            onToggleCategory = {},
            onSelectHideMode = {},
            onRetry = {},
            onStartCards = { _, _ -> },
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
private fun LearnSetupScreenErrorPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnSetupScreen(
            uiState = LearnSetupUiState.Error(
                messageRes = hu.kyokushin.trainer.core.ui.R.string.error_load_failed,
            ),
            onToggleCategory = {},
            onSelectHideMode = {},
            onRetry = {},
            onStartCards = { _, _ -> },
        )
    }
}
