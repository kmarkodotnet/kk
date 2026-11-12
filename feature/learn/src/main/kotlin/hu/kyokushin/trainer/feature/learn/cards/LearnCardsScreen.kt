package hu.kyokushin.trainer.feature.learn.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.core.ui.illustration.TechniqueIllustration
import hu.kyokushin.trainer.core.ui.status.VerificationBadge
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.learn.HideMode
import hu.kyokushin.trainer.feature.learn.R

/**
 * A tanulókártya képernyő route-szintű bekötése (T-304, T-305, T-307, FR-06…FR-09,
 * FUNCTIONAL_SPEC 4.2).
 *
 * Ez az egyetlen composable a `cards` csomagban, amely [LearnCardsViewModel]-t lát (L5 –
 * a feature modul nem navigál magától, lambdát ad kifelé).
 *
 * @param onExit a felső sáv „×" bezárás gombja **és** a záróképernyő „Vissza a
 *   beállításokhoz" gombja hívja – mindkettő ugyanoda vezet: ki a `learn/cards`
 *   képernyőből, vissza a `learn/setup`-ra. A tényleges navigációt a `navigation` modul
 *   dönti el (L5), itt csak esemény megy kifelé.
 * @param onStartQuiz a záróképernyő „Kvíz indítása ezekkel a kategóriákkal" gombja hívja,
 *   a munkamenet kiválasztott kategóriáival. A `quiz/setup` (T-403/T-404) még nem
 *   létezik, ezért a hívó oldalon (ma: a `navigation` modul) ennek a kategóriahalmaznak
 *   a tényleges átvétele is csak akkor köthető be, amikor az a képernyő elkészül – lásd
 *   a `docs/CONVENTIONS.md` 4.1 szakaszának hasonló, korábbi döntését (T-302/T-303).
 * @param modifier a szokásos Compose modifier.
 * @param viewModel a Hilt által adott ViewModel; teszt vagy előnézet nem ezt a
 *   konstruktort hívja, hanem közvetlenül a stateless [LearnCardsScreen]-t.
 */
@Composable
fun LearnCardsRoute(
    onExit: () -> Unit,
    onStartQuiz: (Set<CategoryId>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearnCardsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LearnCardsScreen(
        uiState = uiState,
        onReveal = viewModel::onReveal,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onRestart = viewModel::onRestart,
        onExit = onExit,
        onStartQuiz = onStartQuiz,
        modifier = modifier,
    )
}

/** A tanulókártya képernyő – stateless (FUNCTIONAL_SPEC 4.2). */
@Composable
fun LearnCardsScreen(
    uiState: LearnCardsUiState,
    onReveal: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onStartQuiz: (Set<CategoryId>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            LearnCardsUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            LearnCardsUiState.Empty -> LearnCardsEmptyContent(
                onExit = onExit,
                modifier = Modifier.align(Alignment.Center),
            )

            is LearnCardsUiState.Content -> LearnCardsContent(
                state = uiState,
                onReveal = onReveal,
                onNext = onNext,
                onPrevious = onPrevious,
                onExit = onExit,
            )

            is LearnCardsUiState.Finished -> LearnCardsFinishedContent(
                state = uiState,
                onRestart = onRestart,
                onBackToSetup = onExit,
                onStartQuiz = onStartQuiz,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/**
 * A védelmi háló ága: a kiválasztásban (már) nincs megjeleníthető technika
 * (`LearnCardsUiState.Empty` KDoc-ja, NFR-06). A `learn/setup` a gombot normál esetben
 * letiltva tartja, tehát ez a UI a gyakorlatban ritkán látott, de nem omlik össze.
 */
@Composable
private fun LearnCardsEmptyContent(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.learn_cards_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onExit, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
            Text(text = stringResource(R.string.learn_cards_empty_action))
        }
    }
}

/** A tényleges kártya tartalom: felső sáv, kártya, alsó Előző/Következő sáv. */
@Composable
private fun LearnCardsContent(
    state: LearnCardsUiState.Content,
    onReveal: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LearnCardsTopBar(state = state, onExit = onExit)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            LearnCardSurface(card = state.card, hideMode = state.hideMode, onReveal = onReveal)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = state.hasPrevious,
                modifier = Modifier.weight(1f).heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(R.string.learn_cards_previous))
            }
            // Szándékosan MINDIG engedélyezett, az utolsó kártyán is: az utolsó kártyán
            // a záróképernyőre visz (T-307, `LearnCardsViewModel` osztály KDoc-ja).
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(R.string.learn_cards_next))
            }
        }
    }
}

/**
 * Felső sáv: „`{aktuális}` / `{összes}`", kategória neve, bezárás (×)
 * (FUNCTIONAL_SPEC 4.2/1.).
 */
@Composable
private fun LearnCardsTopBar(
    state: LearnCardsUiState.Content,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(
                    R.string.learn_cards_position_of_total,
                    state.position + 1,
                    state.total,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.card.categoryName.isNotBlank()) {
                Text(
                    text = state.card.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onExit, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.learn_cards_a11y_close),
            )
        }
    }
}

/**
 * A kártya felülete: ábra, a nem elrejtett név, és a felfedésre váró felület
 * (FUNCTIONAL_SPEC 4.2/2–3., FR-07, FR-08).
 *
 * **Akadálymentesség.** A felfedés előtt az ábra [IllustrationLabel.Decorative] leírást
 * kap – **nem** a technika nevét –, mert a [TechniqueIllustration] leírása egyébként
 * kimondaná a rejtett nevet is, és ez ugyanúgy kiszivárogtatná a választ TalkBackkel,
 * mint egy véletlen felfedés (FR-07, NFR-04). Felfedés után a leírás a teljes nevet adja,
 * mert akkor már látványban is látszik mindkét név.
 *
 * A teljes felületen egyetlen `stateDescription` mondja ki, hogy a kártya „elrejtve"
 * vagy „felfedve" van (T-304 „Kész ha").
 */
@Composable
private fun LearnCardSurface(
    card: LearnCardUiModel,
    hideMode: HideMode,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val revealed = card.revealed
    val stateDescriptionText = LearnCardStateDescription.forRevealed(LocalContext.current.resources, revealed)
    val revealPrompt = stringResource(R.string.learn_cards_reveal_prompt)

    Surface(
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .clickable(
                enabled = !revealed,
                role = Role.Button,
                onClick = onReveal,
            )
            .semantics { stateDescription = stateDescriptionText },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VerificationBadge(status = card.status)

            LearnCardIllustration(card = card)

            if (revealed) {
                Text(
                    text = card.romaji,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = card.hungarian,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                val visibleLabel = when (hideMode) {
                    HideMode.HIDE_JAPANESE -> card.hungarian
                    HideMode.HIDE_HUNGARIAN -> card.romaji
                }
                Text(
                    text = visibleLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = revealPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * A kártya ábrarésze: a T-305 szabálya szerint egy feliratos kép felfedésig rejtve marad
 * (`IllustrationRef.Asset.hideableOnStudyCard == false`), és a helyén egy rövid
 * magyarázat jelenik meg, miért nem látszik még a kép.
 */
@Composable
private fun LearnCardIllustration(card: LearnCardUiModel) {
    val illustration = card.illustration

    if (LearnCardImagePolicy.shouldHideUntilRevealed(illustration, card.revealed)) {
        HiddenCaptionedImageNotice()
        return
    }

    // Lásd a `LearnCardSurface` KDoc-ja: felfedés előtt az ábra leírása dekoratív, hogy a
    // rejtett nevet ne mondja ki a TalkBack (FR-07, NFR-04).
    val label = LearnCardIllustrationLabelPolicy.labelFor(
        romaji = card.romaji,
        hungarian = card.hungarian,
        revealed = card.revealed,
    )

    TechniqueIllustration(
        illustration = illustration,
        label = label,
        awaitingReview = card.status != VerificationStatus.VERIFIED,
    )
}

/** T-305: feliratos kép helyőrzője, felfedésig. Egyetlen TalkBack bejárási egység. */
@Composable
private fun HiddenCaptionedImageNotice(modifier: Modifier = Modifier) {
    val explanation = stringResource(R.string.learn_cards_image_hidden_explanation)
    val title = stringResource(R.string.learn_cards_image_hidden_title)

    Surface(
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HIDDEN_IMAGE_ASPECT_RATIO)
            .clearAndSetSemantics { contentDescription = "$title. $explanation" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(text = explanation, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

/**
 * A záróképernyő: összegzés + három továbblépési lehetőség (T-307, FUNCTIONAL_SPEC 4.2:
 * „Az utolsó kártya után záróképernyő").
 */
@Composable
private fun LearnCardsFinishedContent(
    state: LearnCardsUiState.Finished,
    onRestart: () -> Unit,
    onBackToSetup: () -> Unit,
    onStartQuiz: (Set<CategoryId>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.learn_cards_finished_title, state.total),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Text(text = stringResource(R.string.learn_cards_finished_restart))
        }
        OutlinedButton(
            onClick = onBackToSetup,
            modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Text(text = stringResource(R.string.learn_cards_finished_back_to_setup))
        }
        // Kész ha (T-307): a kiválasztott kategóriák átadva a hívónak (a `quiz/setup`
        // felé – lásd `LearnCardsRoute` `onStartQuiz` dokumentációja).
        OutlinedButton(
            onClick = { onStartQuiz(state.categoryIds) },
            modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH_TARGET),
        ) {
            Text(text = stringResource(R.string.learn_cards_finished_start_quiz))
        }
    }
}

/** A kártya és a gombok minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

/** A kártya és a feliratos-kép-helyőrző sarokkerekítése. */
private val CARD_CORNER_RADIUS: Dp = 16.dp

/** A feliratos-kép-helyőrző képaránya – ugyanaz, mint a `core/ui` helyőrzőjéé. */
private const val HIDDEN_IMAGE_ASPECT_RATIO: Float = 4f / 5f

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában, a négy állapotra (T-304, T-305, T-307).
// ---------------------------------------------------------------------------------------

private val PREVIEW_CARD_HIDDEN = LearnCardUiModel(
    techniqueId = TechniqueId.of("KY-0001"),
    categoryName = "Állások",
    romaji = "Kiba Dachi",
    hungarian = "Lovagló állás",
    illustration = null,
    status = VerificationStatus.AWAITING_REVIEW,
    revealed = false,
)

private val PREVIEW_CARD_REVEALED = PREVIEW_CARD_HIDDEN.copy(revealed = true)

private val PREVIEW_CARD_CAPTIONED_IMAGE = PREVIEW_CARD_HIDDEN.copy(
    illustration = IllustrationRef.Asset(
        ref = "KY-0045",
        fileName = "KY-0045.jpg",
        quizUsable = false,
        hideableOnStudyCard = false,
    ),
)

@Preview(name = "Felfedés előtt – világos", showBackground = true)
@Preview(
    name = "Felfedés előtt – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnCardsScreenHiddenPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnCardsScreen(
            uiState = LearnCardsUiState.Content(
                card = PREVIEW_CARD_HIDDEN,
                position = 2,
                total = 12,
                hasPrevious = true,
                hasNext = true,
                hideMode = HideMode.HIDE_JAPANESE,
            ),
            onReveal = {},
            onNext = {},
            onPrevious = {},
            onRestart = {},
            onExit = {},
            onStartQuiz = {},
        )
    }
}

@Preview(name = "Felfedve – világos", showBackground = true)
@Preview(
    name = "Felfedve – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnCardsScreenRevealedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnCardsScreen(
            uiState = LearnCardsUiState.Content(
                card = PREVIEW_CARD_REVEALED,
                position = 2,
                total = 12,
                hasPrevious = true,
                hasNext = true,
                hideMode = HideMode.HIDE_JAPANESE,
            ),
            onReveal = {},
            onNext = {},
            onPrevious = {},
            onRestart = {},
            onExit = {},
            onStartQuiz = {},
        )
    }
}

/** T-305: feliratos kép, felfedés előtt – a kép helyén magyarázat látszik. */
@Preview(name = "Feliratos kép – rejtve – világos", showBackground = true)
@Preview(
    name = "Feliratos kép – rejtve – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnCardsScreenCaptionedImageHiddenPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnCardsScreen(
            uiState = LearnCardsUiState.Content(
                card = PREVIEW_CARD_CAPTIONED_IMAGE,
                position = 0,
                total = 12,
                hasPrevious = false,
                hasNext = true,
                hideMode = HideMode.HIDE_JAPANESE,
            ),
            onReveal = {},
            onNext = {},
            onPrevious = {},
            onRestart = {},
            onExit = {},
            onStartQuiz = {},
        )
    }
}

@Preview(name = "Üres kijelölés – világos", showBackground = true)
@Preview(
    name = "Üres kijelölés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnCardsScreenEmptyPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnCardsScreen(
            uiState = LearnCardsUiState.Empty,
            onReveal = {},
            onNext = {},
            onPrevious = {},
            onRestart = {},
            onExit = {},
            onStartQuiz = {},
        )
    }
}

@Preview(name = "Záróképernyő – világos", showBackground = true)
@Preview(
    name = "Záróképernyő – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LearnCardsScreenFinishedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnCardsScreen(
            uiState = LearnCardsUiState.Finished(
                total = 12,
                categoryIds = setOf(CategoryId.of("dachi")),
            ),
            onReveal = {},
            onNext = {},
            onPrevious = {},
            onRestart = {},
            onExit = {},
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
private fun LearnCardsScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LearnCardsScreen(
            uiState = LearnCardsUiState.Loading,
            onReveal = {},
            onNext = {},
            onPrevious = {},
            onRestart = {},
            onExit = {},
            onStartQuiz = {},
        )
    }
}
