package hu.kyokushin.trainer.feature.progress.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import hu.kyokushin.trainer.core.data.repository.StudyReviewSummary
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.core.ui.illustration.TechniqueIllustration
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.progress.R
import hu.kyokushin.trainer.feature.progress.formatLastPracticed
import java.time.Instant

/**
 * A Haladás áttekintő képernyő route-szintű bekötése (FUNCTIONAL_SPEC 6.1, FR-23, FR-24,
 * FR-26, FR-27, FR-28, T-503/T-504/T-505).
 *
 * Ugyanaz a minta, mint a `QuizResultRoute`-nál: ez az egyetlen composable, amely
 * [ProgressOverviewViewModel]-t lát (L5); a tényleges navigáció kifelé adott lambda.
 *
 * @param onStartQuiz a nulla-adat állapot „Kvíz indítása" gombja (FUNCTIONAL_SPEC
 *   6.1/2.) – a hívó navigál a `quiz/setup`-ra.
 * @param onOpenTechnique a „Gyakorlást igénylő" lista egy tételére koppintva
 *   (FUNCTIONAL_SPEC 6.1/4., T-504 „Teendők" 3.) – a hívó navigál a
 *   `progress/technique/{techniqueId}` útvonalra (T-506).
 * @param onOpenSettings az „Beállítások" link (FUNCTIONAL_SPEC 6.1/6.) – a hívó navigál a
 *   `settings` útvonalra (a tényleges tartalma T-507 tárgya).
 */
@Composable
fun ProgressOverviewRoute(
    onStartQuiz: () -> Unit,
    onOpenTechnique: (TechniqueId) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressOverviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProgressOverviewScreen(
        uiState = uiState,
        onStartQuiz = onStartQuiz,
        onOpenTechnique = onOpenTechnique,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

/** A Haladás áttekintő képernyő – stateless (FUNCTIONAL_SPEC 6.1). */
@Composable
fun ProgressOverviewScreen(
    uiState: ProgressOverviewUiState,
    onStartQuiz: () -> Unit,
    onOpenTechnique: (TechniqueId) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            ProgressOverviewUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            is ProgressOverviewUiState.Content -> ProgressOverviewContent(
                state = uiState,
                onStartQuiz = onStartQuiz,
                onOpenTechnique = onOpenTechnique,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

/**
 * A tényleges tartalom: lektorálatlanság-szalag (ADR-010), összesítő kártya, kategóriánkénti
 * bontás, a „Gyakorlást igénylő" lista (T-504), az önértékelési blokk (T-505) és a
 * „Beállítások" link (FUNCTIONAL_SPEC 6.1).
 */
@Composable
private fun ProgressOverviewContent(
    state: ProgressOverviewUiState.Content,
    onStartQuiz: () -> Unit,
    onOpenTechnique: (TechniqueId) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Nem elutasítható, a UI állapotától független sáv (ADR-010: a Haladás
        // statisztikájának fejlécében ugyanez a megjegyzés jár, mint a kvíz képernyőin).
        UnreviewedBanner(trust = state.datasetTrust)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ProgressOverviewSummaryBlock(score = state.overallScore, onStartQuiz = onStartQuiz)

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.progress_overview_categories_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.categories.forEach { category ->
                    ProgressCategoryRow(item = category)
                }
            }

            HorizontalDivider()

            ProgressNeedsPracticeSection(items = state.needsPractice, onOpenTechnique = onOpenTechnique)

            HorizontalDivider()

            ProgressSelfRatingSection(summary = state.studyReviewSummary)
        }

        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Text(text = stringResource(R.string.progress_overview_settings_link))
        }
    }
}

/**
 * A „Gyakorlást igénylő" lista (FUNCTIONAL_SPEC 6.1/4., FR-26, T-504).
 *
 * Üres lista esetén a „Még nincs elég adat a javaslathoz." üzenet jelenik meg a lista
 * helyén (T-504 „Teendők" 4.) – nem egy üres `Column`, ami a felhasználó számára
 * megkülönböztethetetlen lenne egy hibától.
 */
@Composable
private fun ProgressNeedsPracticeSection(
    items: List<ProgressNeedsPracticeItem>,
    onOpenTechnique: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.progress_overview_needs_practice_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_overview_needs_practice_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    ProgressNeedsPracticeRow(item = item, onClick = { onOpenTechnique(item.techniqueId) })
                }
            }
        }
    }
}

/**
 * Egy tétel a „Gyakorlást igénylő" listában: kis ábra, romaji/magyar név, sikerarány és
 * az utolsó gyakorlás ideje (T-504 „Teendők" 2.) – a teljes sor **egy** TalkBack bejárási
 * egység, ugyanaz a minta, mint a `feature/library` listatételén.
 */
@Composable
private fun ProgressNeedsPracticeRow(
    item: ProgressNeedsPracticeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    // A gyakorlást igénylő lista szerződése szerint `item.score.answered >= 1` (lásd
    // `ProgressOverviewUiState.Content.needsPractice` KDoc-ja), tehát `progressScoreDisplay`
    // itt sosem adhat `NoData`-t.
    val display = progressScoreDisplay(item.score) as ProgressScoreDisplay.Scored
    val lastPracticedText = stringResource(
        R.string.progress_last_practiced,
        formatLastPracticed(item.lastPracticedAt),
    )
    val description = ProgressNeedsPracticeAccessibility.describe(
        resources = resources,
        item = item,
        percent = display.percent,
        lastPracticedText = lastPracticedText,
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET)
                .clickable(role = Role.Button, onClick = onClick)
                .clearAndSetSemantics { contentDescription = description }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.width(NEEDS_PRACTICE_ILLUSTRATION_SIZE)) {
                TechniqueIllustration(
                    illustration = item.illustration,
                    label = IllustrationLabel.Technique(romaji = item.romaji, hungarian = item.hungarian),
                    maxWidth = NEEDS_PRACTICE_ILLUSTRATION_SIZE,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = item.romaji, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = item.hungarian, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(
                        R.string.progress_overview_score_percent,
                        display.percent,
                    ) + " · " + lastPracticedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * Az önértékelési blokk (FUNCTIONAL_SPEC 6.1/5., FR-28, T-505) – vizuálisan **elhatárolt**
 * szakasz (a `HorizontalDivider`-ek a hívóban, [ProgressOverviewContent]-ben), a
 * magyarázó sorral, hogy a két szám (kvíz vs. önértékelés) nem keveredik.
 */
@Composable
private fun ProgressSelfRatingSection(summary: StudyReviewSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.progress_overview_self_rating_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                R.string.progress_overview_self_rating_counts,
                summary.knownCount,
                summary.needsPracticeCount,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.progress_overview_self_rating_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Az összesítő kártya (FUNCTIONAL_SPEC 6.1/2., FR-23, FR-27).
 *
 * Nulla megválaszolt kérdés esetén a kártya **helyén** a „Még nincs adat. Indíts egy
 * kvízt!" üzenet és a hozzá tartozó gomb jelenik meg – nem 0% (lásd [progressScoreDisplay]
 * KDoc-ja, FR-27, T-503 „Kész ha" 1.).
 */
@Composable
private fun ProgressOverviewSummaryBlock(
    score: Score,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val display = progressScoreDisplay(score)) {
        ProgressScoreDisplay.NoData -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.progress_overview_no_data_message),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onStartQuiz, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
                Text(text = stringResource(R.string.progress_overview_start_quiz))
            }
        }

        is ProgressScoreDisplay.Scored -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.progress_overview_score_fraction,
                    display.correct,
                    display.answered,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.progress_overview_score_percent, display.percent),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * Egy kategória sora: név, sáv és **szöveges** érték (FUNCTIONAL_SPEC 6.1/3., FR-24,
 * NFR-04, T-503 „Kész ha" 2.).
 *
 * Próbálkozás nélküli kategóriánál **nincs sáv** – csak a „nincs adat" szöveg. Ez
 * szándékos, nem hiányosság: egy `0f` értékű sáv (akár némán, akár a Material 3
 * beépített `ProgressBarRangeInfo` szemantikájával „0%"-ot jelentene be a TalkBack-nek,
 * pontosan azt a félreértést keltve, amit az FR-27 kizár – lásd a „Kész ha" 1. pontját és
 * `docs/CONVENTIONS.md`.
 */
@Composable
private fun ProgressCategoryRow(item: ProgressCategoryItem, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = item.nameHu, style = MaterialTheme.typography.titleSmall)
            when (val display = progressScoreDisplay(item.score)) {
                ProgressScoreDisplay.NoData -> Text(
                    text = stringResource(R.string.progress_overview_category_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                )

                is ProgressScoreDisplay.Scored -> {
                    // A sáv alatti szöveg hordozza az olvasható értéket (NFR-04) – a
                    // `LinearProgressIndicator` saját, beépített szemantikája (a
                    // Material 3 `ProgressBarRangeInfo`-ja) ugyanezt a számot a
                    // TalkBack-nek is bejelenti, a kettő nem mond ellent egymásnak.
                    Text(
                        text = stringResource(
                            R.string.progress_overview_score_fraction,
                            display.correct,
                            display.answered,
                        ) + " · " + stringResource(R.string.progress_overview_score_percent, display.percent),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { display.percent / PERCENT_SCALE },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** A gombok/linkek minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

/** A „Gyakorlást igénylő" sor ábrájának helye – ugyanaz a méret, mint a Tudástár listáján. */
private val NEEDS_PRACTICE_ILLUSTRATION_SIZE: Dp = 56.dp

private const val PERCENT_SCALE = 100f

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában; betöltés, nulla adat, és próbálkozással/nélküle
// kevert kategóriabontás, gyakorlást igénylő lista és önértékelés (T-503/T-504/T-505 „Kész ha").
// ---------------------------------------------------------------------------------------

private val PREVIEW_CATEGORIES = listOf(
    ProgressCategoryItem(
        categoryId = CategoryId.of("dachi"),
        nameHu = "Állások",
        score = Score(answered = 12, correct = 9, ratio = 0.75),
    ),
    ProgressCategoryItem(
        categoryId = CategoryId.of("tsuki"),
        nameHu = "Ütések",
        score = Score(answered = 0, correct = 0, ratio = null),
    ),
    ProgressCategoryItem(
        categoryId = CategoryId.of("geri"),
        nameHu = "Lábtechnikák",
        score = Score(answered = 5, correct = 2, ratio = 0.4),
    ),
)

private val PREVIEW_NEEDS_PRACTICE = listOf(
    ProgressNeedsPracticeItem(
        techniqueId = TechniqueId.of("KY-0002"),
        romaji = "mae geri",
        hungarian = "egyenes előre rúgás",
        illustration = IllustrationRef.Placeholder,
        score = Score(answered = 4, correct = 1, ratio = 0.25),
        lastPracticedAt = Instant.parse("2026-10-20T08:00:00Z"),
    ),
    ProgressNeedsPracticeItem(
        techniqueId = TechniqueId.of("KY-0003"),
        romaji = "mawashi geri",
        hungarian = "köríves rúgás",
        illustration = null,
        score = Score(answered = 6, correct = 3, ratio = 0.5),
        lastPracticedAt = Instant.parse("2026-11-01T08:00:00Z"),
    ),
)

private val PREVIEW_STUDY_REVIEW_SUMMARY = StudyReviewSummary(knownCount = 9, needsPracticeCount = 4)

private val PREVIEW_CONTENT_WITH_DATA = ProgressOverviewUiState.Content(
    overallScore = Score(answered = 17, correct = 11, ratio = 11.0 / 17.0),
    categories = PREVIEW_CATEGORIES,
    needsPractice = PREVIEW_NEEDS_PRACTICE,
    studyReviewSummary = PREVIEW_STUDY_REVIEW_SUMMARY,
    datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
)

private val PREVIEW_CONTENT_NO_DATA = ProgressOverviewUiState.Content(
    overallScore = Score(answered = 0, correct = 0, ratio = null),
    categories = PREVIEW_CATEGORIES.map { it.copy(score = Score(0, 0, null)) },
    needsPractice = emptyList(),
    studyReviewSummary = StudyReviewSummary(knownCount = 0, needsPracticeCount = 0),
    datasetTrust = DatasetTrust.REVIEWED,
)

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(name = "Betöltés – sötét", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProgressOverviewScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressOverviewScreen(
            uiState = ProgressOverviewUiState.Loading,
            onStartQuiz = {},
            onOpenTechnique = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Van adat – világos", showBackground = true)
@Preview(
    name = "Van adat – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressOverviewScreenWithDataPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressOverviewScreen(
            uiState = PREVIEW_CONTENT_WITH_DATA,
            onStartQuiz = {},
            onOpenTechnique = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Nincs adat – világos", showBackground = true)
@Preview(
    name = "Nincs adat – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressOverviewScreenNoDataPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressOverviewScreen(
            uiState = PREVIEW_CONTENT_NO_DATA,
            onStartQuiz = {},
            onOpenTechnique = {},
            onOpenSettings = {},
        )
    }
}
