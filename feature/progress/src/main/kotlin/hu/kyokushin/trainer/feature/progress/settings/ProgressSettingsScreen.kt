package hu.kyokushin.trainer.feature.progress.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import hu.kyokushin.trainer.feature.progress.BuildConfig
import hu.kyokushin.trainer.feature.progress.R

/**
 * A Beállítások (`settings`) route-szintű bekötése (FUNCTIONAL_SPEC 6.3, FR-32, FR-35,
 * NFR-09, T-507, T-508).
 *
 * Ugyanaz a minta, mint a többi `feature/progress` route-nál: ez az egyetlen composable,
 * amely [ProgressSettingsViewModel]-t lát (L5). Nincs kifelé adott navigációs lambda a
 * vissza-navigációhoz – ez a legmélyebb képernyő a Haladás szekcióban, az a `navigation`
 * modul dolga (a tényleges bekötés, ugyanúgy, mint a `quiz/setup`/`quiz/run` útvonalaknál,
 * nem ennek a feladatnak a tárgya, lásd `docs/CONVENTIONS.md` 5. szakasz).
 *
 * ### A „Tartalomdiagnosztika" menüpont debug kapuja (FR-32, T-508)
 *
 * Ugyanaz a mintázat, mint a `feature/library` „Diagnosztika" gombjánál (T-210): a
 * [BuildConfig.DEBUG] a `feature/progress` **saját** build config-ja (nem az `app`-é,
 * lásd `docs/CONVENTIONS.md` „Egy feature modul nem érheti el az `app` modul
 * `BuildConfig.VERSION_NAME`-jét" sora – a `DEBUG` mező ugyanígy modulszintű). Release
 * buildben `onOpenDiagnostics` `null`, és a [ProgressSettingsScreen] ekkor nem rajzol
 * gombot. A tényleges célképernyő ([hu.kyokushin.trainer.feature.progress.diagnostics.ProgressDiagnosticsScreen])
 * T-508-cal elkészült, de a navigációs bekötés (a `navigation` modulban) továbbra sem a
 * tárgya – addig a debug build csak a gomb **láthatóságát** garantálja, a kattintás no-op.
 */
@Composable
fun ProgressSettingsRoute(
    onOpenDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProgressSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProgressSettingsScreen(
        uiState = uiState,
        onClearProgressClicked = viewModel::onClearProgressClicked,
        onClearFirstWarningConfirmed = viewModel::onClearFirstWarningConfirmed,
        onClearCancelled = viewModel::onClearCancelled,
        onClearConfirmed = viewModel::onClearConfirmed,
        // A navigációs bekötés a `navigation` modul tárgya (lásd a fenti KDoc).
        onOpenDiagnostics = if (BuildConfig.DEBUG) onOpenDiagnostics else null,
        modifier = modifier,
    )
}

/**
 * A Beállítások képernyő – stateless (FUNCTIONAL_SPEC 6.3).
 *
 * @param onOpenDiagnostics a „Tartalomdiagnosztika" akció, vagy `null`, ha ezúttal nem
 *   jár menüpont (release build, FR-32, T-508).
 */
@Composable
fun ProgressSettingsScreen(
    uiState: ProgressSettingsUiState,
    onClearProgressClicked: () -> Unit,
    onClearFirstWarningConfirmed: () -> Unit,
    onClearCancelled: () -> Unit,
    onClearConfirmed: () -> Unit,
    onOpenDiagnostics: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            ProgressSettingsUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            is ProgressSettingsUiState.Content -> ProgressSettingsContent(
                state = uiState,
                onClearProgressClicked = onClearProgressClicked,
                onClearFirstWarningConfirmed = onClearFirstWarningConfirmed,
                onClearCancelled = onClearCancelled,
                onClearConfirmed = onClearConfirmed,
                onOpenDiagnostics = onOpenDiagnostics,
            )
        }
    }
}

@Composable
private fun ProgressSettingsContent(
    state: ProgressSettingsUiState.Content,
    onClearProgressClicked: () -> Unit,
    onClearFirstWarningConfirmed: () -> Unit,
    onClearCancelled: () -> Unit,
    onClearConfirmed: () -> Unit,
    onOpenDiagnostics: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ProgressSettingsContentInfoCard(state = state)
        if (onOpenDiagnostics != null) {
            ProgressSettingsDiagnosticsButton(onClick = onOpenDiagnostics)
        }
        ProgressSettingsVersionLicenseCard()
        ProgressSettingsClearSection(
            isClearing = state.isClearing,
            onClearProgressClicked = onClearProgressClicked,
        )
    }

    when (state.clearStep) {
        ClearProgressStep.NONE -> Unit

        ClearProgressStep.FIRST_WARNING -> ProgressSettingsClearDialog(
            titleRes = R.string.progress_settings_clear_first_title,
            messageRes = R.string.progress_settings_clear_first_message,
            confirmLabelRes = R.string.progress_settings_clear_first_confirm,
            onConfirm = onClearFirstWarningConfirmed,
            onDismiss = onClearCancelled,
        )

        ClearProgressStep.FINAL_WARNING -> ProgressSettingsClearDialog(
            titleRes = R.string.progress_settings_clear_final_title,
            messageRes = R.string.progress_settings_clear_final_message,
            confirmLabelRes = R.string.progress_settings_clear_final_confirm,
            onConfirm = onClearConfirmed,
            onDismiss = onClearCancelled,
            confirmEnabled = !state.isClearing,
        )
    }
}

/**
 * Tartalominfó-kártya (FUNCTIONAL_SPEC 6.3, FR-32, US-19 elfogadási kritérium): séma- és
 * tartalomverzió, szerkesztői állapot, rekord- és kategóriaszám.
 */
@Composable
private fun ProgressSettingsContentInfoCard(state: ProgressSettingsUiState.Content, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.progress_settings_content_info_title),
                style = MaterialTheme.typography.titleMedium,
            )
            val unknown = stringResource(R.string.progress_settings_version_unknown)
            Text(
                text = stringResource(R.string.progress_settings_schema_version, state.schemaVersion ?: unknown),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.progress_settings_content_version, state.contentVersion ?: unknown),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.progress_settings_editorial_status,
                    stringResource(state.datasetTrust.editorialStatusTextRes()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.progress_settings_record_count,
                    state.recordCount,
                    state.categoryCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** A `DatasetTrust` → szöveg leképezés (ADR-010) – lásd a `strings.xml` megjegyzése. */
private fun DatasetTrust.editorialStatusTextRes(): Int = when (this) {
    DatasetTrust.REVIEWED -> R.string.progress_settings_editorial_status_reviewed
    DatasetTrust.PRACTICE_ON_UNREVIEWED -> R.string.progress_settings_editorial_status_unreviewed
}

/**
 * Verzió- és forrás-/licencinformáció (FUNCTIONAL_SPEC 6.3 utolsó pontja). Statikus,
 * nem a tudástárból jövő szöveg – lásd a `strings.xml` megjegyzését: szó szerint a
 * `knowledgebase/README.md` dokumentált korlátozásaiból, nem kitalált állítás.
 */
@Composable
private fun ProgressSettingsVersionLicenseCard(modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.progress_settings_version_license_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.progress_settings_translation_notice),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.progress_settings_illustration_license_notice),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * A „Tartalomdiagnosztika" menüpont (FUNCTIONAL_SPEC 6.3, FR-32, T-508) – csak debug
 * buildben kap gombot, ezt a hívó ([ProgressSettingsContent]) dönti el az [onClick]
 * paraméter `null`/nem-`null` voltával (lásd a [ProgressSettingsRoute] KDoc-ja).
 *
 * Semleges (nem destruktív) szín, hogy vizuálisan megkülönböztethető legyen az alatta
 * álló „Előrehaladás törlése" gombtól – a tartalomdiagnosztika olvasó jellegű akció.
 */
@Composable
private fun ProgressSettingsDiagnosticsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET),
    ) {
        Text(text = stringResource(R.string.progress_settings_diagnostics_button))
    }
}

/** Az „Előrehaladás törlése" gomb – destruktív szín, a párbeszédeket a hívó (lásd fent) nyitja. */
@Composable
private fun ProgressSettingsClearSection(
    isClearing: Boolean,
    onClearProgressClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClearProgressClicked,
        enabled = !isClearing,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET),
    ) {
        Text(text = stringResource(R.string.progress_settings_clear_button))
    }
}

/**
 * Egy megerősítő párbeszéd lépése (FR-35) – ugyanazt az összetevőt használja a
 * [ClearProgressStep.FIRST_WARNING] és a [ClearProgressStep.FINAL_WARNING] ág.
 *
 * A gombok `heightIn(min = MIN_TOUCH_TARGET)`-et kapnak: a Material 3 `AlertDialog`
 * alapértelmezett `TextButton`-ja ~40 dp, nem a projekt 48 dp-es szabálya (lásd
 * `docs/CONVENTIONS.md` 4. szakasz, `QuizCloseConfirmationDialog`).
 */
@Composable
private fun ProgressSettingsClearDialog(
    titleRes: Int,
    messageRes: Int,
    confirmLabelRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleRes)) },
        text = { Text(text = stringResource(messageRes)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(confirmLabelRes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
            ) {
                Text(text = stringResource(R.string.progress_settings_clear_cancel))
            }
        },
    )
}

/** A gombok minimális érintési célpontja (FUNCTIONAL_SPEC 9., NFR-04). */
private val MIN_TOUCH_TARGET: Dp = 48.dp

// ---------------------------------------------------------------------------------------
// Előnézetek – világos és sötét témában; betöltés, importált/nem importált tartalom, és
// a két megerősítési lépés (T-507 „Kész ha").
// ---------------------------------------------------------------------------------------

private val PREVIEW_CONTENT = ProgressSettingsUiState.Content(
    schemaVersion = "1.0.0",
    contentVersion = "2026.11.0",
    datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    recordCount = 161,
    categoryCount = 8,
    clearStep = ClearProgressStep.NONE,
    isClearing = false,
)

private val PREVIEW_NOT_IMPORTED = ProgressSettingsUiState.Content(
    schemaVersion = null,
    contentVersion = null,
    datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    recordCount = 0,
    categoryCount = 0,
    clearStep = ClearProgressStep.NONE,
    isClearing = false,
)

@Preview(name = "Betöltés – világos", showBackground = true)
@Preview(
    name = "Betöltés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressSettingsScreenLoadingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressSettingsScreen(
            uiState = ProgressSettingsUiState.Loading,
            onClearProgressClicked = {},
            onClearFirstWarningConfirmed = {},
            onClearCancelled = {},
            onClearConfirmed = {},
        )
    }
}

@Preview(name = "Betöltve – világos", showBackground = true)
@Preview(
    name = "Betöltve – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressSettingsScreenContentPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressSettingsScreen(
            uiState = PREVIEW_CONTENT,
            onClearProgressClicked = {},
            onClearFirstWarningConfirmed = {},
            onClearCancelled = {},
            onClearConfirmed = {},
        )
    }
}

@Preview(name = "Nincs betöltött tartalom – világos", showBackground = true)
@Preview(
    name = "Nincs betöltött tartalom – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressSettingsScreenNotImportedPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressSettingsScreen(
            uiState = PREVIEW_NOT_IMPORTED,
            onClearProgressClicked = {},
            onClearFirstWarningConfirmed = {},
            onClearCancelled = {},
            onClearConfirmed = {},
        )
    }
}

@Preview(name = "Debug – Tartalomdiagnosztika menüpont – világos", showBackground = true)
@Preview(
    name = "Debug – Tartalomdiagnosztika menüpont – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressSettingsScreenDiagnosticsButtonPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressSettingsScreen(
            uiState = PREVIEW_CONTENT,
            onClearProgressClicked = {},
            onClearFirstWarningConfirmed = {},
            onClearCancelled = {},
            onClearConfirmed = {},
            onOpenDiagnostics = {},
        )
    }
}

@Preview(name = "Első megerősítés – világos", showBackground = true)
@Preview(
    name = "Első megerősítés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressSettingsScreenFirstWarningPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressSettingsScreen(
            uiState = PREVIEW_CONTENT.copy(clearStep = ClearProgressStep.FIRST_WARNING),
            onClearProgressClicked = {},
            onClearFirstWarningConfirmed = {},
            onClearCancelled = {},
            onClearConfirmed = {},
        )
    }
}

@Preview(name = "Végső megerősítés – világos", showBackground = true)
@Preview(
    name = "Végső megerősítés – sötét",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProgressSettingsScreenFinalWarningPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        ProgressSettingsScreen(
            uiState = PREVIEW_CONTENT.copy(clearStep = ClearProgressStep.FINAL_WARNING),
            onClearProgressClicked = {},
            onClearFirstWarningConfirmed = {},
            onClearCancelled = {},
            onClearConfirmed = {},
        )
    }
}
