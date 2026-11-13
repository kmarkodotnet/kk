package hu.kyokushin.trainer.feature.quiz.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.core.ui.illustration.TechniqueIllustration
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.quiz.R
import hu.kyokushin.trainer.feature.quiz.setup.QuizStartResult
import hu.kyokushin.trainer.feature.quiz.setup.questionTypeLabelRes

/**
 * A kvízeredmény képernyő route-szintű bekötése (FUNCTIONAL_SPEC 5.3, FR-20, FR-21, T-411,
 * T-412, T-415, G-09 csoport).
 *
 * Ugyanaz a minta, mint a `QuizSetupRoute`/`QuizRunRoute`-nál: ez az egyetlen composable,
 * amely [QuizResultViewModel]-t lát (L5); a tényleges navigáció ("Hibák újragyakorlása",
 * "Új kvíz", "Vissza a Haladáshoz", egy tétel megnyitása) mind kifelé adott esemény vagy
 * lambda, nem közvetlen `NavController`-hívás.
 *
 * @param onOpenTechnique egy hibás tétel megnyitása (FUNCTIONAL_SPEC 5.3/2.: „Tétel
 *   kattintható") – a helyes technika azonosítójával, ugyanaz a mintázat, mint a
 *   `quiz/run` magyarázó kártyájánál.
 * @param onRetryWrong a „Hibák újragyakorlása" eredménye ([QuizResultViewModel.retryEvent],
 *   T-412) – a hívó navigál az új menet `quiz/run/{sessionId}`-jára.
 * @param onNewQuiz „Új kvíz” (FUNCTIONAL_SPEC 5.3/3.: vissza a konfigurációra, megtartott
 *   beállításokkal) – tisztán navigációs döntés, nem igényel ViewModel-állapotot.
 * @param onBackToProgress „Vissza a Haladáshoz”.
 */
@Composable
fun QuizResultRoute(
    onOpenTechnique: (TechniqueId) -> Unit,
    onRetryWrong: (QuizStartResult) -> Unit,
    onNewQuiz: () -> Unit,
    onBackToProgress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.retryEvent.collect { result -> onRetryWrong(result) }
    }

    QuizResultScreen(
        uiState = uiState,
        onOpenTechnique = onOpenTechnique,
        onRetryWrong = viewModel::onRetryWrong,
        onNewQuiz = onNewQuiz,
        onBackToProgress = onBackToProgress,
        modifier = modifier,
    )
}

/** A kvízeredmény képernyő – stateless (FUNCTIONAL_SPEC 5.3). */
@Composable
fun QuizResultScreen(
    uiState: QuizResultUiState,
    onOpenTechnique: (TechniqueId) -> Unit,
    onRetryWrong: () -> Unit,
    onNewQuiz: () -> Unit,
    onBackToProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            QuizResultUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            QuizResultUiState.NotFound -> QuizResultNotFoundContent(
                onBackToProgress = onBackToProgress,
                modifier = Modifier.align(Alignment.Center),
            )

            is QuizResultUiState.Content -> QuizResultContent(
                state = uiState,
                onOpenTechnique = onOpenTechnique,
                onRetryWrong = onRetryWrong,
                onNewQuiz = onNewQuiz,
                onBackToProgress = onBackToProgress,
            )
        }
    }
}

/** Védelmi háló (NFR-06): a menet nem található – lásd `QuizResultUiState.NotFound`. */
@Composable
private fun QuizResultNotFoundContent(
    onBackToProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.quiz_result_not_found_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onBackToProgress, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
            Text(text = stringResource(R.string.quiz_result_back_to_progress))
        }
    }
}

/**
 * A tényleges eredmény: lektorálatlanság-szalag, nagy összegzés, hibás kérdések listája,
 * és a három akciógomb (FUNCTIONAL_SPEC 5.3).
 */
@Composable
private fun QuizResultContent(
    state: QuizResultUiState.Content,
    onOpenTechnique: (TechniqueId) -> Unit,
    onRetryWrong: () -> Unit,
    onNewQuiz: () -> Unit,
    onBackToProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Nem elutasítható, a UI állapotától független sáv (ADR-010, T-415).
        UnreviewedBanner(trust = state.datasetTrust)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            QuizResultSummaryBlock(state)

            if (state.wrongItems.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.quiz_result_wrong_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.wrongItems.forEach { item ->
                    QuizResultWrongItemRow(
                        item = item,
                        onClick = { onOpenTechnique(item.correctTechniqueId) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.canRetryWrong) {
                Button(
                    onClick = onRetryWrong,
                    modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
                ) {
                    Text(text = stringResource(R.string.quiz_result_retry_wrong))
                }
            }
            OutlinedButton(
                onClick = onNewQuiz,
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(R.string.quiz_result_new_quiz))
            }
            TextButton(
                onClick = onBackToProgress,
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(R.string.quiz_result_back_to_progress))
            }
        }
    }
}

/**
 * A nagy összegzés – a döntést a `@Composable`-mentes [quizResultSummary] adja
 * (CONVENTIONS 4.1), itt csak a szöveggé alakítás történik.
 *
 * A [QuizResultSummary.Scored] ágon a sikerarány **két önálló `Text`-ben** jelenik meg
 * (a tört és a százalék): mindkettő szövegesen kiolvasható, nem csak diagramon (NFR-04,
 * T-411 „Kész ha").
 */
@Composable
private fun QuizResultSummaryBlock(state: QuizResultUiState.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (val summary = quizResultSummary(state)) {
            is QuizResultSummary.Scored -> {
                Text(
                    text = stringResource(
                        R.string.quiz_result_score_fraction,
                        summary.correct,
                        summary.answered,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.quiz_result_score_percent, summary.percent),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            QuizResultSummary.NoAnswers -> Text(
                text = stringResource(R.string.quiz_result_no_answers),
                style = MaterialTheme.typography.headlineSmall,
            )

            is QuizResultSummary.Aborted -> Text(
                text = stringResource(R.string.quiz_result_aborted, summary.answered, summary.requested),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

/**
 * Egy hibás kérdés tétele: típus, a helyes technika (romaji + magyar + kis ábra), és a
 * felhasználó saját válasza (FUNCTIONAL_SPEC 5.3/2.). A teljes sor kattintható, a helyes
 * technika részletezőjére navigál (US-14/3. kritériumának megfelelő, a `quiz/run`
 * magyarázó kártyájával azonos mintázat).
 */
@Composable
private fun QuizResultWrongItemRow(
    item: QuizResultWrongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(questionTypeLabelRes(item.questionType)),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TechniqueIllustration(
                    illustration = item.correctIllustration,
                    label = IllustrationLabel.Technique(romaji = item.correctRomaji, hungarian = item.correctHungarian),
                    maxWidth = WRONG_ITEM_ILLUSTRATION_WIDTH,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.quiz_result_wrong_correct_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(text = item.correctRomaji, style = MaterialTheme.typography.bodyLarge)
                    Text(text = item.correctHungarian, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.quiz_result_wrong_selected_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(text = item.selectedRomaji, style = MaterialTheme.typography.bodyLarge)
                Text(text = item.selectedHungarian, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A gombok minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

/** A hibás tétel sorának ábraszélessége. */
private val WRONG_ITEM_ILLUSTRATION_WIDTH: Dp = 64.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában; betöltés, nem található, normál/megszakított/
// 0-válaszos eredmény, hibás kérdésekkel és nélkülük (T-411/T-412/T-415 „Kész ha").
// ---------------------------------------------------------------------------------------

private val PREVIEW_WRONG_ITEM = QuizResultWrongItem(
    questionType = QuestionType.JP_TO_HU,
    correctTechniqueId = TechniqueId.of("KY-0001"),
    correctRomaji = "Heisoku Dachi",
    correctHungarian = "Zárt lábú állás",
    correctIllustration = IllustrationRef.Placeholder,
    selectedTechniqueId = TechniqueId.of("KY-0002"),
    selectedRomaji = "Fudo Dachi",
    selectedHungarian = "Természetes állás",
)

private val PREVIEW_CONTENT_WITH_WRONG = QuizResultUiState.Content(
    sessionId = SessionId.of("preview-session"),
    score = Score(answered = 10, correct = 7, ratio = 0.7),
    requestedCount = 10,
    aborted = false,
    wrongItems = listOf(
        PREVIEW_WRONG_ITEM,
        PREVIEW_WRONG_ITEM.copy(
            questionType = QuestionType.HU_TO_JP,
            correctTechniqueId = TechniqueId.of("KY-0003"),
            correctRomaji = "Musubi Dachi",
            correctHungarian = "Összezárt sarkú állás",
        ),
    ),
    canRetryWrong = true,
    datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
)

private val PREVIEW_CONTENT_PERFECT = QuizResultUiState.Content(
    sessionId = SessionId.of("preview-session"),
    score = Score(answered = 10, correct = 10, ratio = 1.0),
    requestedCount = 10,
    aborted = false,
    wrongItems = emptyList(),
    canRetryWrong = false,
    datasetTrust = DatasetTrust.REVIEWED,
)

private val PREVIEW_CONTENT_ABORTED = QuizResultUiState.Content(
    sessionId = SessionId.of("preview-session"),
    score = Score(answered = 3, correct = 2, ratio = 2.0 / 3.0),
    requestedCount = 10,
    aborted = true,
    wrongItems = listOf(PREVIEW_WRONG_ITEM),
    canRetryWrong = true,
    datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
)

private val PREVIEW_CONTENT_NO_ANSWERS = QuizResultUiState.Content(
    sessionId = SessionId.of("preview-session"),
    score = Score(answered = 0, correct = 0, ratio = null),
    requestedCount = 10,
    aborted = true,
    wrongItems = emptyList(),
    canRetryWrong = false,
    datasetTrust = DatasetTrust.REVIEWED,
)

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(
    name = "Betöltés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizResultScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizResultScreen(
            uiState = QuizResultUiState.Loading,
            onOpenTechnique = {},
            onRetryWrong = {},
            onNewQuiz = {},
            onBackToProgress = {},
        )
    }
}

@Preview(name = "Nem található – világos", showBackground = true)
@Preview(
    name = "Nem található – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizResultScreenNotFoundPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizResultScreen(
            uiState = QuizResultUiState.NotFound,
            onOpenTechnique = {},
            onRetryWrong = {},
            onNewQuiz = {},
            onBackToProgress = {},
        )
    }
}

@Preview(name = "Hibás kérdésekkel – világos", showBackground = true)
@Preview(
    name = "Hibás kérdésekkel – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizResultScreenWithWrongItemsPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizResultScreen(
            uiState = PREVIEW_CONTENT_WITH_WRONG,
            onOpenTechnique = {},
            onRetryWrong = {},
            onNewQuiz = {},
            onBackToProgress = {},
        )
    }
}

@Preview(name = "Hibátlan – nincs újragyakorlás gomb – világos", showBackground = true)
@Preview(
    name = "Hibátlan – nincs újragyakorlás gomb – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizResultScreenPerfectPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizResultScreen(
            uiState = PREVIEW_CONTENT_PERFECT,
            onOpenTechnique = {},
            onRetryWrong = {},
            onNewQuiz = {},
            onBackToProgress = {},
        )
    }
}

@Preview(name = "Megszakított kvíz – világos", showBackground = true)
@Preview(
    name = "Megszakított kvíz – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizResultScreenAbortedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizResultScreen(
            uiState = PREVIEW_CONTENT_ABORTED,
            onOpenTechnique = {},
            onRetryWrong = {},
            onNewQuiz = {},
            onBackToProgress = {},
        )
    }
}

@Preview(name = "0 válasz – világos", showBackground = true)
@Preview(
    name = "0 válasz – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizResultScreenNoAnswersPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizResultScreen(
            uiState = PREVIEW_CONTENT_NO_ANSWERS,
            onOpenTechnique = {},
            onRetryWrong = {},
            onNewQuiz = {},
            onBackToProgress = {},
        )
    }
}
