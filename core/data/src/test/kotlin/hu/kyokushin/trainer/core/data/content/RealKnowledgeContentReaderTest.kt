package hu.kyokushin.trainer.core.data.content

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeInput
import hu.kyokushin.trainer.core.quiz.validation.Severity
import org.junit.Test

/**
 * A beolvasó a **valódi** tudástáron (FR-29, DR-05, ADR-011).
 *
 * Miért kell ez a szintetikus készlet mellé? Mert a szintetikus bemenetet a teszt írta: az
 * pontosan azt a sémát követi, amit a leképezés olvas. Ez a készlet a
 * `knowledgebase_transfer/` tényleges 161 rekordját olvassa be a `core/data` DTO-ival, és
 * **ugyanazokat** az elvárásokat állítja, mint a `core/quiz` oldali
 * `RealKnowledgeBaseValidationTest` a saját, teszt-oldali leképezésével.
 *
 * Ez a szinkron szándékos: két leképezés létezik ugyanarra a tartalomra (a validátor
 * tesztfixture-je és ez az importréteg), és ha eltérnének, a két modul már nem ugyanarról a
 * tudástárról beszélne. Az eltérés itt bukik el – a számok (161 rekord, 9 kategória, 10
 * illusztráció, 3 feliratos kép) ezért vannak kőbe zárva.
 */
class RealKnowledgeContentReaderTest {
    private val validator = DefaultKnowledgeValidator()

    @Test
    fun read_realKnowledgeBase_parsesTheCanonicalSchema_FR29() {
        val input = readRealContent()

        assertThat(input.manifest.schemaVersion).isEqualTo("1.0.0")
        assertThat(input.manifest.contentVersion).isEqualTo("0.1.0")
        assertThat(input.manifest.datasetKind).isEqualTo("draft_review_required")
        assertThat(input.manifest.categories).hasSize(EXPECTED_CATEGORY_COUNT)
        assertThat(input.manifest.categories.mapNotNull { it.sortOrder })
            .containsExactlyElementsIn(1..EXPECTED_CATEGORY_COUNT).inOrder()
        assertThat(input.manifest.illustrations).hasSize(EXPECTED_ILLUSTRATION_COUNT)
        assertThat(input.entries).hasSize(EXPECTED_ENTRY_COUNT)
        assertThat(input.entries.mapNotNull { it.id }.toSet()).hasSize(EXPECTED_ENTRY_COUNT)
    }

    @Test
    fun read_realKnowledgeBase_collectsThePackagedIllustrationFiles_FR29() {
        val input = readRealContent()

        // A validátor V005 szabálya a manifest `file` értékének az utolsó szegmensét keresi
        // ebben a halmazban, ezért itt csupasz fájlnév a szerződés, nem útvonal.
        assertThat(input.availableIllustrationFiles).hasSize(EXPECTED_ILLUSTRATION_COUNT)
        assertThat(input.availableIllustrationFiles)
            .containsExactlyElementsIn(input.manifest.illustrations.map { it.file?.substringAfterLast('/') })
    }

    @Test
    fun read_realKnowledgeBase_neverClaimsVerifiedContent_CR01() {
        val input = readRealContent()

        // A tartalomban egyetlen rekord sincs `review_status = reviewed` állapotban,
        // tehát a leképezés egyetlen rekordot sem jelölhet hitelesnek.
        assertThat(input.entries.map { it.verified }.toSet()).containsExactly(false)
        // A kvízjogosultság számított érték (ADR-010), nem a fájlból jön.
        assertThat(input.entries.map { it.quizEligible }.toSet()).containsExactly(null)
    }

    @Test
    fun read_realKnowledgeBase_passesTheValidatorWithoutBlockingIssue_DR05() {
        val report = validator.validate(readRealContent())

        // A hibalista a hibaüzenet része: ha elbukik, látni kell, melyik rekordon.
        assertThat(
            report.issues.filter { it.severity == Severity.BLOCKING }
                .map { "${it.code}/${it.entryId ?: "-"}/${it.field ?: "-"}" },
        ).isEmpty()
        assertThat(report.datasetRejected).isFalse()
        assertThat(report.rejected).isEmpty()
        assertThat(report.accepted).hasSize(EXPECTED_ENTRY_COUNT)
        assertThat(report.schemaVersion).isEqualTo("1.0.0")
        assertThat(report.datasetVersion).isEqualTo("0.1.0")
        assertThat(report.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
        assertThat(report.accepted.map { it.status }.toSet())
            .containsExactly(VerificationStatus.AWAITING_REVIEW)
        // A `V010` ebben az ágban nem keletkezhet: a fájlok értelmezhetők voltak.
        assertThat(report.issuesOf(IssueCode.V010)).isEmpty()
    }

    @Test
    fun read_realKnowledgeBase_resolvesEveryIllustrationToAnAsset_DR05() {
        val report = validator.validate(readRealContent())

        val assets = report.accepted.mapNotNull { it.illustration as? IllustrationRef.Asset }
        assertThat(assets).hasSize(EXPECTED_ILLUSTRATION_COUNT)
        assertThat(report.accepted.none { it.illustration == IllustrationRef.Placeholder }).isTrue()
        // A három feliratos kép kvízben nem használható (ADR-011) – a `quiz_usable` kapu
        // leképezése dönti el, nem a `review_status`.
        assertThat(assets.filterNot { it.quizUsable }.map { it.ref })
            .containsExactly("KY-0045", "KY-0050", "KY-0052")
    }

    @Test
    fun read_realKnowledgeBase_findsNoSynonymCollision_DR05() {
        val report = validator.validate(readRealContent())

        // A konverter ugyanerre jutott: a manifest `synonym_groups` listája is üres.
        assertThat(report.issuesOf(IssueCode.V006)).isEmpty()
        assertThat(report.synonymGroups).isEmpty()
    }

    private fun readRealContent(): KnowledgeInput {
        val result = KnowledgeContentReader(RealContentAssetSource()).read()
        // Ha a valódi tartalom nem parse-olható, az V010 lenne – és éppen ezt zárja ki.
        assertThat(result).isInstanceOf(KnowledgeContentReadResult.Parsed::class.java)
        return (result as KnowledgeContentReadResult.Parsed).input
    }

    private companion object {
        /** `knowledgebase_transfer/README.md`: 161 rekord, 9 kategória, 10 illusztráció. */
        const val EXPECTED_ENTRY_COUNT = 161
        const val EXPECTED_CATEGORY_COUNT = 9
        const val EXPECTED_ILLUSTRATION_COUNT = 10
    }
}
