package hu.kyokushin.trainer.core.quiz.validation

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.Technique
import org.junit.Test

/**
 * A validátor **szerződésének** tesztjei (T-104): a bemeneti és kimeneti típusok, valamint
 * a „soha nem dob kivételt" garancia. A szabályonkénti (V001…V010) táblázatos tesztkészlet
 * a TR-01 teljes lefedésével a T-106 feladat tárgya.
 */
class KnowledgeValidatorContractTest {

    /**
     * A szerződés lelke (TR-01, FR-31): a hibás tartalomnak *reprezentálhatónak* kell
     * lennie. Ha a bemeneti típusok validálnának, a hiba már a jelentés elkészítése előtt
     * kivételt dobna.
     */
    @Test
    fun knowledgeInput_blankAndMissingMandatoryFields_areRepresentableWithoutException_TR01() {
        val input = KnowledgeInput(
            manifest = ManifestInput(schemaVersion = null, contentVersion = "   "),
            entries = listOf(
                TechniqueInput(id = "", categoryId = null, romaji = "   ", hungarian = null),
                TechniqueInput(id = "KY-0001", aliases = listOf("", null)),
            ),
        )

        assertThat(input.entries).hasSize(2)
        assertThat(input.entries[0].hungarian).isNull()
        assertThat(input.entries[1].aliases).containsExactly("", null)
        assertThat(input.availableIllustrationFiles).isEmpty()
    }

    /**
     * Ugyanaz a tartalom a domain modellben **kivételt** dob – ezért kell a validátornak a
     * hibás rekordot a [Technique] létrehozása *előtt* elvetnie (DR-01, FR-31).
     */
    @Test
    fun technique_blankMandatoryField_throws_soValidatorMustRejectBeforeMapping_FR31() {
        val thrown = runCatching {
            Technique(
                id = hu.kyokushin.trainer.core.model.TechniqueId.of("KY-0001"),
                categoryId = hu.kyokushin.trainer.core.model.CategoryId.of("uke"),
                romaji = "gedan barai",
                romajiAscii = "gedan barai",
                hungarian = "   ",
                datasetVersion = "0.1.0-demo",
            )
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
    }

    /** A szerződés nem dob kivételt: a hiba a [ValidationReport] része (TR-01). */
    @Test
    fun validator_invalidContent_reportsIssuesInsteadOfThrowing_TR01() {
        val validator = RejectEverythingValidator()
        val input = KnowledgeInput(
            manifest = ManifestInput(schemaVersion = 99),
            entries = listOf(TechniqueInput(id = "", romaji = null, hungarian = null)),
        )

        val report = runCatching { validator.validate(input) }

        assertThat(report.exceptionOrNull()).isNull()
        val value = report.getOrThrow()
        assertThat(value.accepted).isEmpty()
        assertThat(value.rejected).hasSize(1)
        assertThat(value.rejected.single().index).isEqualTo(0)
        assertThat(value.issuesOf(IssueCode.V002)).hasSize(1)
        assertThat(value.issuesOf(IssueCode.V001).single().severity).isEqualTo(Severity.BLOCKING)
        assertThat(value.datasetRejected).isTrue()
        assertThat(value.schemaVersion).isEqualTo(99)
    }

    /**
     * Minden szabálykód létezik és a szabálytáblázat szerinti alapsúlyosságot hordozza
     * (DR-05). A dokumentált kódkészlet elmozdulása a jelentés és a diagnosztika
     * hivatkozásait törné el.
     */
    @Test
    fun issueCodes_coverV001ToV010WithDocumentedSeverityAndScope_TR01() {
        assertThat(IssueCode.entries.map { it.name })
            .containsExactly("V001", "V002", "V003", "V004", "V005", "V006", "V007", "V008", "V009", "V010")
            .inOrder()

        assertThat(IssueCode.entries.filter { it.defaultSeverity == Severity.WARNING })
            .containsExactly(IssueCode.V005, IssueCode.V006, IssueCode.V009)

        assertThat(IssueCode.entries.filter { it.scope == IssueScope.DATASET })
            .containsExactly(IssueCode.V001, IssueCode.V010)
        assertThat(IssueCode.entries.filter { it.scope == IssueScope.RECORD })
            .containsExactly(IssueCode.V002, IssueCode.V003, IssueCode.V004)
    }

    /**
     * Üres jelentés: a bizalmi szint alapértéke a **szigorúbb** olvasat, mert kód soha nem
     * emel bizalmi szintet vagy hitelesítési státuszt (CR-02).
     */
    @Test
    fun validationReport_defaultTrustIsTheStricterReading_CR02() {
        val empty = ValidationReport()

        assertThat(empty.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
        assertThat(empty.datasetRejected).isFalse()
        assertThat(empty.accepted).isEmpty()
        assertThat(empty.synonymGroups).isEmpty()
        assertThat(empty.datasetVersion).isNull()
        assertThat(empty.schemaVersion).isNull()
    }

    /**
     * Teszt-dubla: azt bizonyítja, hogy a [KnowledgeValidator] szerződése kivétel nélkül
     * implementálható. A tényleges szabályok a T-105 feladat tárgyai.
     */
    private class RejectEverythingValidator : KnowledgeValidator {
        override fun validate(input: KnowledgeInput): ValidationReport {
            val schemaIssue = ValidationIssue(
                code = IssueCode.V001,
                severity = IssueCode.V001.defaultSeverity,
                detail = "Nem támogatott schemaVersion: ${input.manifest.schemaVersion}.",
            )
            val recordIssues = input.entries.mapIndexed { index, entry ->
                ValidationIssue(
                    code = IssueCode.V002,
                    severity = IssueCode.V002.defaultSeverity,
                    entryId = entry.id?.takeIf { it.isNotBlank() },
                    entryIndex = index,
                    field = "hungarian",
                    detail = "Kötelező mező hiányzik.",
                )
            }
            return ValidationReport(
                rejected = input.entries.mapIndexed { index, entry ->
                    RejectedRecord(index = index, entry = entry, issues = listOf(recordIssues[index]))
                },
                issues = listOf(schemaIssue) + recordIssues,
                schemaVersion = input.manifest.schemaVersion,
            )
        }
    }
}
