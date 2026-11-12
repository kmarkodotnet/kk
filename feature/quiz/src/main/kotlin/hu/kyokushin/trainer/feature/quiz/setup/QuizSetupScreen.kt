package hu.kyokushin.trainer.feature.quiz.setup

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.quiz.generator.Shortfall
import hu.kyokushin.trainer.core.quiz.generator.ShortfallReason
import hu.kyokushin.trainer.core.ui.state.ErrorState
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.quiz.R

/**
 * A kvízbeállító képernyő route-szintű bekötése (FR-11, FR-12, FR-13, FR-22,
 * FUNCTIONAL_SPEC 5.1, US-12, T-404).
 *
 * Ez az egyetlen composable a `setup` csomagban, amely [QuizSetupViewModel]-t lát: innen
 * lefelé minden összetevő [QuizSetupUiState]-et és lambda eseményeket kap, nem ViewModelt
 * (L5 – a feature modul nem navigál magától, lambdát ad kifelé). A „Kvíz indítása" gomb
 * eredménye (a [QuizSetupViewModel.startEvent] egyszeri eseménye) ugyanígy kifelé megy: a
 * hívó dönt arról, mi történik egy elkészült [QuizStartResult]-tal (a tényleges navigáció a
 * `quiz/run`-ra és a terv átadása a `QuizRunState`, T-405, bekötésének a feladata).
 *
 * @param onStartQuiz az elindított kvízmenet eredménye – kifelé adott esemény, nem
 *   közvetlen navigáció.
 * @param modifier a szokásos Compose modifier.
 * @param viewModel a Hilt által adott ViewModel; teszt vagy előnézet nem ezt a
 *   konstruktort hívja, hanem közvetlenül a stateless [QuizSetupScreen]-t.
 */
@Composable
fun QuizSetupRoute(
    onStartQuiz: (QuizStartResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.startEvent.collect { result -> onStartQuiz(result) }
    }

    QuizSetupScreen(
        uiState = uiState,
        onToggleCategory = viewModel::onToggleCategory,
        onSelectRequestedCount = viewModel::onSelectRequestedCount,
        onToggleType = viewModel::onToggleType,
        onRetry = viewModel::onRetry,
        onStartQuiz = viewModel::onStartQuiz,
        modifier = modifier,
    )
}

/** A kvízbeállító képernyő – stateless (FUNCTIONAL_SPEC 5.1). */
@Composable
fun QuizSetupScreen(
    uiState: QuizSetupUiState,
    onToggleCategory: (CategoryId) -> Unit,
    onSelectRequestedCount: (Int) -> Unit,
    onToggleType: (QuestionType) -> Unit,
    onRetry: () -> Unit,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            QuizSetupUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            is QuizSetupUiState.Error -> ErrorState(
                onRetry = onRetry,
                messageRes = uiState.messageRes,
                modifier = Modifier.align(Alignment.Center),
            )

            is QuizSetupUiState.Content -> QuizSetupContent(
                state = uiState,
                onToggleCategory = onToggleCategory,
                onSelectRequestedCount = onSelectRequestedCount,
                onToggleType = onToggleType,
                onStartQuiz = onStartQuiz,
            )
        }
    }
}

/**
 * A tényleges beállító tartalom: kategórialista, kérdésszám-választó, kérdéstípusok,
 * élő készenléti összegzés és indítógomb.
 */
@Composable
private fun QuizSetupContent(
    state: QuizSetupUiState.Content,
    onToggleCategory: (CategoryId) -> Unit,
    onSelectRequestedCount: (Int) -> Unit,
    onToggleType: (QuestionType) -> Unit,
    onStartQuiz: () -> Unit,
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
                text = stringResource(R.string.quiz_setup_categories_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            CategoryCheckboxList(categories = state.categories, onToggleCategory = onToggleCategory)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.quiz_setup_count_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            RequestedCountSelector(
                requestedCount = state.requestedCount,
                onSelectRequestedCount = onSelectRequestedCount,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.quiz_setup_types_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            QuestionTypeCheckboxList(questionTypes = state.questionTypes, onToggleType = onToggleType)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // FUNCTIONAL_SPEC 5.1/5.: élő készenléti összegzés – „ennyi kérdés áll
            // össze", vagy a hiány oka (FR-22). Ugyanaz a motorkimenet, amit a
            // `canStart`/`notEnoughDataMessageRes` is használ – lásd `QuizSetupMessages.kt`.
            val summary = quizReadinessSummary(state)
            Text(
                text = stringResource(summary.textRes, *summary.args.toTypedArray()),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // FUNCTIONAL_SPEC 5.1/5.: 0 kérdés esetén a letiltott gomb alatt konkrét
            // javaslat (FR-22).
            if (state.notEnoughDataMessageRes != null) {
                Text(
                    text = stringResource(state.notEnoughDataMessageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Button(
                onClick = onStartQuiz,
                enabled = state.canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(R.string.quiz_setup_start))
            }
        }
    }
}

/**
 * Többszörös választású kategórialista, jelölőnégyzettel (FUNCTIONAL_SPEC 5.1/2.).
 *
 * `Modifier.toggleable` adja a jelölőnégyzet szemantikáját (szerep + kijelölt/nem
 * kijelölt állapot) a teljes sorra, nem csak a [Checkbox]-ra – a TalkBack a sor teljes
 * szövegét (kategórianév + darabszám) egy bejárási egységként mondja ki, ugyanaz a minta,
 * mint a `feature/learn`-ben (lásd `docs/CONVENTIONS.md`).
 */
@Composable
private fun CategoryCheckboxList(
    categories: List<QuizCategoryUiModel>,
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
                            R.string.quiz_setup_category_count,
                            category.quizEligibleCount,
                            category.totalCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Szegmentált kérdésszám-választó: 5 / 10 / 20 (FUNCTIONAL_SPEC 5.1/3., FR-11). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestedCountSelector(
    requestedCount: Int,
    onSelectRequestedCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = QuizSetupViewModel.REQUESTED_COUNT_OPTIONS
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET),
    ) {
        options.forEachIndexed { index, count ->
            SegmentedButton(
                selected = requestedCount == count,
                onClick = { onSelectRequestedCount(count) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(text = count.toString())
            }
        }
    }
}

/**
 * Négy kérdéstípus jelölőnégyzete, letiltási indoklással (FUNCTIONAL_SPEC 5.1/4., FR-12,
 * FR-13). A letiltás oka a sor ALATT mindig látható szöveg, nem `stateDescription` – a
 * TalkBack a normál bejárási sorrendben a letiltott sor után automatikusan kimondja,
 * ugyanaz a minta, mint a `LearnCardSelfRatingRow`-nál (`docs/CONVENTIONS.md` 4.1).
 */
@Composable
private fun QuestionTypeCheckboxList(
    questionTypes: List<QuizQuestionTypeUiModel>,
    onToggleType: (QuestionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        questionTypes.forEach { typeModel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_TOUCH_TARGET)
                    .toggleable(
                        value = typeModel.selected,
                        enabled = typeModel.enabled,
                        role = Role.Checkbox,
                        onValueChange = { onToggleType(typeModel.type) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = typeModel.selected,
                    onCheckedChange = null,
                    enabled = typeModel.enabled,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(questionTypeLabelRes(typeModel.type)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            // FR-13: a letiltás oka TARTALMI, konkrét darabszámmal – nem általános szöveg
            // (T-404 „Kész ha").
            if (typeModel.disabledReasonRes != null) {
                Text(
                    text = stringResource(typeModel.disabledReasonRes, typeModel.disabledReasonArg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = MIN_TOUCH_TARGET, bottom = 4.dp),
                )
            }
        }
    }
}

/** A gomb minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában; tartalom, 0 kérdéses és shortfall eset (T-404
// „Kész ha": „@Preview a 0 kérdéses és a shortfall esetre is").
// ---------------------------------------------------------------------------------------

private val PREVIEW_CATEGORIES = listOf(
    QuizCategoryUiModel(
        id = CategoryId.of("dachi"),
        nameHu = "Állások",
        quizEligibleCount = 8,
        totalCount = 12,
        selected = true,
    ),
    QuizCategoryUiModel(
        id = CategoryId.of("tsuki"),
        nameHu = "Ütések",
        quizEligibleCount = 3,
        totalCount = 6,
        selected = false,
    ),
)

private val PREVIEW_QUESTION_TYPES_ENABLED = listOf(
    QuizQuestionTypeUiModel(QuestionType.JP_TO_HU, selected = true, enabled = true, disabledReasonRes = null, disabledReasonArg = 0),
    QuizQuestionTypeUiModel(QuestionType.HU_TO_JP, selected = true, enabled = true, disabledReasonRes = null, disabledReasonArg = 0),
    QuizQuestionTypeUiModel(
        type = QuestionType.IMAGE_TO_JP,
        selected = false,
        enabled = false,
        disabledReasonRes = R.string.quiz_setup_type_disabled_images,
        disabledReasonArg = 2,
    ),
    QuizQuestionTypeUiModel(
        type = QuestionType.JP_TO_IMAGE,
        selected = false,
        enabled = false,
        disabledReasonRes = R.string.quiz_setup_type_disabled_images,
        disabledReasonArg = 2,
    ),
)

@Preview(name = "Tartalom – indítható – világos", showBackground = true)
@Preview(
    name = "Tartalom – indítható – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizSetupScreenContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizSetupScreen(
            uiState = QuizSetupUiState.Content(
                categories = PREVIEW_CATEGORIES,
                selectedCategories = setOf(CategoryId.of("dachi")),
                requestedCount = 10,
                questionTypes = PREVIEW_QUESTION_TYPES_ENABLED,
                expectedQuestionCount = 10,
                shortfall = null,
                canStart = true,
                notEnoughDataMessageRes = null,
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
            ),
            onToggleCategory = {},
            onSelectRequestedCount = {},
            onToggleType = {},
            onRetry = {},
            onStartQuiz = {},
        )
    }
}

@Preview(name = "Tartalom – hiány (shortfall) – világos", showBackground = true)
@Preview(
    name = "Tartalom – hiány (shortfall) – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizSetupScreenShortfallPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizSetupScreen(
            uiState = QuizSetupUiState.Content(
                categories = PREVIEW_CATEGORIES,
                selectedCategories = setOf(CategoryId.of("dachi")),
                requestedCount = 20,
                questionTypes = PREVIEW_QUESTION_TYPES_ENABLED,
                expectedQuestionCount = 12,
                shortfall = Shortfall(
                    requestedCount = 20,
                    actualCount = 12,
                    reason = ShortfallReason.NOT_ENOUGH_CANDIDATES,
                ),
                canStart = true,
                notEnoughDataMessageRes = null,
                datasetTrust = DatasetTrust.REVIEWED,
            ),
            onToggleCategory = {},
            onSelectRequestedCount = {},
            onToggleType = {},
            onRetry = {},
            onStartQuiz = {},
        )
    }
}

@Preview(name = "Tartalom – 0 kérdés, letiltva – világos", showBackground = true)
@Preview(
    name = "Tartalom – 0 kérdés, letiltva – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizSetupScreenZeroQuestionsPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizSetupScreen(
            uiState = QuizSetupUiState.Content(
                categories = PREVIEW_CATEGORIES.map { it.copy(selected = false) },
                selectedCategories = emptySet(),
                requestedCount = 10,
                questionTypes = PREVIEW_QUESTION_TYPES_ENABLED.map {
                    it.copy(
                        selected = false,
                        enabled = false,
                        disabledReasonRes = R.string.quiz_setup_type_disabled_candidates,
                        disabledReasonArg = 0,
                    )
                },
                expectedQuestionCount = 0,
                shortfall = null,
                canStart = false,
                notEnoughDataMessageRes = R.string.quiz_setup_message_not_enough_data,
                datasetTrust = DatasetTrust.REVIEWED,
            ),
            onToggleCategory = {},
            onSelectRequestedCount = {},
            onToggleType = {},
            onRetry = {},
            onStartQuiz = {},
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
private fun QuizSetupScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizSetupScreen(
            uiState = QuizSetupUiState.Loading,
            onToggleCategory = {},
            onSelectRequestedCount = {},
            onToggleType = {},
            onRetry = {},
            onStartQuiz = {},
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
private fun QuizSetupScreenErrorPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizSetupScreen(
            uiState = QuizSetupUiState.Error(
                messageRes = hu.kyokushin.trainer.core.ui.R.string.error_load_failed,
            ),
            onToggleCategory = {},
            onSelectRequestedCount = {},
            onToggleType = {},
            onRetry = {},
            onStartQuiz = {},
        )
    }
}
