package hu.kyokushin.trainer.feature.quiz.run

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizQuestion
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.core.ui.illustration.TechniqueIllustration
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.quiz.R

/**
 * A futó kvíz képernyő route-szintű bekötése (FR-16, FR-17, FR-18, FR-19,
 * FUNCTIONAL_SPEC 5.2, US-13, US-14, US-15, T-407, T-409).
 *
 * Ez az egyetlen composable a `run` csomagban, amely [QuizRunViewModel]-t lát: innen
 * lefelé minden összetevő [QuizRunUiState]-et és lambda eseményeket kap (L5 – a feature
 * modul nem navigál magától, lambdát ad kifelé). A [QuizRunViewModel.closeEvent] egyszeri
 * esemény – a hívó dönt arról, hova navigál a megszakítás megerősítése után.
 *
 * @param onOpenTechnique a magyarázó kártyára koppintva (US-14/3. kritérium): a hívó
 *   navigál a Tudástár részletezőjére.
 * @param onExit a megszakítás megerősítve ([QuizRunViewModel.closeEvent]) – kifelé adott
 *   esemény, nem közvetlen navigáció.
 * @param onFinish a kvíz normál befejezése, az utolsó kérdés „Eredmény" gombjával
 *   ([QuizRunViewModel.finishEvent], T-411) – a hívó navigál a `quiz/result/{sessionId}`-ra.
 * @param modifier a szokásos Compose modifier.
 * @param viewModel a Hilt által adott ViewModel; teszt vagy előnézet nem ezt a
 *   konstruktort hívja, hanem közvetlenül a stateless [QuizRunScreen]-t.
 */
@Composable
fun QuizRunRoute(
    onOpenTechnique: (TechniqueId) -> Unit,
    onExit: () -> Unit,
    onFinish: (SessionId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizRunViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.closeEvent.collect { onExit() }
    }
    LaunchedEffect(viewModel) {
        viewModel.finishEvent.collect { sessionId -> onFinish(sessionId) }
    }

    QuizRunScreen(
        uiState = uiState,
        onAnswer = viewModel::onAnswer,
        onNext = viewModel::onNext,
        onRequestClose = viewModel::onRequestClose,
        onCancelClose = viewModel::onCancelClose,
        onConfirmClose = viewModel::onConfirmClose,
        onOpenTechnique = onOpenTechnique,
        modifier = modifier,
    )
}

/**
 * A futó kvíz képernyő – stateless (FUNCTIONAL_SPEC 5.2).
 *
 * A rendszer vissza-gombja ugyanazt a megerősítő útvonalat indítja, mint a bezárás (×)
 * gomb (FUNCTIONAL_SPEC 5.2/5.: „Vissza-gomb = bezárás, ugyanaz a megerősítés") – kivéve,
 * ha a párbeszéd már nyitva van, ott a vissza-gomb a „Folytatás"-sal egyenértékű (bezárja
 * a párbeszédet, nem lép ki még egyszer).
 */
@Composable
fun QuizRunScreen(
    uiState: QuizRunUiState,
    onAnswer: (QuestionId, TechniqueId) -> Unit,
    onNext: () -> Unit,
    onRequestClose: () -> Unit,
    onCancelClose: () -> Unit,
    onConfirmClose: () -> Unit,
    onOpenTechnique: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = uiState is QuizRunUiState.Content) {
        if (uiState is QuizRunUiState.Content && uiState.closeConfirmationVisible) {
            onCancelClose()
        } else {
            onRequestClose()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            QuizRunUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            QuizRunUiState.Empty -> QuizRunEmptyContent(
                // Nincs mit visszavonni (nincs futó menet) – a kilépés közvetlenül a
                // `closeEvent`-et váltja ki, a megerősítő párbeszéd kihagyásával.
                onExit = onConfirmClose,
                modifier = Modifier.align(Alignment.Center),
            )

            is QuizRunUiState.Content -> {
                QuizRunContent(
                    content = uiState,
                    onAnswer = onAnswer,
                    onNext = onNext,
                    onRequestClose = onRequestClose,
                    onOpenTechnique = onOpenTechnique,
                )
                if (uiState.closeConfirmationVisible) {
                    QuizCloseConfirmationDialog(onConfirm = onConfirmClose, onDismiss = onCancelClose)
                }
            }
        }
    }
}

/**
 * A védelmi háló ága: sérült/üres terv, vagy egy opció technikája nem oldható fel
 * (`QuizRunUiState.Empty` KDoc-ja, NFR-06). A `quiz/setup` normál esetben nem engedi ide
 * eljutni a felhasználót, ezért ez a UI a gyakorlatban ritkán látott, de nem omlik össze –
 * ugyanaz a minta, mint a `LearnCardsEmptyContent` (`docs/CONVENTIONS.md`).
 */
@Composable
private fun QuizRunEmptyContent(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.quiz_run_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onExit, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
            Text(text = stringResource(R.string.quiz_run_empty_action))
        }
    }
}

/**
 * A tényleges kérdéstartalom: felső sáv (index/összes + bezárás + előrehaladásjelző),
 * a kérdéstípus szerinti blokk, válasz után a magyarázó kártya, és a „Következő"/
 * „Eredmény" gomb (FUNCTIONAL_SPEC 5.2/1–4.).
 */
@Composable
private fun QuizRunContent(
    content: QuizRunUiState.Content,
    onAnswer: (QuestionId, TechniqueId) -> Unit,
    onNext: () -> Unit,
    onRequestClose: () -> Unit,
    onOpenTechnique: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        QuizRunTopBar(
            position = content.position,
            total = content.total,
            onRequestClose = onRequestClose,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (content.question.type) {
                QuestionType.JP_TO_HU, QuestionType.HU_TO_JP -> QuizTextQuestion(
                    question = content.question,
                    options = content.options,
                    selectedTechniqueId = content.selectedTechniqueId,
                    onSelect = { techniqueId -> onAnswer(content.question.id, techniqueId) },
                )

                QuestionType.IMAGE_TO_JP -> QuizImageToJpQuestion(
                    question = content.question,
                    options = content.options,
                    selectedTechniqueId = content.selectedTechniqueId,
                    onSelect = { techniqueId -> onAnswer(content.question.id, techniqueId) },
                )

                QuestionType.JP_TO_IMAGE -> QuizJpToImageQuestion(
                    question = content.question,
                    options = content.options,
                    selectedTechniqueId = content.selectedTechniqueId,
                    onSelect = { techniqueId -> onAnswer(content.question.id, techniqueId) },
                )
            }

            if (content.selectedTechniqueId != null) {
                val correctOption = content.options.first { it.techniqueId == content.question.correctTechniqueId }
                QuizExplanationCard(option = correctOption, onOpenTechnique = onOpenTechnique)
            }
        }

        if (content.selectedTechniqueId != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MIN_TOUCH_TARGET),
                ) {
                    Text(
                        text = stringResource(
                            if (content.isLastQuestion) R.string.quiz_run_result else R.string.quiz_run_next,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Felső sáv: „`{index}` / `{összes}`" + bezárás (×) + lineáris előrehaladásjelző
 * (FUNCTIONAL_SPEC 5.2/1.).
 */
@Composable
private fun QuizRunTopBar(
    position: Int,
    total: Int,
    onRequestClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1-alapú megjelenítés: az első kérdés „1 / {összes}", nem „0 / {összes}".
            Text(
                text = stringResource(R.string.quiz_run_progress, position + 1, total),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onRequestClose, modifier = Modifier.size(MIN_TOUCH_TARGET)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.quiz_run_close_a11y),
                )
            }
        }
        val progress = if (total > 0) (position + 1).toFloat() / total else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * (A)/(B) szöveges kérdés: kiemelt kérdéstárgy + 4 szöveges opciógomb (FUNCTIONAL_SPEC
 * 5.2/2., T-407). A kérdés tárgya és az opció felirata a típustól függően romaji vagy
 * magyar jelentés – (A)-nál a tárgy romaji, az opció magyar; (B)-nél fordítva.
 */
@Composable
private fun QuizTextQuestion(
    question: QuizQuestion,
    options: List<QuizOptionDisplay>,
    selectedTechniqueId: TechniqueId?,
    onSelect: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val correctOption = options.first { it.techniqueId == question.correctTechniqueId }
    val isJpToHu = question.type == QuestionType.JP_TO_HU
    val promptPrefixRes = if (isJpToHu) {
        R.string.quiz_run_prompt_jp_to_hu_prefix
    } else {
        R.string.quiz_run_prompt_hu_to_jp_prefix
    }
    val promptValue = if (isJpToHu) correctOption.romaji else correctOption.hungarian

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(promptPrefixRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.quiz_run_prompt_value, promptValue),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                // Hosszú szöveg tördelése (T-407 „Kész ha"): sem a felirat, sem a
                // gomb nem korlátoz sortörést vagy sorszámot – a `QuizOptionRow` natív
                // `Text` tördelését használja, a gomb magassága a tartalomhoz igazodik.
                val label = if (isJpToHu) option.hungarian else option.romaji
                val feedback = QuizOptionFeedbackResolver.resolve(
                    optionId = option.techniqueId,
                    correctTechniqueId = question.correctTechniqueId,
                    selectedTechniqueId = selectedTechniqueId,
                )
                QuizOptionRow(
                    label = label,
                    feedback = feedback,
                    onClick = { onSelect(option.techniqueId) },
                )
            }
        }
    }
}

/**
 * (C) ábra → japán név kérdés: a kérdés tárgya a kép maga, az opciók szöveges romaji
 * gombok (FUNCTIONAL_SPEC 5.2/2.: „Melyik technika ez?" + illusztráció, T-408, FR-12).
 *
 * A kérdésábra a HELYES opció illusztrációja – ezt a felhasználónak ki kell találnia,
 * ezért a leírása [IllustrationLabel.QuizPrompt] (nem [IllustrationLabel.Technique]):
 * a technika neve itt maga lenne a válasz, tehát a `contentDescription` nem árulhatja
 * el (NFR-04, ugyanaz a csapda, mint a `LearnCardIllustrationLabelPolicy`-nál, lásd
 * `docs/CONVENTIONS.md`). Az opciógombok a meglévő [QuizOptionRow]-t használják, ugyanazt
 * a visszajelzést kapva, mint az (A)/(B) típus.
 */
@Composable
private fun QuizImageToJpQuestion(
    question: QuizQuestion,
    options: List<QuizOptionDisplay>,
    selectedTechniqueId: TechniqueId?,
    onSelect: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val correctOption = options.first { it.techniqueId == question.correctTechniqueId }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(R.string.quiz_run_prompt_image_to_jp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        TechniqueIllustration(
            illustration = correctOption.illustration,
            label = IllustrationLabel.QuizPrompt,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val feedback = QuizOptionFeedbackResolver.resolve(
                    optionId = option.techniqueId,
                    correctTechniqueId = question.correctTechniqueId,
                    selectedTechniqueId = selectedTechniqueId,
                )
                QuizOptionRow(
                    label = option.romaji,
                    feedback = feedback,
                    onClick = { onSelect(option.techniqueId) },
                )
            }
        }
    }
}

/**
 * (D) japán név → ábra kérdés: a kérdés tárgya a kiemelt romaji név, az opciók egy 2×2
 * képrács (FUNCTIONAL_SPEC 5.2/2.: „Melyik ábra: **`{romaji}`**?", T-408, FR-12).
 *
 * A [QuizImageOptionCard] minden opció leírását [IllustrationLabel.QuizOption]-nal adja –
 * ez **pozíció alapú**, a technika nevét nem tartalmazza (FUNCTIONAL_SPEC 9., T-408 „Kész
 * ha"). A sorrend **ugyanaz**, mint az [options] lista sorrendje (ADR-007 seedelt
 * keverés) – a 2×2 rács csak vizuálisan bontja két sorra, nem rendezi újra.
 */
@Composable
private fun QuizJpToImageQuestion(
    question: QuizQuestion,
    options: List<QuizOptionDisplay>,
    selectedTechniqueId: TechniqueId?,
    onSelect: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val correctOption = options.first { it.techniqueId == question.correctTechniqueId }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.quiz_run_prompt_jp_to_image_prefix),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.quiz_run_prompt_value, correctOption.romaji),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuizImageOptionGrid.rows(options).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { cell ->
                        val feedback = QuizOptionFeedbackResolver.resolve(
                            optionId = cell.option.techniqueId,
                            correctTechniqueId = question.correctTechniqueId,
                            selectedTechniqueId = selectedTechniqueId,
                        )
                        QuizImageOptionCard(
                            position = cell.position,
                            illustration = cell.option.illustration,
                            feedback = feedback,
                            onClick = { onSelect(cell.option.techniqueId) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Egy (D) típusú képopció kártyája: a [TechniqueIllustration] + a visszajelzés ikonja és
 * szövege a kép alatt (FUNCTIONAL_SPEC 5.2/4., NFR-04 „ikon + szöveg, nem csak szín" –
 * ugyanaz a visszajelzés-logika, mint a [QuizOptionRow]-nál, csak a felirat helyén kép
 * áll). A teljes kártya (kép + felirat) a kattintható/`selectable` felület, ezért az
 * érintési célpont messze meghaladja a 48 dp-t (FUNCTIONAL_SPEC 9., NFR-04).
 */
@Composable
private fun QuizImageOptionCard(
    position: Int,
    illustration: IllustrationRef?,
    feedback: QuizOptionFeedback,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectedSemantics = feedback == QuizOptionFeedback.SELECTED_CORRECT ||
        feedback == QuizOptionFeedback.SELECTED_INCORRECT
    val enabled = feedback == QuizOptionFeedback.UNANSWERED
    val stateDescriptionText = quizOptionStateDescriptionRes(feedback)?.let { stringResource(it) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = quizOptionContainerColor(feedback),
        contentColor = quizOptionContentColor(feedback),
        modifier = modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .selectable(
                selected = isSelectedSemantics,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .then(
                if (stateDescriptionText != null) {
                    Modifier.semantics { this.stateDescription = stateDescriptionText }
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TechniqueIllustration(
                illustration = illustration,
                label = IllustrationLabel.QuizOption(position = position),
                modifier = Modifier.fillMaxWidth(),
            )

            val icon = quizOptionIcon(feedback)
            val feedbackTextRes = quizOptionFeedbackTextRes(feedback)
            if (icon != null || feedbackTextRes != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            // A szöveg (lásd lent) kimondja a „Helyes"/„Helytelen"
                            // jelentést – az ikon itt dekoratív (ugyanaz a minta, mint
                            // a QuizOptionRow-ban).
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    if (feedbackTextRes != null) {
                        Text(text = stringResource(feedbackTextRes), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * Egy válaszopció gombja, a válasz-visszajelzéssel (FUNCTIONAL_SPEC 5.2/4., FR-17,
 * NFR-04, T-409 „Kész ha").
 *
 * A helyesség **sosem csak szín**: pipa/kereszt ikon + „Helyes"/„Helytelen" szöveg is jár
 * (NFR-04, színtévesztés). A `stateDescription` a [QuizOptionFeedback.SELECTED_CORRECT]/
 * [QuizOptionFeedback.CORRECT_UNSELECTED] és a [QuizOptionFeedback.SELECTED_INCORRECT]
 * ágon van beállítva – a `selectable` szemantikája adja az `opciók selected szemantikával"
 * (FUNCTIONAL_SPEC 9.) követelményt.
 */
@Composable
private fun QuizOptionRow(
    label: String,
    feedback: QuizOptionFeedback,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectedSemantics = feedback == QuizOptionFeedback.SELECTED_CORRECT ||
        feedback == QuizOptionFeedback.SELECTED_INCORRECT
    val enabled = feedback == QuizOptionFeedback.UNANSWERED
    val stateDescriptionText = quizOptionStateDescriptionRes(feedback)?.let { stringResource(it) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = quizOptionContainerColor(feedback),
        contentColor = quizOptionContentColor(feedback),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .selectable(
                selected = isSelectedSemantics,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .then(
                if (stateDescriptionText != null) {
                    Modifier.semantics { this.stateDescription = stateDescriptionText }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = quizOptionIcon(feedback)
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    // A szöveg (lásd lent) már kimondja a „Helyes"/„Helytelen" jelentést –
                    // az ikon itt dekoratív, hogy ne duplázza a TalkBack bejelentését.
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                val feedbackTextRes = quizOptionFeedbackTextRes(feedback)
                if (feedbackTextRes != null) {
                    Text(
                        text = stringResource(feedbackTextRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun quizOptionContainerColor(feedback: QuizOptionFeedback): Color = when (feedback) {
    QuizOptionFeedback.UNANSWERED, QuizOptionFeedback.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    QuizOptionFeedback.SELECTED_CORRECT,
    QuizOptionFeedback.CORRECT_UNSELECTED,
    -> MaterialTheme.colorScheme.primaryContainer
    QuizOptionFeedback.SELECTED_INCORRECT -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun quizOptionContentColor(feedback: QuizOptionFeedback): Color = when (feedback) {
    QuizOptionFeedback.UNANSWERED, QuizOptionFeedback.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    QuizOptionFeedback.SELECTED_CORRECT,
    QuizOptionFeedback.CORRECT_UNSELECTED,
    -> MaterialTheme.colorScheme.onPrimaryContainer
    QuizOptionFeedback.SELECTED_INCORRECT -> MaterialTheme.colorScheme.onErrorContainer
}

/**
 * Az ikon-leképezés `internal` (nem `private`), hogy a döntés a Composable rétegtől
 * függetlenül, sima JUnit teszttel bizonyítható legyen (T-413, NFR-04, lásd
 * `docs/CONVENTIONS.md` 4.1 „Compose döntési logika tesztelése emulátor nélkül" –
 * ugyanaz a minta, mint a [QuizOptionFeedbackResolver]-nél).
 */
internal fun quizOptionIcon(feedback: QuizOptionFeedback): ImageVector? = when (feedback) {
    QuizOptionFeedback.SELECTED_CORRECT, QuizOptionFeedback.CORRECT_UNSELECTED -> Icons.Filled.Check
    QuizOptionFeedback.SELECTED_INCORRECT -> Icons.Filled.Close
    QuizOptionFeedback.UNANSWERED, QuizOptionFeedback.NEUTRAL -> null
}

/** Lásd [quizOptionIcon] KDoc-ja – ugyanaz az `internal`-ra nyitás oka (T-413). */
internal fun quizOptionFeedbackTextRes(feedback: QuizOptionFeedback): Int? = when (feedback) {
    QuizOptionFeedback.SELECTED_CORRECT, QuizOptionFeedback.CORRECT_UNSELECTED -> R.string.quiz_run_answer_correct
    QuizOptionFeedback.SELECTED_INCORRECT -> R.string.quiz_run_answer_wrong
    QuizOptionFeedback.UNANSWERED, QuizOptionFeedback.NEUTRAL -> null
}

/**
 * Lásd [quizOptionIcon] KDoc-ja – ugyanaz az `internal`-ra nyitás oka (T-413). Ez a
 * leképezés adja a T-413 „Opciók `selected` szemantikával; válasz után `stateDescription`"
 * teendőjének bizonyítékát: válasz ELŐTT ([QuizOptionFeedback.UNANSWERED]) és egy nem
 * kiválasztott, helytelen opciónál ([QuizOptionFeedback.NEUTRAL]) `null` jár – a
 * TalkBack ezeknél nem jelent be semmilyen helyesség-állapotot.
 */
internal fun quizOptionStateDescriptionRes(feedback: QuizOptionFeedback): Int? = when (feedback) {
    QuizOptionFeedback.SELECTED_CORRECT,
    QuizOptionFeedback.CORRECT_UNSELECTED,
    -> R.string.quiz_run_option_state_correct
    QuizOptionFeedback.SELECTED_INCORRECT -> R.string.quiz_run_option_state_incorrect
    QuizOptionFeedback.UNANSWERED, QuizOptionFeedback.NEUTRAL -> null
}

/**
 * Magyarázó kártya: a helyes technika romaji + magyar név + ábra hármasa, és link a
 * tudástári részletezőre (FUNCTIONAL_SPEC 5.2/4., FR-17, US-14/2–3. kritérium).
 */
@Composable
private fun QuizExplanationCard(
    option: QuizOptionDisplay,
    onOpenTechnique: (TechniqueId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onOpenTechnique(option.techniqueId) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TechniqueIllustration(
                illustration = option.illustration,
                label = IllustrationLabel.Technique(romaji = option.romaji, hungarian = option.hungarian),
                maxWidth = EXPLANATION_ILLUSTRATION_WIDTH,
                modifier = Modifier.padding(end = 16.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quiz_run_explanation_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(text = option.romaji, style = MaterialTheme.typography.titleMedium)
                Text(text = option.hungarian, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.quiz_run_open_detail),
            )
        }
    }
}

/**
 * A megszakítást megerősítő párbeszéd (FUNCTIONAL_SPEC 5.2/5., FUNCTIONAL_SPEC 8.).
 *
 * A Material 3 `AlertDialog` gombjai alapértelmezésben a `TextButton` ~40 dp-es
 * minimumát kapják, nem a projekt 48 dp-es érintési célpont-szabályát (T-413, NFR-04,
 * FUNCTIONAL_SPEC 9.) – ezért explicit `heightIn(min = MIN_TOUCH_TARGET)` kell mindkét
 * gombra, ugyanúgy, mint a `QuizResultScreen` „Vissza a Haladáshoz" `TextButton`-jénél.
 */
@Composable
private fun QuizCloseConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.quiz_run_abort_title)) },
        text = { Text(text = stringResource(R.string.quiz_run_abort_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
                Text(text = stringResource(R.string.quiz_run_abort_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
                Text(text = stringResource(R.string.quiz_run_abort_dismiss))
            }
        },
    )
}

/** A gombok/opciók minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

/** A magyarázó kártya ábrájának legnagyobb szélessége. */
private val EXPLANATION_ILLUSTRATION_WIDTH: Dp = 72.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában; betöltés, üres, (A)/(B) típus, válasz előtt/után,
// megszakítás-megerősítés és hosszú magyar név (T-407 „Kész ha": „Hosszú magyar neveknél
// sem törik a layout, és nem vágódik le a szöveg" – globális DoD 6. pont).
// ---------------------------------------------------------------------------------------

private val PREVIEW_QUESTION_A = QuizQuestion(
    id = QuestionId.of("Q-1"),
    type = QuestionType.JP_TO_HU,
    correctTechniqueId = TechniqueId.of("KY-0001"),
    optionTechniqueIds = listOf("KY-0001", "KY-0002", "KY-0003", "KY-0004").map(TechniqueId::of),
)

private val PREVIEW_QUESTION_B = PREVIEW_QUESTION_A.copy(type = QuestionType.HU_TO_JP)

private val PREVIEW_OPTIONS = listOf(
    QuizOptionDisplay(
        techniqueId = TechniqueId.of("KY-0001"),
        romaji = "Kiba Dachi",
        hungarian = "Lovagló állás",
        illustration = null,
    ),
    QuizOptionDisplay(
        techniqueId = TechniqueId.of("KY-0002"),
        romaji = "Fudo Dachi",
        hungarian = "Természetes állás",
        illustration = null,
    ),
    QuizOptionDisplay(
        techniqueId = TechniqueId.of("KY-0003"),
        romaji = "Musubi Dachi",
        hungarian = "Összezárt sarkú állás",
        illustration = null,
    ),
    QuizOptionDisplay(
        techniqueId = TechniqueId.of("KY-0004"),
        romaji = "Heiko Dachi",
        hungarian = "Párhuzamos állás",
        illustration = IllustrationRef.Placeholder,
    ),
)

/** Az egyik opció felirata ~55 karakter – hosszabb, mint a valós tartalom leghosszabb neve. */
private val PREVIEW_OPTIONS_LONG_NAME = listOf(
    PREVIEW_OPTIONS[0].copy(hungarian = "Hátralépéssel összekötött, kétkezes alsó szintű hárítás"),
    PREVIEW_OPTIONS[1],
    PREVIEW_OPTIONS[2],
    PREVIEW_OPTIONS[3],
)

private val PREVIEW_CONTENT_A_UNANSWERED = QuizRunUiState.Content(
    sessionId = SessionId.of("preview-session"),
    question = PREVIEW_QUESTION_A,
    position = 2,
    total = 10,
    answeredCount = 2,
    selectedTechniqueId = null,
    isLastQuestion = false,
    closeConfirmationVisible = false,
    options = PREVIEW_OPTIONS,
)

private val PREVIEW_CONTENT_A_CORRECT = PREVIEW_CONTENT_A_UNANSWERED.copy(
    selectedTechniqueId = TechniqueId.of("KY-0001"),
    answeredCount = 3,
)

private val PREVIEW_CONTENT_A_INCORRECT = PREVIEW_CONTENT_A_UNANSWERED.copy(
    selectedTechniqueId = TechniqueId.of("KY-0002"),
    answeredCount = 3,
)

private val PREVIEW_CONTENT_B_UNANSWERED = PREVIEW_CONTENT_A_UNANSWERED.copy(question = PREVIEW_QUESTION_B)

private val PREVIEW_CONTENT_CLOSE_CONFIRMATION = PREVIEW_CONTENT_A_UNANSWERED.copy(closeConfirmationVisible = true)

// (C)/(D) képes kérdéstípusok (T-408). A `Placeholder` hivatkozás az előnézetben ugyanúgy
// renderelhető, mint a `TechniqueIllustrationPlaceholderPreview` – a tényleges csomagolt
// képet (`Asset`) az előnézet-renderelő nem tudja megnyitni (nincs `AssetManager`), lásd
// `TechniqueIllustration.kt` dokumentációja.
private val PREVIEW_QUESTION_C = PREVIEW_QUESTION_A.copy(type = QuestionType.IMAGE_TO_JP)
private val PREVIEW_QUESTION_D = PREVIEW_QUESTION_A.copy(type = QuestionType.JP_TO_IMAGE)

private val PREVIEW_OPTIONS_WITH_IMAGES = PREVIEW_OPTIONS.map { it.copy(illustration = IllustrationRef.Placeholder) }

private val PREVIEW_CONTENT_C_UNANSWERED = PREVIEW_CONTENT_A_UNANSWERED.copy(
    question = PREVIEW_QUESTION_C,
    options = PREVIEW_OPTIONS_WITH_IMAGES,
)

private val PREVIEW_CONTENT_C_CORRECT = PREVIEW_CONTENT_C_UNANSWERED.copy(
    selectedTechniqueId = TechniqueId.of("KY-0001"),
    answeredCount = 3,
)

private val PREVIEW_CONTENT_D_UNANSWERED = PREVIEW_CONTENT_A_UNANSWERED.copy(
    question = PREVIEW_QUESTION_D,
    options = PREVIEW_OPTIONS_WITH_IMAGES,
)

private val PREVIEW_CONTENT_D_CORRECT = PREVIEW_CONTENT_D_UNANSWERED.copy(
    selectedTechniqueId = TechniqueId.of("KY-0001"),
    answeredCount = 3,
)

private val PREVIEW_CONTENT_D_INCORRECT = PREVIEW_CONTENT_D_UNANSWERED.copy(
    selectedTechniqueId = TechniqueId.of("KY-0002"),
    answeredCount = 3,
)

private val PREVIEW_CONTENT_LONG_NAME = PREVIEW_CONTENT_A_UNANSWERED.copy(
    position = 9,
    total = 10,
    isLastQuestion = true,
    options = PREVIEW_OPTIONS_LONG_NAME,
)

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(
    name = "Betöltés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = QuizRunUiState.Loading,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "Üres – védelmi háló – világos", showBackground = true)
@Preview(
    name = "Üres – védelmi háló – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenEmptyPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = QuizRunUiState.Empty,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(A) típus – válasz előtt – világos", showBackground = true)
@Preview(
    name = "(A) típus – válasz előtt – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeAUnansweredPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_A_UNANSWERED,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(A) típus – helyes válasz – világos", showBackground = true)
@Preview(
    name = "(A) típus – helyes válasz – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeACorrectPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_A_CORRECT,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(A) típus – helytelen válasz – világos", showBackground = true)
@Preview(
    name = "(A) típus – helytelen válasz – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeAIncorrectPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_A_INCORRECT,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(B) típus – válasz előtt – világos", showBackground = true)
@Preview(
    name = "(B) típus – válasz előtt – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeBUnansweredPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_B_UNANSWERED,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "Megszakítás megerősítése – világos", showBackground = true)
@Preview(
    name = "Megszakítás megerősítése – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenCloseConfirmationPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_CLOSE_CONFIRMATION,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "Hosszú magyar név – utolsó kérdés – világos", showBackground = true)
@Preview(
    name = "Hosszú magyar név – utolsó kérdés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenLongNamePreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_LONG_NAME,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

// ---------------------------------------------------------------------------------------
// (C)/(D) képes kérdéstípusok (T-408 „Kész ha"): válasz előtt/után, és egy ~400 dp-es
// keskeny képernyő a (D) 2×2 rácshoz (T-408 „Kész ha": „Keskeny képernyőn (≈400 dp) is
// használható a rács").
// ---------------------------------------------------------------------------------------

@Preview(name = "(C) típus – válasz előtt – világos", showBackground = true)
@Preview(
    name = "(C) típus – válasz előtt – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeCUnansweredPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_C_UNANSWERED,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(C) típus – helyes válasz – világos", showBackground = true)
@Preview(
    name = "(C) típus – helyes válasz – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeCCorrectPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_C_CORRECT,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(D) típus – válasz előtt – világos", showBackground = true)
@Preview(
    name = "(D) típus – válasz előtt – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeDUnansweredPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_D_UNANSWERED,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(D) típus – helyes válasz – világos", showBackground = true)
@Preview(
    name = "(D) típus – helyes válasz – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeDCorrectPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_D_CORRECT,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

@Preview(name = "(D) típus – helytelen válasz – világos", showBackground = true)
@Preview(
    name = "(D) típus – helytelen válasz – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeDIncorrectPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_D_INCORRECT,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}

/**
 * Keskeny (~400 dp) képernyő – a (D) 2×2 rács (T-408 „Kész ha": „Keskeny képernyőn
 * (≈400 dp) is használható a rács"). A `widthDp` az előnézet vászonát szűkíti, nem a
 * tényleges eszközt – ez a bevett Compose-mód egy kis telefon szimulálására.
 */
@Preview(name = "(D) típus – keskeny (400 dp) – világos", showBackground = true, widthDp = 400)
@Preview(
    name = "(D) típus – keskeny (400 dp) – sötét",
    showBackground = true,
    widthDp = 400,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuizRunScreenTypeDNarrowPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        QuizRunScreen(
            uiState = PREVIEW_CONTENT_D_UNANSWERED,
            onAnswer = { _, _ -> },
            onNext = {},
            onRequestClose = {},
            onCancelClose = {},
            onConfirmClose = {},
            onOpenTechnique = {},
        )
    }
}
