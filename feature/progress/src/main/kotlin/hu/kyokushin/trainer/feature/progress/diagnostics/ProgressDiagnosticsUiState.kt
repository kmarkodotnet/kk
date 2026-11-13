package hu.kyokushin.trainer.feature.progress.diagnostics

import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationIssue

/**
 * A fejlesztői diagnosztika képernyő (FUNCTIONAL_SPEC 6.3, FR-32, T-508) állapota.
 *
 * Ez a képernyő a [hu.kyokushin.trainer.core.data.content.ContentImporter.lastValidationReport]
 * **ebben a processzben** lefutott jelentését jeleníti meg – lásd a [Loading]/[NoReport]
 * ágak KDoc-ját arra, miért nem elegendő egyetlen „betöltve" ág.
 */
sealed interface ProgressDiagnosticsUiState {
    /** A [hu.kyokushin.trainer.core.data.content.ContentImporter.lastValidationReport] hívása még fut. */
    data object Loading : ProgressDiagnosticsUiState

    /**
     * Ebben a processzben **nem futott** validáció – tipikusan azért, mert a tartalom
     * verziója nem változott az app indulása óta
     * ([hu.kyokushin.trainer.core.data.content.ImportResult.UpToDate]), és az import
     * emiatt kihagyta a validátor futtatását (lásd `RoomContentImporter.importIfNeeded`
     * verziókapuja). Ez **nem hibaállapot**: a tartalom rendben lehet, csak ebben a
     * munkamenetben nincs részletes jelentés – a felhasználót (itt: a fejlesztőt) ennek
     * meg kell mondania, nem egy üres listát mutatnia.
     */
    data object NoReport : ProgressDiagnosticsUiState

    /**
     * @property severityFilter a jelenleg kiválasztott súlyosságszűrő – a [groups] már
     *   ezzel szűrve érkezik (lásd [ProgressDiagnosticsGrouping]).
     * @property datasetRejected igaz, ha a **teljes** tudástár elutasítva (V001/V010) –
     *   ilyenkor az [acceptedCount]/[rejectedCount] nem a jelenleg érvényben lévő
     *   tartalomra vonatkozik, hanem az elutasított próbálkozásra.
     * @property acceptedCount a jelentésben elfogadott rekordok száma.
     * @property rejectedCount a jelentésben elvetett rekordok száma.
     * @property groups a jelenlegi [severityFilter] szerint szűrt észrevételek,
     *   [hu.kyokushin.trainer.core.quiz.validation.IssueCode] szerint csoportosítva.
     */
    data class Content(
        val severityFilter: SeverityFilter,
        val datasetRejected: Boolean,
        val acceptedCount: Int,
        val rejectedCount: Int,
        val groups: List<ProgressDiagnosticsGroup>,
    ) : ProgressDiagnosticsUiState
}

/**
 * Egy [IssueCode] alá tartozó észrevételek (FR-32: „kódonkénti bontás").
 *
 * Az [issues] elemei **nem** feltétlenül azonos súlyosságúak: egy adott kód tényleges
 * súlyossága rekordonként eltérhet a kód alapértelmezésétől (lásd
 * [ValidationIssue.severity] KDoc-ja, pl. V005 lógó hivatkozásnál BLOKKOLÓ, hiányzó
 * fájlnál csak FIGYELMEZTETÉS) – ezért minden egyes észrevétel saját [Severity]-t hordoz,
 * nem a csoport egy közös mezője.
 */
data class ProgressDiagnosticsGroup(
    val code: IssueCode,
    val issues: List<ValidationIssue>,
)

/** A súlyosságszűrő három állása (FR-32 „Szűrés súlyosságra"). */
enum class SeverityFilter {
    ALL,
    BLOCKING,
    WARNING,
}
