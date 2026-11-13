package hu.kyokushin.trainer.core.quiz.validation

import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId

/**
 * A tudástár-validáció teljes, strukturált eredménye (DR-05, FR-31).
 *
 * Ez a validátor **egyetlen** kimenete: nincs kivétel, nincs napló-mellékhatás, nincs
 * részben feldolgozott állapot. Érvénytelen tartalom következménye mindig egy ilyen
 * jelentés, amelyből az importréteg tovább tud dolgozni, a fejlesztői diagnosztika pedig
 * meg tudja jeleníteni (FR-45).
 *
 * @property accepted a domainbe átemelt technikák. Csak olyan rekord kerül ide, amely
 *   minden rekordszintű BLOKKOLÓ szabályon átjutott – a [Technique] konstruktora maga is
 *   `require()`-rel validál, ezért a validátor a domain objektumot *csak* az elfogadás
 *   után állítja elő.
 * @property rejected az elvetett rekordok, nyers alakban és az elvetés okaival.
 * @property issues **minden** észrevétel, súlyosságtól és hatókörtől függetlenül,
 *   beleértve a [rejected] rekordokhoz tartozókat is. Ez a lista a diagnosztika forrása.
 * @property synonymGroups a kölcsönös distraktor-kizárási csoportok (V006). A validátor
 *   kimenete és a kvízgenerátor bemenete: ez a csatolási pont kényszeríti ki a CR-07-et
 *   (azonos jelentésű kifejezés nem lehet egymás distraktora). Minden csoport legalább két
 *   [TechniqueId]-t tartalmaz, és csak [accepted] rekordokra hivatkozik.
 * @property datasetTrust a betöltött tudástár bizalmi szintje a manifest `datasetKind`
 *   értékéből. Kódból soha nem emelkedik (CR-02): ismeretlen, hiányzó vagy értelmezhetetlen
 *   érték esetén a szigorúbb [DatasetTrust.PRACTICE_ON_UNREVIEWED] a helyes olvasat.
 * @property datasetVersion a manifest `contentVersion` értéke, vagy `null`, ha hiányzik.
 *   A tartalomfrissítés idempotenciája (FR-32) ezen az értéken dől el.
 * @property schemaVersion a manifestben talált `schemaVersion` semver string, vagy `null`,
 *   ha hiányzott. A V001 szabály elutasításakor is kitöltött, ha olvasható volt – a
 *   diagnosztikának meg kell tudnia mondani, *melyik* nem támogatott verziót kaptuk.
 */
data class ValidationReport(
    val accepted: List<Technique> = emptyList(),
    val rejected: List<RejectedRecord> = emptyList(),
    val issues: List<ValidationIssue> = emptyList(),
    val synonymGroups: List<Set<TechniqueId>> = emptyList(),
    val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    val datasetVersion: String? = null,
    val schemaVersion: String? = null,
) {
    /**
     * A teljes tudástár elutasítva – tipikusan nem támogatott `schemaVersion` (V001) vagy
     * értelmezhetetlen JSON (V010) miatt.
     *
     * Ha `true`, az [accepted] lista tartalmára nem szabad támaszkodni: az app a korábban
     * betöltött tartalommal működik tovább, és diagnosztikát ad – nem omlik össze (FR-31).
     */
    val datasetRejected: Boolean
        get() = issues.any { it.severity == Severity.BLOCKING && it.code.scope == IssueScope.DATASET }

    /** A [code] kódhoz tartozó észrevételek – elsősorban a TR-01 táblázatos tesztekhez. */
    fun issuesOf(code: IssueCode): List<ValidationIssue> = issues.filter { it.code == code }
}

/**
 * Egy elvetett technikarekord a jelentésben.
 *
 * A rekord **nyers** alakját tartjuk meg, nem domain objektumot: éppen azért vetettük el,
 * mert domain objektumot nem lehetett volna belőle építeni (a [Technique] konstruktora
 * kivételt dobna a hiányzó kötelező mezőre).
 *
 * @property index a rekord nulla alapú indexe a [KnowledgeInput.entries] listában. Ez az
 *   egyetlen mindig létező azonosítója: az `id` maga is lehet hiányzó (V002).
 * @property entry az elvetett rekord, ahogy a tartalomból érkezett.
 * @property issues az elvetés okai; legalább egy BLOKKOLÓ, [IssueScope.RECORD] hatókörű
 *   észrevétel. Ugyanezek az elemek a [ValidationReport.issues] listában is szerepelnek.
 */
data class RejectedRecord(
    val index: Int,
    val entry: TechniqueInput,
    val issues: List<ValidationIssue> = emptyList(),
)
