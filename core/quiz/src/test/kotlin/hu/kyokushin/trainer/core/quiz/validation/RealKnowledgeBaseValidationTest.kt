package hu.kyokushin.trainer.core.quiz.validation

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Test

/**
 * A validátor a **valódi tartalmon** (TR-01, DR-05, FR-31).
 *
 * Miért kell ez a teszt a szabálytáblázat mellé? Mert a táblázat szintetikus: minden
 * bemenetét a teszt írta, és pontosan azt a hibát tartalmazza, amit a teszt keres. Egy
 * ilyen készlet zöld tud lenni akkor is, ha a validátor a tényleges tartalom egyetlen
 * alakját sem tudja feldolgozni – például mert a kanonikus séma `snake_case` mezőneveit
 * félreolvassuk, vagy mert a 161 rekord között olyan név van, amin a normalizálás elhasal.
 * Ez a teszt a `knowledgebase_transfer/` tényleges 161 rekordját futtatja át, és **0
 * BLOKKOLÓ hibát** követel meg.
 *
 * ## Amit ez a teszt nem helyettesít
 *
 * A JSON → [KnowledgeInput] leképezés itt **teszt-oldali**, és szándékosan minimális: a
 * tartalomimport igazi helye a `core/data` (T-115), ez a leképezés csak a
 * validátor bemenetét állítja elő a kanonikus séma szerint (ADR-011). Ha T-115 elkészül,
 * ez a teszt marad a validátor-oldali garancia, és a két leképezés eltérése maga is hiba.
 *
 * A `V010` (olvashatatlan JSON) itt sem keletkezhet: azt a kódot az importréteg állítja
 * elő, nem a validátor – ha a `Json.parseToJsonElement` elbukna, ez a teszt a parse-nál
 * dőlne el, és éppen ezt jelzi.
 */
class RealKnowledgeBaseValidationTest {
    private val validator = DefaultKnowledgeValidator()

    @Test
    fun validate_realKnowledgeBase_hasNoBlockingIssue_TR01() {
        val report = validator.validate(loadRealKnowledgeBase())

        val blocking = report.issues.filter { it.severity == Severity.BLOCKING }
        // A hibalista a hibaüzenet része: ha elbukik, látni kell, melyik rekordon.
        assertThat(blocking.map { "${it.code}/${it.entryId ?: "-"}/${it.field ?: "-"}" }).isEmpty()
        assertThat(report.datasetRejected).isFalse()
        assertThat(report.rejected).isEmpty()
    }

    @Test
    fun validate_realKnowledgeBase_acceptsEveryRecord_TR01() {
        val input = loadRealKnowledgeBase()

        val report = validator.validate(input)

        // A pontos szám szándékosan rögzített: ha a tartalom változik, ez a teszt
        // kikényszeríti, hogy a változást valaki tudomásul vegye (FR-32).
        assertThat(input.entries).hasSize(EXPECTED_ENTRY_COUNT)
        assertThat(report.accepted).hasSize(EXPECTED_ENTRY_COUNT)
        assertThat(report.schemaVersion).isEqualTo("1.0.0")
        assertThat(report.datasetVersion).isEqualTo("0.1.0")
        // `editorial_status = draft_review_required` → a szigorúbb olvasat (CR-02, ADR-010).
        assertThat(report.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
        // Egyetlen rekord sincs `review_status = reviewed`-en, tehát egyik sem hiteles.
        assertThat(report.accepted.map { it.status }.toSet())
            .containsExactly(VerificationStatus.AWAITING_REVIEW)
        assertThat(report.accepted.map { it.id.value }.toSet()).hasSize(EXPECTED_ENTRY_COUNT)
    }

    @Test
    fun validate_realKnowledgeBase_resolvesEveryIllustrationToAnAsset_TR01() {
        val report = validator.validate(loadRealKnowledgeBase())

        val assets = report.accepted.mapNotNull { it.illustration as? IllustrationRef.Asset }
        assertThat(assets).hasSize(EXPECTED_ILLUSTRATION_COUNT)
        assertThat(report.accepted.none { it.illustration == IllustrationRef.Placeholder }).isTrue()
        // A három feliratos kép (KY-0045, KY-0050, KY-0052) kvízben nem használható (ADR-011).
        assertThat(assets.filterNot { it.quizUsable }.map { it.ref })
            .containsExactly("KY-0045", "KY-0050", "KY-0052")
    }

    @Test
    fun validate_realKnowledgeBase_findsNoSynonymCollision_TR01_CR07() {
        val report = validator.validate(loadRealKnowledgeBase())

        // A konverter ugyanerre jutott: a manifest `synonym_groups` listája is üres.
        assertThat(report.issuesOf(IssueCode.V006)).isEmpty()
        assertThat(report.synonymGroups).isEmpty()
    }

    // A kanonikus séma beolvasása a `RealKnowledgeBase` közös fixture-ben él (ADR-011):
    // a TR-03 jelöltszűrés tesztje ugyanazt a leképezést használja, hogy a két készlet
    // ne tudjon eltérő tartalomról beszélni.
    private fun loadRealKnowledgeBase(): KnowledgeInput = RealKnowledgeBase.load()

    private companion object {
        const val EXPECTED_ENTRY_COUNT = RealKnowledgeBase.EXPECTED_ENTRY_COUNT
        const val EXPECTED_ILLUSTRATION_COUNT = RealKnowledgeBase.EXPECTED_ILLUSTRATION_COUNT
    }
}
