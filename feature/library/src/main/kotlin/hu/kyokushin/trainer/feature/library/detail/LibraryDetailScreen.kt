package hu.kyokushin.trainer.feature.library.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import hu.kyokushin.trainer.core.ui.state.ErrorState
import hu.kyokushin.trainer.core.ui.status.TechniqueField
import hu.kyokushin.trainer.core.ui.status.VerificationBadge
import hu.kyokushin.trainer.core.ui.status.UnreviewedBanner
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.library.R

/**
 * A Tudástár részletező képernyő route-szintű bekötése (FR-04, FR-05, FUNCTIONAL_SPEC 3.2).
 *
 * Ez az egyetlen composable a `detail` csomagban, amely [LibraryDetailViewModel]-t lát
 * (L5 – a feature modul nem navigál magától, lambdát ad kifelé).
 *
 * @param onOpenCategory a kategóriachipre koppintva (US-07/Kész ha 2. pont): a `navigation`
 *   modul ebből navigál vissza a listára, arra a kategóriára szűrve. A vissza nyíl és a
 *   felső sáv a megosztott `KyokushinNavHost` Scaffoldjából jön, ezért itt nincs saját
 *   `onBack` paraméter.
 * @param modifier a szokásos Compose modifier.
 * @param viewModel a Hilt által adott ViewModel; teszt vagy előnézet nem ezt a konstruktort
 *   hívja, hanem közvetlenül a stateless [LibraryDetailScreen]-t.
 */
@Composable
fun LibraryDetailRoute(
    onOpenCategory: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryDetailScreen(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onOpenCategory = onOpenCategory,
        modifier = modifier,
    )
}

/**
 * A Tudástár részletező képernyő – stateless (FUNCTIONAL_SPEC 3.2, 2.4, 10.).
 *
 * A lektorálatlanság-szalag mindhárom ágon látszik (ugyanaz a döntés, mint a listánál) –
 * csak a tartalom helye vált `Loading`/`Content`/`Error` között.
 */
@Composable
fun LibraryDetailScreen(
    uiState: LibraryDetailUiState,
    onRetry: () -> Unit,
    onOpenCategory: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Nem elutasítható, nem a UI állapotától függő sáv (ADR-010) – minden ágon látszik.
        UnreviewedBanner(trust = uiState.datasetTrust)

        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is LibraryDetailUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                is LibraryDetailUiState.Error -> ErrorState(
                    onRetry = onRetry,
                    messageRes = uiState.messageRes,
                    modifier = Modifier.align(Alignment.Center),
                )

                is LibraryDetailUiState.Content -> LibraryDetailContent(
                    content = uiState,
                    onOpenCategory = onOpenCategory,
                )
            }
        }
    }
}

/**
 * A betöltött technika adatlapja (FUNCTIONAL_SPEC 3.2 1–10. pont).
 *
 * Minden opcionális mező (japán íráskép, aliasok, leírás) csak akkor rajzol szakaszt, ha
 * van tartalma – a halmozott „nincs adat" felirat zajos lenne (3.2 „Figyelj rá").
 */
@Composable
private fun LibraryDetailContent(
    content: LibraryDetailUiState.Content,
    onOpenCategory: (CategoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // US-07 2. kritérium: a hiányzó képre is helyőrző jár a részletezőn – a `null`
        // itt Placeholder-ré válik, szemben a listával, ahol a hely egyszerűen üres
        // marad (T-204, T-208 döntése; lásd `TechniqueIllustration` dokumentációja).
        TechniqueIllustration(
            illustration = content.illustration ?: IllustrationRef.Placeholder,
            label = IllustrationLabel.Technique(romaji = content.romaji, hungarian = content.hungarian),
            awaitingReview = content.status != VerificationStatus.VERIFIED,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = content.romaji, style = MaterialTheme.typography.headlineMedium)
        Text(text = content.hungarian, style = MaterialTheme.typography.titleLarge)

        if (!content.japaneseScript.isNullOrBlank()) {
            Text(text = content.japaneseScript, style = MaterialTheme.typography.bodyMedium)
        }

        if (content.aliases.isNotEmpty()) {
            Text(
                text = stringResource(R.string.library_detail_aliases, content.aliases.joinToString(", ")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // US-07/Kész ha 2. pont: koppintásra vissza a listára, arra a kategóriára szűrve.
        AssistChip(
            onClick = { onOpenCategory(content.categoryId) },
            label = { Text(text = content.categoryNameHu) },
            modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
        )

        if (!content.descriptionHu.isNullOrBlank()) {
            Text(text = content.descriptionHu, style = MaterialTheme.typography.bodyLarge)
        }

        TechniqueStatusSection(status = content.status, missingFields = content.missingFields)
    }
}

/**
 * Hitelesítési blokk (FUNCTIONAL_SPEC 3.2 10. pont, US-08 3. kritérium).
 *
 * [VerificationStatus.VERIFIED] esetén a [VerificationBadge] nem bocsát ki UI-t – a
 * FUNCTIONAL_SPEC szerint ilyenkor a `source` is megjelenne, de a kanonikus sémában
 * (ADR-011) nincs technikaszintű `source` mező, csak az illusztrációknak van saját
 * forrása; ez dokumentált eltérés (`docs/CONVENTIONS.md` 8.).
 *
 * [VerificationStatus.INCOMPLETE] esetén a hiányzó mezők felsorolása a badge-ből jön,
 * a kvízkizárás ténye pedig ennek a szakasznak a saját, önálló szövege – ez a kritérium
 * által megkövetelt HARMADIK elem.
 */
@Composable
private fun TechniqueStatusSection(
    status: VerificationStatus,
    missingFields: List<TechniqueField>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VerificationBadge(status = status, missingFields = missingFields)
        if (status == VerificationStatus.INCOMPLETE) {
            Text(
                text = stringResource(R.string.library_detail_quiz_excluded_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** A kategóriachip minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – mind a három állapot, világos és sötét témában (globális DoD 6. pont).
// ---------------------------------------------------------------------------------------

private val PREVIEW_CONTENT_VERIFIED = LibraryDetailUiState.Content(
    id = TechniqueId.of("KY-0044"),
    romaji = "Kiba Dachi",
    hungarian = "Lovagló állás",
    japaneseScript = "騎馬立ち",
    aliases = listOf("shiko dachi"),
    descriptionHu = "Széles, alacsony állás, a csípő stabilan előre néz.",
    illustration = IllustrationRef.Asset(
        ref = "KY-0044",
        fileName = "KY-0044.jpg",
        quizUsable = true,
        hideableOnStudyCard = true,
    ),
    categoryId = CategoryId.of("dachi"),
    categoryNameHu = "Állások",
    status = VerificationStatus.VERIFIED,
    missingFields = emptyList(),
    datasetTrust = DatasetTrust.REVIEWED,
)

private val PREVIEW_CONTENT_INCOMPLETE = LibraryDetailUiState.Content(
    id = TechniqueId.of("KY-0099"),
    romaji = "Gedan Barai",
    hungarian = "Lefelé hárítás",
    japaneseScript = null,
    aliases = emptyList(),
    descriptionHu = null,
    illustration = null,
    categoryId = CategoryId.of("uke"),
    categoryNameHu = "Hárítások",
    status = VerificationStatus.INCOMPLETE,
    missingFields = listOf(TechniqueField.ILLUSTRATION),
    datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
)

@Preview(name = "Részletező – hitelesített – világos", showBackground = true)
@Preview(
    name = "Részletező – hitelesített – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryDetailScreenVerifiedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryDetailScreen(
            uiState = PREVIEW_CONTENT_VERIFIED,
            onRetry = {},
            onOpenCategory = {},
        )
    }
}

@Preview(name = "Részletező – hiányos – világos", showBackground = true)
@Preview(
    name = "Részletező – hiányos – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryDetailScreenIncompletePreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryDetailScreen(
            uiState = PREVIEW_CONTENT_INCOMPLETE,
            onRetry = {},
            onOpenCategory = {},
        )
    }
}

@Preview(name = "Részletező – betöltés – világos", showBackground = true)
@Preview(
    name = "Részletező – betöltés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryDetailScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryDetailScreen(
            uiState = LibraryDetailUiState.Loading(),
            onRetry = {},
            onOpenCategory = {},
        )
    }
}

@Preview(name = "Részletező – hiba – világos", showBackground = true)
@Preview(
    name = "Részletező – hiba – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LibraryDetailScreenErrorPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        LibraryDetailScreen(
            uiState = LibraryDetailUiState.Error(
                messageRes = hu.kyokushin.trainer.core.ui.R.string.error_load_failed,
            ),
            onRetry = {},
            onOpenCategory = {},
        )
    }
}
