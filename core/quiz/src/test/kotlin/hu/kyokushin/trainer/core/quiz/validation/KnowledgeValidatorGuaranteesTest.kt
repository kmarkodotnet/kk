package hu.kyokushin.trainer.core.quiz.validation

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Test

/**
 * A validátor **két szerződési garanciája**, a szabálytáblázattól külön (TR-01, FR-31, CR-02).
 *
 * Ezek nem egy-egy V-kódról szólnak, hanem a validátor egészéről: bármelyik V-kód
 * implementációja megváltozhat, ezek az állítások akkor is állnak. Ezért van külön
 * osztályuk – ha a szabálytáblázat egyik sorával együtt bukna el, nem derülne ki, hogy
 * nem a szabály, hanem a *szerződés* sérült.
 */
class KnowledgeValidatorGuaranteesTest {
    private val validator = DefaultKnowledgeValidator()

    // ----------------------------------------------------------------------------
    // 1. garancia: érvénytelen bemenet soha nem dob kivételt (FR-31)
    // ----------------------------------------------------------------------------

    @Test
    fun validate_everyMalformedInputShape_reportsInsteadOfThrowing_TR01() {
        malformedInputs().forEach { (shape, input) ->
            val outcome = runCatching { validator.validate(input) }

            assertThat(outcome.exceptionOrNull()).isNull()
            val report = outcome.getOrThrow()

            // A jelentés nem lehet „üres siker" sem: minden rekordról számot kell adni –
            // vagy elfogadva, vagy elvetve, az indoklásával együtt.
            if (report.datasetRejected) {
                assertThat(report.accepted).isEmpty()
            } else {
                assertThat(report.accepted.size + report.rejected.size).isEqualTo(input.entries.size)
            }
            report.rejected.forEach { rejected ->
                assertThat(
                    rejected.issues.any {
                        it.severity == Severity.BLOCKING && it.code.scope == IssueScope.RECORD
                    },
                )
                    .isTrue()
                // Ugyanaz az észrevétel a diagnosztikai listában is megjelenik.
                assertThat(report.issues).containsAtLeastElementsIn(rejected.issues)
                assertThat(rejected.index).isIn(input.entries.indices.toList())
            }
            // A szinonimacsoportok csak elfogadott rekordra hivatkozhatnak (CR-07).
            val acceptedIds = report.accepted.map { it.id }.toSet()
            report.synonymGroups.forEach { group ->
                assertThat(group.size).isAtLeast(2)
                assertThat(acceptedIds).containsAtLeastElementsIn(group)
            }
            assertThat(shape).isNotEmpty()
        }
    }

    /**
     * A romlás **alakjai**, nem a V-kódjai: üres tudástár, csupa `null` rekord, `null`
     * elemek a listákban, whitespace-mezők, önmagára hivatkozó duplikációk, olvashatatlan
     * verziószámok. A szabálytáblázat a *hatást* méri, ez a készlet a *túlélést*.
     */
    private fun malformedInputs(): List<Pair<String, KnowledgeInput>> = listOf(
        "üres manifest, üres rekordlista" to KnowledgeInput(ManifestInput(), emptyList()),
        "minden mező null" to KnowledgeInput(
            ManifestInput(
                schemaVersion = "1.0.0",
                contentVersion = "0.1.0",
                categories = listOf(CategoryInput(), CategoryInput(id = "   ")),
                illustrations = listOf(IllustrationInput(), IllustrationInput(ref = " ")),
            ),
            listOf(TechniqueInput(), TechniqueInput()),
        ),
        "csak whitespace mezők" to KnowledgeInput(
            manifest(),
            listOf(validEntry(id = " ", categoryId = " ", romaji = " ", hungarian = " ", aliases = listOf(" ", null))),
        ),
        "olvashatatlan sémaverzió" to KnowledgeInput(manifest(schemaVersion = "v1.0.0"), listOf(validEntry())),
        "hiányzó tartalomverzió" to KnowledgeInput(manifest(contentVersion = "  "), listOf(validEntry())),
        "duplikált id és ismeretlen kategória együtt" to KnowledgeInput(
            manifest(),
            listOf(validEntry(id = "KY-0001"), validEntry(id = "KY-0001", categoryId = "nincs-ilyen")),
        ),
        "lógó képhivatkozás, nyilvántartás nélkül" to KnowledgeInput(
            manifest(),
            listOf(validEntry(illustration = "  KY-9999  ", quizEligible = true)),
        ),
        "csak kombináló jelekből álló romaji" to KnowledgeInput(
            manifest(),
            listOf(validEntry(romaji = "́̄")),
        ),
    )

    // ----------------------------------------------------------------------------
    // 2. garancia: a validátor soha nem emel státuszt (CR-02, ADR-002, ADR-010)
    // ----------------------------------------------------------------------------

    @Test
    fun validate_contentWithoutVerifiedFlag_neverBecomesVerified_TR01_CR02() {
        // Minden olyan bemeneti alak, ami a tartalom szerint NEM hitelesített.
        val unverifiedFlags: List<Boolean?> = listOf(null, false)

        unverifiedFlags.forEach { verified ->
            val report = validator.validate(
                KnowledgeInput(
                    manifest(
                        // Még a legmegengedőbb manifest sem emelhet: `reviewed` tudástár…
                        datasetKind = "reviewed",
                        illustrations = listOf(
                            IllustrationInput(
                                ref = "KY-0044",
                                file = "KY-0044.jpg",
                                license = "project-internal",
                                quizUsable = true,
                                hideableOnStudyCard = true,
                            ),
                        ),
                    ),
                    // …hibátlan, képpel ellátott rekord, ami kvízjogosultságot is kér.
                    listOf(validEntry(illustration = "KY-0044", verified = verified, quizEligible = true)),
                    availableIllustrationFiles = setOf("KY-0044.jpg"),
                ),
            )

            assertThat(report.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
            assertThat(report.accepted.single().status).isEqualTo(VerificationStatus.AWAITING_REVIEW)
        }
    }

    @Test
    fun validate_incompleteRecord_isNeverRaisedBackToVerified_TR01_CR02() {
        // A tartalom hitelesnek jelöli, de a képhivatkozás lógó: a státusz csak LEFELÉ mozdul.
        val report = validator.validate(
            KnowledgeInput(
                manifest(datasetKind = "reviewed"),
                listOf(validEntry(illustration = "KY-9999", verified = true, quizEligible = true)),
            ),
        )

        val technique = report.accepted.single()
        assertThat(technique.status).isEqualTo(VerificationStatus.INCOMPLETE)
        assertThat(technique.illustration).isEqualTo(IllustrationRef.Placeholder)
        // A kvízképesség is lefokozva: hiányos rekord egyetlen kérdéstípusban sem jelölt.
        assertThat(QuizEligibility.eligibleTypes(technique, report.datasetTrust)).isEmpty()
    }

    @Test
    fun validate_unknownDatasetKind_neverRaisesTrust_TR01_CR02() {
        listOf(null, "", "   ", "demo", "ellenorizve", "REVIEWED_SOON").forEach { datasetKind ->
            val report = validator.validate(
                KnowledgeInput(manifest(datasetKind = datasetKind), listOf(validEntry())),
            )

            assertThat(report.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
        }
    }
}
