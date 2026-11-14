package hu.kyokushin.trainer.feature.progress.technique

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.StudyReview
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.core.ui.illustration.TechniqueIllustration
import hu.kyokushin.trainer.core.ui.state.ErrorState
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.progress.R
import hu.kyokushin.trainer.feature.progress.formatLastPracticed
import hu.kyokushin.trainer.feature.progress.overview.ProgressScoreDisplay
import hu.kyokushin.trainer.feature.progress.overview.progressScoreDisplay
import java.time.Instant

/**
 * A technika-részletező route-szintű bekötése (FUNCTIONAL_SPEC 6.2, FR-25, FR-28, DR-07,
 * T-506).
 *
 * Ugyanaz a minta, mint a `feature/library` `LibraryDetailRoute`-nál: ez az egyetlen
 * composable a `technique` csomagban, amely [ProgressTechniqueViewModel]-t lát (L5).
 *
 * @param onOpenLibraryDetail a „Megnyitás a Tudástárban" link (T-506 „Teendők" 1.) – a
 *   hívó navigál a `library/detail/{techniqueId}` útvonalra.
 */
@Composable
fun ProgressTechniqueRoute(
    onOpenLibraryDetail: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressTechniqueViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProgressTechniqueScreen(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onOpenLibraryDetail = onOpenLibraryDetail,
        modifier = modifier,
    )
}

/**
 * A technika-részletező – stateless (FUNCTIONAL_SPEC 6.2, 2.4, 10.).
 *
 * A lektorálatlanság-szalag mindhárom ágon látszik (ugyanaz a döntés, mint a Tudástár
 * részletezőjén) – csak a tartalom helye vált `Loading`/`Content`/`Error` között.
 */
@Composable
fun ProgressTechniqueScreen(
    uiState: ProgressTechniqueUiState,
    onRetry: () -> Unit,
    onOpenLibraryDetail: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        UnreviewedBanner(trust = uiState.datasetTrust)

        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is ProgressTechniqueUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                is ProgressTechniqueUiState.Error -> ErrorState(
                    onRetry = onRetry,
                    messageRes = uiState.messageRes,
                    modifier = Modifier.align(Alignment.Center),
                )

                is ProgressTechniqueUiState.Content -> ProgressTechniqueContent(
                    content = uiState,
                    onOpenLibraryDetail = onOpenLibraryDetail,
                )
            }
        }
    }
}

/** A betöltött technika statisztikája (FUNCTIONAL_SPEC 6.2 1–4. pont). */
@Composable
private fun ProgressTechniqueContent(
    content: ProgressTechniqueUiState.Content,
    onOpenLibraryDetail: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Ugyanaz a döntés, mint a `LibraryDetailContent`-nél: a `null` itt
        // helyőrzővé válik (a részletezőn, nem a listán, T-204).
        TechniqueIllustration(
            illustration = content.illustration ?: IllustrationRef.Placeholder,
            label = IllustrationLabel.Technique(romaji = content.romaji, hungarian = content.hungarian),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = content.romaji, style = MaterialTheme.typography.headlineMedium)
        Text(text = content.hungarian, style = MaterialTheme.typography.titleLarge)

        TextButton(
            onClick = { onOpenLibraryDetail(content.id) },
            modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Text(text = stringResource(R.string.progress_technique_open_library_detail))
        }

        if (content.retired) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.progress_technique_retired_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        HorizontalDivider()

        ProgressTechniqueStatsSection(
            score = content.score,
            lastPracticedAt = content.lastPracticedAt,
            byQuestionType = content.byQuestionType,
        )

        HorizontalDivider()

        ProgressTechniqueSelfRatingHistorySection(history = content.selfRatingHistory)
    }
}

/**
 * Az összesített kvízstatisztika és a kérdéstípus szerinti bontás (FUNCTIONAL_SPEC 6.2
 * 2–3. pont, FR-25).
 *
 * Nulla megválaszolt kérdés esetén „nincs adat" jár, nem 0% (FR-27) – ugyanaz a
 * [progressScoreDisplay] döntési logika, mint az áttekintő képernyőn (nem duplikált
 * megfogalmazás, DR-09).
 */
@Composable
private fun ProgressTechniqueStatsSection(
    score: Score,
    lastPracticedAt: Instant?,
    byQuestionType: Map<QuestionType, Score>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.progress_technique_stats_title),
            style = MaterialTheme.typography.titleMedium,
        )
        when (val display = progressScoreDisplay(score)) {
            ProgressScoreDisplay.NoData -> Text(
                text = stringResource(R.string.progress_technique_stats_no_data),
                style = MaterialTheme.typography.bodyMedium,
            )

            is ProgressScoreDisplay.Scored -> {
                Text(
                    text = stringResource(
                        R.string.progress_overview_score_fraction,
                        display.correct,
                        display.answered,
                    ) + " · " + stringResource(R.string.progress_overview_score_percent, display.percent),
                    style = MaterialTheme.typography.bodyLarge,
                )
                // `score.answered > 0` esetén a szerződés szerint sosem `null` (lásd
                // `TechniqueProgress` KDoc-ja a `core/data`-ban).
                lastPracticedAt?.let {
                    Text(
                        text = stringResource(R.string.progress_last_practiced, formatLastPracticed(it)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (byQuestionType.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.progress_technique_by_question_type_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        byQuestionType.forEach { (type, typeScore) ->
                            ProgressTechniqueQuestionTypeRow(type = type, score = typeScore)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Egy kérdéstípus sora a bontásban. A [score] e lista szerződése szerint mindig legalább
 * egy próbálkozással rendelkezik (lásd `TechniqueProgress.byQuestionType` KDoc-ja a
 * `core/data`-ban: csak az a típus szerepel kulcsként, amelyben volt próbálkozás).
 */
@Composable
private fun ProgressTechniqueQuestionTypeRow(type: QuestionType, score: Score, modifier: Modifier = Modifier) {
    val display = progressScoreDisplay(score) as ProgressScoreDisplay.Scored
    Text(
        text = stringResource(ProgressTechniqueLabels.questionTypeLabelRes(type)) + ": " +
            stringResource(R.string.progress_overview_score_percent, display.percent),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

/**
 * Az önértékelési előzmény, külön blokkban (FUNCTIONAL_SPEC 6.2 4. pont, FR-28, T-506
 * „Teendők" 4.) – soha nem hat a kvízstatisztikára, ugyanaz a döntés, mint az áttekintő
 * képernyő önértékelési blokkjánál (T-505).
 */
@Composable
private fun ProgressTechniqueSelfRatingHistorySection(history: List<StudyReview>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.progress_technique_self_rating_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (history.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_technique_self_rating_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                history.forEach { review ->
                    Text(
                        text = stringResource(ProgressTechniqueLabels.selfRatingLabelRes(review.selfRating)) +
                            " · " +
                            stringResource(R.string.progress_last_practiced, formatLastPracticed(review.reviewedAt)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** A gombok minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában; betöltés, hiba, aktív és visszavont technika,
// próbálkozás nélküli és önértékelés nélküli állapot (T-506 „Kész ha").
// ---------------------------------------------------------------------------------------

private val PREVIEW_CONTENT = ProgressTechniqueUiState.Content(
    id = TechniqueId.of("KY-0002"),
    romaji = "mae geri",
    hungarian = "egyenes előre rúgás",
    illustration = IllustrationRef.Placeholder,
    retired = false,
    score = Score(answered = 10, correct = 7, ratio = 0.7),
    lastPracticedAt = Instant.parse("2026-11-10T08:00:00Z"),
    byQuestionType = mapOf(
        QuestionType.JP_TO_HU to Score(answered = 6, correct = 5, ratio = 5.0 / 6.0),
        QuestionType.IMAGE_TO_JP to Score(answered = 4, correct = 2, ratio = 0.5),
    ),
    selfRatingHistory = listOf(
        StudyReview(
            id = 2,
            techniqueId = TechniqueId.of("KY-0002"),
            selfRating = SelfRating.KNEW,
            reviewedAt = Instant.parse("2026-11-09T08:00:00Z"),
        ),
        StudyReview(
            id = 1,
            techniqueId = TechniqueId.of("KY-0002"),
            selfRating = SelfRating.NEEDS_PRACTICE,
            reviewedAt = Instant.parse("2026-10-20T08:00:00Z"),
        ),
    ),
    datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
)

private val PREVIEW_CONTENT_RETIRED_NO_DATA = PREVIEW_CONTENT.copy(
    retired = true,
    score = Score(answered = 0, correct = 0, ratio = null),
    lastPracticedAt = null,
    byQuestionType = emptyMap(),
    selfRatingHistory = emptyList(),
    datasetTrust = DatasetTrust.REVIEWED,
)

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(
    name = "Betöltés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressTechniqueScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressTechniqueScreen(
            uiState = ProgressTechniqueUiState.Loading(),
            onRetry = {},
            onOpenLibraryDetail = {},
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
private fun ProgressTechniqueScreenErrorPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressTechniqueScreen(
            uiState = ProgressTechniqueUiState.Error(messageRes = R.string.progress_technique_not_found),
            onRetry = {},
            onOpenLibraryDetail = {},
        )
    }
}

@Preview(name = "Aktív, van adat – világos", showBackground = true)
@Preview(
    name = "Aktív, van adat – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressTechniqueScreenWithDataPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressTechniqueScreen(
            uiState = PREVIEW_CONTENT,
            onRetry = {},
            onOpenLibraryDetail = {},
        )
    }
}

@Preview(name = "Visszavont, nincs adat – világos", showBackground = true)
@Preview(
    name = "Visszavont, nincs adat – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressTechniqueScreenRetiredNoDataPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressTechniqueScreen(
            uiState = PREVIEW_CONTENT_RETIRED_NO_DATA,
            onRetry = {},
            onOpenLibraryDetail = {},
        )
    }
}
